package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final CastleSiege plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(CastleSiege plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        boolean changed = false;
        if (!config.contains("boss-bar.enabled")) {
            config.set("boss-bar.enabled", config.getBoolean("bossbar-enabled", true));
            changed = true;
        }
        if (!config.contains("attackers-nametag-color")) { config.set("attackers-nametag-color", "&c"); changed = true; }
        if (!config.contains("defenders-nametag-color")) { config.set("defenders-nametag-color", "&9"); changed = true; }
        if (changed) saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getTeamName(Team team){
        if (team == Team.Attackers){
            return config.getString("attackersTeamName");
        }else{
            return config.getString("defendersTeamName");
        }
    }

    public String getAttacker() {
        return config.getString("attackersTeamName");
    }

    public String getDefender() {
        return config.getString("defendersTeamName");
    }


    public double getKingHealth() {
        return config.getDouble("king-health", 80.0);
    }

    public String getKingName() {
        return config.getString("king-name", "Charles");
    }

    public int getCoinsOnKill(){
        return config.getInt("coins-on-kill", 1);
    }
    public int getCoinsOnWin(){
        return config.getInt("coins-on-win", 1);
    }

    public boolean getBossBarEnabled() {
        return config.getBoolean("boss-bar.enabled", config.getBoolean("bossbar-enabled", true));
    }

    public ChatColor getDefendersColor() {
        String colorCode = config.getString("defenders-nametag-color", "&9");
        return parseColor(colorCode, ChatColor.BLUE);
    }

    public ChatColor getAttackersColor() {
        String colorCode = config.getString("attackers-nametag-color", "&c");
        return parseColor(colorCode, ChatColor.RED);
    }

    private ChatColor parseColor(String colorCode, ChatColor fallback) {
        if (colorCode == null || colorCode.length() < 2 || colorCode.charAt(0) != '&') {
            return fallback;
        }

        ChatColor color = ChatColor.getByChar(colorCode.charAt(1));
        return color != null ? color : fallback;
    }

}
