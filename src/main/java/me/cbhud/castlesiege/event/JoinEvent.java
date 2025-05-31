package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.player.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinEvent implements Listener {

    private final CastleSiege plugin;

    public JoinEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        e.setJoinMessage(null);
        Player p = e.getPlayer();

        if(plugin.getSlc().getLobby() == null){
            p.sendMessage("§CLobby location is not set!");
            p.sendMessage("§cUse /setlobby to set lobby location");
        }

        for (String i:  plugin.getMsg().getMessage("join-server-msg", p)){
            p.sendMessage(i);
        }

        plugin.getPlayerManager().setPlayerAsLobby(p);

        plugin.getDataManager().createProfile(e.getPlayer().getUniqueId(), e.getPlayer().getName());

    }


    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        if (plugin.getPlayerManager().getPlayerState(p) == PlayerState.PLAYING || plugin.getPlayerManager().getPlayerState(p) == PlayerState.WAITING || plugin.getPlayerManager().getPlayerState(p) == PlayerState.SPECTATOR) {
            UUID playerId = p.getUniqueId();
            plugin.getArenaManager().getArenaByPlayer(playerId).removePlayer(p);
        }
        plugin.getScoreboardManager().removeScoreboard(p);
    }

}