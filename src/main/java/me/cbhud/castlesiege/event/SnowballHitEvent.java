package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SnowballHitEvent implements Listener {

    private final CastleSiege plugin;

    public SnowballHitEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(event.getHitEntity() instanceof Player hit)) return;
        if (!(snowball.getShooter() instanceof Player thrower)) return;

        if (plugin.getPlayerManager().getPlayerState(thrower) != PlayerState.PLAYING) return;

        Arena throwerArena = plugin.getArenaManager().getArenaByPlayer(thrower.getUniqueId());

        Team hitTeam = throwerArena.getTeam(hit);
        Team throwTeam = throwerArena.getTeam(thrower);
        if (hitTeam != null && hitTeam == throwTeam) return;

        int addFreezeTicks = 100;
        int newFreeze = Math.min(hit.getMaxFreezeTicks(), hit.getFreezeTicks() + addFreezeTicks);
        hit.setFreezeTicks(newFreeze);

        hit.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_DIGGING,
                50,
                0,    // level 1
                true, // ambient
                false,// no potion particles (clean)
                true  // show icon
        ));

        hit.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW,
                30,   // 1.5 seconds
                0,    // level 1
                true, // ambient
                false,// no potion particles (clean)
                true  // show icon
        ));

        // Sounds only for involved players
        Location loc = hit.getLocation();
        hit.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 1.2f);
        thrower.playSound(thrower.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.35f, 1.35f);
        hit.damage(0.001);

        // Visual feedback (particles are world-side, but lightweight)
        hit.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1.0, 0),
                18, 0.28, 0.25, 0.28, 0.02);
        hit.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.0, 0),
                6, 0.18, 0.18, 0.18, 0.01);
    }
}
