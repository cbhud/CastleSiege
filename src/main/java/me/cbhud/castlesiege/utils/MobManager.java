package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.ThreadLocalRandom;

public class MobManager implements Listener {

    private final CastleSiege plugin;
    private Zombie kingZombie;
    private final double TNT_DAMAGE;
    private final String kingName;

    public MobManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.TNT_DAMAGE = plugin.getConfigManager().getConfig().getDouble("tntDamage", 3);
        this.kingName = plugin.getConfigManager().getKingName();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void spawnCustomMob(Location l) {
        if (l == null) return;
        World w = l.getWorld();
        if (w == null) return;

        Entity spawned = w.spawnEntity(l, EntityType.ZOMBIE);
        if (!(spawned instanceof Zombie z)) return;

        kingZombie = z;

        kingZombie.setCustomNameVisible(true);
        kingZombie.setCustomName("§6§lKing " + kingName);

        kingZombie.setAI(false);
        kingZombie.setSilent(true);
        kingZombie.setCanPickupItems(false);
        kingZombie.setRemoveWhenFarAway(false);
        kingZombie.setAdult();

        double maxHealth = plugin.getConfigManager().getKingHealth();

        AttributeInstance attr = kingZombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHealth);
        }
        kingZombie.setHealth(maxHealth);

        EntityEquipment eq = kingZombie.getEquipment();
        if (eq != null) {
            eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        }
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

    private boolean looksLikeKing(Zombie z) {
        if (z == null) return false;
        String name = z.getCustomName();
        return name != null && name.contains("King");
    }

    /**
     * ✅ FIX #1: Return the actual king zombie.
     * - If stored kingZombie is valid and in this world, return it.
     * - Otherwise scan zombies in the given world and pick the one named "King".
     */
    public Zombie getKingZombie(World world) {
        if (world == null) return null;

        if (kingZombie != null && kingZombie.isValid() && !kingZombie.isDead()) {
            if (kingZombie.getWorld().equals(world)) {
                return kingZombie;
            }
        }

        // Re-find by scanning zombies only (not all entities)
        for (Zombie z : world.getEntitiesByClass(Zombie.class)) {
            if (looksLikeKing(z)) {
                kingZombie = z; // rebind reference for future calls
                return z;
            }
        }

        return null;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // ✅ TNT logic unchanged (as requested)
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

        // Player melee vs Zombie
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Zombie) {

            if (plugin.getPlayerManager().getPlayerState(damager) != PlayerState.PLAYING) {
                return;
            }

            // ✅ FIX #3: null-safe arena lookup
            Arena arena = plugin.getArenaManager().getArenaByPlayer(damager.getUniqueId());
            if (arena == null) return;

            Team damagerTeam = arena.getTeam(damager);
            if (damagerTeam == Team.Defenders) {
                event.setCancelled(true);
            }
            return;
        }

        // Projectile vs Zombie
        if (event.getDamager() instanceof Projectile projectile && event.getEntity() instanceof Zombie) {

            if (!(projectile.getShooter() instanceof Player shooter)) return;

            // ✅ FIX #3: null-safe arena lookup
            Arena arena = plugin.getArenaManager().getArenaByPlayer(shooter.getUniqueId());
            if (arena == null) return;

            Team shooterTeam = arena.getTeam(shooter);
            if (shooterTeam == Team.Defenders) {
                event.setCancelled(true);
            }
        }
    }

    public void removeCustomZombie(Arena arena) {
        if (arena == null) return;

        Location kingSpawn = arena.getKingSpawn();
        Location fireworkLoc = kingSpawn;

        if (kingSpawn != null && kingSpawn.getWorld() != null) {
            for (LivingEntity entity : kingSpawn.getWorld().getLivingEntities()) {
                if (entity instanceof Zombie z && looksLikeKing(z)) {
                    fireworkLoc = z.getLocation().clone();
                    z.remove();
                    break;
                }
            }
        }

        spawnWinFireworks(fireworkLoc);
    }

    private void spawnWinFireworks(Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        final int[] ticks = {0};
        final int durationTicks = 80;
        final int period = 10;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (ticks[0] >= durationTicks) {
                task.cancel();
                return;
            }
            ticks[0] += period;

            Firework fw = loc.getWorld().spawn(loc.clone().add(0, 0.2, 0), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();

            FireworkEffect.Type type = switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0 -> FireworkEffect.Type.BALL;
                case 1 -> FireworkEffect.Type.BALL_LARGE;
                default -> FireworkEffect.Type.STAR;
            };

            FireworkEffect effect = FireworkEffect.builder()
                    .with(type)
                    .withColor(Color.RED, Color.GREEN, Color.WHITE, Color.AQUA, Color.YELLOW)
                    .withFlicker()
                    .withTrail()
                    .build();

            meta.clearEffects();
            meta.addEffect(effect);
            meta.setPower(1);
            fw.setFireworkMeta(meta);

            Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 8L);

        }, 0L, period);
    }

    @EventHandler
    public void onZombieDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!looksLikeKing(zombie)) return;

        event.getDrops().clear();

        // Keep effects (but never allow them to prevent endGame)
        try {
            playDragonLikeDeath(zombie.getLocation());
        } catch (Exception ex) {
            plugin.getLogger().warning("King death effect failed: " + ex.getMessage());
        }

        // ✅ FIX #4: end game safely
        Arena arena = null;

        Player killer = zombie.getKiller();
        if (killer != null) {
            arena = plugin.getArenaManager().getArenaByPlayer(killer.getUniqueId());
        }

        // Fallback: 1 arena per world
        if (arena == null) {
            World w = zombie.getWorld();
            String worldName = (w != null) ? w.getName() : null;

            if (worldName != null) {
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    if (worldName.equalsIgnoreCase(a.getWorldName())) {
                        arena = a;
                        break;
                    }
                }
            }
        }

        if (arena != null) {
            arena.endGame();
        }
    }

    private void playDragonLikeDeath(Location center) {
        World w = center.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.EXPLOSION_HUGE, center, 1, 0, 0, 0, 0);

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
