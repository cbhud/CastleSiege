package me.cbhud.castlesiege.arena;

import io.github.regenerato.worldedit.NoSchematicException;
import io.github.regenerato.worldedit.SchematicProcessor;
import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ArenaResetManager {

    private final CastleSiege plugin;

    public ArenaResetManager(CastleSiege plugin) {
        this.plugin = plugin;
    }

    /**
     * Resets the given arena using the schematic and config bounds.
     */
    public void resetArena(Arena arena) {
        try {
            File schematicFolder = new File(plugin.getDataFolder(), "schematics");
            SchematicProcessor processor = SchematicProcessor.newSchematicProcessor(plugin.getWorldEdit(), arena.getId(), schematicFolder);
            World world = arena.getWorld();
            if (world == null) {
                Bukkit.getLogger().severe("[CastleSiege] World " + arena.getWorldName() + " for arena " + arena.getId() + " is not loaded!");
                return;
            }

            File configFile = new File(plugin.getDataFolder(), "arenas.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            String arenaPath = "arenas." + arena.getId();

            if (!config.contains(arenaPath + ".minX")) {
                Bukkit.getLogger().warning("[CastleSiege] Arena location is missing for " + arena.getId());
                return;
            }

            int minX = config.getInt(arenaPath + ".minX");
            int maxX = config.getInt(arenaPath + ".maxX");
            int minY = config.getInt(arenaPath + ".minY");
            int maxY = config.getInt(arenaPath + ".maxY");
            int minZ = config.getInt(arenaPath + ".minZ");
            int maxZ = config.getInt(arenaPath + ".maxZ");

            int pasteX = config.getInt(arenaPath + ".pasteX");
            int pasteY = config.getInt(arenaPath + ".pasteY");
            int pasteZ = config.getInt(arenaPath + ".pasteZ");

            // First clear, then paste schematic
            clearArena(world, minX, maxX, minY, maxY, minZ, maxZ, () -> {
                try {
                    Location pasteLocation = new Location(world, pasteX, pasteY, pasteZ);
                    processor.paste(pasteLocation);
                } catch (NoSchematicException e) {
                    throw new RuntimeException(e);
                }
                Bukkit.getLogger().info("Arena " + arena.getId() + " reset successfully!");
                arena.setState(ArenaState.WAITING);
            });

        } catch (Exception e) {
            Bukkit.getLogger().severe("[CastleSiege] Error resetting arena " + arena.getId());
            e.printStackTrace();
        }
    }

    /**
     * Clears a region block by block, with batching.
     */
    private void clearArena(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Runnable onComplete) {
        int batchSize = plugin.getConfigManager().getConfig().getInt("arenaBlockRegenPerSecond", 2500);
        List<Block> blocks = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < blocks.size() && processed < batchSize) {
                    Block block = blocks.get(index++);
                    block.setType(Material.AIR, false);
                    processed++;
                }

                if (index >= blocks.size()) {
                    cancel();
                    if (onComplete != null) {
                        Bukkit.getScheduler().runTask(plugin, onComplete);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}

