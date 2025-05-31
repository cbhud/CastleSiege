package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.player.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class MiscEvents implements Listener {

    private final CastleSiege plugin;

    public MiscEvents(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.isOp() || player.hasPermission("cs.admin")) return;

        if (event.getItem().getItemStack().getType() == Material.OAK_FENCE && plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING) {
            return;
        }
        event.setCancelled(true);
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Allow if player is in PLAYING state and breaking an OAK_FENCE
        if (plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING
                && event.getBlock().getType() == Material.OAK_FENCE) {
            return; // Exit early to avoid overriding
        }

        // Allow if player has admin permissions or is OP
        if (player.hasPermission("cs.admin") || player.isOp()) {
            return;
        }

        // If none of the above conditions are met, cancel the event
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Allow if player is in PLAYING state and placing an OAK_FENCE
        if (plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING
                && event.getBlock().getType() == Material.OAK_FENCE) {
            return;
        }

        // Allow if player has admin permissions or is OP
        if (player.hasPermission("cs.admin") || player.isOp()) {
            return;
        }

        // If none of the above conditions are met, cancel the event
        event.setCancelled(true);
    }



}
