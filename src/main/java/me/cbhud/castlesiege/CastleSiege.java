package me.cbhud.castlesiege;

import me.cbhud.castlesiege.commands.CoinsCommand;
import me.cbhud.castlesiege.commands.Commands;
import me.cbhud.castlesiege.commands.StatsCommand;
import me.cbhud.castlesiege.commands.UnlockKitCommand;
import me.cbhud.castlesiege.util.*;
import org.bukkit.plugin.java.*;
import me.cbhud.castlesiege.player.*;
import me.cbhud.castlesiege.scoreboard.*;
import me.cbhud.castlesiege.gui.*;
import me.cbhud.castlesiege.team.*;
import me.cbhud.castlesiege.kits.*;
import org.bukkit.plugin.*;
import me.cbhud.castlesiege.event.*;
import me.cbhud.castlesiege.game.*;


public class CastleSiege extends JavaPlugin
{
    private Game game;
    private GameEndHandler gameEndHandler;
    private TeamManager teamManager;
    private PlayerManager playerManager;
    private ScoreboardManager scoreboardManager;
    private Timers timers;
    private MapRegeneration mapRegeneration;
    private TeamSelector teamSelector;
    private KitSelector kitSelector;
    private MobManager mobManager;
    private LocationManager locationManager;
    private Manager manager;
    private Team team;
    private KitManager kitManager;
    private PlayerKitManager playerKitManager;
    private DataManager dataManager;
    private ConfigManager configManager;
    private MessagesConfiguration messagesConfig;
    private TNTThrower tntThrower;
    private BBar bossBar;
    private Commands commands;

    public void onEnable() {

        configManager = new ConfigManager(this);
        configManager.setup();
        dataManager = new DataManager(this);
        dataManager.connect();
        messagesConfig = new MessagesConfiguration(this);
        messagesConfig.loadConfig();
        bossBar = new BBar(this);
        this.game = new Game(this);
        manager = new Manager();
        kitManager = new KitManager(this);
        this.playerKitManager = new PlayerKitManager(this);
        this.teamManager = new TeamManager(this, this.getConfig());
        this.playerManager = new PlayerManager(this);
        this.mobManager = new MobManager(this);
        this.locationManager = new LocationManager(this);
        this.timers = new Timers(this);
        this.gameEndHandler = new GameEndHandler(this, team);
        this.saveDefaultConfig();
        this.reloadConfig();
        this.getCommand("kit").setExecutor(new UnlockKitCommand(this));
        this.getCommand("stats").setExecutor(new StatsCommand(this));
        this.getCommand("coins").setExecutor(new CoinsCommand(this));
        commands = new Commands(this);
        this.getCommand("cs").setExecutor(commands);
        this.getServer().getPluginManager().registerEvents(new PlayerConnection(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerDeathHandler(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents(new DamageListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents(new RightClickEffects(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents(new MiscEvents(this), (Plugin)this);

        this.teamSelector = new TeamSelector(this);
        this.kitSelector = new KitSelector(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.game.setState(GameState.LOBBY);
        mapRegeneration = new MapRegeneration(this);
        getServer().getPluginManager().registerEvents(mapRegeneration, this);
        tntThrower = new TNTThrower(this);
        getServer().getPluginManager().registerEvents(tntThrower, this);
        this.getServer().getConsoleSender().sendMessage("CastleSiege has been enabled!");
    }

    public void onDisable() {
        dataManager.disconnect();
        this.getServer().getConsoleSender().sendMessage("CastleSiege has been disabled!");
    }

    public Game getGame() {
        return this.game;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TeamManager getTeamManager() {
        return this.teamManager;
    }

    public GameEndHandler getGameEndHandler() {
        return this.gameEndHandler;
    }

    public Timers getTimer() {
        return this.timers;
    }
    public TNTThrower tntThrower(){return this.tntThrower;}

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public PlayerKitManager getPlayerKitManager() {
        return this.playerKitManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }
    public KitManager getKitManager(){return  this.kitManager;}
    public TeamSelector getTeamSelector() {
        return this.teamSelector;
    }

    public DataManager getDbConnection() {
        return dataManager;
    }

    public MobManager getMobManager(){
        return this.mobManager;
    }

    public KitSelector getKitSelector() {
        return this.kitSelector;
    }
    public MapRegeneration getMapRegeneration() {
        return mapRegeneration;
    }

    public LocationManager getLocationManager(){return  locationManager;}
    public MessagesConfiguration getMessagesConfig(){return messagesConfig;}
    public BBar getBossBar(){return  bossBar;}
    public Commands getCommands() {
        return commands;
    }
}
