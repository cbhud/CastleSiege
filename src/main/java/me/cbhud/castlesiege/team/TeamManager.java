package me.cbhud.castlesiege.team;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    public boolean tryToJoinTeam(Player player, Team newTeam) {
        if (newTeam == null) return false;

        if (getPlayersInTeam(newTeam) >= maxPlayersPerTeam){
            player.sendMessage(plugin.getMsg().getMessage("team-full").get(0));
            return false;
        }

        Team previousTeam = getTeam(player);

        if (previousTeam == newTeam) {
            player.sendMessage(plugin.getMsg().getMessage("team-already-joined").get(0));
            return false;
        }

        if (previousTeam == null) {
            return joinTeam(player, newTeam);
        }

        int n = getPlayersInTeam(newTeam);
        int p = getPlayersInTeam(previousTeam);
        int delta = n - p;

        if (delta < -3 || delta > -1) {
            player.sendMessage(plugin.getMsg().getMessage("team-balance").get(0));
            return false;
        }

        return joinTeam(player, newTeam);
    }

    private boolean joinTeam(Player player, Team newTeam) {
        if (newTeam == null) return false;

        Team previousTeam = getTeam(player);
        if (previousTeam == newTeam) return false;

        if (previousTeam != null) {
            updateTeamCount(previousTeam, -1);
        }

        if (newTeam == Team.Defenders) {
            player.sendMessage(plugin.getMsg().getMessage("team-join").get(0).replace("{team}", plugin.getConfigManager().getTeamName(Team.Defenders)));
        } else if (newTeam == Team.Attackers) {
            player.sendMessage(plugin.getMsg().getMessage("team-join").get(0).replace("{team}", plugin.getConfigManager().getTeamName(Team.Attackers)));
        }

        playerTeams.put(player.getUniqueId().toString(), newTeam);

        updateTeamCount(newTeam, +1);

        plugin.getPlayerKitManager().setDefaultKit(player);
        plugin.getScoreboardManager().updateScoreboard(player, "pre-game");

        return true;
    }

    public boolean tryRandomTeamJoin(Player player) {
        if (getTeam(player) != null) {
            Team target = pickSmallerTeamWithRoom();
            return target != null && tryToJoinTeam(player, target);
        }

        int a = getPlayersInTeam(Team.Attackers);
        int d = getPlayersInTeam(Team.Defenders);

        if (a >= maxPlayersPerTeam && d >= maxPlayersPerTeam) return false;

        if (a > d && d < maxPlayersPerTeam) return tryToJoinTeam(player, Team.Defenders);
        if (d > a && a < maxPlayersPerTeam) return tryToJoinTeam(player, Team.Attackers);

        List<Team> options = new ArrayList<>(2);
        if (a < maxPlayersPerTeam) options.add(Team.Attackers);
        if (d < maxPlayersPerTeam) options.add(Team.Defenders);
        if (options.isEmpty()) return false;

        Team pick = options.get(ThreadLocalRandom.current().nextInt(options.size()));

        return joinTeam(player, pick);
    }

    private Team pickSmallerTeamWithRoom() {
        int a = getPlayersInTeam(Team.Attackers);
        int d = getPlayersInTeam(Team.Defenders);
        if (a > d && d < maxPlayersPerTeam) return Team.Defenders;
        if (d > a && a < maxPlayersPerTeam) return Team.Attackers;

        if (a < maxPlayersPerTeam) return Team.Attackers;
        if (d < maxPlayersPerTeam) return Team.Defenders;
        return null;
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


    public void clearTeams() {
        playerTeams.clear();
        attackers = 0;
        defenders = 0;
    }

    private void updateTeamCount(Team team, int change) {
        if (team == Team.Attackers) {
            attackers += change;
        } else if (team == Team.Defenders) {
            defenders += change;
        }

        if (attackers < 0) attackers = 0;
        if (defenders < 0) defenders = 0;
    }

}
