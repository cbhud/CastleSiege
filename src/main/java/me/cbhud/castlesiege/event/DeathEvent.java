package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.kit.KitManager;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
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

        event.getDrops().clear();

        // ❄️ Winter death-side effects (sound only to victim)
        playRandomDeathEffects(player);

        plugin.getDataManager().incrementDeaths(player.getUniqueId());
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            plugin.getDataManager().addPlayerCoins(killer.getUniqueId(), plugin.getConfigManager().getCoinsOnKill());
            plugin.getDataManager().incrementKills(killer.getUniqueId(), 1);

            applyKillEffects(killer, plugin.getPlayerKitManager().getSelectedKit(killer));

            // ❄️ Winter killer-side effects (sound only to killer)
            // IMPORTANT: pass victim location (not killer location)
            playRandomKillerEffects(killer, player.getLocation());
        }

        Team team = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player);

        if (plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).isHardcore()) {
            Arena arena = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
            player.sendTitle(
                    plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(0),
                    plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(1),
                    10, 70, 20
            );
            player.setGameMode(GameMode.SPECTATOR);
            arena.removeHardcore(player);
            player.teleport(arena.getKSpawn());
            if (team == Team.Attackers && arena.getTeamManager().getPlayersInTeam(team) == 0) {
                arena.endGame();
            }
            return;
        } else {
            player.sendTitle(
                    plugin.getMsg().getMessage("respawnTitle", player).get(0),
                    plugin.getMsg().getMessage("respawnTitle", player).get(1),
                    10, 70, 20
            );
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
    // ❄️ WINTER / CHRISTMAS EFFECTS
    // =========================

    /** Randomly plays one of three winter death-side effects at the victim’s location. */
    private void playRandomDeathEffects(Player victim) {
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0 -> deathSnowPuff(victim);
            case 1 -> deathCandyCaneSpiral(victim);
            default -> deathWinterSpirit(victim);
        }
    }

    /** Randomly plays one of three winter killer-side effects using both killer & victim locations. */
    private void playRandomKillerEffects(Player killer, Location victimLoc) {
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0 -> killerJingleBurst(killer, victimLoc);
            case 1 -> killerFrozenBeam(killer, victimLoc);
            default -> killerSantasCheerAura(killer);
        }
    }

    // ------- Death-side variants -------

    /**
     * (1) Snow Puff + Ice Shards:
     * snowflake/cloud burst + icy sounds + small packed-ice shatter for depth.
     */
    private void deathSnowPuff(Player victim) {
        Location loc = victim.getLocation().clone().add(0, 0.15, 0);
        World w = victim.getWorld();

        // Sound only to victim
        victim.playSound(loc, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.1f);
        victim.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.18f, 1.7f); // softer "shatter"

        // Quick snow burst + a bit of mist
        w.spawnParticle(Particle.SNOWFLAKE, loc, 40, 0.6, 0.35, 0.6, 0.02);
        w.spawnParticle(Particle.CLOUD, loc, 14, 0.4, 0.2, 0.4, 0.01);

        // Gentle upward drift
        for (int i = 0; i < 5; i++) {
            double yOff = 0.15 + (i * 0.18);
            w.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, yOff, 0), 10, 0.25, 0.05, 0.25, 0.01);
        }

        // Extra depth: tiny packed-ice shards at the end
        BlockData ice = Material.PACKED_ICE.createBlockData();
        w.spawnParticle(Particle.BLOCK_CRACK, loc, 12, 0.35, 0.12, 0.35, 0.08, ice);
    }

    /**
     * (2) Candy Cane Spiral + Sparkle Pop:
     * red/white dust spiral + chime + a small sparkle "pop" at the top.
     */
    private void deathCandyCaneSpiral(Player victim) {
        World w = victim.getWorld();
        Location base = victim.getLocation().clone().add(0, 0.15, 0);

        // Sound only to victim
        victim.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.25f);

        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(200, 30, 40), 1.2f);
        Particle.DustOptions white = new Particle.DustOptions(Color.fromRGB(245, 245, 245), 1.2f);

        int points = 22;
        double radius = 0.55;
        for (int i = 0; i < points; i++) {
            double t = (i / (double) points) * Math.PI * 3.6;
            double y = 0.05 + (i * 0.06);
            double r = radius * (1.0 - (i / (double) points) * 0.25);

            Location p = base.clone().add(Math.cos(t) * r, y, Math.sin(t) * r);
            w.spawnParticle(Particle.REDSTONE, p, 1, 0.0, 0.0, 0.0, 0.0, (i % 2 == 0) ? red : white);

            if (i % 3 == 0) {
                w.spawnParticle(Particle.FIREWORKS_SPARK, p, 2, 0.02, 0.02, 0.02, 0.0);
            }
        }

        // Sparkle pop at the top (depth)
        Location top = base.clone().add(0, 1.25, 0);
        w.spawnParticle(Particle.FIREWORKS_SPARK, top, 14, 0.18, 0.12, 0.18, 0.02);
        w.spawnParticle(Particle.END_ROD, top, 3, 0.08, 0.05, 0.08, 0.0);

        // Tiny "sparkle" sound only to victim
        victim.playSound(top, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 1.8f);
    }

    /**
     * (3) Winter Spirit (Aurora) + Soft Snow:
     * end-rod wisps + icy-blue dust + bell + gentle snowfall curtain.
     */
    private void deathWinterSpirit(Player victim) {
        World w = victim.getWorld();
        Location base = victim.getLocation().clone().add(0, 0.2, 0);

        // Sound only to victim
        victim.playSound(base, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.15f);
        victim.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.18f, 1.35f); // very subtle

        Particle.DustOptions icyBlue = new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1.1f);

        for (int i = 0; i < 14; i++) {
            double t = (i / 14.0) * Math.PI * 2;
            double r = 0.45;
            Location p = base.clone().add(Math.cos(t) * r, 0.1 + i * 0.07, Math.sin(t) * r);

            w.spawnParticle(Particle.END_ROD, p, 2, 0.02, 0.02, 0.02, 0.0);
            w.spawnParticle(Particle.REDSTONE, p, 1, 0.0, 0.0, 0.0, 0.0, icyBlue);
        }

        // Soft snow curtain (depth)
        Location snow = base.clone().add(0, 1.05, 0);
        w.spawnParticle(Particle.SNOWFLAKE, snow, 22, 0.55, 0.25, 0.55, 0.01);
    }

    // ------- Killer-side variants -------

    /** Jingle Burst: bell + xp ping (to killer only). */
    private void killerJingleBurst(Player killer, Location victimLoc) {
        Location kLoc = killer.getLocation().clone().add(0, 1.0, 0);

        // Sound only to killer
        killer.playSound(kLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.9f, 1.2f);
        killer.playSound(kLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.7f);

        World w = killer.getWorld();
        w.spawnParticle(Particle.FIREWORKS_SPARK, kLoc, 26, 0.35, 0.35, 0.35, 0.03);
        w.spawnParticle(Particle.SNOWFLAKE, victimLoc.clone().add(0, 0.2, 0), 18, 0.35, 0.25, 0.35, 0.02);
    }

    /**
     * (5) Frozen Beam + Snow Pulse:
     * beam line + extra pulse at victim start so it has a clear origin.
     */
    private void killerFrozenBeam(Player killer, Location victimLoc) {
        World w = killer.getWorld();
        Location kLoc = killer.getLocation().clone().add(0, 1.2, 0);
        Location vLoc = victimLoc.clone().add(0, 1.0, 0);

        // Sound only to killer
        killer.playSound(kLoc, Sound.BLOCK_GLASS_PLACE, 0.6f, 1.35f);
        killer.playSound(kLoc, Sound.ITEM_TRIDENT_RETURN, 0.35f, 1.6f);
        killer.playSound(kLoc, Sound.BLOCK_SNOW_BREAK, 0.28f, 1.25f);

        // Pulse at victim start (depth)
        w.spawnParticle(Particle.SNOWFLAKE, vLoc, 22, 0.25, 0.18, 0.25, 0.02);
        w.spawnParticle(Particle.CLOUD, vLoc, 8, 0.18, 0.12, 0.18, 0.01);

        Vector dir = kLoc.toVector().subtract(vLoc.toVector());
        int steps = 16;
        Vector step = dir.multiply(1.0 / steps);

        Location curr = vLoc.clone();
        for (int i = 0; i < steps; i++) {
            curr.add(step);
            w.spawnParticle(Particle.SNOWFLAKE, curr, 3, 0.02, 0.02, 0.02, 0.0);
            if (i % 2 == 0) w.spawnParticle(Particle.END_ROD, curr, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    /**
     * (6) Santa’s Cheer Aura + Falling Confetti:
     * red/green ring + sparkle + falling fireworks spark from above + tiny level-up ping.
     */
    private void killerSantasCheerAura(Player killer) {
        World w = killer.getWorld();
        Location loc = killer.getLocation().clone().add(0, 1.0, 0);

        // Sound only to killer
        killer.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 0.95f);
        killer.playSound(loc, Sound.ENTITY_VILLAGER_CELEBRATE, 0.35f, 1.2f);
        killer.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.20f, 1.8f); // tiny "cheer" ping

        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(200, 30, 40), 1.15f);
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(40, 170, 60), 1.15f);

        for (int i = 0; i < 18; i++) {
            double t = (i / 18.0) * Math.PI * 2;
            double r = 0.85;
            Location p = loc.clone().add(Math.cos(t) * r, 0.0, Math.sin(t) * r);

            w.spawnParticle(Particle.REDSTONE, p, 1, 0.0, 0.0, 0.0, 0.0, (i % 2 == 0) ? red : green);
            if (i % 3 == 0) w.spawnParticle(Particle.SPELL_INSTANT, p, 2, 0.03, 0.03, 0.03, 0.0);
        }

        // Falling confetti sparkle (depth)
        Location above = loc.clone().add(0, 1.1, 0); // ~2.1 blocks above feet
        w.spawnParticle(Particle.FIREWORKS_SPARK, above, 18, 0.45, 0.25, 0.45, 0.03);
    }
}
