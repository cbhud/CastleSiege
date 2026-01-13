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
import me.cbhud.castlesiege.scoreboard.ScoreboardManager;
import me.cbhud.castlesiege.team.TeamManager;
import me.cbhud.castlesiege.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

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
    private ArenaEditChatListener arenaEditChatListener;

    PlayerManager playerManager;
    WorldEditPlugin worldEdit;
    KitManager kitManager;
    PlayerKitManager playerKitManager;
    KitSelector kitSelector;
    ItemManager itemManager;
    DataManager dataManager;
    KillEffectManager killEffectManager;
    BossBar bossBar;

    @Override
    public void onEnable() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomPlaceholder(this).register();
        }
        configManager = new ConfigManager(this);
        msg = new Messages(this);
        arenaManager = new ArenaManager(this);
        arenaResetManager = new ArenaResetManager(this);
        worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            getLogger().severe("Regenerato or FAWE plugin not found! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        arenaSelector = new ArenaSelector(this);
        teamSelector = new TeamSelector(this);

        getServer().getPluginManager().registerEvents(new JoinEvent(this), this);
        getServer().getPluginManager().registerEvents(new DeathEvent(this), this);
        getServer().getPluginManager().registerEvents(new RightClickEffects(this), this);
        getServer().getPluginManager().registerEvents(new DamageEvent(this), this);
        teamManager = new TeamManager(this, this.getConfig());
        mobManager = new MobManager(this);

        scoreboardManager = new ScoreboardManager(this);
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
    }

    @Override
    public void onDisable() {
        getDataManager().disconnect();
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


}
