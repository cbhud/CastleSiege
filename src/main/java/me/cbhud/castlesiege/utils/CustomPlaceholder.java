package me.cbhud.castlesiege.utils;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomPlaceholder extends PlaceholderExpansion {
    private final CastleSiege plugin;

    public CustomPlaceholder(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cs"; // %cs_<placeholder>%
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true; // Prevents the placeholder from being removed when PAPI reloads
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null) return null;

        // Convert OfflinePlayer to Player (only if online)
        Player player = offlinePlayer.getPlayer();
        if (player == null) return null; // Ensures the player is online before proceeding

        // Custom placeholders
        switch (identifier.toLowerCase()) {
            case "timer":
                return String.valueOf(getTimer(player)); // Example: %cs_timer%
            case "starting-in":
                return String.valueOf(getStartingIn(player)); // Example: %castlesiege_timer%
            case "kills":
                return String.valueOf(getPlayerKills(player));
            case "wins":
                return String.valueOf(getPlayerWins(player));
            case "deaths":
                return String.valueOf(getPlayerDeaths(player));
            case "coins":
                return String.valueOf(getPlayerCoins(player));
            case "king":
                return String.valueOf(getKingHealth(player)); // Example: %castlesiege_timer%
            case "team":
                return getPlayerTeam(player); // Example: %castlesiege_team%
            case "attackers_size":
                return String.valueOf(getAttackersSize(player)); // Example: %castlesiege_team%
            case "defenders_size":
                return String.valueOf(getDefendersSize(player)); // Example: %castlesiege_team%
            case "arena":
                return String.valueOf(getArena(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId())));
            case "arenasize":
                return String.valueOf(getArenaSize(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId())));
            case "winner":
                return getWinner(player);
            case "attackers":
                return getAttackersName();
            case "defenders":
                return getDefendersName();
            case "kit":
                if (plugin.getPlayerKitManager().hasSelectedKit(player)) {
                    return getPlayerKit(player);
                } else {
                    return "No kit selected";
                }

            default:
                return null; // Unknown placeholder
        }
    }

    private int getStartingIn(Player player) {
        return  plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getAutoStartTimer();
    }

    private int getTimer(Player player) {
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTimer();
    }

    private String getArena(Arena arena){
        return arena.getId();
    }

    private int getArenaSize(Arena arena){
        return arena.getPlayers().size();
    }

    private double getKingHealth(Player player){
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getKingZombieHealth();
    }
    private String getAttackersName(){
        return plugin.getConfigManager().getAttacker();
    }
    private String getDefendersName(){
        return plugin.getConfigManager().getDefender();
    }
    private int getAttackersSize(Player player){
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getAttackersSize();
    }
    private int getDefendersSize(Player player){
        return plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getDefendersSize();
    }
    private String getWinner(Player player){
        if(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getWinner() == 1){
            return plugin.getConfigManager().getTeamName(Team.Attackers);
        }else {
            return plugin.getConfigManager().getTeamName(Team.Defenders);
        }
    }
    private String getPlayerKit(Player player) {
        return plugin.getPlayerKitManager().getSelectedKit(player).getName();
    }

    private String getPlayerTeam(Player player) {
        return plugin.getConfigManager().getTeamName(plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player));
    }
    private int getPlayerKills(Player player){
        return plugin.getDataManager().getPlayerKills(player.getUniqueId());
    }
    private int getPlayerCoins(Player player){
        return plugin.getDataManager().getPlayerCoins(player.getUniqueId());
    }
    private int getPlayerWins(Player player){
        return plugin.getDataManager().getPlayerWins(player.getUniqueId());
    }
    private int getPlayerDeaths(Player player){
        return plugin.getDataManager().getPlayerDeaths(player.getUniqueId());
    }
}
