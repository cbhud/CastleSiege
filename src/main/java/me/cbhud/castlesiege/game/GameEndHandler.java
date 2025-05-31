package me.cbhud.castlesiege.game;

import me.cbhud.castlesiege.*;
import org.bukkit.*;
import org.bukkit.plugin.*;
import me.cbhud.castlesiege.team.*;
import org.bukkit.event.*;
import org.bukkit.entity.*;
public class GameEndHandler implements Listener
{
    private final CastleSiege plugin;
    private Team winner;
    static String killername;


    public GameEndHandler(final CastleSiege plugin, Team winner) {
        this.plugin = plugin;
        this.winner = winner;
    }

    public void handleGameEnd() {
        this.plugin.getGame().setState(GameState.END);
        plugin.getMobManager().removeCustomZombie();
        plugin.getBossBar().removeBar();
        if(getWinner() != null){setPlayerWins();}
        plugin.tntThrower().clearCooldowns();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.plugin.getGame().setState(GameState.LOBBY);
            plugin.getLocationManager().teleportPlayersToLobby();
        }, 200L);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.plugin.getMapRegeneration().regenerateChangedBlocks();
            this.plugin.getTimer().checkAutoStart(Bukkit.getOnlinePlayers().size());
        }, 300L);
    }

    public void setKiller(String name){
        killername = name;
    }
    public String getKillername() {
        return killername;
    }



    private void setPlayerWins() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Team winner = getWinner();
            if (winner != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getTeamManager().getTeam(player) == winner) {
                        plugin.getDbConnection().incrementWins(player.getUniqueId(), plugin.getConfigManager().getWc());
                    }
                }
            }
        });
    }


    public void setWinner(Team winner) {
        this.winner = winner;
        broadcastWinnerMessage();
    }

    public Team getWinner() {
        return winner;
    }

    private void broadcastWinnerMessage() {
        if (winner == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Game cancelled, both teams have disconnected.");
            return;
        }

        String winnerName = winner.toString();

        if ("Attackers".equalsIgnoreCase(winnerName)) {
            for (String line : plugin.getMessagesConfig().getVikingsWinMsg()) {
                line = line.replace("{killer}", getKillername());
                line = line.replace("{attackers}", plugin.getConfigManager().getAttacker());
                Bukkit.broadcastMessage(line);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(ChatColor.RED + plugin.getConfigManager().getConfig().getString("attackersTeamName"), ChatColor.YELLOW + "won the game", 10, 70, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1.0f, 0.9f);
            }

            return;
        }

        // If not attackers defenders won
        for (String line : plugin.getMessagesConfig().getFranksWinMsg()) {
            line = line.replace("{defenders}", plugin.getConfigManager().getDefender());
            Bukkit.broadcastMessage(line);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.BLUE + plugin.getConfigManager().getConfig().getString("defendersTeamName"), ChatColor.YELLOW + "won the game", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1.0f, 0.9f);
        }
    }



}
