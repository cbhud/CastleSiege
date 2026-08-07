package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import me.cbhud.castlesiege.kit.CustomItem;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AbilityHandler implements Listener {

    private final CastleSiege plugin;
    private final CustomItemInteractionHandler customItemHandler;

    // State tracking
    private final Set<UUID> switcherProjectiles = new HashSet<>();
    private final Set<UUID> grappleFallDamageImmune = new HashSet<>();
    private final Map<UUID, Location> timeWarpLocations = new HashMap<>();
    private final Set<UUID> stunnedPlayers = new HashSet<>();
    private final Set<UUID> antiBlockPlayers = new HashSet<>();
    private final Set<UUID> rottenEggProjectiles = new HashSet<>();

    public AbilityHandler(CastleSiege plugin, CustomItemInteractionHandler customItemHandler) {
        this.plugin = plugin;
        this.customItemHandler = customItemHandler;
    }

    public boolean handleInteract(PlayerInteractEvent event, CustomItem customItem) {
        Player player = event.getPlayer();
        if (!isPlayerPlaying(player)) return false;

        String ability = customItem.getAbility();

        if ("switcher-snowball".equals(ability)) {
            if (!customItemHandler.canUse(player, customItem)) return false;
            // Launch is handled by vanilla, tracked in ProjectileLaunchEvent
            return true;
        }

        if ("grappling-hook".equals(ability)) {
            // Handled in PlayerFishEvent
            return false;
        }

        if ("time-warp-pearl".equals(ability)) {
            if (!customItemHandler.canUse(player, customItem)) {
                event.setCancelled(true);
                return false;
            }
            int seconds = customItem.getConfigSection() != null ? customItem.getConfigSection().getInt("seconds", 5) : 5;
            
            String msg = plugin.getMsg().getGuiMessage("ability-timewarp-used").get(0).replace("{seconds}", String.valueOf(seconds));
            player.sendMessage(msg);
            
            timeWarpLocations.put(player.getUniqueId(), player.getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            return true;
        }

        if ("rotten-egg".equals(ability)) {
            if (!customItemHandler.canUse(player, customItem)) return false;
            return true;
        }

        // Target abilities (stunner, antiblock, bamboozle) are handled via left-click (EntityDamageByEntityEvent)
        return false;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player shooter = (Player) event.getEntity().getShooter();
        
        if (stunnedPlayers.contains(shooter.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (!isPlayerPlaying(shooter)) return;

        ItemStack item = shooter.getInventory().getItemInMainHand();
        Optional<CustomItem> match = plugin.getItemManager().matchCustomItem(item);
        if (match.isEmpty()) return;

        CustomItem customItem = match.get();
        String ability = customItem.getAbility();

        if ("switcher-snowball".equals(ability) && event.getEntity() instanceof Snowball) {
            switcherProjectiles.add(event.getEntity().getUniqueId());
        } else if ("rotten-egg".equals(ability) && event.getEntity() instanceof Egg) {
            rottenEggProjectiles.add(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player shooter = (Player) event.getEntity().getShooter();
        if (!isPlayerPlaying(shooter)) return;

        UUID projId = event.getEntity().getUniqueId();

        // Time-Warp Pearl
        if (event.getEntity() instanceof EnderPearl) {
            if (timeWarpLocations.containsKey(shooter.getUniqueId())) {
                CustomItem match = getCustomItemByAbility("time-warp-pearl");
                int seconds = 5;
                if (match != null && match.getConfigSection() != null) {
                    seconds = match.getConfigSection().getInt("seconds", 5);
                }

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (timeWarpLocations.containsKey(shooter.getUniqueId()) && isPlayerPlaying(shooter)) {
                        shooter.teleport(timeWarpLocations.remove(shooter.getUniqueId()));
                        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    }
                }, seconds * 20L);
            }
        }

        // Switcher Snowball
        if (event.getEntity() instanceof Snowball && switcherProjectiles.contains(projId)) {
            switcherProjectiles.remove(projId);
            if (event.getHitEntity() instanceof Player) {
                Player hit = (Player) event.getHitEntity();
                if (isPlayerPlaying(hit) && !areTeammates(shooter, hit)) {
                    Location hitLoc = hit.getLocation().clone();
                    Location shooterLoc = shooter.getLocation().clone();
                    hit.teleport(shooterLoc);
                    shooter.teleport(hitLoc);
                    
                    shooter.sendMessage(plugin.getMsg().getGuiMessage("ability-switcher-used").get(0).replace("{target}", hit.getName()));
                    hit.sendMessage(plugin.getMsg().getGuiMessage("ability-switcher-hit").get(0).replace("{player}", shooter.getName()));
                    
                    shooter.playSound(shooterLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    hit.playSound(hitLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                }
            }
        }

        // Rotten Egg
        if (event.getEntity() instanceof Egg && rottenEggProjectiles.contains(projId)) {
            rottenEggProjectiles.remove(projId);
            if (event.getHitEntity() instanceof Player) {
                Player hit = (Player) event.getHitEntity();
                if (isPlayerPlaying(hit) && !areTeammates(shooter, hit)) {
                    CustomItem match = getCustomItemByAbility("rotten-egg");
                    if (match != null) {
                        customItemHandler.applyEffects(hit, match);
                        shooter.sendMessage(plugin.getMsg().getGuiMessage("ability-rottenegg-used").get(0).replace("{target}", hit.getName()));
                        hit.sendMessage(plugin.getMsg().getGuiMessage("ability-rottenegg-hit").get(0).replace("{player}", shooter.getName()));
                        shooter.playSound(shooter.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1f, 1f);
                        hit.playSound(hit.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1f, 1f);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerPlaying(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<CustomItem> match = plugin.getItemManager().matchCustomItem(item);
        if (match.isEmpty() || !"grappling-hook".equals(match.get().getAbility())) return;

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (!customItemHandler.canUse(player, match.get())) {
                event.setCancelled(true);
                return;
            }
            
            // Adjust throwing speed
            Vector v = event.getHook().getVelocity();
            v.setX(v.getX() * 1.3);
            v.setY(v.getY() * 1.5);
            v.setZ(v.getZ() * 1.3);
            event.getHook().setVelocity(v);
            
        } else if (event.getState() == PlayerFishEvent.State.IN_GROUND || 
                   event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY ||
                   event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT ||
                   event.getState() == PlayerFishEvent.State.REEL_IN) {
            
            Location loc = player.getLocation();
            Location hookLoc = event.getHook().getLocation();
            
            grappleFallDamageImmune.add(player.getUniqueId());
            
            double g = -0.08;
            double d = hookLoc.distance(loc);
            double minBoost = 0.5F;
            double calculatedY = (((1.0F + d) * (hookLoc.getY() - loc.getY()) / d - g * d) / 25.0F);
            final double finalY = calculatedY > 0.0F ? calculatedY + minBoost : calculatedY - minBoost;
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Vector v0 = player.getVelocity().clone().add(new Vector(0.0, 0.25, 0.0));
                player.setVelocity(v0);
            });
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Vector v1 = player.getVelocity().clone();
                v1.multiply(new Vector(0.3, 0.3, 0.3));
                player.setVelocity(v1);
            }, 1L);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Vector pull = hookLoc.toVector().subtract(player.getLocation().toVector());
                if (pull.lengthSquared() > 0) {
                    pull = pull.normalize();
                    pull.setY(finalY);
                    
                    // Multiply by user requested amounts
                    pull.setX(pull.getX() * 1.5);
                    pull.setY(pull.getY() * 1.2);
                    pull.setZ(pull.getZ() * 1.5);
                    
                    Vector v2 = player.getVelocity().clone();
                    v2.add(pull);
                    player.setVelocity(v2);
                }
            }, 2L);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                item.setDurability((short) 0);
            }, 1L);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player target = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (!isPlayerPlaying(attacker) || !isPlayerPlaying(target)) return;
        if (areTeammates(attacker, target)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        Optional<CustomItem> match = plugin.getItemManager().matchCustomItem(item);
        if (match.isEmpty()) return;

        CustomItem customItem = match.get();
        String ability = customItem.getAbility();

        if ("stunner".equals(ability)) {
            if (!customItemHandler.canUse(attacker, customItem)) return;
            
            if (stunnedPlayers.contains(target.getUniqueId())) {
                attacker.sendMessage(plugin.getMsg().getGuiMessage("ability-stunner-already").get(0));
                return;
            }
            
            int seconds = customItem.getConfigSection() != null ? customItem.getConfigSection().getInt("seconds", 3) : 3;
            stunnedPlayers.add(target.getUniqueId());
            
            attacker.sendMessage(plugin.getMsg().getGuiMessage("ability-stunner-used").get(0).replace("{target}", target.getName()).replace("{seconds}", String.valueOf(seconds)));
            target.sendMessage(plugin.getMsg().getGuiMessage("ability-stunner-hit").get(0).replace("{player}", attacker.getName()).replace("{seconds}", String.valueOf(seconds)));
            target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f);
            
            target.setWalkSpeed(0);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                stunnedPlayers.remove(target.getUniqueId());
                if (target.isOnline()) target.setWalkSpeed(0.2f);
            }, seconds * 20L);
            
            event.setDamage(0); 
        }
        else if ("anti-blockup".equals(ability)) {
            if (!customItemHandler.canUse(attacker, customItem)) return;
            
            int seconds = customItem.getConfigSection() != null ? customItem.getConfigSection().getInt("seconds", 5) : 5;
            antiBlockPlayers.add(target.getUniqueId());
            
            attacker.sendMessage(plugin.getMsg().getGuiMessage("ability-antiblock-used").get(0).replace("{target}", target.getName()).replace("{seconds}", String.valueOf(seconds)));
            target.sendMessage(plugin.getMsg().getGuiMessage("ability-antiblock-hit").get(0).replace("{player}", attacker.getName()).replace("{seconds}", String.valueOf(seconds)));
            target.playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> antiBlockPlayers.remove(target.getUniqueId()), seconds * 20L);
            event.setDamage(0);
        }
        else if ("bamboozle".equals(ability)) {
            if (!customItemHandler.canUse(attacker, customItem)) return;
            
            List<ItemStack> hotbar = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                hotbar.add(target.getInventory().getItem(i));
            }
            Collections.shuffle(hotbar);
            for (int i = 0; i < 9; i++) {
                target.getInventory().setItem(i, hotbar.get(i));
            }
            
            attacker.sendMessage(plugin.getMsg().getGuiMessage("ability-bamboozle-used").get(0).replace("{target}", target.getName()));
            target.sendMessage(plugin.getMsg().getGuiMessage("ability-bamboozle-hit").get(0).replace("{player}", attacker.getName()));
            target.playSound(target.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1f);
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (grappleFallDamageImmune.contains(event.getEntity().getUniqueId())) {
                grappleFallDamageImmune.remove(event.getEntity().getUniqueId());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (stunnedPlayers.contains(event.getPlayer().getUniqueId()) || antiBlockPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (stunnedPlayers.contains(event.getPlayer().getUniqueId()) || antiBlockPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (stunnedPlayers.contains(event.getPlayer().getUniqueId())) {
            if (event.getTo() != null && event.getTo().getY() > event.getFrom().getY()) {
                event.getPlayer().teleport(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        if (rottenEggProjectiles.contains(event.getEgg().getUniqueId())) {
            event.setHatching(false);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanup(event.getEntity().getUniqueId());
        timeWarpLocations.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    private void cleanup(UUID uuid) {
        grappleFallDamageImmune.remove(uuid);
        timeWarpLocations.remove(uuid);
        stunnedPlayers.remove(uuid);
        antiBlockPlayers.remove(uuid);
    }

    private CustomItem getCustomItemByAbility(String ability) {
        for (CustomItem ci : plugin.getItemManager().getCustomItems().values()) {
            if (ability.equals(ci.getAbility())) {
                return ci;
            }
        }
        return null;
    }

    private boolean isPlayerPlaying(Player player) {
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
        return arena != null && arena.getState() == ArenaState.IN_GAME && plugin.getPlayerManager().getPlayerState(player.getUniqueId()) == PlayerState.PLAYING;
    }

    private boolean areTeammates(Player p1, Player p2) {
        Team t1 = plugin.getTeamManager().getTeam(p1);
        Team t2 = plugin.getTeamManager().getTeam(p2);
        return t1 != null && t2 != null && t1 == t2;
    }
}
