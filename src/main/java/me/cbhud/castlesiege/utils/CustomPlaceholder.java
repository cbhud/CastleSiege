package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import me.cbhud.castlesiege.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPlaceholder extends PlaceholderExpansion {

    private final CastleSiege plugin;

    // --- Leaderboard caches (strings are read/written across threads) ---
    private volatile String cachedTopWins = "Loading...";
    private volatile String cachedTopKills = "Loading...";
    private volatile String cachedTopDeaths = "Loading...";

    // --- Player stat caches ---
    private final Map<UUID, Integer> cachedKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedDeaths = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedWins = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cachedCoins = new ConcurrentHashMap<>();

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
        return String.valueOf(plugin.getDescription().getAuthors());
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

        UUID uuid = player.getUniqueId();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(uuid);

        switch (identifier.toLowerCase(Locale.ROOT)) {
            case "top_10_wins":
                return cachedTopWins;
            case "top_10_kills":
                return cachedTopKills;
            case "top_10_deaths":
                return cachedTopDeaths;

            case "kills":
                return String.valueOf(getCached(cachedKills, uuid));
            case "deaths":
                return String.valueOf(getCached(cachedDeaths, uuid));
            case "wins":
                return String.valueOf(getCached(cachedWins, uuid));
            case "coins":
                return String.valueOf(getCached(cachedCoins, uuid));
            case "kd":
                return String.valueOf(getPlayerKd(uuid));

            // --- Arena-related placeholders ---
            case "arena":
                return getArenaId(arena);
            case "arenasize":
                return String.valueOf(getArenaSize(arena)); // ✅ fixed
            case "arena_type":
                return getArenaType(arena);
            case "team":
                return getPlayerTeam(arena, player);
            case "attackers_size":
                return String.valueOf(getAttackersSize(arena));
            case "defenders_size":
                return String.valueOf(getDefendersSize(arena));
            case "winner":
                return getWinner(arena);
            case "king":
                return String.valueOf(getKingHealth(arena));
            case "attackers":
                return plugin.getConfigManager().getAttacker();
            case "defenders":
                return plugin.getConfigManager().getDefender();
            case "timer":
                return String.valueOf(getTimer(arena));
            case "starting-in":
                return String.valueOf(getStartingIn(arena));
            case "kit":
                return plugin.getPlayerKitManager().hasSelectedKit(player)
                        ? plugin.getPlayerKitManager().getSelectedKit(player).getName()
                        : "No kit selected";
        }

        return null;
    }

    // ------------------ CACHE UPDATERS ------------------

    private void startCacheUpdateTask() {
        // Leaderboards: no Bukkit calls, so async is fine
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateLeaderboardCache, 0L, 600L); // 30s

        // Stats: must NOT call Bukkit.getOnlinePlayers() off-thread.
        // Snapshot UUIDs on main thread, then do DataManager reads async.
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::scheduleStatsUpdate, 0L, 200L); // 10s
    }

    private void updateLeaderboardCache() {
        try {
            cachedTopWins = String.join("\n", plugin.getDataManager().getTopWins());
            cachedTopKills = String.join("\n", plugin.getDataManager().getTopKills());
            cachedTopDeaths = String.join("\n", plugin.getDataManager().getTopDeaths());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update leaderboard cache: " + e.getMessage());
        }
    }

    private void scheduleStatsUpdate() {
        // Main thread snapshot
        Set<UUID> online = new HashSet<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            online.add(p.getUniqueId());
        }

        // Async fetch + cache update
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> updateStatsCache(online));
    }

    private void updateStatsCache(Set<UUID> onlineUuids) {
        try {
            // Remove stale entries so maps don't grow forever
            purgeNotOnline(cachedKills, onlineUuids);
            purgeNotOnline(cachedDeaths, onlineUuids);
            purgeNotOnline(cachedWins, onlineUuids);
            purgeNotOnline(cachedCoins, onlineUuids);

            for (UUID uuid : onlineUuids) {
                cachedKills.put(uuid, plugin.getDataManager().getPlayerKills(uuid));
                cachedDeaths.put(uuid, plugin.getDataManager().getPlayerDeaths(uuid));
                cachedWins.put(uuid, plugin.getDataManager().getPlayerWins(uuid));
                cachedCoins.put(uuid, plugin.getDataManager().getPlayerCoins(uuid));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update stats cache: " + e.getMessage());
        }
    }

    private void purgeNotOnline(Map<UUID, Integer> map, Set<UUID> online) {
        for (UUID uuid : map.keySet()) {
            if (!online.contains(uuid)) {
                map.remove(uuid);
            }
        }
    }

    // ------------------ ARENA HELPERS ------------------

    private String getArenaId(Arena arena) {
        return arena != null ? arena.getId() : "N/A";
    }

    private int getArenaSize(Arena arena) {
        // ✅ FIX: Arena no longer has getPlayers()
        return arena != null ? arena.getNoPlayers() : 0;
    }

    private String getArenaType(Arena arena) {
        if (arena == null) return "N/A";
        return arena.isHardcore() ? "Hardcore" : "Normal";
    }

    private int getStartingIn(Arena arena) {
        if (arena == null) return 0;
        return arena.getTimerManager().getAutoStartTimer();
    }

    private int getTimer(Arena arena) {
        if (arena == null) return 0;
        return arena.getTimerManager().getCountdownTimer();
    }

    private double getKingHealth(Arena arena) {
        if (arena == null) return 0.0;
        return arena.getKingZombieHealth();
    }

    private int getAttackersSize(Arena arena) {
        if (arena == null) return 0;
        return arena.getAttackersSize();
    }

    private int getDefendersSize(Arena arena) {
        if (arena == null) return 0;
        return arena.getDefendersSize();
    }

    private String getWinner(Arena arena) {
        if (arena == null) return "N/A";
        int w = arena.getWinner();
        if (w != 0 && w != 1) return "N/A";
        return (w == 1)
                ? plugin.getConfigManager().getTeamName(Team.Attackers)
                : plugin.getConfigManager().getTeamName(Team.Defenders);
    }

    private String getPlayerTeam(Arena arena, Player player) {
        if (arena == null) return "N/A";
        Team team = arena.getTeam(player);
        if (team == null) return "N/A";
        return plugin.getConfigManager().getTeamName(team);
    }

    // ------------------ STAT HELPERS ------------------

    private int getCached(Map<UUID, Integer> map, UUID uuid) {
        return map.getOrDefault(uuid, 0);
    }

    private double getPlayerKd(UUID uuid) {
        double kills = getCached(cachedKills, uuid);
        double deaths = getCached(cachedDeaths, uuid);
        double kd = (deaths == 0) ? kills : (kills / deaths);
        return Math.round(kd * 10.0) / 10.0;
    }
}