package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class Messages {
    private final CastleSiege plugin;
    private FileConfiguration config;
    private File configFile;

    public Messages(CastleSiege plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public List<String> getGuiMessage(String key) {
        if (!config.contains(key)) {
            return List.of(ChatColor.RED + "Message not found: " + key);
        }

        return config.getStringList(key).stream()
                .map(message -> ChatColor.translateAlternateColorCodes('&', message))
                .collect(Collectors.toList());
    }

    public List<String> getMessage(String key, Player player) {
        if (!config.contains(key)) {
            return List.of(ChatColor.RED + "Message not found: " + key);
        }

        return config.getStringList(key).stream()
                .map(message -> ChatColor.translateAlternateColorCodes('&', applyPlaceholders(player, message)))
                .collect(Collectors.toList());
    }

    public List<String> getMessage(String key) {
        if (!config.contains(key)) {
            return List.of(ChatColor.RED + "Message not found: " + key);
        }
        return config.getStringList(key).stream()
                .map(message -> ChatColor.translateAlternateColorCodes('&', message))
                .collect(Collectors.toList());
    }

    private String applyPlaceholders(Player player, String message) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

}
