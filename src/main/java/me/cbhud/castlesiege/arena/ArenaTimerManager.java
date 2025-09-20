package me.cbhud.castlesiege.arena;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ArenaTimerManager {

    private final CastleSiege plugin;
    private final Arena arena;

    // Configuration values (final - these don't change)
    private final int maxCountdownTime;
    private final int maxAutoStartTime;

    // Current timer values (not final - these change during execution)
    private int currentCountdownTimer;
    private int currentAutoStartTimer;

    // Task IDs
    private int countdownTaskId = -1;
    private int autoStartTaskId = -1;

    public ArenaTimerManager(CastleSiege plugin, Arena arena, int countdownTimer, int autoStartTimer) {
        this.plugin = plugin;
        this.arena = arena;
        this.maxCountdownTime = countdownTimer;
        this.maxAutoStartTime = autoStartTimer;
    }

    // ---------------- Countdown ----------------
    public void startCountdown() {
        if (countdownTaskId != -1) return;

        currentCountdownTimer = maxCountdownTime; // Reset to max time

        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (currentCountdownTimer <= 0) {
                stopCountdown();
                arena.endGame();
                return;
            }

            // Get fresh player set each tick (players can join/leave)
            arena.getPlayers().forEach(player ->
                    plugin.getScoreboardManager().updateScoreboard(player, "in-game"));

            currentCountdownTimer--;
        }, 0L, 20L);
    }

    public void stopCountdown() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
    }

    public int getCountdownTimer() {
        return currentCountdownTimer;
    }

    public int getCountdownTaskId() {
        return countdownTaskId;
    }

    // ---------------- AutoStart ----------------
    public void startAutoStart() {
        if (autoStartTaskId != -1) return;

        currentAutoStartTimer = maxAutoStartTime; // Reset to max time

        autoStartTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (currentAutoStartTimer <= 0) {
                stopAutoStart();
                arena.startGame();
                return;
            }

            // Get fresh player set each tick (players can join/leave)
            arena.getPlayers().forEach(player -> {
                switch (currentAutoStartTimer) {
                    case 60, 45, 30, 15, 10, 5, 4, 3, 2, 1 ->
                            player.sendMessage(plugin.getMsg().getMessage("starting-in", player).get(0));
                }
            });

            currentAutoStartTimer--;
        }, 0L, 20L);
    }

    public void stopAutoStart() {
        if (autoStartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoStartTaskId);
            autoStartTaskId = -1;
        }
    }

    public int getAutoStartTimer() {
        return currentAutoStartTimer;
    }

    public int getAutoStartTaskId() {
        return autoStartTaskId;
    }
}