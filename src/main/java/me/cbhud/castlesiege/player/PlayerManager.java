package me.cbhud.castlesiege.player;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static me.cbhud.castlesiege.player.PlayerState.*;

public class PlayerManager {

    private final CastleSiege plugin;
    private final Map<Player, PlayerState> playerStates;

    public PlayerManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.playerStates = new HashMap<>();
    }

    public PlayerState getPlayerState(Player player) {
        return (player != null) ? playerStates.getOrDefault(player, IN_LOBBY) : IN_LOBBY;
    }

    public void setPlayerAsPlaying(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setLevel(0);
            player.getActivePotionEffects().clear();
            playerStates.put(player, PLAYING);
        });
    }

    public void setPlayerAsLobby(Player player) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20);
            player.setLevel(0);
            player.getActivePotionEffects().clear();
            player.getInventory().setItem(4, ItemBuilder.from(Material.EMERALD)
                    .name(Component.text(plugin.getMsg().getMessage("selectArenaItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectArenaItem", player).get(1))).build());
            playerStates.put(player, IN_LOBBY);

            if (plugin.getSlc().getLobbySpawn() == null){
                Bukkit.broadcastMessage("§cThe Main lobby spawn has not been set, please set it or notify the admin");
                Bukkit.broadcastMessage("§cCommand: /setlobby");
            }else {
            player.teleport(plugin.getSlc().getLobbySpawn());
            }

            plugin.getScoreboardManager().setupScoreboard(player);
    }

    public void setPlayerAsWaiting(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.setHealth(20);
        player.setLevel(0);
        player.getActivePotionEffects().clear();
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().setItem(3, ItemBuilder.from(Material.CLOCK)
                    .name(Component.text(plugin.getMsg().getMessage("selectTeamItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectTeamItem", player).get(1)))
                    .build());
            player.getInventory().setItem(5, ItemBuilder.from(Material.NETHER_STAR)
                    .name(Component.text(plugin.getMsg().getMessage("selectKitItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("selectKitItem", player).get(1)))
                    .build());
            playerStates.put(player, WAITING);
            player.getInventory().setItem(8, ItemBuilder.from(Material.RED_DYE)
                    .name(Component.text(plugin.getMsg().getMessage("leaveArenaItem", player).get(0)))
                    .lore(Component.text(plugin.getMsg().getMessage("leaveArenaItem", player).get(1)))
                    .build());
            playerStates.put(player, WAITING);

        });
        plugin.getScoreboardManager().updateScoreboard(player, "pre-game");
    }


    public void setPlayerAsSpectating(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playerStates.put(player, SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }, 1L);
    }



}