package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;

import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.kit.KitManager;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class DeathEvent implements Listener {
    private final CastleSiege plugin;

    public DeathEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (plugin.getPlayerManager().getPlayerState(player) != PlayerState.PLAYING) {
            return;
        }

        // Keep your existing logic intact
        event.getDrops().clear();

        // 🎃 NEW: Halloween death-side effects (randomized 1 of 3)
        playRandomDeathEffects(player);

        plugin.getDataManager().incrementDeaths(player.getUniqueId());
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            plugin.getDataManager().addPlayerCoins(killer.getUniqueId(), plugin.getConfigManager().getCoinsOnKill());
            plugin.getDataManager().incrementKills(killer.getUniqueId(), 1);

            // 🎃 NEW: Halloween killer-side effects (randomized 1 of 3)
            playRandomKillerEffects(killer, player.getLocation());

            applyKillEffects(killer, plugin.getPlayerKitManager().getSelectedKit(killer));
        }

        Team team = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player);

        if (plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).isHardcore()) {
            Arena arena = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
            player.sendTitle(plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(0), plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(1), 10, 70, 20);
            player.setGameMode(GameMode.SPECTATOR);
            arena.removeHardcore(player);
            player.teleport(arena.getKSpawn());
            if (team == Team.Attackers && arena.getTeamManager().getPlayersInTeam(team) == 0) {
                arena.endGame();
            }
            return;
        } else {
            player.sendTitle(plugin.getMsg().getMessage("respawnTitle", player).get(0), plugin.getMsg().getMessage("respawnTitle", player).get(1), 10, 70, 20);
            player.setGameMode(GameMode.SPECTATOR);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
            player.teleport(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeamSpawn(team));
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getPlayerKitManager().giveKit(player, plugin.getPlayerKitManager().getSelectedKit(player));
        }, 5 * 20); // 5 seconds
    }

    private void applyKillEffects(Player killer, KitManager.KitData kitData) {
        plugin.getKillEffectManager().applyKillEffects(killer, kitData);
    }

    // =========================
    // 🎃 HALLOWEEN EFFECTS
    // =========================

    /** Randomly plays one of three death-side effects at the victim’s location. */
    private void playRandomDeathEffects(Player victim) {
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0 -> deathSoulRelease(victim);
            case 1 -> deathAshPuff(victim);
            default -> deathGhostHiss(victim);
        }
    }

    /** Randomly plays one of three killer-side effects using both killer & victim locations. */
    private void playRandomKillerEffects(Player killer, Location victimLoc) {
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0 -> killerSoulHarvest(killer, victimLoc);
            case 1 -> killerBatBurst(killer, victimLoc);
            default -> killerVampiricAura(killer);
        }
    }

    // ------- Death-side variants -------

    /** Soul Release: eerie death sound + rising soul-flame + 1–2 short-lived bats. */
    private void deathSoulRelease(Player victim) {
        World w = victim.getWorld();
        Location loc = victim.getLocation().clone().add(0, 0.2, 0);

        // Sound: phantom/vex death (soft, spooky)
        w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.7f, 0.7f);

        // Particles: a short vertical column of soul-flame drifting up
        for (int i = 0; i < 6; i++) {
            double yOff = 0.2 + (i * 0.25);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, yOff, 0), 8, 0.15, 0.05, 0.15, 0.0);
        }

        // 1–2 bats that immediately fly away and despawn
        int bats = 1 + ThreadLocalRandom.current().nextInt(2);
        for (int i = 0; i < bats; i++) {
            Bat bat = (Bat) w.spawnEntity(loc, EntityType.BAT);
            bat.setSilent(true);
            // give a tiny nudge so they don't hang
            Vector v = new Vector(ThreadLocalRandom.current().nextDouble(-0.2, 0.2), 0.4, ThreadLocalRandom.current().nextDouble(-0.2, 0.2));
            bat.setVelocity(v);
            Bukkit.getScheduler().runTaskLater(plugin, bat::remove, 40L); // 2 seconds
        }
    }

    /** Ash Puff: respawn anchor deplete sting + ash/smoke burst. */
    private void deathAshPuff(Player victim) {
        World w = victim.getWorld();
        Location loc = victim.getLocation();

        w.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 1.0f);
        // A quick ash cloud with some smoke
        w.spawnParticle(Particle.ASH, loc, 30, 0.6, 0.4, 0.6, 0.01);
        w.spawnParticle(Particle.SMOKE_NORMAL, loc, 12, 0.4, 0.2, 0.4, 0.01);
    }

    /** Ghost Hiss: soft hiss + faint end-rod wisps spiraling upward. */
    private void deathGhostHiss(Player victim) {
        World w = victim.getWorld();
        Location base = victim.getLocation();

        w.playSound(base, Sound.ENTITY_CAT_HISS, 0.35f, 1.2f);
        // End-rod wisps rising in a small spiral
        for (int i = 0; i < 10; i++) {
            double t = i / 10.0 * Math.PI * 2;
            double radius = 0.4;
            Location p = base.clone().add(Math.cos(t) * radius, 0.15 + i * 0.08, Math.sin(t) * radius);
            w.spawnParticle(Particle.END_ROD, p, 2, 0.02, 0.02, 0.02, 0.0);
        }
    }

    // ------- Killer-side variants -------

    /** Soul Harvest: a quick soul-flame beam from victim to killer + subtle ritual sound. */
    private void killerSoulHarvest(Player killer, Location victimLoc) {
        World w = killer.getWorld();
        Location kLoc = killer.getLocation().add(0, 1.2, 0);
        Location vLoc = victimLoc.clone().add(0, 1.0, 0);

        // Sound: ritualistic enchant tone on killer
        w.playSound(kLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.8f);

        // Particle "beam" from victim to killer
        Vector dir = kLoc.toVector().subtract(vLoc.toVector());
        int steps = 14;
        Vector step = dir.multiply(1.0 / steps);
        Location curr = vLoc.clone();
        for (int i = 0; i < steps; i++) {
            curr.add(step);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, curr, 3, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** Bat Burst: a quick squeak/flap and 3 short-lived bats from the corpse. */
    private void killerBatBurst(Player killer, Location victimLoc) {
        World w = killer.getWorld();
        w.playSound(victimLoc, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.9f);
        w.playSound(victimLoc, Sound.ENTITY_WITCH_CELEBRATE, 0.4f, 1.1f);

        for (int i = 0; i < 3; i++) {
            Bat bat = (Bat) w.spawnEntity(victimLoc, EntityType.BAT);
            bat.setSilent(true);
            Vector v = new Vector(ThreadLocalRandom.current().nextDouble(-0.25, 0.25), 0.45, ThreadLocalRandom.current().nextDouble(-0.25, 0.25));
            bat.setVelocity(v);
            Bukkit.getScheduler().runTaskLater(plugin, bat::remove, 40L); // 2s
        }
    }

    /** Vampiric Aura: red dust swirl around the killer + low, dark tone. */
    private void killerVampiricAura(Player killer) {
        World w = killer.getWorld();
        Location loc = killer.getLocation().add(0, 1.0, 0);

        // Sound: dark, subtle pulse
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.2f, 0.5f);

        // Particles: red dust ring that pops once
        DustOptions red = new DustOptions(Color.fromRGB(190, 12, 40), 1.1f);
        for (int i = 0; i < 16; i++) {
            double t = (i / 16.0) * Math.PI * 2;
            double r = 0.8;
            Location p = loc.clone().add(Math.cos(t) * r, 0.0, Math.sin(t) * r);
            w.spawnParticle(Particle.REDSTONE, p, 1, 0.02, 0.02, 0.02, 0.0, red);
        }
    }
}
