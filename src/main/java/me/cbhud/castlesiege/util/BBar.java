package me.cbhud.castlesiege.util;

import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;

public class BBar {

    private BukkitAudiences adventure;
    private BossBar bar;
    private final CastleSiege plugin;

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Adventure failed!");
        } else {
            return this.adventure;
        }
    }

    public BBar(CastleSiege plugin) {
        this.plugin = plugin;
        this.adventure = BukkitAudiences.create(plugin);
    }

    public void removeBar() {
        if (this.adventure != null) {
            this.adventure.players().hideBossBar(this.bar);
            this.adventure = null;
        }
    }

    public void showZombieHealthBar(Zombie zombie) {
        // Use Component.text to create the title as a Component
        Component title = Component.text(ChatColor.GOLD + zombie.getCustomName()); // Appending the health part

        // Create a boss bar with the initial title and properties
        this.bar = BossBar.bossBar(
                title,
                (float) (zombie.getHealth() / zombie.getMaxHealth()),
                Color.RED,
                Overlay.PROGRESS
        );

        this.adventure().players().showBossBar(this.bar);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!zombie.isValid() || zombie.isDead()) {
                    adventure().players().hideBossBar(bar);
                    cancel();
                    return;
                }

                // Update the progress (health percentage)
                bar.progress((float) (zombie.getHealth() / zombie.getMaxHealth()));
            }
        }.runTaskTimer(this.plugin, 0L, 20L); // Update every 20 ticks (1 second)
    }
}
