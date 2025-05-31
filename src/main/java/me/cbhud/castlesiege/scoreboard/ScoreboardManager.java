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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardManager {
    private final CastleSiege plugin;
    private final Map<Player, FastBoard> scoreboards;
    private FileConfiguration scoreboardConfig;

    public ScoreboardManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.scoreboards = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "scoreboards.yml");
        if (!file.exists()) {
            plugin.saveResource("scoreboards.yml", false);
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void setupScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        scoreboards.put(player, board);
        updateScoreboard(player, "lobby");
    }

    public void updateScoreboard(Player player, String state) {
        FastBoard board = scoreboards.get(player);
        if (board == null) return;

        String gameState = state.toLowerCase();
        String title = applyPlaceholders(player, applyColorCodes(scoreboardConfig.getString(gameState + ".Title", "&6Default Title")));

        board.updateTitle(title);
        board.updateLines(getScoreboardLines(gameState, player));
    }

    private List<String> getScoreboardLines(String gameState, Player player) {
        List<String> lines = scoreboardConfig.getStringList(gameState + ".lines");
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            processedLines.add(applyPlaceholders(player, applyColorCodes(line)));
        }

        return processedLines;
    }

    private String applyColorCodes(String text) {
        if (text == null) return "";

        // Convert & to color codes
        text = ChatColor.translateAlternateColorCodes('&', text);

        // Convert RGB placeholders (<#RRGGBB>) to ChatColor.of()
        Pattern pattern = Pattern.compile("<#([A-Fa-f0-9]{6})>");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hexCode);
            text = text.replace(matcher.group(), color.toString());
        }

        return text;
    }


    private String applyGradient(String text) {
        Pattern pattern = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)<#([A-Fa-f0-9]{6})>");
        Matcher matcher = pattern.matcher(text);

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

    private String gradientText(String text, String startHex, String endHex) {
        ChatColor start = ChatColor.of("#" + startHex);
        ChatColor end = ChatColor.of("#" + endHex);
        StringBuilder gradient = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            double ratio = (double) i / (text.length() - 1);
            ChatColor color = interpolateColor(start, end, ratio);
            gradient.append(color).append(text.charAt(i));
        }

        return gradient.toString();
    }

    private ChatColor interpolateColor(ChatColor start, ChatColor end, double ratio) {
        int r = (int) (start.getColor().getRed() + ratio * (end.getColor().getRed() - start.getColor().getRed()));
        int g = (int) (start.getColor().getGreen() + ratio * (end.getColor().getGreen() - start.getColor().getGreen()));
        int b = (int) (start.getColor().getBlue() + ratio * (end.getColor().getBlue() - start.getColor().getBlue()));
        return ChatColor.of(new java.awt.Color(r, g, b));
    }

    private String applyPlaceholders(Player player, String text) {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? PlaceholderAPI.setPlaceholders(player, text)
                : text;
    }

    public void removeScoreboard(Player player) {
        FastBoard board = scoreboards.remove(player);
        if (board != null) {
            board.delete();
        }
    }
}
