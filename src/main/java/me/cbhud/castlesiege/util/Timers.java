package me.cbhud.castlesiege.util;

import fr.mrmicky.fastboard.FastBoard;
import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import me.cbhud.castlesiege.game.GameState;
import me.cbhud.castlesiege.team.Team;
import java.util.ArrayList;
import java.util.List;

public class Timers {
    private final CastleSiege plugin;
    private final int playersToStart;
    private final int initialCountdownSeconds;
    private int countdownSeconds;
    private int taskId;
    private int countdownTimer;
    private final List<Integer> taskIds = new ArrayList<>();
    private BukkitRunnable timerTask;
    private boolean isRunning;

    public Timers(CastleSiege plugin) {
        this.plugin = plugin;
        this.playersToStart = plugin.getConfigManager().getConfig().getInt("auto-start-players");
        this.initialCountdownSeconds = plugin.getConfigManager().getAutoStartCountdown();
        this.countdownSeconds = initialCountdownSeconds;
        this.isRunning = false;
    }

    public void checkAutoStart(int currentPlayers) {
        if(isRunning){
            return;
        }
        if (currentPlayers >= playersToStart) {
            startCountdown();
        } else {
            cancelCountdown();
        }
    }

    private void startCountdown() {
        resetCountdown();
        this.isRunning = true;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (countdownSeconds > 0) {
                if (countdownSeconds == initialCountdownSeconds || countdownSeconds == 45 || countdownSeconds == 30 || countdownSeconds == 15 || countdownSeconds == 10 || countdownSeconds <= 5) {
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMainColor() + "Game is starting in " + ChatColor.WHITE + countdownSeconds + plugin.getConfigManager().getMainColor() + " seconds!");
                }
                countdownSeconds--;
            } else {
                cancelCountdown();
                Bukkit.broadcastMessage(ChatColor.GREEN + "Starting the game!");
                plugin.getCommands().startGame();
                this.isRunning = false;
            }
        }, 0L, 20L);
        taskIds.add(taskId);
    }

    private void cancelCountdown() {
        for (int taskId : taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskIds.clear();
        if (plugin.getGame().getState() == GameState.IN_GAME) {
            plugin.getGameEndHandler().setWinner(Team.Defenders);
            plugin.getGameEndHandler().handleGameEnd();
            cancelTimer();
        } else {
            for (String line : plugin.getMessagesConfig().getNotStartMsg()) {
                Bukkit.broadcastMessage(line);
            }
        }
    }
    public void cancelCountdown2() {
        for (int taskId : taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        this.isRunning = false;
        taskIds.clear();
        }

    private void resetCountdown() {
        countdownSeconds = initialCountdownSeconds;
    }

    public void startTimer(int minutes) {
        countdownTimer = minutes * 60;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownTimer > 0) {
                    countdownTimer--;
                    if (plugin.getGame().getState() == GameState.IN_GAME) {
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            FastBoard board = plugin.getScoreboardManager().scoreboards.get(player);
                            if (board != null) {
                                plugin.getScoreboardManager().updateInGameScoreboard(board);
                            }
                        });
                    }
                } else {
                    if (plugin.getGame().getState() == GameState.IN_GAME) {
                        plugin.getGameEndHandler().setWinner(Team.Defenders);
                        plugin.getGameEndHandler().handleGameEnd();
                        cancelTimer();
                    }
                }
            }
        };

        timerTask.runTaskTimer(plugin, 0, 20); // Run every second (20 ticks)
    }

    public void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public int getSecondsLeft() {
        return countdownTimer;
    }

    public boolean isRun(){
        return this.isRunning;
    }

}
