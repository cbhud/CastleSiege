package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.ThreadLocalRandom;

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

    public void removeCustomZombie(Arena arena) {
        Location fireworkLoc = arena.getKingSpawn(); // fallback

        for (LivingEntity entity : arena.getKingSpawn().getWorld().getLivingEntities()) {
            if (entity instanceof Zombie && entity.getCustomName() != null && entity.getCustomName().contains("King")) {
                fireworkLoc = entity.getLocation().clone();
                entity.remove();
                break; // only one king
            }
        }

        // 🎆 Celebrate defenders win (3–4 seconds)
        spawnWinFireworks(fireworkLoc);
    }

    private void spawnWinFireworks(Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        final int[] ticks = {0};
        final int durationTicks = 80; // 4 seconds
        final int period = 10;        // every 0.5 sec

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (ticks[0] >= durationTicks) {
                task.cancel();
                return;
            }
            ticks[0] += period;

            // spawn 1 firework each burst
            Firework fw = loc.getWorld().spawn(loc.clone().add(0, 0.2, 0), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();

            FireworkEffect.Type type = switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0 -> FireworkEffect.Type.BALL;
                case 1 -> FireworkEffect.Type.BALL_LARGE;
                default -> FireworkEffect.Type.STAR;
            };

            FireworkEffect effect = FireworkEffect.builder()
                    .with(type)
                    .withColor(
                            Color.RED, Color.GREEN, Color.WHITE,
                            Color.AQUA, Color.YELLOW
                    )
                    .withFlicker()
                    .withTrail()
                    .build();

            meta.clearEffects();
            meta.addEffect(effect);
            meta.setPower(1); // small
            fw.setFireworkMeta(meta);

            // detonate quickly so it looks like a celebration "pop" at the spot
            Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 8L);

        }, 0L, period);
    }


    @EventHandler
    public void onZombieDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.getCustomName() == null || !zombie.getCustomName().contains("King")) return;

        event.getDrops().clear();

        playDragonLikeDeath(zombie.getLocation());

        Player killer = zombie.getKiller();
        if (killer != null) {
            plugin.getArenaManager().getArenaByPlayer(killer.getUniqueId()).endGame();
        }

    }

    private void playDragonLikeDeath(Location center) {
        World w = center.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.EXPLOSION_HUGE, center, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 0.5, 0),
                80, 1.0, 0.6, 1.0, 0.02);

        final Location base = center.clone().add(0, 0.2, 0);

        final int[] taskId = new int[1];
        final int[] ticks = new int[]{0};
        final double[] angle = new double[]{0.0};

        taskId[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0] += 2;
            angle[0] += 0.35;

            if (ticks[0] >= 80) {
                w.spawnParticle(Particle.EXPLOSION_LARGE, base, 2, 0.2, 0.2, 0.2, 0.0);
                w.spawnParticle(Particle.END_ROD, base.clone().add(0, 1.5, 0),
                        18, 0.6, 0.6, 0.6, 0.02);
                Bukkit.getScheduler().cancelTask(taskId[0]);
                return;
            }

            double y = ticks[0] * 0.03;
            double r = 1.1 - (ticks[0] * 0.01);
            Location p = base.clone().add(Math.cos(angle[0]) * r, y, Math.sin(angle[0]) * r);

            w.spawnParticle(Particle.PORTAL, p, 10, 0.05, 0.05, 0.05, 0.35);
            w.spawnParticle(Particle.END_ROD, p, 2, 0.02, 0.02, 0.02, 0.0);

            if (ticks[0] % 10 == 0) {
                for (int i = 0; i < 10; i++) {
                    Location streak = base.clone().add(
                            ThreadLocalRandom.current().nextDouble(-2.0, 2.0),
                            ThreadLocalRandom.current().nextDouble(0.5, 3.0),
                            ThreadLocalRandom.current().nextDouble(-2.0, 2.0)
                    );
                    w.spawnParticle(Particle.END_ROD, streak, 3, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }, 0L, 2L).getTaskId();
    }





}