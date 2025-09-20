package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPlaceholder extends PlaceholderExpansion {
    private final CastleSiege plugin;

    // --- Leaderboard caches ---
    private String cachedTopWins = "Loading...";
    private String cachedTopKills = "Loading...";
    private String cachedTopDeaths = "Loading...";
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_CACHE_DURATION = 30000; // 30s

    // --- Player stat caches ---
    private final Map<UUID, Integer> cachedKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedDeaths = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedWins = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedCoins = new ConcurrentHashMap<>();
    private long lastStatsUpdate = 0;
    private static final long STATS_CACHE_DURATION = 10000; // 10s

    public CustomPlaceholder(CastleSiege plugin) {
        this.plugin = plugin;
        startCacheUpdateTask();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cs"; // %cs_<placeholder>%
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null) return null;
        Player player = offlinePlayer.getPlayer();
        if (player == null) return null;

        switch (identifier.toLowerCase()) {
            case "top_10_wins":
                return cachedTopWins;
            case "top_10_kills":
                return cachedTopKills;
            case "top_10_deaths":
                return cachedTopDeaths;
            case "kills":
                return String.valueOf(getPlayerKills(player));
            case "deaths":
                return String.valueOf(getPlayerDeaths(player));
            case "wins":
                return String.valueOf(getPlayerWins(player));
            case "coins":
                return String.valueOf(getPlayerCoins(player));
            case "kd":
                return String.valueOf(getPlayerKd(player));

            // --- Arena-related placeholders (not cached, lightweight) ---
            case "arena":
                return String.valueOf(getArena(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId())));
            case "arenasize":
                return String.valueOf(getArenaSize(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId())));
            case "arena_type":
                return getArenaType(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()));
            case "team":
                return getPlayerTeam(player);
            case "attackers_size":
                return String.valueOf(getAttackersSize(player));
            case "defenders_size":
                return String.valueOf(getDefendersSize(player));
            case "winner":
                return getWinner(player);
            case "king":
                return String.valueOf(getKingHealth(player));
            case "attackers":
                return getAttackersName();
            case "defenders":
                return getDefendersName();
            case "timer":
                return String.valueOf(getTimer(player));
            case "starting-in":
                return String.valueOf(getStartingIn(player));
            case "kit":
                return plugin.getPlayerKitManager().hasSelectedKit(player)
                        ? getPlayerKit(player)
                        : "No kit selected";
        }
        return null;
    }

    // ------------------ CACHE UPDATERS ------------------

    private void startCacheUpdateTask() {
        // Update leaderboard cache every 30s
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateLeaderboardCache, 0L, 600L);

        // Update player stats cache every 10s
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateStatsCache, 0L, 200L);
    }

    private void updateLeaderboardCache() {
        try {
            cachedTopWins = String.join("\n", plugin.getDataManager().getTopWins());
            cachedTopKills = String.join("\n", plugin.getDataManager().getTopKills());
            cachedTopDeaths = String.join("\n", plugin.getDataManager().getTopDeaths());
            lastLeaderboardUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update leaderboard cache: " + e.getMessage());
        }
    }

    private void updateStatsCache() {
        try {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                cachedKills.put(uuid, plugin.getDataManager().getPlayerKills(uuid));
                cachedDeaths.put(uuid, plugin.getDataManager().getPlayerDeaths(uuid));
                cachedWins.put(uuid, plugin.getDataManager().getPlayerWins(uuid));
                cachedCoins.put(uuid, plugin.getDataManager().getPlayerCoins(uuid));
            }
            lastStatsUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update stats cache: " + e.getMessage());
        }
    }

    // ------------------ PLACEHOLDER HELPERS ------------------

    private int getStartingIn(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTimerManager().getAutoStartTimer();
    }

    private int getTimer(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTimerManager().getCountdownTimer();
    }

    private String getArenaType(Arena arena) {
        if (arena.isHardcore()){
            return "Hardcore";
        }else {
            return "Normal";
        }
    }

    private String getArena(Arena arena) {
        return arena != null ? arena.getId() : "N/A";
    }

    private int getArenaSize(Arena arena) {
        return arena != null ? arena.getPlayers().size() : 0;
    }

    private double getKingHealth(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getKingZombieHealth();
    }

    private String getAttackersName() {
        return plugin.getConfigManager().getAttacker();
    }

    private String getDefendersName() {
        return plugin.getConfigManager().getDefender();
    }

    private int getAttackersSize(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getAttackersSize();
    }

    private int getDefendersSize(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getDefendersSize();
    }

    private String getWinner(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getWinner() == 1
                ? plugin.getConfigManager().getTeamName(Team.Attackers)
                : plugin.getConfigManager().getTeamName(Team.Defenders);
    }

    private String getPlayerKit(Player player) {
        return plugin.getPlayerKitManager().getSelectedKit(player).getName();
    }

    private String getPlayerTeam(Player player) {
        return plugin.getConfigManager().getTeamName(
                plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player)
        );
    }

    // --- Cached player stats ---
    private int getPlayerKills(Player player) {
        return cachedKills.getOrDefault(player.getUniqueId(), 0);
    }

    private int getPlayerDeaths(Player player) {
        return cachedDeaths.getOrDefault(player.getUniqueId(), 0);
    }

    private int getPlayerWins(Player player) {
        return cachedWins.getOrDefault(player.getUniqueId(), 0);
    }

    private int getPlayerCoins(Player player) {
        return cachedCoins.getOrDefault(player.getUniqueId(), 0);
    }

    private double getPlayerKd(Player player) {
        double kills = getPlayerKills(player);
        double deaths = getPlayerDeaths(player);
        double kd = deaths == 0 ? kills : kills / deaths;
        return Math.round(kd * 10.0) / 10.0;
    }
}