package me.cbhud.castlesiege.arena;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ArenaTimerManager {

    private final CastleSiege plugin;
    private final Arena arena;

    // Configuration values
    private final int maxCountdownTime;
    private final int maxAutoStartTime;

    // Current timer values
    private int currentCountdownTimer;
    private int currentAutoStartTimer;

    // Task IDs
    private int countdownTaskId = -1;
    private int autoStartTaskId = -1;

    // IMPORTANT: decouple winner logic from task id
    private boolean countdownExpired = false;

    // Announcement schedule (seconds remaining)
    private static final int[] AUTO_START_ANNOUNCE = {60, 45, 30, 15, 10, 5, 4, 3, 2, 1};

    public ArenaTimerManager(CastleSiege plugin, Arena arena, int countdownTimer, int autoStartTimer) {
        this.plugin = plugin;
        this.arena = arena;
        this.maxCountdownTime = countdownTimer;
        this.maxAutoStartTime = autoStartTimer;
    }

    /* ---------------- Countdown ---------------- */

    public void startCountdown() {
        if (countdownTaskId != -1) return;
        if (arena.getState() != ArenaState.IN_GAME) return;

        countdownExpired = false;
        currentCountdownTimer = maxCountdownTime;

        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Safety: if arena state changed, stop
            if (arena.getState() != ArenaState.IN_GAME) {
                stopCountdown();
                return;
            }

            if (currentCountdownTimer <= 0) {
                countdownExpired = true;
                stopCountdown();
                arena.endGame();
                return;
            }

            // Update scoreboard for current arena players (copy to avoid CME)
            for (Player p : arena.getOnlinePlayersSnapshot()) {
                if (p == null || !p.isOnline()) continue;
                plugin.getScoreboardManager().updateScoreboard(p, "in-game");
            }

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

    /** True iff the countdown reached 0 and triggered endGame. */
    public boolean didCountdownExpire() {
        return countdownExpired;
    }

    public boolean isCountdownRunning() {
        return countdownTaskId != -1;
    }

    /* ---------------- AutoStart ---------------- */

    public void startAutoStart() {
        if (autoStartTaskId != -1) return;
        if (arena.getState() != ArenaState.WAITING) return;

        currentAutoStartTimer = maxAutoStartTime;

        autoStartTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Safety: stop if arena isn't waiting anymore or player count dropped below min
            if (arena.getState() != ArenaState.WAITING || arena.getNoPlayers() < arena.getMin()) {
                stopAutoStart();
                return;
            }

            if (currentAutoStartTimer <= 0) {
                stopAutoStart();
                arena.startGame();
                return;
            }

            // Announcements
            if (shouldAnnounce(currentAutoStartTimer)) {
                for (Player p : arena.getOnlinePlayersSnapshot()) {
                    if (p == null || !p.isOnline()) continue;

                    String line = firstLine(plugin.getMsg().getMessage("starting-in", p));
                    if (line != null && !line.isEmpty()) {
                        p.sendMessage(line);
                    }
                }
            }

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

    public boolean isAutoStartRunning() {
        return autoStartTaskId != -1;
    }

    /* ---------------- Small helpers ---------------- */

    private boolean shouldAnnounce(int secondsLeft) {
        for (int t : AUTO_START_ANNOUNCE) {
            if (secondsLeft == t) return true;
        }
        return false;
    }

    private String firstLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return lines.get(0);
    }
}
