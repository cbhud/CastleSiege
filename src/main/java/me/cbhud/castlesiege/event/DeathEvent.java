package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;

import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.kit.KitManager;
import me.cbhud.castlesiege.player.PlayerState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

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


            plugin.getDataManager().incrementDeaths(player.getUniqueId());
            if(event.getEntity().getKiller() != null) {
                Player killer = event.getEntity().getKiller();
                plugin.getDataManager().addPlayerCoins(killer.getUniqueId(), plugin.getConfigManager().getCoinsOnKill());
                plugin.getDataManager().incrementKills(killer.getUniqueId(), 1);
                applyKillEffects(killer, plugin.getPlayerKitManager().getSelectedKit(killer));
            }

            Team team = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player);

            if (plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).isHardcore()) {
                Arena arena = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
                player.sendTitle(plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(0), plugin.getMsg().getMessage("hardcoreDeathTitle", player).get(1), 10, 70, 20);
                player.setGameMode(GameMode.SPECTATOR);
                arena.removeHardcore(player);
                player.teleport(arena.getKingSpawn());
                if (team == Team.Attackers && arena.getTeamManager().getPlayersInTeam(team) == 0) {
                    arena.endGame();
                }
                return;
            }else {
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


}