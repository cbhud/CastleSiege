package me.cbhud.castlesiege.util;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.game.GameState;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MapRegeneration implements Listener {

    private List<Location> brokenBlocks;
    private List<Location> placedBlocks;
    private CastleSiege plugin;

    public MapRegeneration(CastleSiege plugin) {
        brokenBlocks = new ArrayList<Location>();
        placedBlocks = new ArrayList<Location>();
        this.plugin = plugin;
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGame().getState() == GameState.IN_GAME && event.getBlock().getType() == Material.OAK_FENCE){
            brokenBlocks.add(event.getBlock().getLocation());
            if (plugin.getTeamManager().getTeam(player) == Team.Defenders){
                player.getInventory().addItem(new ItemStack(Material.OAK_FENCE, 1));
                event.getBlock().setType(Material.AIR);
            }else {
                event.getBlock().setType(Material.AIR);
            }
        }
        if (player.hasPermission("cs.admin") && plugin.getGame().getState() != GameState.IN_GAME || player.isOp() && plugin.getGame().getState() != GameState.IN_GAME){
            event.setCancelled(false);
        }else {
            event.setCancelled(true);
        }
    }


    public void add(Location location) {
        brokenBlocks.add(location); // Add broken oak fence to the list
    }


        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event){

            Player player = event.getPlayer();
            if (plugin.getGame().getState() == GameState.IN_GAME && event.getBlock().getType() == Material.OAK_FENCE && plugin.getTeamManager().getTeam(player) == Team.Defenders){
                placedBlocks.add(event.getBlock().getLocation());
            }

            if (player.hasPermission("cs.admin") || player.isOp() && plugin.getGame().getState() != GameState.IN_GAME){
                event.setCancelled(false);
            }else {
                event.setCancelled(true);
            }
    }


    public void regenerateChangedBlocks() {

            for (Location location : brokenBlocks) {
                Block block = location.getBlock();
                block.setType(Material.OAK_FENCE);
            }

            for (Location location : placedBlocks) {
                Block block = location.getBlock();
                block.setType(Material.AIR);
            }

            brokenBlocks.clear();
            placedBlocks.clear();

    }



    }