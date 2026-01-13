package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageEvent implements Listener {

    private final CastleSiege plugin;

    public DamageEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player damagedPlayer)) return;

        // No damage outside PLAYING state (your original behavior)
        if (plugin.getPlayerManager().getPlayerState(damagedPlayer) != PlayerState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        Arena damagedArena = plugin.getArenaManager().getArenaByPlayer(damagedPlayer.getUniqueId());
        if (damagedArena == null || damagedArena.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        // Only handle friendly fire for entity-vs-entity damage
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;

        Player damager = null;

        if (byEntity.getDamager() instanceof Player p) {
            damager = p;
        } else if (byEntity.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                damager = shooter;
            }
        }

        if (damager == null) return;

        // Damager must be PLAYING too (prevents lobby/other arena weirdness)
        if (plugin.getPlayerManager().getPlayerState(damager) != PlayerState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        Arena damagerArena = plugin.getArenaManager().getArenaByPlayer(damager.getUniqueId());
        if (damagerArena == null || damagerArena != damagedArena) {
            // If you want cross-arena damage blocked, cancel it
            event.setCancelled(true);
            return;
        }

        Team damagedTeam = damagedArena.getTeam(damagedPlayer);
        Team damagerTeam = damagedArena.getTeam(damager);

        if (damagedTeam != null && damagedTeam == damagerTeam) {
            event.setCancelled(true);
        }
    }
}
