package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final CastleSiege plugin;

    // Cooldown storage - UUID to last usage time in milliseconds
    private final Map<UUID, Long> cooldowns = new HashMap<>();


    public StatsCommand(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;


        // Check arguments
        if (args.length == 0) {
            // /stats - show own stats
            showStats(player, player.getName(), true);
        } else if (args.length == 1) {
            // /stats <username> - show other player's stats
            String targetName = args[0];

            showStats(player, targetName, false);
        } else {
            player.sendMessage("§cUsage: /stats [player]");
            return true;
        }

        return true;
    }

    /**
     * Shows stats for a player
     * @param viewer The player viewing the stats
     * @param targetName The name of the player whose stats to show
     * @param isOwnStats Whether the viewer is looking at their own stats
     */
    private void showStats(Player viewer, String targetName, boolean isOwnStats) {
        // Async database call to prevent blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Get player UUID (you might need to adjust this based on your implementation)
                UUID targetUUID;
                if (isOwnStats) {
                    targetUUID = viewer.getUniqueId();
                } else {
                    targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
                }

                // Fetch stats from database (adjust method names to match your DataManager)
                int wins = plugin.getDataManager().getPlayerWins(targetUUID);
                int kills = plugin.getDataManager().getPlayerKills(targetUUID);
                int deaths = plugin.getDataManager().getPlayerDeaths(targetUUID);
                int coins = plugin.getDataManager().getPlayerCoins(targetUUID);

                // Calculate stats
                double kdr = deaths > 0 ? (double) kills / deaths : kills;

                // Switch back to main thread for sending messages
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (isOwnStats) {
                        sendStatsMessage(viewer, viewer.getName(), wins, kills, deaths, coins, kdr);
                    } else {
                        sendStatsMessage(viewer, targetName, wins, kills, deaths, coins, kdr);
                    }
                });

            } catch (Exception e) {
                // Handle database errors
                plugin.getLogger().warning("Error fetching stats for " + targetName + ": " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    viewer.sendMessage("§cError retrieving stats. Please try again later.");
                });
            }
        });
    }

    /**
     * Sends formatted stats message to player
     */
    private void sendStatsMessage(Player player, String title, int wins, int kills, int deaths,
                                  int coins, double kdr) {
        for (String i:  plugin.getMsg().getMessage("stats-msg", player)){
            if (i.contains("{cs_player_name}")){
                i = i.replace("{cs_player_name}", title);
            }

            if (i.contains("{cs_wins}")){
                i = i.replace("{cs_wins}", String.valueOf(wins));
            }

            if (i.contains("{cs_kills}")){
                i = i.replace("{cs_kills}", String.valueOf(kills));
            }

            if (i.contains("{cs_deaths}")){
                i = i.replace("{cs_deaths}", String.valueOf(deaths));
            }

            if (i.contains("{cs_coins}")){
                i = i.replace("{cs_coins}", String.valueOf(coins));
            }

            if (i.contains("{cs_kdr}")){
                i = i.replace("{cs_kdr}", String.valueOf(kdr));
            }

            player.sendMessage(i);
        }

    }

    /**
     * Check if player is on cooldown
     */



}