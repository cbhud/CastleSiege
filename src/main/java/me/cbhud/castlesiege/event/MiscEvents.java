package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.player.PlayerState;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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

        if (event.getItem().getItemStack().getType() == Material.OAK_FENCE || event.getItem().getItemStack().getType() == Material.SPRUCE_FENCE && plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING) {
            return;
        }

        event.setCancelled(true);
    }



    private boolean canDoFence(Player player, Material type) {
        if (player.hasPermission("cs.admin") || player.isOp()) return true;
        return plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING
                && Tag.WOODEN_FENCES.isTagged(type); // includes oak, spruce, birch, etc.
    }

    private boolean canDoCobweb(Player player, Material type) {
        if (player.hasPermission("cs.admin") || player.isOp()) return true;
        return plugin.getPlayerManager().getPlayerState(player) == PlayerState.PLAYING
                && type == Material.COBWEB;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canDoFence(event.getPlayer(), event.getBlock().getType()) && !canDoCobweb(event.getPlayer(), event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canDoFence(event.getPlayer(), event.getBlockPlaced().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onNaturalRegeneration(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
                event.setAmount(0.1);
            }
        }
    }



}
