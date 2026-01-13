package me.cbhud.castlesiege.team;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {
    private final CastleSiege plugin;

    // Source of truth: UUID -> Team
    private final Map<UUID, Team> playerTeams = new HashMap<>();

    // Fast membership lookup without scanning online players
    private final EnumMap<Team, Set<UUID>> teamMembers = new EnumMap<>(Team.class);

    private final int maxPlayersPerTeam;
    private final int maxTeamDiff;

    public TeamManager(CastleSiege plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.maxPlayersPerTeam = config.getInt("maxPlayersPerTeam", 16);
        this.maxTeamDiff = config.getInt("maxTeamDiff", 1);

        // init sets
        for (Team t : Team.values()) {
            teamMembers.put(t, new HashSet<>());
        }
    }

    /* ---------------------- Core queries ---------------------- */

    public Team getTeam(Player player) {
        if (player == null) return null;
        return playerTeams.get(player.getUniqueId());
    }

    public int getPlayersInTeam(Team team) {
        if (team == null) return 0;
        return teamMembers.get(team).size();
    }

    /**
     * Compatibility method: returns ONLINE players in this team, but without scanning Bukkit.getOnlinePlayers().
     */
    public Set<Player> getPlayersInTeams(Team team) {
        if (team == null) return Collections.emptySet();

        Set<Player> result = new HashSet<>();
        for (UUID uuid : teamMembers.get(team)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return result;
    }

    /**
     * New: returns UUIDs of all members of a team (online or offline).
     * Great for rewards at endGame.
     */
    public Set<UUID> getTeamMemberIds(Team team) {
        if (team == null) return Collections.emptySet();
        return new HashSet<>(teamMembers.get(team));
    }

    /* ---------------------- Join logic ---------------------- */

    public boolean tryToJoinTeam(Player player, Team newTeam) {
        if (player == null || newTeam == null) return false;

        if (getPlayersInTeam(newTeam) >= maxPlayersPerTeam) {
            safeMsg(player, "team-full");
            return false;
        }

        Team previousTeam = getTeam(player);
        if (previousTeam == newTeam) {
            safeMsg(player, "team-already-joined");
            return false;
        }

        if (previousTeam == null) {
            return joinTeam(player, newTeam);
        }

        // Balance check: simulate move
        int newSize = getPlayersInTeam(newTeam);
        int prevSize = getPlayersInTeam(previousTeam);

        int afterNew = newSize + 1;
        int afterPrev = prevSize - 1;

        int diffAfter = afterNew - afterPrev;
        if (Math.abs(diffAfter) > maxTeamDiff) {
            safeMsg(player, "team-balance");
            return false;
        }

        return joinTeam(player, newTeam);
    }

    public boolean tryRandomTeamJoin(Player player) {
        if (player == null) return false;

        // Already assigned
        if (getTeam(player) != null) return true;

        int a = getPlayersInTeam(Team.Attackers);
        int d = getPlayersInTeam(Team.Defenders);

        Team preferred = (d <= a) ? Team.Defenders : Team.Attackers;
        Team other = (preferred == Team.Defenders) ? Team.Attackers : Team.Defenders;

        if (canJoin(preferred, a, d)) {
            return joinTeam(player, preferred);
        }
        if (canJoin(other, a, d)) {
            return joinTeam(player, other);
        }

        safeMsg(player, "team-full");
        return false;
    }

    private boolean canJoin(Team team, int attackersSize, int defendersSize) {
        if (team == Team.Attackers) {
            return attackersSize < maxPlayersPerTeam && Math.abs((attackersSize + 1) - defendersSize) <= maxTeamDiff;
        } else {
            return defendersSize < maxPlayersPerTeam && Math.abs((defendersSize + 1) - attackersSize) <= maxTeamDiff;
        }
    }

    private boolean joinTeam(Player player, Team newTeam) {
        if (player == null || newTeam == null) return false;

        UUID uuid = player.getUniqueId();
        Team previousTeam = playerTeams.get(uuid);
        if (previousTeam == newTeam) return false;

        // Remove from previous
        if (previousTeam != null) {
            teamMembers.get(previousTeam).remove(uuid);
        }

        // Add to new
        playerTeams.put(uuid, newTeam);
        teamMembers.get(newTeam).add(uuid);

        // Messaging
        safeTeamJoinMsg(player, newTeam);

        // Side effects (kept as you had)
        plugin.getPlayerKitManager().setDefaultKit(player);
        plugin.getScoreboardManager().updateScoreboard(player, "pre-game");

        return true;
    }

    /* ---------------------- Removal / reset ---------------------- */

    public void removePlayerFromTeam(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        Team prev = playerTeams.remove(uuid);
        if (prev != null) {
            teamMembers.get(prev).remove(uuid);
        }
    }

    public void clearTeams() {
        playerTeams.clear();
        for (Team t : Team.values()) {
            teamMembers.get(t).clear();
        }
    }

    /* ---------------------- Small message helpers ---------------------- */

    private void safeMsg(Player player, String key) {
        List<String> lines = plugin.getMsg().getMessage(key, player);
        if (lines == null || lines.isEmpty()) return;
        player.sendMessage(lines.get(0));
    }

    private void safeTeamJoinMsg(Player player, Team team) {
        List<String> lines = plugin.getMsg().getMessage("team-join", player);
        if (lines == null || lines.isEmpty()) return;

        String msg = lines.get(0);
        String teamName = plugin.getConfigManager().getTeamName(team);
        if (teamName == null) teamName = team.name();

        player.sendMessage(msg.replace("{team}", teamName));
    }
}
