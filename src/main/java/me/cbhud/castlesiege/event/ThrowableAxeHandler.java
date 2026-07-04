package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

class ThrowableAxeHandler {

    private final CastleSiege plugin;

    ThrowableAxeHandler(CastleSiege plugin) {
        this.plugin = plugin;
    }

    boolean handle(PlayerInteractEvent event, Arena arena) {
        if (event.getItem().getType() != Material.GOLDEN_AXE || arena.getState() != ArenaState.IN_GAME) {
            return false;
        }

        throwAxe(event.getPlayer());
        event.getPlayer().getInventory().remove(event.getItem());
        event.setCancelled(true);
        return true;
    }

    private void throwAxe(Player player) {
        try {
            Item axe = player.getWorld().dropItem(player.getEyeLocation(), player.getInventory().getItemInMainHand());
            axe.setVelocity(player.getEyeLocation().getDirection().multiply(1.1));
            player.getInventory().getItemInMainHand().setAmount(0);

            new BukkitRunnable() {
                public void run() {
                    for (Entity ent : axe.getNearbyEntities(0.5, 0.5, 0.5)) {
                        if (ent instanceof LivingEntity && ent != player) {
                            LivingEntity target = (LivingEntity) ent;
                            target.damage(2.5);
                            axe.setVelocity(new Vector(0, 0, 0));
                            this.cancel();
                            axe.remove();
                        }
                    }
                    if (axe.isOnGround()) {
                        axe.setVelocity(new Vector(0, 0, 0));
                        axe.remove();
                        this.cancel();
                    }
                }
            }.runTaskTimer(this.plugin, 0L, 1L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
