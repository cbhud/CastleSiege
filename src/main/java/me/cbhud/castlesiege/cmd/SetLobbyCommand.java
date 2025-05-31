package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SetLobbyCommand implements CommandExecutor {

    private final CastleSiege plugin;
    private File lobbyFile;
    private FileConfiguration lobbyConfig;
    private Location lobbySpawn;

    public SetLobbyCommand(CastleSiege plugin) {
        this.plugin = plugin;
        createLobbyFile();
    }

    private void createLobbyFile() {
        lobbyFile = new File(plugin.getDataFolder(), "lobby-location.yml");
        if (!lobbyFile.exists()) {
            try {
                lobbyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lobbyConfig = YamlConfiguration.loadConfiguration(lobbyFile);
        lobbySpawn = getLobby();
    }

    private void saveLobbyFile() {
        try {
            lobbyConfig.save(lobbyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLobby(Location loc) {
        lobbyConfig.set("lobby.world", loc.getWorld().getName());
        lobbyConfig.set("lobby.x", loc.getX());
        lobbyConfig.set("lobby.y", loc.getY());
        lobbyConfig.set("lobby.z", loc.getZ());
        lobbyConfig.set("lobby.yaw", loc.getYaw());
        lobbyConfig.set("lobby.pitch", loc.getPitch());
        saveLobbyFile();
        lobbySpawn = loc;
    }

    public Location getLobby() {
        if (!lobbyConfig.contains("lobby")) return null;
        World world = Bukkit.getWorld(lobbyConfig.getString("lobby.world"));
        if (world == null) return null; // Ensure the world exists

        double x = lobbyConfig.getDouble("lobby.x");
        double y = lobbyConfig.getDouble("lobby.y");
        double z = lobbyConfig.getDouble("lobby.z");
        float yaw = (float) lobbyConfig.getDouble("lobby.yaw");
        float pitch = (float) lobbyConfig.getDouble("lobby.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("setlobby")) {
            if (!player.hasPermission("cs.admin")){
                for (String i:  plugin.getMsg().getMessage("join-server-msg", player)){
                    player.sendMessage(i);
                }
            }
            setLobby(player.getLocation());
            player.sendMessage("§aLobby location set!");
            return true;
        }
        return false;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }


}

