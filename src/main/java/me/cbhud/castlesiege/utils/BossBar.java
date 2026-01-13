package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBar {

    private BukkitAudiences adventure;
    private final CastleSiege plugin;
    private boolean enabled;

    // Store individual boss bars and tasks for each player
    private final Map<UUID, net.kyori.adventure.bossbar.BossBar> playerBossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> playerTasks = new HashMap<>();

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            this.adventure = BukkitAudiences.create(plugin);
        }
        return this.adventure;
    }

    public BossBar(CastleSiege plugin) {
        this.plugin = plugin;
        this.adventure = BukkitAudiences.create(plugin);
    }

    public void removeBarForPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        // Hide and remove the boss bar for this specific player
        net.kyori.adventure.bossbar.BossBar bar = playerBossBars.get(playerId);
        if (bar != null) {
            this.adventure().player(player).hideBossBar(bar);
            playerBossBars.remove(playerId);
        }

        // Cancel the task for this player
        BukkitTask task = playerTasks.get(playerId);
        if (task != null) {
            task.cancel();
            playerTasks.remove(playerId);
        }
    }

    public void removeAllBars() {
        // Remove all boss bars and cancel all tasks
        for (UUID playerId : playerBossBars.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                this.adventure().player(player).hideBossBar(playerBossBars.get(playerId));
            }
        }

        for (BukkitTask task : playerTasks.values()) {
            task.cancel();
        }

        playerBossBars.clear();
        playerTasks.clear();
    }

    public void showZombieHealthBarToPlayer(Zombie zombie, Player player) {
        // Check if zombie is valid before creating the boss bar
        if (!zombie.isValid() || zombie.isDead()) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Remove any existing boss bar for this player first
        removeBarForPlayer(player);

        // Use Component.text to create the title as a Component
        Component title = Component.text(ChatColor.GOLD + zombie.getCustomName());

        // Create a new boss bar for this specific player
        net.kyori.adventure.bossbar.BossBar bar = net.kyori.adventure.bossbar.BossBar.bossBar(
                title,
                (float) (zombie.getHealth() / zombie.getMaxHealth()),
                Color.PURPLE,
                Overlay.PROGRESS
        );

        // Store the boss bar for this player
        playerBossBars.put(playerId, bar);

        // Show the boss bar to this specific player
        this.adventure().player(player).showBossBar(bar);

        // Create and store the update task for this player
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player is still online
                Player currentPlayer = plugin.getServer().getPlayer(playerId);
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    cancel();
                    playerBossBars.remove(playerId);
                    playerTasks.remove(playerId);
                    return;
                }

                if (!zombie.isValid() || zombie.isDead()) {
                    adventure().player(currentPlayer).hideBossBar(bar);
                    cancel();
                    playerBossBars.remove(playerId);
                    playerTasks.remove(playerId);
                    return;
                }

                // Update the progress (health percentage)
                bar.progress((float) (zombie.getHealth() / zombie.getMaxHealth()));
            }
        }.runTaskTimer(this.plugin, 0L, 20L);

        playerTasks.put(playerId, task);
    }

    // Your original method - now creates individual boss bars for each player
    public void showZombieHealthBar(Zombie zombie) {
        // This method can now be used in a foreach loop
        // You'll need to pass the player parameter, or modify to get nearby players

        // Example: Show to all online players (you can modify this logic)
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            showZombieHealthBarToPlayer(zombie, player);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setIsEnabled(boolean bossBarEnabled) {
        this.enabled = bossBarEnabled;
    }

    // Cleanup method for when the plugin is disabled
    public void cleanup() {
        removeAllBars();

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
}