package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MobManager implements Listener {

    private final CastleSiege plugin;
    private Zombie kingZombie;
    private final double TNT_DAMAGE;
    private String kingName;

    public MobManager(CastleSiege plugin) {
        this.plugin = plugin;
        TNT_DAMAGE = plugin.getConfigManager().getConfig().getDouble("tntDamage", 3);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        kingName = plugin.getConfigManager().getKingName();
    }

    public void spawnCustomMob(Location l) {

        if (l == null) {
            return;
        }

        kingZombie = (Zombie) l.getWorld().spawnEntity(l, EntityType.ZOMBIE);

        kingZombie.setCustomNameVisible(true);
        kingZombie.setCustomName("§6§lKing " + kingName);

        kingZombie.setAI(false);
        kingZombie.setSilent(true);
        kingZombie.setCanPickupItems(false);
        kingZombie.setRemoveWhenFarAway(false);
        kingZombie.setAdult();
        double maxHealth = plugin.getConfigManager().getKingHealth();
        kingZombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        kingZombie.setHealth(maxHealth);

        kingZombie.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
    }

    public double getZombieHealth(Zombie zombie) {
        if (isKingZombie(zombie)) {
            return Math.round(zombie.getHealth());
        }
        return 0.0;
    }

    private boolean isKingZombie(Zombie zombie) {
        return zombie != null && zombie == kingZombie;
    }

    public Zombie getKingZombie(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Zombie && entity.getCustomName() != null) {
                return kingZombie;
            }
        }
        return null;
    }

    public void removeCustomZombie(Arena arena) {
        for (LivingEntity entity : arena.getKingSpawn().getWorld().getLivingEntities()) {
            if (entity instanceof Zombie && entity.getCustomName() != null && entity.getCustomName().contains("King")) {
                entity.remove();
            }
        }
    }


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof TNTPrimed) {

            if (event.getEntity() instanceof Zombie) {
                Zombie zombie = (Zombie) event.getEntity();
                if (zombie.getCustomName() != null && zombie.getCustomName().contains("King")) {
                    event.setCancelled(true);
                }
            }

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                event.setDamage(TNT_DAMAGE);
            }


        }

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Zombie) {
            Player damager = (Player) event.getDamager();
            if (plugin.getPlayerManager().getPlayerState(damager.getPlayer()) != PlayerState.PLAYING){
                return;
            }
                Team damagerTeam = plugin.getArenaManager().getArenaByPlayer(damager.getUniqueId()).getTeam(damager);

            // Cancel event only if the damager is a Defender and the damaged entity is a Zombie
            if (damagerTeam == Team.Defenders) {
                event.setCancelled(true);
            }
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();

            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                Team shooterTeam = plugin.getArenaManager().getArenaByPlayer(shooter.getUniqueId()).getTeam(shooter);

                // Cancel event only if the shooter is a Defender and the damaged entity is a Zombie
                if (shooterTeam == Team.Defenders && event.getEntity() instanceof Zombie) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onZombieDeath(final EntityDeathEvent event) {
        final Player player = event.getEntity().getKiller();
        if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().contains("King") && event.getEntity() instanceof Zombie) {
            event.getDrops().clear();
            plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).endGame();
        }
    }

}