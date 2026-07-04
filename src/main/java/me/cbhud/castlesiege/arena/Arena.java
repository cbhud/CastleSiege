package me.cbhud.castlesiege.arena;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import me.cbhud.castlesiege.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.*;

public class Arena {

    private final CastleSiege plugin;
    private final String id;

    private Location lobbySpawn;
    private Location kingSpawn;
    private Location attackersSpawn;
    private Location defendersSpawn;

    private final int maxPlayers;
    private final int minPlayers;

    // I store UUIDs instead of Player objects so arena membership does not keep player instances alive.
    private final Set<UUID> playerIds = new HashSet<>();

    private ArenaState state = ArenaState.WAITING;
    private final TeamManager teamManager;

    private int winner = -1;

    private final String worldName;
    private final ArenaTimerManager timerManager;
    private final boolean hardcore;

    public Arena(
            CastleSiege plugin,
            String id,
            Location lobbySpawn,
            Location kingSpawn,
            Location attackersSpawn,
            Location defendersSpawn,
            int max,
            int min,
            int autoStart,
            int countdown,
            String worldName,
            boolean hardcore
    ) {
        this.plugin = plugin;
        this.id = id;

        this.lobbySpawn = lobbySpawn;
        this.kingSpawn = kingSpawn;
        this.attackersSpawn = attackersSpawn;
        this.defendersSpawn = defendersSpawn;

        this.maxPlayers = max;
        this.minPlayers = min;

        this.timerManager = new ArenaTimerManager(plugin, this, countdown, autoStart);
        this.teamManager = new TeamManager(plugin, plugin.getConfigManager().getConfig());

        this.worldName = worldName;
        this.hardcore = hardcore;
    }

    /* ------------------------- Basics ------------------------- */

    public String getId() { return id; }
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }

    public int getMax() { return maxPlayers; }
    public int getMin() { return minPlayers; }
    public int getNoPlayers() { return playerIds.size(); }

    public boolean isHardcore() { return hardcore; }
    public int getWinner() { return winner; }

    public ArenaTimerManager getTimerManager() { return timerManager; }
    public TeamManager getTeamManager() { return teamManager; }

    public String getWorldName() { return worldName; }
    public World getWorld() { return Bukkit.getWorld(worldName); }

    /** Raw UUID set (unmodifiable). Useful for managers. */
    public Set<UUID> getPlayerIds() {
        return Collections.unmodifiableSet(playerIds);
    }

    /** Online players snapshot for safe iteration. */
    public List<Player> getOnlinePlayersSnapshot() {
        List<Player> list = new ArrayList<>(playerIds.size());
        for (UUID uuid : playerIds) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) list.add(p);
        }
        return list;
    }

    public boolean containsPlayer(Player player) {
        return player != null && playerIds.contains(player.getUniqueId());
    }

    public boolean containsPlayer(UUID uuid) {
        return uuid != null && playerIds.contains(uuid);
    }

    /* ------------------------- Spawns ------------------------- */

    public Location getAttackersSpawn() { return attackersSpawn; }
    public Location getDefendersSpawn() { return defendersSpawn; }
    public Location getTeamSpawn(Team team) { return (team == Team.Attackers) ? attackersSpawn : defendersSpawn; }

    public Location getLSpawn() { return lobbySpawn; } // I keep this alias for compatibility.
    public Location getKingSpawn() { return kingSpawn; }

    public Location getKSpawn() {
        Location loc = plugin.getArenaManager().getMobLocation(this);
        if (loc != null) kingSpawn = loc;
        return kingSpawn;
    }

    public Location getLobbySpawn() {
        Location loc = plugin.getArenaManager().getLocation(this);
        if (loc != null) lobbySpawn = loc;
        return lobbySpawn;
    }

    private Location resolveKingSpawn() {
        if (kingSpawn == null) kingSpawn = getKSpawn();
        return kingSpawn;
    }

    private Location resolveLobbySpawn() {
        if (lobbySpawn == null) lobbySpawn = getLobbySpawn();
        return lobbySpawn;
    }

    /* ------------------------- Team helpers ------------------------- */

    public int getAttackersSize() { return teamManager.getPlayersInTeam(Team.Attackers); }
    public int getDefendersSize() { return teamManager.getPlayersInTeam(Team.Defenders); }

    public Team getTeam(Player player) {
        return teamManager.getTeam(player);
    }

    public boolean joinTeam(Player p, Team t) {
        return teamManager.tryToJoinTeam(p, t);
    }

    /* ------------------------- Join / Leave ------------------------- */

    public boolean addPlayer(Player player) {
        if (player == null) return false;

        if (state == ArenaState.ENDED) {
            sendLines(player, "arenaEnded");
            return false;
        }

        if (playerIds.size() >= maxPlayers) {
            sendLines(player, "arenaFull");
            return false;
        }

        UUID uuid = player.getUniqueId();

        // In-game join = spectator
        if (state == ArenaState.IN_GAME) {
            playerIds.add(uuid);

            Location ks = resolveKingSpawn();
            if (ks != null) player.teleport(ks);
            plugin.getPlayerManager().setPlayerAsSpectating(player);

            sendTitleSafe(player, "spectatorTitle", 10, 70, 20);
            return false;
        }

        sendLines(player, "arenaJoin");

        // Try team join; if fail -> spectator
        if (!teamManager.tryRandomTeamJoin(player)) {
            playerIds.add(uuid);

            Location ks = resolveKingSpawn();
            if (ks != null) player.teleport(ks);
            plugin.getPlayerManager().setPlayerAsSpectating(player);
            sendTitleSafe(player, "spectatorTitle", 10, 70, 20);
            return false;
        }

        // Normal waiting join
        playerIds.add(uuid);

        Location ls = resolveLobbySpawn();
        if (ls != null) player.teleport(ls);

        plugin.getPlayerManager().setPlayerAsWaiting(player);

        if (playerIds.size() >= minPlayers) {
            timerManager.startAutoStart();
        }

        return true;
    }

    public void removePlayer(Player player) {
        if (player == null) return;

        Team removedTeam = teamManager.getTeam(player);
        playerIds.remove(player.getUniqueId());
        teamManager.removePlayerFromTeam(player);

        if (state == ArenaState.WAITING && playerIds.size() < minPlayers && timerManager.getAutoStartTaskId() != -1) {
            timerManager.stopAutoStart();
        }

        if (player.isOnline()) {
            plugin.getPlayerManager().setPlayerAsLobby(player);
        }

        plugin.getArenaManager().unmapPlayer(player.getUniqueId());
        endGameIfNoDefendersRemain(removedTeam);
    }

    /** I remove hardcore deaths from their team while leaving them in the arena as spectators. */
    public void removeHardcore(Player player) {
        if (player == null) return;
        Team removedTeam = teamManager.getTeam(player);
        teamManager.removePlayerFromTeam(player);
        endGameIfNoDefendersRemain(removedTeam);
    }

    /* ------------------------- Game flow ------------------------- */

    public void startGame() {
        if (state != ArenaState.WAITING) return;

        state = ArenaState.IN_GAME;

        World world = getWorld();
        if (world != null) world.setTime(14000);

        Location ks = resolveKingSpawn();
        if (ks != null) {
            plugin.getMobManager().spawnCustomMob(ks);
        }

        teleportTeamsToSpawns();

        for (Player p : getOnlinePlayersSnapshot()) {
            sendLines(p, "game-start-msg");
            if (hardcore) sendLines(p, "game-start-hardcore-msg-addition");
        }

        timerManager.startCountdown();
    }

    public double getKingZombieHealth() {
        Location ks = resolveKingSpawn();
        if (ks == null || ks.getWorld() == null) return 0.0;

        if (plugin.getMobManager().getKingZombie(ks.getWorld()) != null) {
            return plugin.getMobManager().getZombieHealth(plugin.getMobManager().getKingZombie(ks.getWorld()));
        }
        return 0.0;
    }

    public void endGame() {
        if (state == ArenaState.ENDED) return;

        state = ArenaState.ENDED;

        // I use the timer flag so winner logic does not depend on task ids.
        boolean attackersWon = !timerManager.didCountdownExpire();
        if (teamManager.getPlayersInTeam(Team.Attackers) == 0) attackersWon = false;

        final Team winningTeam;
        final Sound winSound;
        final String winMessageKey;
        final String winTitleKey;

        if (attackersWon) {
            winner = 1;
            winningTeam = Team.Attackers;
            winSound = Sound.ENTITY_ENDER_DRAGON_DEATH;
            winMessageKey = "attackers-win-msg";
            winTitleKey = "attackersWinTitle";
            timerManager.stopCountdown();
        } else {
            winner = 0;
            winningTeam = Team.Defenders;
            winSound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
            winMessageKey = "defenders-win-msg";
            winTitleKey = "defendersWinTitle";

            plugin.getMobManager().removeCustomZombie(this);

            World world = getWorld();
            if (world != null) world.setTime(1000);
        }

        for (Player p : getOnlinePlayersSnapshot()) {
            if (plugin.getBBar().isEnabled()) {
                plugin.getBBar().removeBarForPlayer(p);
            }

            p.playSound(p.getLocation(), winSound, 1.0f, 1.0f);
            plugin.updateScoreboard(p, "end");

            sendLines(p, winMessageKey);
            sendTitleSafe(p, winTitleKey, 10, 70, 20);
            plugin.getNameTagManager().removeNametag(p);
        }

        // Rewards using UUIDs (works even if winners disconnected)
        int coinsReward = plugin.getConfigManager().getCoinsOnWin();
        for (UUID uuid : teamManager.getTeamMemberIds(winningTeam)) {
            plugin.getDataManager().addPlayerCoins(uuid, coinsReward);
            plugin.getDataManager().incrementWins(uuid);
            sendWinCoinsMessage(uuid, coinsReward);
        }

        // Cleanup players via ArenaManager (single source of truth)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : new HashSet<>(playerIds)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    plugin.getArenaManager().removePlayerFromArena(p);
                } else {
                    // offline: just unmap + drop from this arena
                    plugin.getArenaManager().unmapPlayer(uuid);
                    playerIds.remove(uuid);
                }
            }

            playerIds.clear();
            teamManager.clearTeams();
        }, 15 * 20L);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> plugin.getArenaResetManager().resetArena(this),
                25 * 20L
        );
    }

    /* ------------------------- Teleport teams ------------------------- */

    public void teleportTeamsToSpawns() {
        // Main-thread only
        for (Team team : Team.values()) {
            Location spawn = (team == Team.Attackers) ? attackersSpawn : defendersSpawn;
            if (spawn == null) continue;

            Set<Player> teamPlayers = teamManager.getPlayersInTeams(team);
            if (teamPlayers == null || teamPlayers.isEmpty()) continue;

            for (Player p : teamPlayers) {
                if (p == null || !p.isOnline()) continue;

                plugin.getPlayerManager().setPlayerAsPlaying(p);
                p.teleport(spawn);

                if (!plugin.getPlayerKitManager().hasSelectedKit(p)) {
                    plugin.getPlayerKitManager().setDefaultKit(p);
                }
                plugin.getPlayerKitManager().giveKit(p, plugin.getPlayerKitManager().getSelectedKit(p));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (team == Team.Defenders) {
                        plugin.getNameTagManager().setDefender(p);
                        sendTitleSafe(p, "defendersTitle", 10, 100, 20);
                    } else {
                        plugin.getNameTagManager().setAttacker(p);
                        sendTitleSafe(p, "attackersTitle", 10, 100, 20);
                    }
                    if (plugin.getBBar().isEnabled()) {
                        showKingBossBarToPlayer(p);
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                }, 20L);
            }
        }
    }

    private void endGameIfNoDefendersRemain(Team removedTeam) {
        if (state != ArenaState.IN_GAME || removedTeam != Team.Defenders) return;
        if (teamManager.getPlayersInTeam(Team.Defenders) > 0) return;
        if (teamManager.getPlayersInTeam(Team.Attackers) <= 0) return;

        plugin.getMobManager().removeCustomZombie(this);
        endGame();
    }

    private void showKingBossBarToPlayer(Player player) {
        Location kingLocation = resolveKingSpawn();
        if (kingLocation == null || kingLocation.getWorld() == null) return;

        Zombie king = plugin.getMobManager().getKingZombie(kingLocation.getWorld());
        if (king == null || !king.isValid() || king.isDead()) return;

        plugin.getBBar().showZombieHealthBarToPlayer(king, player);
    }

    /* ------------------------- Message helpers ------------------------- */

    private void sendLines(Player player, String key) {
        List<String> lines = plugin.getMsg().getMessage(key, player);
        if (lines == null || lines.isEmpty()) return;
        for (String s : lines) {
            if (s != null && !s.isEmpty()) player.sendMessage(s);
        }
    }

    private void sendTitleSafe(Player player, String key, int fadeIn, int stay, int fadeOut) {
        List<String> title = plugin.getMsg().getMessage(key, player);
        if (title == null || title.isEmpty()) return;

        String line1 = title.size() >= 1 ? title.get(0) : "";
        String line2 = title.size() >= 2 ? title.get(1) : "";

        player.sendTitle(line1, line2, fadeIn, stay, fadeOut);
    }

    private void sendWinCoinsMessage(UUID uuid, int coinsReward) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        for (String line : plugin.getMsg().getMessage("win-coins-received", player)) {
            if (line != null && !line.isEmpty()) {
                player.sendMessage(line.replace("{coins}", String.valueOf(coinsReward)));
            }
        }
    }

    /* ------------------------- Setters ------------------------- */

    public void setLobbySpawn(Location loc) { this.lobbySpawn = loc; }
    public void setKingSpawn(Location loc) { this.kingSpawn = loc; }
    public void setDefendersSpawn(Location loc) { this.defendersSpawn = loc; }
    public void setAttackersSpawn(Location loc) { this.attackersSpawn = loc; }
}
