package me.cbhud.castlesiege.scoreboard;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager {

    private final CastleSiege plugin;
    private final ChatColor defenderColor;
    private final ChatColor attackerColor;

    public NameTagManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.defenderColor = plugin.getConfigManager().getDefendersColor();
        this.attackerColor = plugin.getConfigManager().getAttackersColor();
    }

    // Helper method for quick assignment
    public void setDefender(Player player) {
        setNametagColor(player, defenderColor);
    }

    // Helper method for quick assignment
    public void setAttacker(Player player) {
        setNametagColor(player, attackerColor);
    }

    private void setNametagColor(Player player, ChatColor color) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamName = color.name();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(color);
        }

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void removeNametag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getEntryTeam(player.getName());

        if (team != null) {
            team.removeEntry(player.getName());
        }
    }
}