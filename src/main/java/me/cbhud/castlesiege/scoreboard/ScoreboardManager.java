package me.cbhud.castlesiege.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import me.cbhud.castlesiege.CastleSiege;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardManager {

    private final CastleSiege plugin;

    // UUID -> FastBoard (no Player keys)
    private final Map<UUID, FastBoard> scoreboards = new ConcurrentHashMap<>();

    private FileConfiguration scoreboardConfig;
    private boolean placeholderEnabled;

    // Precompiled patterns (performance)
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)<#([A-Fa-f0-9]{6})>");

    public ScoreboardManager(CastleSiege plugin) {
        this.plugin = plugin;
        loadConfig();
        refreshPlaceholderFlag();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "scoreboards.yml");
        if (!file.exists()) {
            plugin.saveResource("scoreboards.yml", false);
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void refreshPlaceholderFlag() {
        this.placeholderEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /** Optional: call if you support /reloadscoreboards or similar. */
    public void reloadConfig() {
        loadConfig();
        refreshPlaceholderFlag();
    }

    public void setupScoreboard(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        // If one already exists, delete it cleanly first
        FastBoard existing = scoreboards.remove(uuid);
        if (existing != null) existing.delete();

        FastBoard board = new FastBoard(player);
        scoreboards.put(uuid, board);

        updateScoreboard(player, "lobby");
    }

    public void updateScoreboard(Player player, String state) {
        if (player == null || state == null) return;

        UUID uuid = player.getUniqueId();
        FastBoard board = scoreboards.get(uuid);

        // If board missing (e.g., plugin reload edge case), create it
        if (board == null) {
            setupScoreboard(player);
            board = scoreboards.get(uuid);
            if (board == null) return;
        }

        String gameState = state.toLowerCase(Locale.ROOT);

        String rawTitle = scoreboardConfig.getString(gameState + ".Title", "&6Default Title");
        String title = processText(player, rawTitle);

        board.updateTitle(title);
        board.updateLines(getScoreboardLines(gameState, player));
    }

    private List<String> getScoreboardLines(String gameState, Player player) {
        List<String> lines = scoreboardConfig.getStringList(gameState + ".lines");
        if (lines == null || lines.isEmpty()) return Collections.emptyList();

        List<String> processed = new ArrayList<>(lines.size());
        for (String line : lines) {
            processed.add(processText(player, line));
        }
        return processed;
    }

    private String processText(Player player, String text) {
        if (text == null) return "";
        String out = applyColorCodes(text);
        // If you ever want gradients, uncomment next line and allow gradient syntax
        // out = applyGradient(out);
        return applyPlaceholders(player, out);
    }

    private String applyColorCodes(String text) {
        // Convert & codes
        String out = ChatColor.translateAlternateColorCodes('&', text);

        // Convert <#RRGGBB> to ChatColor.of()
        Matcher matcher = HEX_PATTERN.matcher(out);
        while (matcher.find()) {
            String hex = matcher.group(1);
            out = out.replace(matcher.group(), ChatColor.of("#" + hex).toString());
        }

        return out;
    }

    // Currently unused; kept for future use safely
    @SuppressWarnings("unused")
    private String applyGradient(String text) {
        if (text == null || text.isEmpty()) return "";

        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String content = matcher.group(2);
            String endColor = matcher.group(3);
            matcher.appendReplacement(buffer, gradientText(content, startColor, endColor));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    @SuppressWarnings("unused")
    private String gradientText(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return "";

        // Prevent divide-by-zero for 1-char strings
        if (text.length() == 1) {
            return ChatColor.of("#" + startHex) + text;
        }

        ChatColor start = ChatColor.of("#" + startHex);
        ChatColor end = ChatColor.of("#" + endHex);

        StringBuilder gradient = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            double ratio = (double) i / (text.length() - 1);
            ChatColor color = interpolateColor(start, end, ratio);
            gradient.append(color).append(text.charAt(i));
        }
        return gradient.toString();
    }

    @SuppressWarnings("unused")
    private ChatColor interpolateColor(ChatColor start, ChatColor end, double ratio) {
        int r = (int) (start.getColor().getRed() + ratio * (end.getColor().getRed() - start.getColor().getRed()));
        int g = (int) (start.getColor().getGreen() + ratio * (end.getColor().getGreen() - start.getColor().getGreen()));
        int b = (int) (start.getColor().getBlue() + ratio * (end.getColor().getBlue() - start.getColor().getBlue()));
        return ChatColor.of(new java.awt.Color(r, g, b));
    }

    private String applyPlaceholders(Player player, String text) {
        if (!placeholderEnabled || player == null || text == null) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public void removeScoreboard(Player player) {
        if (player == null) return;
        removeScoreboard(player.getUniqueId());
    }

    public void removeScoreboard(UUID uuid) {
        if (uuid == null) return;
        FastBoard board = scoreboards.remove(uuid);
        if (board != null) board.delete();
    }
}
