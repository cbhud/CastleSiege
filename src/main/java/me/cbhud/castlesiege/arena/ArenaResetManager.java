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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaResetManager {

    private final CastleSiege plugin;

    private final File arenasFile;
    private FileConfiguration arenasConfig;

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

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> resetArena(arena));
            return;
        }

        // Prevent double reset scheduling
        if (!resettingArenas.add(arena.getId())) {
            plugin.getLogger().warning("[CastleSiege] Reset already in progress for arena " + arena.getId());
            return;
        }

        boolean pasteStarted = false;

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

            if (!arenasConfig.contains(arenaPath + ".pasteX")
                    || !arenasConfig.contains(arenaPath + ".pasteY")
                    || !arenasConfig.contains(arenaPath + ".pasteZ")) {
                plugin.getLogger().warning("[CastleSiege] Paste location missing for arena " + arena.getId());
                return;
            }

            int pasteX = arenasConfig.getInt(arenaPath + ".pasteX");
            int pasteY = arenasConfig.getInt(arenaPath + ".pasteY");
            int pasteZ = arenasConfig.getInt(arenaPath + ".pasteZ");

            File schematicFolder = new File(plugin.getDataFolder(), "schematics");
            SchematicProcessor processor = SchematicProcessor.newSchematicProcessor(
                    plugin.getWorldEdit(),
                    arena.getId(),
                    schematicFolder
            );

            Location pasteLocation = new Location(world, pasteX, pasteY, pasteZ);

            plugin.getLogger().info("[CastleSiege] Resetting arena " + arena.getId()
                    + " by pasting its saved schematic at "
                    + pasteX + ", " + pasteY + ", " + pasteZ + ".");

            // The saved schematic must cover the full arena reset cuboid, including air.
            // Regenerato pastes air too, so blocks placed in originally empty space are removed
            // only if that air exists inside the saved schematic region.
            CompletableFuture<?> pasteFuture = processor.paste(pasteLocation);
            pasteFuture.whenComplete((ignored, throwable) -> finishReset(arena, throwable));
            pasteStarted = true;
        } catch (NoSchematicException e) {
            plugin.getLogger().severe("[CastleSiege] Missing schematic for arena " + arena.getId()
                    + ". Reset failed; arena will stay unavailable.");
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().severe("[CastleSiege] Error resetting arena " + arena.getId()
                    + ". Arena will stay unavailable.");
            e.printStackTrace();
        } finally {
            if (!pasteStarted) {
                resettingArenas.remove(arena.getId());
            }
        }
    }

    private void finishReset(Arena arena, Throwable throwable) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (throwable != null) {
                    Throwable cause = unwrapThrowable(throwable);
                    plugin.getLogger().severe("[CastleSiege] Failed to reset arena " + arena.getId()
                            + ". Arena will stay in RESETTING until an admin intervenes.");
                    cause.printStackTrace();
                    return;
                }

                plugin.getLogger().info("[CastleSiege] Arena " + arena.getId() + " reset successfully!");
                arena.setState(ArenaState.WAITING);
            } finally {
                resettingArenas.remove(arena.getId());
            }
        });
    }

    private Throwable unwrapThrowable(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
