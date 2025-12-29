package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
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
        if (!(event.getEntity() instanceof Player)) return;

        Player damagedPlayer = (Player) event.getEntity();

        if (plugin.getPlayerManager().getPlayerState(damagedPlayer) != PlayerState.PLAYING ) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getArenaManager().getArenaByPlayer(damagedPlayer.getUniqueId()).getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            return;
        }


        if (!(event instanceof EntityDamageByEntityEvent)) return;

        EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) event;
        Player damager = null;

        if (damageByEntityEvent.getDamager() instanceof Player) {
            damager = (Player) damageByEntityEvent.getDamager();
        } else if (damageByEntityEvent.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) damageByEntityEvent.getDamager();
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }

        if (damager == null) return;

        Team damagedPlayerTeam = plugin.getArenaManager().getArenaByPlayer(damagedPlayer.getUniqueId()).getTeam(damagedPlayer);
        Team damagerTeam = plugin.getArenaManager().getArenaByPlayer(damager.getUniqueId()).getTeam(damager);

        if (damagedPlayerTeam != null && damagedPlayerTeam == damagerTeam) {
            event.setCancelled(true);
        }
    }
}