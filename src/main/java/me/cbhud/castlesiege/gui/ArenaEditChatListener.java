package me.cbhud.castlesiege.gui;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaEditChatListener implements Listener {

    public enum PendingKey {
        AUTO_START("auto-start", 0, 3600),
        GAME_TIMER("game-timer", 5, 7200),
        MIN_PLAYERS("min-players", 1, 200),
        MAX_PLAYERS("max-players", 1, 500);

        public final String pathKey;
        public final int min;
        public final int max;

        PendingKey(String pathKey, int min, int max) {
            this.pathKey = pathKey;
            this.min = min;
            this.max = max;
        }
    }

    public static class PendingEdit {
        public final String arenaId;
        public final PendingKey key;

        public PendingEdit(String arenaId, PendingKey key) {
            this.arenaId = arenaId;
            this.key = key;
        }
    }

    private final CastleSiege plugin;
    private final Map<UUID, PendingEdit> pending = new ConcurrentHashMap<>();

    public ArenaEditChatListener(CastleSiege plugin) {
        this.plugin = plugin;
    }

    public void begin(Player player, String arenaId, PendingKey key) {
        pending.put(player.getUniqueId(), new PendingEdit(arenaId, key));
        player.sendMessage(ChatColor.YELLOW + "Type a number for " + key.pathKey + " in chat. Type 'cancel' to abort.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        PendingEdit edit = pending.get(player.getUniqueId());
        if (edit == null) return;

        e.setCancelled(true);

        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            pending.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.GRAY + "Edit cancelled.");
                new ArenaSettingsGui(plugin, this, edit.arenaId).open(player);
            });
            return;
        }

        int value;
        try {
            value = Integer.parseInt(msg);
        } catch (NumberFormatException ex) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "Please type a valid number (or 'cancel').")
            );
            return;
        }

        if (value < edit.key.min || value > edit.key.max) {
            int v = value;
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "Value out of range (" + edit.key.min + " - " + edit.key.max + "). You typed: " + v)
            );
            return;
        }

        pending.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean ok = ArenaYmlEditor.setInt(plugin, edit.arenaId, edit.key.pathKey, value);
            if (!ok) {
                player.sendMessage(ChatColor.RED + "Failed to save arenas.yml for arena '" + edit.arenaId + "'.");
                return;
            }

            // Reload arenas so timer/hardcore changes actually apply
            plugin.getArenaManager().reload();

            player.sendMessage(ChatColor.GREEN + "Updated " + edit.key.pathKey + " to " + value + " for arena '" + edit.arenaId + "'.");
            new ArenaSettingsGui(plugin, this, edit.arenaId).open(player);
        });
    }
}
