package me.cbhud.castlesiege.arena;

import org.bukkit.Bukkit;
import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import me.cbhud.castlesiege.team.TeamManager;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Arena {
    private final String id;
    private Location lobbySpawn;
    private Location kingSpawn;
    private Location attackersSpawn;
    private Location defendersSpawn;
    private final int maxPlayers;
    private final int minPlayers;
    private final Set<Player> players;
    private final CastleSiege plugin;
    private ArenaState state;
    private TeamManager teamManager;
    private int winner;
    String worldName;
    private ArenaTimerManager timerManager;
    private boolean hardcore;

    public Arena(CastleSiege plugin, String id, Location lobbySpawn, Location kingSpawn, Location attackersSpawn, Location defendersSpawn, int max, int min, int autoStart, int countdown, String worldName, boolean hardcore) {
        this.plugin = plugin;
        this.id = id;
        this.lobbySpawn = lobbySpawn;
        this.kingSpawn = kingSpawn;
        this.attackersSpawn = attackersSpawn;
        this.defendersSpawn = defendersSpawn;
        this.maxPlayers = max;
        this.minPlayers = min;
        this.timerManager = new ArenaTimerManager(plugin, this, countdown, autoStart);
        this.players = new HashSet<>();
        this.state = ArenaState.WAITING;
        this.teamManager = new TeamManager(plugin, plugin.getConfigManager().getConfig());
        this.worldName = worldName;
        this.winner = -1;
        this.hardcore = hardcore;
    }

    public ArenaTimerManager getTimerManager() {
        return timerManager;
    }

    public Location getAttackersSpawn() {
        return attackersSpawn;
    }

    public Location getDefendersSpawn() {
        return defendersSpawn;
    }

    public int getNoPlayers() {
        return players.size();
    }

    public int getMax() {
        return maxPlayers;
    }

    public int getMin() {
        return minPlayers;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public String getId() {
        return id;
    }

    public ArenaState getState() {
        return state;
    }

    public boolean addPlayer(Player player) {
        if (state == ArenaState.ENDED){
            for (String i:  plugin.getMsg().getMessage("arenaEnded", player)){
                player.sendMessage(i);
            }
            return false;
        }

        if (players.size() >= getMax()) {
            for (String i:  plugin.getMsg().getMessage("arenaFull", player)){
                player.sendMessage(i);
            }
            return false;
        }

        if(state == ArenaState.IN_GAME) {
            player.sendTitle(plugin.getMsg().getMessage("spectatorTitle", player).get(0), plugin.getMsg().getMessage("spectatorTitle", player).get(1), 10, 70, 20);
            player.teleport(getKingSpawn());
            players.add(player);
            plugin.getPlayerManager().setPlayerAsSpectating(player);
            return false;
        }

        if(!teamManager.tryRandomTeamJoin(player)){
            player.teleport(getKingSpawn());
            player.sendTitle(plugin.getMsg().getMessage("spectatorTitle", player).get(0), plugin.getMsg().getMessage("spectatorTitle", player).get(1), 10, 70, 20);
            players.add(player);
            plugin.getPlayerManager().setPlayerAsSpectating(player);
            return false;
        }

        for (String i:  plugin.getMsg().getMessage("arenaJoin", player)){
            player.sendMessage(i);
        }

        players.add(player);

        if (lobbySpawn != null){
            player.teleport(getLobbySpawn());
        }

        if (players.size() >= getMin()) {
            timerManager.startAutoStart();
        }
        plugin.getPlayerManager().setPlayerAsWaiting(player);

        return true;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        teamManager.removePlayerFromTeam(player);
        plugin.getScoreboardManager().updateScoreboard(player, "lobby");
        if (players.size() < minPlayers && state == ArenaState.WAITING && timerManager.getAutoStartTaskId() != -1) {
            timerManager.stopAutoStart();
        }
        if (player != null) {
            plugin.getPlayerManager().setPlayerAsLobby(player);
        }
        plugin.getArenaManager().playerArenaMap.remove(player.getUniqueId());
    }

    public void removeHardcore(Player player) {
        teamManager.removePlayerFromTeam(player);
    }

    public void startGame() {
        if (state != ArenaState.WAITING) {
            Bukkit.broadcastMessage("You cannot start game in this state!");
        }
        state = ArenaState.IN_GAME;
        if (getKingSpawn() == null){
            getKSpawn();
        }
        Bukkit.getWorld(worldName).setTime(14000);
        plugin.getMobManager().spawnCustomMob(getKSpawn());
        players.forEach(player -> {
            if (plugin.getBBar().isEnabled()) {
            plugin.getBBar().showZombieHealthBarToPlayer(plugin.getMobManager().getKingZombie(getKSpawn().getWorld()), player);
            }
            player.playSound(player.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL,
                    1.0f, // volume
                    1.0f  // pitch
            );
            plugin.getMsg().getMessage("game-start-msg", player).forEach(player::sendMessage);
        });
        teleportTeamsToSpawns();
        timerManager.startCountdown();
    }

    public double getKingZombieHealth() {
        if (plugin.getMobManager().getKingZombie(kingSpawn.getWorld()) != null) {
            return plugin.getMobManager().getZombieHealth(plugin.getMobManager().getKingZombie(kingSpawn.getWorld()));
        } else {
            return 0.0;
        }
    }

    public void endGame() {
        state = ArenaState.ENDED;

        // Determine winner and set up game-specific logic
        boolean attackersWon = timerManager.getCountdownTaskId() != -1;
        Team winningTeam;
        Sound winSound;
        String winMessageKey;
        String winTitleKey;

        if (teamManager.getPlayersInTeam(Team.Attackers) == 0){
            attackersWon = false;
        }


        if (attackersWon) {
            winner = 1;
            winningTeam = Team.Attackers;
            winSound = Sound.ENTITY_WITHER_SPAWN;
            winMessageKey = "attackers-win-msg";
            winTitleKey = "attackersWinTitle";
            timerManager.stopCountdown();
        } else {
            winner = 0;
            winningTeam = Team.Defenders;
            winSound = Sound.ENTITY_PLAYER_LEVELUP;
            winMessageKey = "defenders-win-msg";
            winTitleKey = "defendersWinTitle";
            plugin.getMobManager().removeCustomZombie(this);

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setTime(1000);
            }
        }

        // Process all players once
        players.forEach(player -> {
            if (player == null || !player.isOnline()) return;

            if (plugin.getBBar().isEnabled()) {
                plugin.getBBar().removeBarForPlayer(player);
            }

            // Play sound and update scoreboard for all players
            player.playSound(player.getLocation(), winSound, 1.0f, 1.0f);
            plugin.getScoreboardManager().updateScoreboard(player, "end");

            // Send win messages
            List<String> winMessages = plugin.getMsg().getMessage(winMessageKey, player);
            if (winMessages != null) {
                winMessages.forEach(player::sendMessage);
            }

            // Send title
            List<String> titleMessages = plugin.getMsg().getMessage(winTitleKey, player);
            if (titleMessages != null && titleMessages.size() >= 2) {
                player.sendTitle(titleMessages.get(0), titleMessages.get(1), 10, 70, 20);
            }
        });

        // Award coins and wins to winning team
        Set<Player> winningPlayers = teamManager.getPlayersInTeams(winningTeam);
        if (winningPlayers != null) {
            int coinsReward = plugin.getConfigManager().getCoinsOnWin();
            winningPlayers.forEach(player -> {
                if (player != null) {
                    plugin.getDataManager().addPlayerCoins(player.getUniqueId(), coinsReward);
                    plugin.getDataManager().incrementWins(player.getUniqueId(), 1);
                }
            });
        }

        // Schedule lobby teleportation
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Create a copy to avoid concurrent modification
            List<Player> playersToTeleport = new ArrayList<>(players);

            playersToTeleport.forEach(player -> {
                if (player != null && player.isOnline()) {
                    Location lobby = plugin.getSlc().getLobby();
                    if (lobby != null) {
                        player.teleport(lobby);
                    }
                    plugin.getArenaManager().removePlayerFromArena(player);
                    plugin.getScoreboardManager().updateScoreboard(player, "lobby");
                    plugin.getPlayerManager().setPlayerAsLobby(player);
                }
            });

            // Clear collections
            players.clear();
            teamManager.clearTeams();

        }, 10 * 20L);

        // Schedule arena reset
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                        plugin.getArenaResetManager().resetArena(this),
                20 * 20L
        );
    }

    public Location getLSpawn() {
        return lobbySpawn;
    }

    public Location getKingSpawn(){
        return kingSpawn;
    }
    public Location getKSpawn() {
        kingSpawn = plugin.getArenaManager().getMobLocation(this);
        return kingSpawn;
    }

    public Location getLobbySpawn() {
        lobbySpawn = plugin.getArenaManager().getLocation(this);
        return lobbySpawn;
    }

    public int getAttackersSize() {
        return teamManager.getPlayersInTeam(Team.Attackers);
    }

    public int getDefendersSize() {
        return teamManager.getPlayersInTeam(Team.Defenders);
    }

    public Team getTeam(Player player) {
        return teamManager.getTeam(player);
    }

    public boolean joinTeam(Player p, Team t) {
        teamManager.joinTeam(p, t);
        return true;
    }

    public void teleportTeamsToSpawns() {
        CompletableFuture.runAsync(() -> {
            for (Team team : Team.values()) {
                Location teamSpawn = getSpawnLocationForTeam(team);
                if (teamSpawn == null) continue;

                Set<Player> teamPlayers = teamManager.getPlayersInTeams(team);
                if (teamPlayers.isEmpty()) continue;

                Bukkit.getScheduler().runTask(plugin, () -> teamPlayers.forEach(player -> {
                    plugin.getPlayerManager().setPlayerAsPlaying(player);
                    player.teleport(teamSpawn);
                    if (!plugin.getPlayerKitManager().hasSelectedKit(player)){
                        plugin.getPlayerKitManager().setDefaultKit(player);
                    }
                    plugin.getPlayerKitManager().giveKit(player, plugin.getPlayerKitManager().getSelectedKit(player));
                    if (team == Team.Defenders) {
                        player.sendTitle(plugin.getMsg().getMessage("defendersTitle", player).get(0), plugin.getMsg().getMessage("defendersTitle", player).get(1), 10, 70, 20);
                    } else {
                        player.sendTitle(plugin.getMsg().getMessage("attackersTitle", player).get(0), plugin.getMsg().getMessage("attackersTitle", player).get(1), 10, 70, 20);
                    }

                }));
            }
        });
    }

    private Location getSpawnLocationForTeam(Team team) {
        if (team == Team.Attackers) {
            return getAttackersSpawn();
        } else {
            return getDefendersSpawn();
        }
    }

    public int getWinner() {
        return winner;
    }

    public Location getTeamSpawn(Team team) {
        if (team == Team.Attackers) {return attackersSpawn;}
        return defendersSpawn;
    }

    public void setLobbySpawn(Location loc) {
        this.lobbySpawn = loc;
    }

    public void setKingSpawn(Location loc) {
        this.kingSpawn = loc;
    }

    public void setDefendersSpawn(Location loc) {
        defendersSpawn = loc;
    }

    public void setAttackersSpawn(Location loc) {
        attackersSpawn = loc;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public boolean isHardcore() {
        return hardcore;
    }
    public TeamManager getTeamManager() {
        return teamManager;
    }
}