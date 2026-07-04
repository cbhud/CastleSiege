package me.cbhud.castlesiege;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.cbhud.castlesiege.arena.ArenaManager;
import me.cbhud.castlesiege.arena.ArenaResetManager;
import me.cbhud.castlesiege.cmd.CseCommand;
import me.cbhud.castlesiege.event.*;
import me.cbhud.castlesiege.gui.ArenaEditChatListener;
import me.cbhud.castlesiege.gui.ArenaSelector;
import me.cbhud.castlesiege.gui.KitSelector;
import me.cbhud.castlesiege.gui.TeamSelector;
import me.cbhud.castlesiege.kit.ItemManager;
import me.cbhud.castlesiege.kit.KillEffectManager;
import me.cbhud.castlesiege.kit.KitManager;
import me.cbhud.castlesiege.kit.PlayerKitManager;
import me.cbhud.castlesiege.player.PlayerManager;
import me.cbhud.castlesiege.scoreboard.NameTagManager;
import me.cbhud.castlesiege.scoreboard.ScoreboardManager;
import me.cbhud.castlesiege.team.TeamManager;
import me.cbhud.castlesiege.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class CastleSiege extends JavaPlugin {

    Messages msg;
    ConfigManager configManager;
    TeamManager teamManager;
    ScoreboardManager scoreboardManager;

    ArenaManager arenaManager;
    ArenaResetManager arenaResetManager;

    MobManager mobManager;

    ArenaSelector arenaSelector;
    TeamSelector teamSelector;
    ArenaEditChatListener arenaEditChatListener;

    PlayerManager playerManager;
    WorldEditPlugin worldEdit;
    KitManager kitManager;
    PlayerKitManager playerKitManager;
    KitSelector kitSelector;
    ItemManager itemManager;
    DataManager dataManager;
    KillEffectManager killEffectManager;
    BossBar bossBar;
    NameTagManager nameTagManager;

    @Override
    public void onEnable() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomPlaceholder(this).register();
        }
        configManager = new ConfigManager(this);
        msg = new Messages(this);
        arenaManager = new ArenaManager(this);
        arenaResetManager = new ArenaResetManager(this);

        Plugin fawePlugin = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (fawePlugin == null) {
            getLogger().severe("FastAsyncWorldEdit is required for CastleSiege arena regeneration. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Plugin worldEditPlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEditPlugin instanceof WorldEditPlugin) {
            worldEdit = (WorldEditPlugin) worldEditPlugin;
        } else {
            worldEdit = null;
            getLogger().info("Using FastAsyncWorldEdit and Regenerato without a standalone WorldEdit plugin.");
        }

        arenaSelector = new ArenaSelector(this);
        teamSelector = new TeamSelector(this);

        getServer().getPluginManager().registerEvents(new JoinEvent(this), this);
        getServer().getPluginManager().registerEvents(new DeathEvent(this), this);
        getServer().getPluginManager().registerEvents(new RightClickEffects(this), this);
        getServer().getPluginManager().registerEvents(new DamageEvent(this), this);
        teamManager = new TeamManager(this, this.getConfig());
        mobManager = new MobManager(this);

        if (areScoreboardsEnabled()) {
            scoreboardManager = new ScoreboardManager(this);
        } else {
            scoreboardManager = null;
            getLogger().info("Scoreboards are disabled in scoreboards.yml.");
        }
        playerManager = new PlayerManager(this);
        getServer().getPluginManager().registerEvents(new MiscEvents(this), this);

        CseCommand cse = new CseCommand(this);

        getCommand("cse").setExecutor(cse);
        getCommand("cse").setTabCompleter(cse);

        itemManager = new ItemManager(this);
        dataManager = new DataManager(this);
        kitManager = new KitManager(this);
        playerKitManager = new PlayerKitManager(this);
        kitSelector = new KitSelector(this);
        killEffectManager = new KillEffectManager(this);
        getServer().getPluginManager().registerEvents(new TNTThrower(this), this);
        bossBar = new BossBar(this);
        bossBar.setIsEnabled( getConfigManager().getBossBarEnabled());

        this.arenaEditChatListener = new ArenaEditChatListener(this);
        Bukkit.getPluginManager().registerEvents(arenaEditChatListener, this);
        this.nameTagManager = new NameTagManager(this);
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.disconnect();
        }
        if (bossBar != null) {
            bossBar.cleanup();
        }
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public TeamSelector getTeamSelector() {
        return teamSelector;
    }

    public ArenaSelector getArenaSelector() {
        return arenaSelector;
    }

    public Messages getMsg(){
        return msg;
    }

    public ConfigManager getConfigManager(){
        return configManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ArenaManager getArenaManager(){
        return arenaManager;
    }

    public ArenaResetManager getArenaResetManager(){
        return arenaResetManager;
    }

    public ScoreboardManager getScoreboardManager(){
        return scoreboardManager;
    }

    public void setupScoreboard(Player player) {
        if (scoreboardManager != null) {
            scoreboardManager.setupScoreboard(player);
        }
    }

    public void updateScoreboard(Player player, String state) {
        if (scoreboardManager != null) {
            scoreboardManager.updateScoreboard(player, state);
        }
    }

    public void removeScoreboard(Player player) {
        if (scoreboardManager != null) {
            scoreboardManager.removeScoreboard(player);
        }
    }

    public void removeScoreboard(UUID uuid) {
        if (scoreboardManager != null) {
            scoreboardManager.removeScoreboard(uuid);
        }
    }

    private boolean areScoreboardsEnabled() {
        File file = new File(getDataFolder(), "scoreboards.yml");
        if (!file.exists()) {
            saveResource("scoreboards.yml", false);
        }

        return YamlConfiguration.loadConfiguration(file).getBoolean("settings.enabled", true);
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEdit;
    }
    public KitManager getKitManager() {return kitManager;}
    public PlayerKitManager getPlayerKitManager() {
        return playerKitManager;
    }
    public KitSelector getKitSelector() {
        return kitSelector;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public KillEffectManager getKillEffectManager(){
        return killEffectManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public BossBar getBBar() {
        return bossBar;
    }

    public ArenaEditChatListener getArenaEditChatListener() { return arenaEditChatListener; }

    public NameTagManager getNameTagManager(){
        return nameTagManager;
    }


}
