package me.cbhud.castlesiege.arena;

import org.bukkit.Bukkit;
import java.io.File;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import io.github.regenerato.worldedit.SchematicProcessor;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    final Map<UUID, Arena> playerArenaMap = new ConcurrentHashMap<>();
    private final CastleSiege plugin;
    private final File configFile;
    private final FileConfiguration config;

    public ArenaManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadArenas();
    }

    private void loadArenas() {
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection == null) return;

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(arenaId);
            if (section == null) continue;

            String worldName = section.getString("king-spawn").split(",")[0];
            Location lobbySpawn = parseLocation(section.getString("lobby-spawn"));
            Location kingSpawn = parseLocation(section.getString("king-spawn"));
            Location defendersSpawn = parseLocation(section.getString("defenders-spawn"));
            Location attackersSpawn = parseLocation(section.getString("attackers-spawn"));
            int autoStart = section.getInt("auto-start");
            int countdown = section.getInt("game-timer");
            int minPlayers = section.getInt("min-players");
            int maxPlayers = section.getInt("max-players");
            Arena arena = new Arena(plugin, arenaId, lobbySpawn, kingSpawn, attackersSpawn, defendersSpawn, maxPlayers, minPlayers, autoStart, countdown, worldName);
            arenas.put(arenaId, arena);
        }
    }

    public Location getLocation(Arena arena) {
        String id = arena.getId();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection == null) {
            plugin.getLogger().warning("No 'arenas' section found in config!");
            return null;
        }

        ConfigurationSection section = arenasSection.getConfigurationSection(id);
        if (section == null) {
            plugin.getLogger().warning("Arena with ID '" + id + "' not found in config.");
            return null;
        }

        String locationString = section.getString("lobby-spawn");
        if (locationString == null) {
            plugin.getLogger().warning("Lobby spawn not set for arena '" + id + "'.");
            return null;
        }

        return parseLocation(locationString);
    }

    public Location getMobLocation(Arena arena) {
        String id = arena.getId();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection == null) {
            plugin.getLogger().warning("No 'arenas' section found in config!");
            return null;
        }

        ConfigurationSection section = arenasSection.getConfigurationSection(id);
        if (section == null) {
            plugin.getLogger().warning("Arena with ID '" + id + "' not found in config.");
            return null;
        }

        String locationString = section.getString("King-spawn");
        if (locationString == null) {
            plugin.getLogger().warning("King spawn not set for arena '" + id + "'.");
            return null;
        }

        return parseLocation(locationString);
    }


    private Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        String[] parts = locString.split(",");
        if (parts.length != 4) return null;
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public Set<Arena> getArenas() {
        return new HashSet<>(arenas.values());
    }

    public boolean addArena(Arena arena, Player player) {
        if (arenas.containsKey(arena.getId())) return false;
        arenas.put(arena.getId(), arena);
        saveArena(player, arena);
        return true;
    }

    public Arena getArenaByPlayer(UUID playerId) {
        return playerArenaMap.get(playerId);
    }

    public void addPlayerToArena(Player player, Arena arena) {
        UUID playerId = player.getUniqueId();
        playerArenaMap.put(playerId, arena);
        arena.addPlayer(player);
    }

    public void removePlayerFromArena(Player player) {
        UUID playerId = player.getUniqueId();
        Arena arena = getArenaByPlayer(playerId);
        if (arena != null) {
            arena.removePlayer(player);
            playerArenaMap.remove(playerId);
        }
    }

    public void saveArena(Player player, Arena arena) {
        try {
            File schematicFolder = new File(plugin.getDataFolder(), "schematics");
            if (!schematicFolder.exists()) schematicFolder.mkdirs();

            // Save schematic inside the schematics folder
            SchematicProcessor processor = SchematicProcessor.newSchematicProcessor(plugin.getWorldEdit(), arena.getId(), schematicFolder);
            processor.write(player);

            Actor actor = BukkitAdapter.adapt(player);
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(actor);

            if (session == null || session.getSelectionWorld() == null) {
                player.sendMessage(ChatColor.RED + "No WorldEdit selection found! Select an area first.");
                return;
            }

            Region region = session.getSelection(session.getSelectionWorld());

            // Get min and max coordinates
            int minX = region.getMinimumPoint().getBlockX();
            int minY = region.getMinimumPoint().getBlockY();
            int minZ = region.getMinimumPoint().getBlockZ();
            int maxX = region.getMaximumPoint().getBlockX();
            int maxY = region.getMaximumPoint().getBlockY();
            int maxZ = region.getMaximumPoint().getBlockZ();

            // Save paste location
            Location loc = player.getLocation();
            int pasteX = loc.getBlockX();
            int pasteY = loc.getBlockY();
            int pasteZ = loc.getBlockZ();

            // Save locations to arenas.yml
            ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
            if (arenasSection == null) {
                arenasSection = config.createSection("arenas");
            }

            ConfigurationSection section = arenasSection.createSection(arena.getId());
            section.set("lobby-spawn", formatLocation(arena.getLSpawn()));
            section.set("king-spawn", formatLocation(arena.getKingSpawn()));
            section.set("defenders-spawn", formatLocation(arena.getDefendersSpawn()));
            section.set("attackers-spawn", formatLocation(arena.getAttackersSpawn()));
            section.set("auto-start", 60);
            section.set("game-timer", 300);
            section.set("min-players", arena.getMin());
            section.set("max-players", arena.getMax());

            // Save WorldEdit region info
            section.set("minX", minX);
            section.set("maxX", maxX);
            section.set("minY", minY);
            section.set("maxY", maxY);
            section.set("minZ", minZ);
            section.set("maxZ", maxZ);
            section.set("pasteX", pasteX);
            section.set("pasteY", pasteY);
            section.set("pasteZ", pasteZ);

            // Save config to file
            config.save(configFile);

            player.sendMessage(ChatColor.GREEN + "Arena saved successfully!");

        } catch (EmptyClipboardException e) {
            player.sendMessage(ChatColor.RED + "No WorldEdit clipboard found.");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error saving arena! Make sure you have a valid selection.");
            e.printStackTrace();
        }
    }


    // Helper method to format a Location as a string
    private String formatLocation(Location location) {
        if (location == null) return null;
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
    }






}