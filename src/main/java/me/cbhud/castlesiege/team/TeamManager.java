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
    private final int maxTeamDiff;

    public TeamManager(CastleSiege plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.playerTeams = new HashMap<>();
        this.attackers = 0;
        this.defenders = 0;
        this.maxPlayersPerTeam = config.getInt("maxPlayersPerTeam", 16);
        this.maxTeamDiff = config.getInt("maxTeamDiff", 1);
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

        int afterNew = n + 1;
        int afterPrev = p - 1;

        int diffAfter = afterNew - afterPrev; // positive => newTeam would be larger
        if (Math.abs(diffAfter) > maxTeamDiff) {
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

        // If they're already assigned, don't re-assign
        if (getTeam(player) != null) {
            return true;
        }

        int a = getPlayersInTeam(Team.Attackers);
        int d = getPlayersInTeam(Team.Defenders);

        // Preferred: smaller team; if equal -> Defenders (so it starts Defenders, then alternates)
        Team preferred = (d <= a) ? Team.Defenders : Team.Attackers;
        Team other = (preferred == Team.Defenders) ? Team.Attackers : Team.Defenders;

        // Try preferred first
        if (canJoin(preferred, a, d)) {
            return joinTeam(player, preferred);
        }

        // Fallback to the other team if preferred isn't possible due to cap/balance
        if (canJoin(other, a, d)) {
            return joinTeam(player, other);
        }

        player.sendMessage(plugin.getMsg().getMessage("team-full").get(0));
        return false;
    }

    private boolean canJoin(Team team, int a, int d) {
        if (team == Team.Attackers) {
            return a < maxPlayersPerTeam && Math.abs((a + 1) - d) <= maxTeamDiff;
        } else { // Defenders
            return d < maxPlayersPerTeam && Math.abs((d + 1) - a) <= maxTeamDiff;
        }
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
