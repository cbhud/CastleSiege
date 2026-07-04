package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

class WizardSpellHandler {

    private static final int EFFECT_DURATION = 100;

    private final CastleSiege plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> attackCooldowns = new HashMap<>();
    private final Map<UUID, Long> supportCooldowns = new HashMap<>();
    private final long attackCooldown;
    private final long supportCooldown;

    WizardSpellHandler(CastleSiege plugin) {
        this.plugin = plugin;
        this.attackCooldown = plugin.getConfigManager().getConfig().getInt("wizardAttackSpellCooldown", 30) * 1000L;
        this.supportCooldown = plugin.getConfigManager().getConfig().getInt("wizardSupportSpellCooldown", 30) * 1000L;
    }

    boolean handle(PlayerInteractEvent event, Arena arena) {
        Material type = event.getItem().getType();

        if (type == Material.BLAZE_ROD) {
            castSpell(event, arena, attackCooldowns, attackCooldown, Team.Attackers, true);
            return true;
        }

        if (type == Material.STICK) {
            castSpell(event, arena, supportCooldowns, supportCooldown, Team.Defenders, false);
            return true;
        }

        return false;
    }

    void clearCooldowns(UUID uuid) {
        attackCooldowns.remove(uuid);
        supportCooldowns.remove(uuid);
    }

    private void castSpell(
            PlayerInteractEvent event,
            Arena arena,
            Map<UUID, Long> cooldowns,
            long cooldown,
            Team affectedTeam,
            boolean attackSpell
    ) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long lastUsed = cooldowns.getOrDefault(uuid, 0L);
        if (now - lastUsed < cooldown) {
            int seconds = (int) ((cooldown - (now - lastUsed)) / 1000);
            sendCooldown(player, seconds);
            return;
        }
        cooldowns.put(uuid, now);

        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 10
                    && arena.getTeam(nearbyPlayer) == affectedTeam) {
                if (attackSpell) {
                    applyRandomAttackEffect(nearbyPlayer);
                } else {
                    applyRandomSupportEffect(nearbyPlayer);
                }
                player.sendMessage(plugin.getMsg().getGuiMessage(
                        attackSpell ? "wizardAttackSpell" : "wizardSupportSpell").get(0));
            }
        }

        event.setCancelled(true);
    }

    private void sendCooldown(Player player, int seconds) {
        String msg = plugin.getMsg().getGuiMessage("customitem-cooldown").get(0);
        msg = msg.replace("{seconds}", String.valueOf(seconds));
        player.sendMessage(msg);
    }

    private void applyRandomAttackEffect(Player player) {
        PotionEffectType[] effects = {PotionEffectType.POISON, PotionEffectType.SLOW, PotionEffectType.BLINDNESS};
        PotionEffectType effect = effects[random.nextInt(effects.length)];
        player.addPotionEffect(new PotionEffect(effect, EFFECT_DURATION, 1));
    }

    private void applyRandomSupportEffect(Player player) {
        PotionEffectType[] effects = {PotionEffectType.REGENERATION, PotionEffectType.ABSORPTION, PotionEffectType.SPEED};
        PotionEffectType effect = effects[random.nextInt(effects.length)];
        player.addPotionEffect(new PotionEffect(effect, EFFECT_DURATION, 1));
    }
}
