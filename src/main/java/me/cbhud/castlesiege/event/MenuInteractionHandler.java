package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

class MenuInteractionHandler {

    private final CastleSiege plugin;

    MenuInteractionHandler(CastleSiege plugin) {
        this.plugin = plugin;
    }

    boolean handleLobbyItem(PlayerInteractEvent event) {
        if (event.getItem().getType() != Material.EMERALD) {
            return false;
        }

        plugin.getArenaSelector().open(event.getPlayer());
        return true;
    }

    boolean handleWaitingArenaItem(PlayerInteractEvent event, Arena arena) {
        Material type = event.getItem().getType();
        if (arena.getState() != ArenaState.WAITING) {
            return false;
        }

        if (type == Material.CLOCK) {
            plugin.getTeamSelector().open(event.getPlayer());
            return true;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getKitSelector().open(event.getPlayer());
            return true;
        }

        return false;
    }

    boolean handleLeaveItem(PlayerInteractEvent event, Arena arena) {
        if (event.getItem().getType() != Material.RED_DYE) {
            return false;
        }

        Player player = event.getPlayer();
        player.sendMessage(plugin.getMsg().getMessage("leaveArena", player).get(0));
        arena.removePlayer(player);
        return true;
    }
}
