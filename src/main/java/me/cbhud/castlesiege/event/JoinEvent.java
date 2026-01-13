package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinEvent implements Listener {

    private final CastleSiege plugin;

    public JoinEvent(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Player p = e.getPlayer();

        for (String i : plugin.getMsg().getMessage("join-server-msg", p)) {
            p.sendMessage(i);
        }

        plugin.getPlayerManager().setPlayerAsLobby(p);
        plugin.getDataManager().createProfile(p.getUniqueId(), p.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        plugin.getArenaManager().removePlayerFromArena(p);

        plugin.getScoreboardManager().removeScoreboard(p);
        plugin.getPlayerManager().clearPlayerState(p);
    }
}
