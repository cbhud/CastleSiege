package me.cbhud.castlesiege.team;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {
    private final CastleSiege plugin;
    private final Map<String, Team> playerTeams;
    private int attackers;
    private int defenders;
    private final int maxPlayersPerTeam;

    public TeamManager(CastleSiege plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.playerTeams = new HashMap<>();
        this.attackers = 0;
        this.defenders = 0;
        this.maxPlayersPerTeam = config.getInt("maxPlayersPerTeam", 16);
    }

    public boolean joinTeam(Player player, Team newTeam) {
        if (getPlayersInTeam(newTeam) >= maxPlayersPerTeam) {
            return false; // Team is full
        }

        Team previousTeam = getTeam(player);

        if (previousTeam == newTeam) {
            return false; // Already in this team
        }

        if (previousTeam != null) {
            removePlayerFromTeam(player); // Remove from previous team before switching
        }

        // Assign player to new team
        playerTeams.put(player.getUniqueId().toString(), newTeam);
        updateTeamCount(newTeam, 1); // Increase the count of the new team
        plugin.getPlayerKitManager().setDefaultKit(player);
        plugin.getScoreboardManager().updateScoreboard(player, "pre-game");

        return true;
    }

    public Team getTeam(Player player) {
        return playerTeams.get(player.getUniqueId().toString());
    }

    public void removePlayerFromTeam(Player player) {
        Team previousTeam = getTeam(player);
        if (previousTeam != null) {
            updateTeamCount(previousTeam, -1); // Decrease the count of the old team
            playerTeams.remove(player.getUniqueId().toString());
        }
    }

    public Set<Player> getPlayersInTeams(Team team) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> getTeam(player) == team)
                .collect(Collectors.toSet());
    }

    public int getPlayersInTeam(Team team) {
        return (team == Team.Attackers) ? attackers : defenders;
    }

    public boolean tryRandomTeamJoin(Player player) {
        if (attackers >= maxPlayersPerTeam && defenders >= maxPlayersPerTeam) {
            return false; // Both teams are full
        }
        Random random = new Random();
        Team[] teams = {Team.Attackers, Team.Defenders};

        while (true) {
            Team team = teams[random.nextInt(teams.length)];
            if (getPlayersInTeam(team) < maxPlayersPerTeam) {
                return joinTeam(player, team);
            }
        }
    }

    public void clearTeams() {
        playerTeams.clear();
        attackers = 0;
        defenders = 0;
    }

    // Updates team count, ensuring the count never goes negative
    private void updateTeamCount(Team team, int change) {
        if (team == Team.Attackers) {
            attackers += change;
        } else if (team == Team.Defenders) {
            defenders += change;
        }

        // Prevent negative values
        if (attackers < 0) attackers = 0;
        if (defenders < 0) defenders = 0;
    }

}
