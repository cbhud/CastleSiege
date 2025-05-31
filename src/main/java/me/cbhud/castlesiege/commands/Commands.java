package me.cbhud.castlesiege.commands;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.kits.KitManager;
import me.cbhud.castlesiege.player.PlayerStates;
import me.cbhud.castlesiege.game.GameState;
import me.cbhud.castlesiege.game.Type;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final CastleSiege plugin;

    public Commands(CastleSiege plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /cs <teamspawn|setlobby|setkingspawn|start|endgame|type>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "type":
                return typeCommand(sender);
            case "endgame":
                return endGameCommand(sender);
            case "setlobby":
                return lobbyCommand(sender);
            case "setkingspawn":
                return setMobSpawnCommand(sender);
            case "start":
                return startCommand(sender);
            case "teamspawn":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cs teamspawn <teamName>");
                    return true;
                }
                return teamSpawnCommand(sender, args[1]);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Available commands: endgame, setlobby, setkingspawn, start, teamspawn");
                return true;
        }
    }

    private boolean endGameCommand(CommandSender sender) {
        if (plugin.getGame().getState() == GameState.IN_GAME) {
            if (sender.hasPermission("cs.admin")) {
                plugin.getMobManager().removeCustomZombie();
                plugin.getTimer().cancelTimer();
                plugin.getLocationManager().teleportPlayersToLobby();
                plugin.getGame().setState(GameState.LOBBY);
                this.plugin.getMapRegeneration().regenerateChangedBlocks();
                plugin.tntThrower().clearCooldowns();
                for (String line : plugin.getMessagesConfig().getForceStopMsg()){
                Bukkit.broadcastMessage(line);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You cannot end game in this STATE");
        }

        return true;
    }

    private boolean lobbyCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        plugin.getConfig().set("lobby.world", player.getWorld().getName());
        plugin.getConfig().set("lobby.x", player.getLocation().getX());
        plugin.getConfig().set("lobby.y", player.getLocation().getY());
        plugin.getConfig().set("lobby.z", player.getLocation().getZ());
        plugin.getConfig().set("lobby.yaw", player.getLocation().getYaw());
        plugin.getConfig().set("lobby.pitch", player.getLocation().getPitch());

        plugin.saveConfig();
        player.getWorld().setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        player.sendMessage(ChatColor.GREEN + "Lobby location set!");
        plugin.getLocationManager().loadSpawnLocations();

        return true;
    }

    private boolean setMobSpawnCommand(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            plugin.getConfig().set("mobSpawnLocation.x", player.getLocation().getX());
            plugin.getConfig().set("mobSpawnLocation.y", player.getLocation().getY());
            plugin.getConfig().set("mobSpawnLocation.z", player.getLocation().getZ());
            plugin.getConfig().set("mobSpawnLocation.yaw", player.getLocation().getYaw());
            plugin.getConfig().set("mobSpawnLocation.pitch", player.getLocation().getPitch());
            plugin.getConfig().set("mobSpawnLocation.world", player.getLocation().getWorld().getName());
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "King's spawn location has been updated.");
            plugin.getLocationManager().loadSpawnLocations();
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        }

        return true;
    }
    public boolean startGame(){
        if(plugin.getTimer().isRun()){
            plugin.getTimer().cancelCountdown2();
        }

        if (plugin.getLocationManager().getMobLocation() != null) {
            plugin.getMobManager().spawnCustomMob();
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "King spawn location not set. Use /setkingspawn to set the location.");
            return true;
        }
        plugin.getScoreboardManager().loadTeamCount();


        plugin.getGame().setState(GameState.IN_GAME);
        teleportTeamsToSpawnsAndApplyKits();

        plugin.getTimer().startTimer(plugin.getConfig().getInt("timerMinutes", 10));

        for (String line : plugin.getMessagesConfig().getStartMsg()) {
            line = line.replace("{attackers}", plugin.getConfigManager().getAttacker());
            line = line.replace("{defenders}", plugin.getConfigManager().getDefender());
            Bukkit.broadcastMessage(line);
        }
        if (plugin.getGame().getType() == Type.Hardcore){
            for (String line : plugin.getMessagesConfig().getHardCoreEnabledMsg()){
                Bukkit.broadcastMessage(line);
            }
        }
        return true;
    }

    public boolean startCommand(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender.hasPermission("cs.admin")) {
            if (plugin.getGame().getState() != GameState.LOBBY) {
            sender.sendMessage(ChatColor.RED + "The game is not in LOBBY STATE. You cannot start it now.");
            return true;
        }
            startGame();
            }else {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command!");
                }
        return true;
    }

    private boolean teamSpawnCommand(CommandSender sender, String teamName) {
        if (!(sender instanceof Player) || !sender.hasPermission("cs.admin")) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        try {
            String teamName1 = teamName.substring(0,1).toUpperCase() + teamName.substring(1).toLowerCase();
            Team team = Team.valueOf(teamName1);
            plugin.getLocationManager().setTeamSpawnLocation(team, player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Spawn location for " + team + " set successfully!");
            plugin.getLocationManager().loadSpawnLocations();
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid TEAM. Please USE: Attackers or Defenders");
        }

        return true;
    }



    private void teleportTeamsToSpawnsAndApplyKits() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Team[] teams = Team.values();
            for (Team team : teams) {
                Location teamSpawn = plugin.getLocationManager().getSpawnLocationForTeam(team);
                if (teamSpawn != null) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (plugin.getTeamManager().getTeam(player) == team) {
                            plugin.getPlayerManager().setPlayerAsPlaying(player);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.teleport(teamSpawn);
                                if (plugin.getConfigManager().getBossBar()){
                                plugin.getBossBar().showZombieHealthBar(plugin.getMobManager().getKingZombie());
                                }
                                if (plugin.getPlayerManager().getPlayerState(player) == PlayerStates.PLAYING) {
                                    KitManager.KitData selectedKit = plugin.getPlayerKitManager().getSelectedKit(player);
                                    plugin.getPlayerKitManager().giveKit(player, selectedKit);
                                }
                            });
                        }
                    }
                }
            }
        });
    }


    private boolean typeCommand(CommandSender sender) {

        if (plugin.getGame().getState() == GameState.LOBBY) {
            if (sender.hasPermission("cs.admin")) {

                String msg = plugin.getMessagesConfig().getHardcoreMsg().toString().replaceAll("]", "");
                msg = msg.replace("[", "");

                if (plugin.getGame().getType() == Type.Normal) {
                    plugin.getGame().setType(Type.Hardcore);

                    Bukkit.broadcastMessage(msg + " §aenabled!");
                }else{
                    plugin.getGame().setType(Type.Normal);
                    Bukkit.broadcastMessage(msg + " §cdisabled!");
                }
                plugin.getScoreboardManager().updateScoreboardForAll();
            }
        }
        return true;
    }

}
