package me.cbhud.castlesiege;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import io.github.regenerato.Regenerato;
import me.cbhud.castlesiege.arena.ArenaManager;
import me.cbhud.castlesiege.arena.ArenaResetManager;
import me.cbhud.castlesiege.arena.ArenaTimerManager;
import me.cbhud.castlesiege.cmd.*;
import me.cbhud.castlesiege.event.*;
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
    SetLobbyCommand slc;
    ConfigManager configManager;
    TeamManager teamManager;
    ScoreboardManager scoreboardManager;

    ArenaManager arenaManager;
    ArenaResetManager arenaResetManager;

    MobManager mobManager;

    ArenaSelector arenaSelector;
    TeamSelector teamSelector;

    PlayerManager playerManager;
    WorldEditPlugin worldEdit;
    KitManager kitManager;
    PlayerKitManager playerKitManager;
    KitSelector kitSelector;
    ItemManager itemManager;
    DataManager dataManager;
    KillEffectManager killEffectManager;
    BBar bossBar;

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
        getCommand("arena").setExecutor(new CreateArenaCommand(this, arenaManager));
        arenaSelector = new ArenaSelector(this);
        teamSelector = new TeamSelector(this);

        getCommand("leave").setExecutor(new LeaveArenaCommand(this));
        getCommand("setlobby").setExecutor(slc = new SetLobbyCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));

        getServer().getPluginManager().registerEvents(new JoinEvent(this), this);
        getServer().getPluginManager().registerEvents(new DeathEvent(this), this);
        getServer().getPluginManager().registerEvents(new RightClickEffects(this), this);
        getServer().getPluginManager().registerEvents(new DamageEvent(this), this);
        teamManager = new TeamManager(this, this.getConfig());
        mobManager = new MobManager(this);

        scoreboardManager = new ScoreboardManager(this);
        playerManager = new PlayerManager(this);
        getServer().getPluginManager().registerEvents(new MiscEvents(this), this);


        itemManager = new ItemManager(this);
        dataManager = new DataManager(this);
        kitManager = new KitManager(this);
        playerKitManager = new PlayerKitManager(this);
        kitSelector = new KitSelector(this);
        killEffectManager = new KillEffectManager(this);
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getServer().getPluginManager().registerEvents(new TNTThrower(this), this);
        bossBar = new BBar(this);
        bossBar.setIsEnabled( getConfigManager().getBossBarEnabled());
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

    public SetLobbyCommand getSlc() {
        return slc;
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

    public BBar getBBar() {
        return bossBar;
    }
}
