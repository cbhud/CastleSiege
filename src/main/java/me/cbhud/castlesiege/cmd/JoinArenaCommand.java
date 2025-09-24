package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaManager;
import me.cbhud.castlesiege.arena.ArenaState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class JoinArenaCommand implements CommandExecutor {

    private final CastleSiege plugin;
    private final ArenaManager arenaManager;

    public JoinArenaCommand(CastleSiege plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can join arenas
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        if (commandName.equals("join")) {
            // /join <arenaName>
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /join <arenaName>");
                listAvailableArenas(player);
                return true;
            }
            handleJoinCommand(player, args[0]);
        } else if (commandName.equals("randomjoin")) {
            // /randomjoin
            if (args.length != 0) {
                player.sendMessage(ChatColor.RED + "Usage: /randomjoin");
                return true;
            }
            handleRandomJoinCommand(player);
        }

        return true;
    }

    private void handleJoinCommand(Player player, String arenaName) {
        // Check if arena exists
        Arena targetArena = findArenaByName(arenaName);
        if (targetArena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' does not exist!");
            listAvailableArenas(player);
            return;
        }

        // Attempt to join the specific arena
        attemptJoinArena(player, targetArena, false);
    }

    private void handleRandomJoinCommand(Player player) {
        // First, try to find a waiting arena with players
        Arena waitingArena = findWaitingArenaWithPlayers();

        if (waitingArena != null) {
            // Found a waiting arena with players, join it
            attemptJoinArena(player, waitingArena, true);
        } else {
            // No waiting arenas with players, find any available arena
            Arena availableArena = findRandomAvailableArena();

            if (availableArena != null) {
                attemptJoinArena(player, availableArena, true);
            } else {
                player.sendMessage(ChatColor.RED + "No available arenas found! All arenas are full, in-game, or ended.");
                player.sendMessage(ChatColor.YELLOW + "Try joining a specific arena with /join <arenaName>");
            }
        }
    }

    private void attemptJoinArena(Player player, Arena targetArena, boolean isRandomJoin) {
        // Check if player is already in an arena
        Arena currentArena = arenaManager.getArenaByPlayer(player.getUniqueId());

        if (currentArena != null) {
            // Player is already in an arena
            if (currentArena.equals(targetArena)) {
                // Player is trying to join the same arena they're already in
                player.sendMessage(ChatColor.YELLOW + "You are already in arena '" + targetArena.getId() + "'!");
                return;
            } else {
                // Player is switching arenas - handle cleanup
                handleArenaSwitch(player, currentArena, targetArena, isRandomJoin);
                return;
            }
        }

        // Player is not in any arena, proceed with normal join
        joinArena(player, targetArena, isRandomJoin);
    }

    private void handleArenaSwitch(Player player, Arena currentArena, Arena targetArena, boolean isRandomJoin) {
        // Validate target arena before switching
        if (!isArenaJoinable(targetArena)) {
            String reason = getArenaUnjoinableReason(targetArena);
            if (isRandomJoin) {
                player.sendMessage(ChatColor.RED + "Cannot join arena: " + reason);
            } else {
                player.sendMessage(ChatColor.RED + "Cannot switch to arena '" + targetArena.getId() + "': " + reason);
            }
            return;
        }

        // Check if switching from an in-game arena
        if (currentArena.getState() == ArenaState.IN_GAME) {
            // Warn player about leaving an active game
            player.sendMessage(ChatColor.YELLOW + "You are leaving an active game in arena '" + currentArena.getId() + "'!");

            // Check if this would affect team balance critically
            if (isPlayerCriticalForGame(player, currentArena)) {
                player.sendMessage(ChatColor.RED + "Warning: Your team may be at a severe disadvantage without you!");
            }
        }

        // Remove player from current arena with proper cleanup
        cleanupPlayerFromArena(player, currentArena);

        // Add small delay to ensure cleanup is complete
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            joinArena(player, targetArena, isRandomJoin);
        }, 1L);
    }

    private void cleanupPlayerFromArena(Player player, Arena arena) {
        // Remove player from arena (this should handle team removal and other cleanup)
        arenaManager.removePlayerFromArena(player);

        // Additional cleanup if needed
        if (arena.getState() == ArenaState.IN_GAME && arena.isHardcore()) {
            // Special handling for hardcore mode
            arena.removeHardcore(player);
        }

        // Ensure player is set to lobby state
        plugin.getPlayerManager().setPlayerAsLobby(player);

        // Remove any lingering effects
        if (plugin.getBBar().isEnabled()) {
            plugin.getBBar().removeBarForPlayer(player);
        }

        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player, "lobby");

        player.sendMessage(ChatColor.YELLOW + "Left arena '" + arena.getId() + "'.");
    }

    private void joinArena(Player player, Arena arena, boolean isRandomJoin) {
        // Final validation before joining
        if (!isArenaJoinable(arena)) {
            String reason = getArenaUnjoinableReason(arena);
            if (isRandomJoin) {
                player.sendMessage(ChatColor.RED + "Failed to join arena: " + reason);
            } else {
                player.sendMessage(ChatColor.RED + "Cannot join arena '" + arena.getId() + "': " + reason);
            }
            return;
        }

        // Add player to arena through ArenaManager
        arenaManager.addPlayerToArena(player, arena);

        // The Arena.addPlayer() method handles the actual join logic and messages
        // We don't need to duplicate that logic here

        // Send additional confirmation for random join
        if (isRandomJoin) {
            player.sendMessage(ChatColor.GREEN + "Joined arena: " + arena.getId() + " (" + arena.getNoPlayers() + "/" + arena.getMax() + ")");
        }
    }

    private boolean isArenaJoinable(Arena arena) {
        // Arena must not be null
        if (arena == null) return false;

        // Arena must not be ended
        if (arena.getState() == ArenaState.ENDED) return false;

        // Arena must not be full (unless it's in-game and allows spectators)
        if (arena.getNoPlayers() >= arena.getMax() && arena.getState() != ArenaState.IN_GAME) {
            return false;
        }

        // Arena world must exist and be loaded
        if (arena.getWorld() == null) return false;

        return true;
    }

    private String getArenaUnjoinableReason(Arena arena) {
        if (arena == null) return "Arena does not exist";
        if (arena.getState() == ArenaState.ENDED) return "Arena has ended";
        if (arena.getNoPlayers() >= arena.getMax() && arena.getState() != ArenaState.IN_GAME) return "Arena is full";
        if (arena.getWorld() == null) return "Arena world is not loaded";
        return "Unknown reason";
    }

    private boolean isPlayerCriticalForGame(Player player, Arena arena) {
        // Check if removing this player would create severe team imbalance
        if (arena.getTeam(player) != null) {
            int attackersSize = arena.getAttackersSize();
            int defendersSize = arena.getDefendersSize();

            // If teams are already unbalanced and this player is on the smaller team
            if (Math.abs(attackersSize - defendersSize) >= 2) {
                return (arena.getTeam(player) == me.cbhud.castlesiege.team.Team.Attackers && attackersSize <= defendersSize) ||
                        (arena.getTeam(player) == me.cbhud.castlesiege.team.Team.Defenders && defendersSize <= attackersSize);
            }
        }
        return false;
    }

    private Arena findArenaByName(String name) {
        return arenaManager.getArenas().stream()
                .filter(arena -> arena.getId().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private Arena findWaitingArenaWithPlayers() {
        return arenaManager.getArenas().stream()
                .filter(arena -> arena.getState() == ArenaState.WAITING)
                .filter(arena -> arena.getNoPlayers() > 0)
                .filter(arena -> arena.getNoPlayers() < arena.getMax())
                .filter(arena -> arena.getWorld() != null)
                .findFirst()
                .orElse(null);
    }

    private Arena findRandomAvailableArena() {
        List<Arena> availableArenas = arenaManager.getArenas().stream()
                .filter(this::isArenaJoinable)
                .filter(arena -> arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.IN_GAME)
                .collect(Collectors.toList());

        if (availableArenas.isEmpty()) {
            return null;
        }

        // Prefer waiting arenas over in-game arenas
        List<Arena> waitingArenas = availableArenas.stream()
                .filter(arena -> arena.getState() == ArenaState.WAITING)
                .collect(Collectors.toList());

        if (!waitingArenas.isEmpty()) {
            return waitingArenas.get(new Random().nextInt(waitingArenas.size()));
        }

        // If no waiting arenas, return a random available arena
        return availableArenas.get(new Random().nextInt(availableArenas.size()));
    }

    private void listAvailableArenas(Player player) {
        Set<Arena> arenas = arenaManager.getArenas();
        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No arenas are available.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Available Arenas:");
        for (Arena arena : arenas) {
            if (isArenaJoinable(arena)) {
                String status = getArenaStatusDisplay(arena);
                String playerCount = arena.getNoPlayers() + "/" + arena.getMax();
                player.sendMessage(ChatColor.YELLOW + "  " + arena.getId() + ChatColor.GRAY + " - " +
                        status + ChatColor.GRAY + " (" + playerCount + ")");
            }
        }
    }

    private String getArenaStatusDisplay(Arena arena) {
        switch (arena.getState()) {
            case WAITING:
                return ChatColor.GREEN + "WAITING";
            case IN_GAME:
                return ChatColor.YELLOW + "IN-GAME";
            case ENDED:
                return ChatColor.RED + "ENDED";
            default:
                return ChatColor.GRAY + "UNKNOWN";
        }
    }

}