package me.cbhud.castlesiege.gui;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class ArenaYmlEditor {

    private ArenaYmlEditor() {}

    private static File arenasFile(CastleSiege plugin) {
        return new File(plugin.getDataFolder(), "arenas.yml");
    }

    private static FileConfiguration load(CastleSiege plugin) {
        return YamlConfiguration.loadConfiguration(arenasFile(plugin));
    }

    private static boolean save(CastleSiege plugin, FileConfiguration cfg) {
        try {
            cfg.save(arenasFile(plugin));
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arenas.yml: " + e.getMessage());
            return false;
        }
    }

    public static boolean setBoolean(CastleSiege plugin, String arenaId, String key, boolean value) {
        FileConfiguration cfg = load(plugin);
        String path = "arenas." + arenaId + "." + key;
        cfg.set(path, value);
        return save(plugin, cfg);
    }

    public static boolean setInt(CastleSiege plugin, String arenaId, String key, int value) {
        FileConfiguration cfg = load(plugin);
        String path = "arenas." + arenaId + "." + key;
        cfg.set(path, value);
        return save(plugin, cfg);
    }

    // Spawns: store as "world,x,y,z"
    public static boolean setSpawn(CastleSiege plugin, String arenaId, String key, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        FileConfiguration cfg = load(plugin);
        String path = "arenas." + arenaId + "." + key;

        String s = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
        cfg.set(path, s);

        return save(plugin, cfg);
    }

    public static boolean getBoolean(CastleSiege plugin, String arenaId, String key, boolean def) {
        FileConfiguration cfg = load(plugin);
        return cfg.getBoolean("arenas." + arenaId + "." + key, def);
    }

    public static int getInt(CastleSiege plugin, String arenaId, String key, int def) {
        FileConfiguration cfg = load(plugin);
        return cfg.getInt("arenas." + arenaId + "." + key, def);
    }

    public static String getSpawnRaw(CastleSiege plugin, String arenaId, String key) {
        FileConfiguration cfg = load(plugin);
        return cfg.getString("arenas." + arenaId + "." + key, "Not set");
    }
}
