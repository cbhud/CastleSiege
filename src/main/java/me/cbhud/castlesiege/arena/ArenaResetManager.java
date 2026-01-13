package me.cbhud.castlesiege.arena;

import io.github.regenerato.worldedit.NoSchematicException;
import io.github.regenerato.worldedit.SchematicProcessor;
import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaResetManager {

    private final CastleSiege plugin;

    private final File arenasFile;
    private FileConfiguration arenasConfig;

    // Prevent overlapping resets for the same arena
    private final Set<String> resettingArenas = ConcurrentHashMap.newKeySet();

    public ArenaResetManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    /** Call this if arenas.yml is edited while the server is running. */
    public void reload() {
        this.arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    /**
     * Resets the given arena using the schematic and config bounds.
     */
    public void resetArena(Arena arena) {
        if (arena == null) return;

        // Prevent double reset scheduling
        if (!resettingArenas.add(arena.getId())) {
            plugin.getLogger().warning("[CastleSiege] Reset already in progress for arena " + arena.getId());
            return;
        }

        try {
            World world = arena.getWorld();
            if (world == null) {
                plugin.getLogger().severe("[CastleSiege] World " + arena.getWorldName()
                        + " for arena " + arena.getId() + " is not loaded!");
                return;
            }

            String arenaPath = "arenas." + arena.getId();

            if (!arenasConfig.contains(arenaPath + ".minX")) {
                plugin.getLogger().warning("[CastleSiege] Arena bounds missing for " + arena.getId());
                return;
            }

            int minX = arenasConfig.getInt(arenaPath + ".minX");
            int maxX = arenasConfig.getInt(arenaPath + ".maxX");
            int minY = arenasConfig.getInt(arenaPath + ".minY");
            int maxY = arenasConfig.getInt(arenaPath + ".maxY");
            int minZ = arenasConfig.getInt(arenaPath + ".minZ");
            int maxZ = arenasConfig.getInt(arenaPath + ".maxZ");

            int pasteX = arenasConfig.getInt(arenaPath + ".pasteX");
            int pasteY = arenasConfig.getInt(arenaPath + ".pasteY");
            int pasteZ = arenasConfig.getInt(arenaPath + ".pasteZ");

            // Normalize bounds if misconfigured
            int nMinX = Math.min(minX, maxX), nMaxX = Math.max(minX, maxX);
            int nMinY = Math.min(minY, maxY), nMaxY = Math.max(minY, maxY);
            int nMinZ = Math.min(minZ, maxZ), nMaxZ = Math.max(minZ, maxZ);

            File schematicFolder = new File(plugin.getDataFolder(), "schematics");
            SchematicProcessor processor = SchematicProcessor.newSchematicProcessor(
                    plugin.getWorldEdit(),
                    arena.getId(),
                    schematicFolder
            );

            // Clear first, then paste schematic
            clearArena(world, nMinX, nMaxX, nMinY, nMaxY, nMinZ, nMaxZ, () -> {
                try {
                    processor.paste(new Location(world, pasteX, pasteY, pasteZ));
                    plugin.getLogger().info("[CastleSiege] Arena " + arena.getId() + " reset successfully!");
                    arena.setState(ArenaState.WAITING);
                } catch (NoSchematicException e) {
                    plugin.getLogger().severe("[CastleSiege] Missing schematic for arena " + arena.getId()
                            + ". Cannot paste reset.");
                } finally {
                    resettingArenas.remove(arena.getId());
                }
            });

        } catch (Exception e) {
            plugin.getLogger().severe("[CastleSiege] Error resetting arena " + arena.getId());
            e.printStackTrace();
            resettingArenas.remove(arena.getId());
        }
    }

    /**
     * Clears a region block-by-block with batching, without allocating a huge block list.
     */
    private void clearArena(
            World world,
            int minX, int maxX,
            int minY, int maxY,
            int minZ, int maxZ,
            Runnable onComplete
    ) {
        // Config key is named "...PerSecond", so interpret it as per second and convert to per tick.
        int perSecond = plugin.getConfigManager().getConfig().getInt("arenaBlockRegenPerSecond", 2500);
        int perTick = Math.max(1, (int) Math.ceil(perSecond / 20.0));

        new BukkitRunnable() {
            int x = minX;
            int y = minY;
            int z = minZ;

            boolean done = false;

            @Override
            public void run() {
                int processed = 0;

                while (!done && processed < perTick) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                    }

                    processed++;

                    // advance coordinates in y -> z -> x order
                    y++;
                    if (y > maxY) {
                        y = minY;
                        z++;
                        if (z > maxZ) {
                            z = minZ;
                            x++;
                            if (x > maxX) {
                                done = true;
                            }
                        }
                    }
                }

                if (done) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run(); // already on main thread
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
