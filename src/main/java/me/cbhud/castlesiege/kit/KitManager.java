package me.cbhud.castlesiege.kit;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KitManager {
    private final CastleSiege plugin;
    private List<KitData> kits;

    public KitManager(CastleSiege plugin) {
        this.plugin = plugin;
        createKitsFileIfNotExists();
        kits = new ArrayList<>();
        kits = loadKits();
        syncKitsToDatabase();
    }

    // Load kits from kits.yml
    public List<KitData> loadKits() {
        if (!kits.isEmpty()) return kits;

        File kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);

        for (String kitName : config.getConfigurationSection("kits").getKeys(false)) {
            String path = "kits." + kitName;

            List<String> kitItems = config.getStringList(path + ".kitItems");
            int price = config.getInt(path + ".price");
            Team team = Team.valueOf(config.getString(path + ".team"));
            Boolean isDefault = config.getBoolean(path + ".default", false);
            String kitIcon = config.getString(path + ".kitIcon");

            List<ItemStack> items = new ArrayList<>();

            for (String itemString : kitItems) {
                if (itemString.toUpperCase().startsWith("CUSTOM_ITEM:")) {
                    String customItemName = itemString.substring("CUSTOM_ITEM:".length()).toLowerCase();
                    ItemStack customItem = createCustomItem(customItemName);
                    if (customItem != null && customItem.getType() != Material.AIR) {
                        items.add(customItem);
                    } else {
                        System.out.println("Failed to create custom item: " + customItemName);
                    }
                } else {
                    String[] itemData = itemString.split(":");
                    Material material = Material.getMaterial(itemData[0].toUpperCase());
                    int amount = itemData.length > 1 ? Integer.parseInt(itemData[1]) : 1;
                    items.add(new ItemStack(material, amount));
                }
            }

            kits.add(new KitData(kitName, items, price, team, isDefault, kitIcon));
        }

        return kits;
    }

    private ItemStack createCustomItem(String customItemName) {
        CustomItem customItem = plugin.getItemManager().getById(customItemName.toLowerCase());
        if (customItem != null) {
            return customItem.getItemStack();
        }
        // fallback to some default if no match
        return new ItemStack(Material.STONE);
    }

    private void createKitsFileIfNotExists() {
        File kitsFile = new File(plugin.getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("kits.yml")) {
                if (in != null) {
                    Files.copy(in, kitsFile.toPath());
                    plugin.getLogger().info("kits.yml created successfully.");
                } else {
                    plugin.getLogger().severe("kits.yml not found in resources!");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create kits.yml file.");
                e.printStackTrace();
            }
        }
    }

    public KitData getKitByName(String kitName) {
        for (KitData kit : kits) {
            if (kit.getName().equalsIgnoreCase(kitName)) {
                return kit;
            }
        }
        return null;
    }

    public KitData getDefaultKitForTeam(Team team) {
        for (KitData kit : kits) {
            if (kit.getTeam() == team && kit.isDefault) {
                return kit;
            }
        }
        return null;
    }

    public static class KitData {
        private final String name;
        private final List<ItemStack> items;
        private final int price;
        private final Team team;
        private final Boolean isDefault;
        private final String kitIcon;

        public KitData(String name, List<ItemStack> items, int price, Team team, Boolean isDefault, String kitIcon) {
            this.name = name;
            this.items = items;
            this.price = price;
            this.team = team;
            this.isDefault = isDefault;
            this.kitIcon = kitIcon;
        }

        public String getName() {
            return name;
        }

        public boolean isDefault() {
            return isDefault != null && isDefault;
        }

        public List<ItemStack> getItems() {
            return items;
        }

        public int getPrice() {
            return price;
        }

        public Team getTeam() {
            return team;
        }

        public String getKitIcon() {return kitIcon;}

    }

    public void saveOrUpdateKitInDatabase(KitData kit) {
        try (Connection conn = plugin.getDataManager().getConnection()) {
            String selectSql = "SELECT id, price FROM kits WHERE name = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, kit.getName());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        int currentPrice = rs.getInt("price");
                        if (currentPrice != kit.getPrice()) {
                            String updateSql = "UPDATE kits SET price = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setInt(1, kit.getPrice());
                                updateStmt.setInt(2, id);
                                updateStmt.executeUpdate();
                                plugin.getLogger().info("Updated kit price for " + kit.getName());
                            }
                        } else {
                            plugin.getLogger().info("Kit " + kit.getName() + " already up to date.");
                        }
                    } else {
                        // Insert new kit
                        String insertSql = "INSERT INTO kits (name, price) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, kit.getName());
                            insertStmt.setInt(2, kit.getPrice());
                            insertStmt.executeUpdate();
                            plugin.getLogger().info("Inserted new kit " + kit.getName() + " into DB.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving kit " + kit.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void syncKitsToDatabase() {
        for (KitData kit : kits) {
            saveOrUpdateKitInDatabase(kit);
        }
    }
}

