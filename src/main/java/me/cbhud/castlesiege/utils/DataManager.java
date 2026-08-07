package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

public class DataManager {
    private final CastleSiege plugin;
    private HikariDataSource dataSource;

    public DataManager(CastleSiege plugin) {
        this.plugin = plugin;
        connect();
    }

    public void connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/data");
            config.setDriverClassName("org.h2.Driver");
            config.setMaximumPoolSize(10);
            config.setPoolName("CastleSiegePool");

            dataSource = new HikariDataSource(config);
            createTables();
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            String createPlayerStatsSql = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid TEXT PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                coins INTEGER NOT NULL DEFAULT 0,
                kills INTEGER NOT NULL DEFAULT 0,
                deaths INTEGER NOT NULL DEFAULT 0,
                wins INTEGER NOT NULL DEFAULT 0
            );""";
            String createPlayerSpecialItemsSql = """
            CREATE TABLE IF NOT EXISTS player_special_items (
                player_uuid TEXT NOT NULL,
                item_id VARCHAR(64) NOT NULL,
                PRIMARY KEY (player_uuid, item_id)
            );""";
            String createKitsSql = """
            CREATE TABLE IF NOT EXISTS kits (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(32) NOT NULL UNIQUE,
                price INTEGER NOT NULL
            );""";
            String createPlayerKitsSql = """
            CREATE TABLE IF NOT EXISTS player_kits (
                player_uuid TEXT,
                kit_id INTEGER,
                PRIMARY KEY (player_uuid, kit_id),
                FOREIGN KEY (player_uuid) REFERENCES player_stats(uuid) ON DELETE CASCADE,
                FOREIGN KEY (kit_id) REFERENCES kits(id) ON DELETE CASCADE
            );""";

            statement.execute(createPlayerStatsSql);
            statement.execute(createPlayerSpecialItemsSql);
            statement.execute(createKitsSql);
            statement.execute(createPlayerKitsSql);
        } catch (SQLException e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().severe("Error creating tables: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    public CompletableFuture<Boolean> hasPlayerKit(UUID playerUUID, String kitName) {
        return supplyDatabaseAsync(() -> {
            String sql = """
            SELECT 1
            FROM player_kits pk
            JOIN kits k ON pk.kit_id = k.id
            WHERE pk.player_uuid = ? AND k.name = ?
            LIMIT 1
            """;

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, kitName);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking if player owns kit: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> unlockPlayerKit(UUID uuid, String kitName, int kitPrice) {
        return supplyDatabaseAsync(() -> {
            String checkCoinsSql = "SELECT coins FROM player_stats WHERE uuid = ?";
            String updateCoinsSql = "UPDATE player_stats SET coins = coins - ? WHERE uuid = ?";
            String unlockKitSql = """
            MERGE INTO player_kits (player_uuid, kit_id)
            KEY (player_uuid, kit_id)
            SELECT ?, id FROM kits WHERE name = ?
            """;

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement checkCoinsStmt = conn.prepareStatement(checkCoinsSql)) {
                    checkCoinsStmt.setString(1, uuid.toString());
                    try (ResultSet rs = checkCoinsStmt.executeQuery()) {
                        if (rs.next()) {
                            int coins = rs.getInt("coins");
                            if (coins < kitPrice) {
                                conn.rollback();
                                return false;
                            }
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement updateCoinsStmt = conn.prepareStatement(updateCoinsSql)) {
                    updateCoinsStmt.setInt(1, kitPrice);
                    updateCoinsStmt.setString(2, uuid.toString());
                    updateCoinsStmt.executeUpdate();
                }

                try (PreparedStatement unlockKitStmt = conn.prepareStatement(unlockKitSql)) {
                    unlockKitStmt.setString(1, uuid.toString());
                    unlockKitStmt.setString(2, kitName);
                    unlockKitStmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to unlock kit: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public boolean hasData(UUID uuid) {
        String sql = "SELECT 1 FROM player_stats WHERE uuid = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void createProfile(UUID playerUUID, String username) {
        String insertProfileSql = """
        INSERT INTO player_stats (uuid, username, coins, kills, deaths, wins)
        VALUES (?, ?, 0, 0, 0, 0)
        """;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!hasData(playerUUID)) {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement statement = conn.prepareStatement(insertProfileSql)) {
                    statement.setString(1, playerUUID.toString());
                    statement.setString(2, username);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void incrementWins(UUID uuid, int coinsToAdd) {
        String updateWinsSql = "UPDATE player_stats SET wins = wins + 1 WHERE uuid = ?";
        String updateCoinsSql = "UPDATE player_stats SET coins = coins + ? WHERE uuid = ?";

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = dataSource.getConnection()) {
                    try (PreparedStatement statement = conn.prepareStatement(updateWinsSql)) {
                        statement.setString(1, uuid.toString());
                        statement.executeUpdate();
                    }

                    try (PreparedStatement coinsStatement = conn.prepareStatement(updateCoinsSql)) {
                        coinsStatement.setInt(1, coinsToAdd);
                        coinsStatement.setString(2, uuid.toString());
                        coinsStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void incrementKills(UUID uuid, int coinsToAdd) {
        String updateKillsSql = "UPDATE player_stats SET kills = kills + 1 WHERE uuid = ?";
        String updateCoinsSql = "UPDATE player_stats SET coins = coins + ? WHERE uuid = ?";

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = dataSource.getConnection()) {
                    try (PreparedStatement statement = conn.prepareStatement(updateKillsSql)) {
                        statement.setString(1, uuid.toString());
                        statement.executeUpdate();
                    }

                    try (PreparedStatement coinsStatement = conn.prepareStatement(updateCoinsSql)) {
                        coinsStatement.setInt(1, coinsToAdd);
                        coinsStatement.setString(2, uuid.toString());
                        coinsStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void incrementDeaths(UUID uuid) {
        String updateDeathsSql = "UPDATE player_stats SET deaths = deaths + 1 WHERE uuid = ?";

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement statement = conn.prepareStatement(updateDeathsSql)) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public int getPlayerKills(UUID uuid) {
        String getKillsSql = "SELECT kills FROM player_stats WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(getKillsSql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("kills");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPlayerWins(UUID uuid) {
        String getKillsSql = "SELECT wins FROM player_stats WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(getKillsSql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("wins");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPlayerDeaths(UUID uuid) {
        String getKillsSql = "SELECT deaths FROM player_stats WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(getKillsSql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("deaths");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPlayerCoins(UUID uuid) {
        String getCoinsSql = "SELECT coins FROM player_stats WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(getCoinsSql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("coins");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void removePlayerCoins(UUID uuid, int coinsToRemove, Consumer<Boolean> callback) {
        String updateCoinsSql = "UPDATE player_stats SET coins = coins - ? WHERE uuid = ? AND coins >= ?";

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = false;
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement statement = conn.prepareStatement(updateCoinsSql)) {
                    statement.setInt(1, coinsToRemove);
                    statement.setString(2, uuid.toString());
                    statement.setInt(3, coinsToRemove);

                    int rowsUpdated = statement.executeUpdate();
                    success = rowsUpdated > 0;
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                boolean finalSuccess = success;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
            }
        }.runTaskAsynchronously(plugin);
    }

    public void addPlayerCoins(UUID uuid, int amount) {
        String addCoinsSql = "UPDATE player_stats SET coins = coins + ? WHERE uuid = ?";

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement statement = conn.prepareStatement(addCoinsSql)) {
                    statement.setInt(1, amount);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public Set<String> getPlayerSpecialItems(UUID uuid) {
        String sql = "SELECT item_id FROM player_special_items WHERE player_uuid = ?";
        Set<String> itemIds = new HashSet<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    itemIds.add(rs.getString("item_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load special items for " + uuid + ": " + e.getMessage());
        }

        return itemIds;
    }

    public void getPlayerSpecialItemsAsync(UUID uuid, Consumer<Set<String>> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Set<String> itemIds = getPlayerSpecialItems(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(itemIds));
            }
        }.runTaskAsynchronously(plugin);
    }

    public boolean purchaseSpecialItem(UUID uuid, String itemId, int price) {
        String updateCoinsSql = """
        UPDATE player_stats
        SET coins = coins - ?
        WHERE uuid = ?
          AND coins >= ?
        """;
        String insertItemSql = """
        INSERT INTO player_special_items (player_uuid, item_id)
        VALUES (?, ?)
        """;

        try (Connection conn = getConnection()) {
            if (conn == null) return false;

            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertItemSql)) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setString(2, itemId);
                insertStmt.executeUpdate();
            }

            try (PreparedStatement updateCoinsStmt = conn.prepareStatement(updateCoinsSql)) {
                updateCoinsStmt.setInt(1, price);
                updateCoinsStmt.setString(2, uuid.toString());
                updateCoinsStmt.setInt(3, price);

                if (updateCoinsStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to purchase special item '" + itemId + "' for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    public void purchaseSpecialItemAsync(UUID uuid, String itemId, int price, Consumer<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean purchased = purchaseSpecialItem(uuid, itemId, price);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(purchased));
            }
        }.runTaskAsynchronously(plugin);
    }

    public List<String> getTopWins() {
        String sql = "SELECT username, wins FROM player_stats ORDER BY wins DESC LIMIT 10";
        List<String> topWins = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                String formatted = ChatColor.translateAlternateColorCodes('&',
                        "&6" + rank + ". &e" + username + " &7- &a" + wins);
                topWins.add(formatted);
                rank++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topWins;
    }

    public List<String> getTopDeaths() {
        String sql = "SELECT username, deaths FROM player_stats ORDER BY deaths DESC LIMIT 10";
        List<String> topDeaths = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int deaths = rs.getInt("deaths");
                String formatted = ChatColor.translateAlternateColorCodes('&',
                        "&6" + rank + ". &e" + username + " &7- &c" + deaths);
                topDeaths.add(formatted);
                rank++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topDeaths;
    }

    public List<String> getTopKills() {
        String sql = "SELECT username, kills FROM player_stats ORDER BY kills DESC LIMIT 10";
        List<String> topKills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int kills = rs.getInt("kills");
                String formatted = ChatColor.translateAlternateColorCodes('&',
                        "&6" + rank + ". &e" + username + " &7- &b" + kills);
                topKills.add(formatted);
                rank++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topKills;
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get database connection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    private <T> CompletableFuture<T> supplyDatabaseAsync(java.util.function.Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }
}
