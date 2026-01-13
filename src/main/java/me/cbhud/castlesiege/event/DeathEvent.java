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

import java.util.List;
import java.util.UUID;

public class DeathEvent implements Listener {

    private final CastleSiege plugin;

    public DeathEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Only handle arena-playing deaths
        if (plugin.getPlayerManager().getPlayerState(player) != PlayerState.PLAYING) return;

        // Arena must exist (mapping can be missing in edge cases)
        Arena arena = plugin.getArenaManager().getArenaByPlayer(playerId);
        if (arena == null) return;

        event.getDrops().clear();

        plugin.getDataManager().incrementDeaths(playerId);

        Player killer = player.getKiller();
        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            plugin.getDataManager().addPlayerCoins(killerId, plugin.getConfigManager().getCoinsOnKill());
            plugin.getDataManager().incrementKills(killerId, 1);

            applyKillEffects(killer, plugin.getPlayerKitManager().getSelectedKit(killer));
        }

        Team team = arena.getTeam(player);
        if (team == null) return;

        if (arena.isHardcore()) {
            sendTitleSafe(player, "hardcoreDeathTitle", 10, 70, 20);

            player.setGameMode(GameMode.SPECTATOR);

            // remove from team but keep them as spectator in arena
            arena.removeHardcore(player);

            // teleport to king spawn (safe)
            if (arena.getKSpawn() != null) {
                player.teleport(arena.getKSpawn());
            }

            // if attackers eliminated in hardcore -> end game
            if (team == Team.Attackers && arena.getTeamManager().getPlayersInTeam(Team.Attackers) == 0) {
                arena.endGame();
            }
            return;
        }

        // Non-hardcore: respawn after delay
        sendTitleSafe(player, "respawnTitle", 10, 70, 20);
        player.setGameMode(GameMode.SPECTATOR);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Player might have quit / moved arenas / arena ended
            if (!player.isOnline()) return;

            Arena currentArena = plugin.getArenaManager().getArenaByPlayer(playerId);
            if (currentArena == null || currentArena.getState() != me.cbhud.castlesiege.arena.ArenaState.IN_GAME) return;

            player.spigot().respawn();

            // Team might have changed or been cleared
            Team currentTeam = currentArena.getTeam(player);
            if (currentTeam == null) return;

            if (currentArena.getTeamSpawn(currentTeam) != null) {
                player.teleport(currentArena.getTeamSpawn(currentTeam));
            }

            player.setGameMode(GameMode.SURVIVAL);
            plugin.getPlayerKitManager().giveKit(player, plugin.getPlayerKitManager().getSelectedKit(player));

        }, 5 * 20L);
    }

    private void applyKillEffects(Player killer, KitManager.KitData kitData) {
        plugin.getKillEffectManager().applyKillEffects(killer, kitData);
    }

    private void sendTitleSafe(Player player, String key, int fadeIn, int stay, int fadeOut) {
        List<String> lines = plugin.getMsg().getMessage(key, player);
        if (lines == null || lines.isEmpty()) return;

        String a = lines.size() >= 1 ? lines.get(0) : "";
        String b = lines.size() >= 2 ? lines.get(1) : "";
        player.sendTitle(a, b, fadeIn, stay, fadeOut);
    }
}
