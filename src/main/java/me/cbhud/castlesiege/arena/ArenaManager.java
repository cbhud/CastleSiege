package me.cbhud.castlesiege.arena;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import io.github.regenerato.worldedit.SchematicProcessor;
import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Map<UUID, Arena> playerArenaMap = new ConcurrentHashMap<>();

    private final CastleSiege plugin;
    private final File configFile;
    private FileConfiguration config;

    public ArenaManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "arenas.yml");
        ensureFileExists(configFile);
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadArenas();
    }

    /* ------------------------- Public API ------------------------- */

    public Set<Arena> getArenas() {
        return new HashSet<>(arenas.values());
    }

    public Arena getArenaById(String id) {
        return (id == null) ? null : arenas.get(id);
    }

    public Arena getArenaByPlayer(UUID playerId) {
        return playerArenaMap.get(playerId);
    }

    /** Use this instead of accessing the map directly from other classes. */
    public void unmapPlayer(UUID playerId) {
        if (playerId != null) playerArenaMap.remove(playerId);
    }

    public boolean addArena(Arena arena, Player player) {
        if (arena == null || arena.getId() == null) return false;
        if (arenas.containsKey(arena.getId())) return false;

        arenas.put(arena.getId(), arena);
        saveArena(player, arena);
        return true;
    }

    /**
     * Adds player to the arena and only maps them if they're actually present in arena.getPlayers()
     * afterwards. This covers spectator joins too (your Arena.addPlayer may return false but still add).
     */
    public void addPlayerToArena(Player player, Arena arena) {
        if (player == null || arena == null) return;

        UUID playerId = player.getUniqueId();

        // If player already in an arena, remove cleanly first
        Arena current = playerArenaMap.get(playerId);
        if (current != null && current != arena) {
            removePlayerFromArena(player); // removes mapping + calls arena.removePlayer
        }

        // ✅ Map FIRST so any downstream code (kits/teams) can resolve arena
        playerArenaMap.put(playerId, arena);

        arena.addPlayer(player);

        // ✅ If addPlayer didn't actually keep them in this arena, rollback mapping
        // (works with your UUID-based Arena: containsPlayer(uuid) or getNoPlayers checks)
        if (!arena.containsPlayer(playerId)) {
            playerArenaMap.remove(playerId);
        }
    }


    public void removePlayerFromArena(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        Arena arena = playerArenaMap.remove(playerId);
        if (arena != null) {
            arena.removePlayer(player);
        }
    }

    /** Reload arenas.yml + rebuild Arena objects. */
    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        arenas.clear();
        playerArenaMap.clear();
        loadArenas();
    }

    /* ------------------------- Arena loading ------------------------- */

    private void loadArenas() {
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection == null) return;

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(arenaId);
            if (section == null) continue;

            Location lobbySpawn = parseLocation(section.getString("lobby-spawn"), arenaId, "lobby-spawn");
            Location kingSpawn = parseLocation(section.getString("king-spawn"), arenaId, "king-spawn");
            Location defendersSpawn = parseLocation(section.getString("defenders-spawn"), arenaId, "defenders-spawn");
            Location attackersSpawn = parseLocation(section.getString("attackers-spawn"), arenaId, "attackers-spawn");

            int autoStart = section.getInt("auto-start", 60);
            int countdown = section.getInt("game-timer", 300);
            int minPlayers = section.getInt("min-players", 2);
            int maxPlayers = section.getInt("max-players", 16);
            boolean hardcore = section.getBoolean("hardcore", false);

            // Determine worldName safely
            String worldName = null;
            if (kingSpawn != null && kingSpawn.getWorld() != null) {
                worldName = kingSpawn.getWorld().getName();
            } else {
                String raw = section.getString("king-spawn");
                if (raw != null) {
                    String[] parts = raw.split(",");
                    if (parts.length >= 1) worldName = parts[0].trim();
                }
            }

            if (worldName == null || worldName.isBlank()) {
                plugin.getLogger().warning("Arena '" + arenaId + "' has no valid worldName (king-spawn missing/invalid). Skipping arena load.");
                continue;
            }

            Arena arena = new Arena(plugin, arenaId, lobbySpawn, kingSpawn, attackersSpawn, defendersSpawn,
                    maxPlayers, minPlayers, autoStart, countdown, worldName, hardcore);

            arenas.put(arenaId, arena);
        }
    }

    /* ------------------------- Config helpers ------------------------- */

    /** Generic location getter to avoid duplicated getLocation/getMobLocation patterns. */
    public Location getArenaLocation(Arena arena, String key) {
        if (arena == null || key == null) return null;

        ConfigurationSection section = getArenaSection(arena.getId());
        if (section == null) return null;

        String locationString = section.getString(key);
        if (locationString == null) {
            plugin.getLogger().warning("Location key '" + key + "' not set for arena '" + arena.getId() + "'.");
            return null;
        }

        return parseLocation(locationString, arena.getId(), key);
    }

    public Location getLocation(Arena arena) {
        return getArenaLocation(arena, "lobby-spawn");
    }

    public Location getMobLocation(Arena arena) {
        return getArenaLocation(arena, "king-spawn");
    }

    private ConfigurationSection getArenaSection(String arenaId) {
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("No 'arenas' section found in arenas.yml");
            return null;
        }

        ConfigurationSection section = arenasSection.getConfigurationSection(arenaId);
        if (section == null) {
            plugin.getLogger().warning("Arena with ID '" + arenaId + "' not found in arenas.yml");
        }
        return section;
    }

    /**
     * Supports "world,x,y,z" and also "world,x,y,z,yaw,pitch".
     * Trims spaces and logs bad values.
     */
    private Location parseLocation(String locString, String arenaId, String key) {
        if (locString == null || locString.isBlank()) return null;

        String[] parts = locString.split(",");
        if (parts.length != 4 && parts.length != 6) {
            plugin.getLogger().warning("Arena '" + arenaId + "' has invalid location format for '" + key + "': " + locString);
            return null;
        }

        String worldName = parts[0].trim();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Arena '" + arenaId + "' location '" + key + "' references unloaded/missing world: " + worldName);
            return null;
        }

        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());

            Location loc = new Location(world, x, y, z);

            if (parts.length == 6) {
                float yaw = Float.parseFloat(parts[4].trim());
                float pitch = Float.parseFloat(parts[5].trim());
                loc.setYaw(yaw);
                loc.setPitch(pitch);
            }

            return loc;
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Arena '" + arenaId + "' has non-numeric location for '" + key + "': " + locString);
            return null;
        }
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;
        // Keep existing 4-part format for backwards compatibility
        return location.getWorld().getName() + ","
                + location.getX() + ","
                + location.getY() + ","
                + location.getZ();
    }

    private void ensureFileExists(File file) {
        if (file.exists()) return;
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ------------------------- Save arena ------------------------- */

    public void saveArena(Player player, Arena arena) {
        if (player == null || arena == null) return;

        try {
            File schematicFolder = new File(plugin.getDataFolder(), "schematics");
            if (!schematicFolder.exists()) schematicFolder.mkdirs();

            // Validate WorldEdit selection before writing
            Actor actor = BukkitAdapter.adapt(player);
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(actor);

            if (session == null || session.getSelectionWorld() == null) {
                player.sendMessage(ChatColor.RED + "No WorldEdit selection found! Select an area first.");
                return;
            }

            Region region = session.getSelection(session.getSelectionWorld());

            // Write schematic (after validation)
            SchematicProcessor processor = SchematicProcessor.newSchematicProcessor(plugin.getWorldEdit(), arena.getId(), schematicFolder);
            processor.write(player);

            // Get min/max coords
            int minX = region.getMinimumPoint().getBlockX();
            int minY = region.getMinimumPoint().getBlockY();
            int minZ = region.getMinimumPoint().getBlockZ();
            int maxX = region.getMaximumPoint().getBlockX();
            int maxY = region.getMaximumPoint().getBlockY();
            int maxZ = region.getMaximumPoint().getBlockZ();

            // Paste location (player position)
            Location loc = player.getLocation();
            int pasteX = loc.getBlockX();
            int pasteY = loc.getBlockY();
            int pasteZ = loc.getBlockZ();

            // Create/update section
            ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
            if (arenasSection == null) arenasSection = config.createSection("arenas");

            ConfigurationSection section = arenasSection.getConfigurationSection(arena.getId());
            if (section == null) section = arenasSection.createSection(arena.getId());

            section.set("hardcore", arena.isHardcore());
            section.set("lobby-spawn", formatLocation(arena.getLSpawn()));
            section.set("king-spawn", formatLocation(arena.getKingSpawn()));
            section.set("defenders-spawn", formatLocation(arena.getDefendersSpawn()));
            section.set("attackers-spawn", formatLocation(arena.getAttackersSpawn()));

            // Keep your previous defaults unless you later wire these from arena/timer config
            section.set("auto-start", 60);
            section.set("game-timer", 300);

            section.set("min-players", arena.getMin());
            section.set("max-players", arena.getMax());

            section.set("minX", minX);
            section.set("maxX", maxX);
            section.set("minY", minY);
            section.set("maxY", maxY);
            section.set("minZ", minZ);
            section.set("maxZ", maxZ);

            section.set("pasteX", pasteX);
            section.set("pasteY", pasteY);
            section.set("pasteZ", pasteZ);

            config.save(configFile);
            plugin.getArenaResetManager().reload();

            player.sendMessage(ChatColor.GREEN + "Arena saved successfully!");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error saving arena! Make sure you have a valid selection.");
            e.printStackTrace();
        }
    }
}
