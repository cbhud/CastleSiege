package me.cbhud.castlesiege.player;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.cbhud.castlesiege.player.PlayerState.*;

public class PlayerManager {

    private final CastleSiege plugin;

    // UUID -> state (no Player refs = no leaks)
    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    public PlayerManager(CastleSiege plugin) {
        this.plugin = plugin;
    }

    public PlayerState getPlayerState(Player player) {
        if (player == null) return IN_LOBBY;
        return playerStates.getOrDefault(player.getUniqueId(), IN_LOBBY);
    }

    public PlayerState getPlayerState(UUID uuid) {
        if (uuid == null) return IN_LOBBY;
        return playerStates.getOrDefault(uuid, IN_LOBBY);
    }

    /** Call this on quit to avoid stale entries. */
    public void clearPlayerState(Player player) {
        if (player == null) return;
        playerStates.remove(player.getUniqueId());
    }

    public void setPlayerAsPlaying(Player player) {
        if (player == null) return;
        runSync(() -> {
            if (!player.isOnline()) return;
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setLevel(0);
            clearPotionEffects(player);
            playerStates.put(player.getUniqueId(), PLAYING);
        });
    }

    public void setPlayerAsLobby(Player player) {
        if (player == null) return;
        runSync(() -> {
            if (!player.isOnline()) return;

            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20);
            player.setLevel(0);
            clearPotionEffects(player);

            player.getInventory().setItem(4, ItemBuilder.from(Material.EMERALD)
                    .name(Component.text(plugin.getMsg().getMessage("selectArenaItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectArenaItem", player).get(1)))
                    .build());

            playerStates.put(player.getUniqueId(), IN_LOBBY);

            if (loadLobbyLocation() == null) {
                Bukkit.broadcastMessage("§cThe Main lobby spawn has not been set, please set it or notify the admin");
                Bukkit.broadcastMessage("§cCommand: /cse setlobby");
            } else {
                player.teleport(loadLobbyLocation());
            }

            plugin.getScoreboardManager().setupScoreboard(player);
        });
    }

    public void setPlayerAsWaiting(Player player) {
        if (player == null) return;
        runSync(() -> {
            if (!player.isOnline()) return;

            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setLevel(0);
            clearPotionEffects(player);

            player.getInventory().setItem(3, ItemBuilder.from(Material.CLOCK)
                    .name(Component.text(plugin.getMsg().getMessage("selectTeamItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectTeamItem", player).get(1)))
                    .build());

            player.getInventory().setItem(5, ItemBuilder.from(Material.NETHER_STAR)
                    .name(Component.text(plugin.getMsg().getMessage("selectKitItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectKitItem", player).get(1)))
                    .build());

            player.getInventory().setItem(8, ItemBuilder.from(Material.RED_DYE)
                    .name(Component.text(plugin.getMsg().getMessage("leaveArenaItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("leaveArenaItem", player).get(1)))
                    .build());

            playerStates.put(player.getUniqueId(), WAITING);

            plugin.getScoreboardManager().updateScoreboard(player, "pre-game");
        });
    }

    public void setPlayerAsSpectating(Player player) {
        if (player == null) return;
        // keep your 1 tick delay (often avoids weirdness with recent respawns)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            playerStates.put(player.getUniqueId(), SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }, 1L);
    }

    /* ---------------- helpers ---------------- */

    private void runSync(Runnable r) {
        if (Bukkit.isPrimaryThread()) r.run();
        else Bukkit.getScheduler().runTask(plugin, r);
    }

    private void clearPotionEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private Location loadLobbyLocation() {
        File file = new File(plugin.getDataFolder(), "lobby-location.yml");
        if (!file.exists()) {
            return null;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String worldName = cfg.getString("lobby.world");
        if (worldName == null || worldName.isBlank() || !cfg.contains("lobby.x")) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = cfg.getDouble("lobby.x");
        double y = cfg.getDouble("lobby.y");
        double z = cfg.getDouble("lobby.z");
        float yaw = (float) cfg.getDouble("lobby.yaw", 0.0);
        float pitch = (float) cfg.getDouble("lobby.pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }


}
