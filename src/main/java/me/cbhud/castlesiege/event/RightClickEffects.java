package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import me.cbhud.castlesiege.kit.CustomItem;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RightClickEffects implements Listener {

    private final CastleSiege plugin;
    private final Random rand = new Random();
    private static final int EFFECT_DURATION = 100;
    private final Map<UUID, Long> attackCooldowns = new HashMap<>();
    private final Map<UUID, Long> supportCooldowns = new HashMap<>();
    private long ATTACK_COOLDOWN;
    private long SUPPORT_COOLDOWN;

    public RightClickEffects(CastleSiege plugin) {
        this.plugin = plugin;
        this.ATTACK_COOLDOWN = plugin.getConfigManager().getConfig().getInt("wizardAttackSpellCooldown", 30) * 1000;
        this.SUPPORT_COOLDOWN = plugin.getConfigManager().getConfig().getInt("wizardSupportSpellCooldown", 30) * 1000;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack clickedItem = event.getItem();

        if (clickedItem == null) {
            return;
        }

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());

        if (clickedItem.getType() == Material.GOLDEN_AXE && arena.getState() == ArenaState.IN_GAME) {
            throwAxe(player);
            player.getInventory().remove(clickedItem);
            event.setCancelled(true);
            return;
        }

        // Example: If player right-clicks with a blaze rod (just as example for wizard attack spell)
        if (clickedItem.getType() == Material.BLAZE_ROD) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            Long lastUsed = attackCooldowns.getOrDefault(uuid, 0L);
            if (now - lastUsed < ATTACK_COOLDOWN) {
                int seconds = (int) ((ATTACK_COOLDOWN - (now - lastUsed)) / 1000);
                String msg = plugin.getMsg().getGuiMessage("customitem-cooldown").get(0);
                msg = msg.replace("{seconds}", String.valueOf(seconds));
                player.sendMessage(msg);
                return;
            }
            attackCooldowns.put(uuid, now);

            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 10 &&
                        plugin.getArenaManager().getArenaByPlayer(uuid).getTeam(nearbyPlayer) == Team.Attackers) {
                    applyRandomEffect(nearbyPlayer);
                    player.sendMessage(plugin.getMsg().getGuiMessage("wizardAttackSpell").get(0));
                }
            }

            event.setCancelled(true);
            return;
        }

// Example: If player right-clicks with a stick (example support spell)
        if (clickedItem.getType() == Material.STICK) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            Long lastUsed = supportCooldowns.getOrDefault(uuid, 0L);
            if (now - lastUsed < SUPPORT_COOLDOWN) {
                int seconds = (int) ((SUPPORT_COOLDOWN - (now - lastUsed)) / 1000);
                String msg = plugin.getMsg().getGuiMessage("customitem-cooldown").get(0);
                msg = msg.replace("{seconds}", String.valueOf(seconds));
                player.sendMessage(msg);
                return;
            }
            supportCooldowns.put(uuid, now);

            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 10 &&
                        plugin.getArenaManager().getArenaByPlayer(uuid).getTeam(nearbyPlayer) == Team.Defenders) {
                    applyRandomSupportEffect(nearbyPlayer);
                    player.sendMessage(plugin.getMsg().getGuiMessage("wizardSupportSpell").get(0));
                }
            }

            event.setCancelled(true);
            return;
        }

        if (clickedItem.getType() == Material.EMERALD) {
            plugin.getArenaSelector().open(player);
            return;
        }

        if (clickedItem.getType() == Material.CLOCK && arena != null && arena.getState() == ArenaState.WAITING) {
            plugin.getTeamSelector().open(player);
            return;
        }

        if (clickedItem.getType() == Material.NETHER_STAR && arena != null && arena.getState() == ArenaState.WAITING) {
            plugin.getKitSelector().open(player);
            return;
        }

        if (arena == null || arena.getState() == ArenaState.ENDED) {
            return;
        }

        if (clickedItem.getType() == Material.RED_DYE) {
            player.sendMessage(plugin.getMsg().getMessage("leaveArena", player).get(0));
            arena.removePlayer(player);
            return;
        }

        useSpecialItem(player, clickedItem);

    }

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    private boolean useSpecialItem(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Optional<CustomItem> match = plugin.getItemManager().matchCustomItem(item);
        if (match.isEmpty()) return false;

        CustomItem customItem = match.get();

        Material mat = item.getType();
        if (mat == Material.MUSHROOM_STEW) {
            removeOneItem(player, item);

            for (PotionEffect effect : customItem.getEffects()) {
                player.addPotionEffect(effect);
            }

            return true;
        }

        long cooldown = customItem.getCooldown();

        if (cooldown > 0) {
            long lastUsed = cooldowns
                    .computeIfAbsent(uuid, k -> new HashMap<>())
                    .getOrDefault(customItem.getId(), 0L);

            if (now - lastUsed < cooldown) {
                int seconds = (int) ((cooldown - (now - lastUsed)) / 1000);
                String msg = plugin.getMsg().getGuiMessage("customitem-cooldown").get(0);
                msg = msg.replace("{seconds}", String.valueOf(seconds));
                player.sendMessage(msg);
                return false;
            }

            cooldowns.get(uuid).put(customItem.getId(), now);
        }

        for (PotionEffect effect : customItem.getEffects()) {
            player.addPotionEffect(effect);
        }

        return true;
    }

    private void removeOneItem(Player player, ItemStack item) {
        ItemStack clone = item.clone();
        clone.setAmount(1);
        player.getInventory().removeItem(clone);
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

    private void applyRandomEffect(Player player) {
        PotionEffectType[] effects = {PotionEffectType.POISON, PotionEffectType.SLOW, PotionEffectType.BLINDNESS};
        PotionEffectType effect = effects[rand.nextInt(effects.length)];
        player.addPotionEffect(new PotionEffect(effect, EFFECT_DURATION, 1));
    }

    private void applyRandomSupportEffect(Player player) {
        PotionEffectType[] effects = {PotionEffectType.REGENERATION, PotionEffectType.ABSORPTION, PotionEffectType.SPEED};
        PotionEffectType effect = effects[rand.nextInt(effects.length)];
        player.addPotionEffect(new PotionEffect(effect, EFFECT_DURATION, 1));
    }
}
