package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaManager;
import me.cbhud.castlesiege.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CseCommand implements CommandExecutor, TabCompleter {

    private final CastleSiege plugin;
    private final ArenaManager arenaManager;

    // Simple per-player arena setup session
    private final Map<UUID, Arena> setupSession = new HashMap<>();

    public CseCommand(CastleSiege plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Allow old commands to route through here
        List<String> tokens = new ArrayList<>();
        String root = command.getName().toLowerCase();

        if (!root.equals("cse")) {
            tokens.add(root); // treat it like /cse <root> ...
        }
        tokens.addAll(Arrays.asList(args));

        if (tokens.isEmpty()) {
            sendHelp(sender);
            return true;
        }

        String sub = tokens.get(0).toLowerCase();
        String[] subArgs = tokens.size() > 1
                ? tokens.subList(1, tokens.size()).toArray(new String[0])
                : new String[0];

        return switch (sub) {
            case "join" -> cmdJoin(sender, subArgs);
            case "randomjoin" -> cmdRandomJoin(sender, subArgs);
            case "leave" -> cmdLeave(sender, subArgs);
            case "stats" -> cmdStats(sender, subArgs);
            case "coins" -> cmdCoins(sender, subArgs);
            case "arena" -> cmdArena(sender, subArgs);
            case "setlobby" -> cmdSetLobby(sender, subArgs);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    // ---------------- HELP ----------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "CastleSiege Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/cse join <arena>");
        sender.sendMessage(ChatColor.YELLOW + "/cse randomjoin");
        sender.sendMessage(ChatColor.YELLOW + "/cse leave");
        sender.sendMessage(ChatColor.YELLOW + "/cse stats [player]");
        sender.sendMessage(ChatColor.YELLOW + "/cse arena <create|setlobby|setking|setdefenders|setattackers|finish|cancel>");
        sender.sendMessage(ChatColor.YELLOW + "/cse coins <add|remove> <player> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/cse setlobby");
    }

    // ---------------- JOIN ----------------

    private boolean cmdJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /cse join <arenaName>");
            listArenas(player);
            return true;
        }

        Arena target = findArena(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + args[0] + "' not found.");
            listArenas(player);
            return true;
        }

        return joinFlow(player, target);
    }

    private boolean cmdRandomJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Usage: /cse randomjoin");
            return true;
        }

        Arena target = pickRandomArena();
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No available arenas found.");
            return true;
        }

        return joinFlow(player, target);
    }

    private boolean joinFlow(Player player, Arena target) {
        // If already in an arena, leave first
        Arena current = arenaManager.getArenaByPlayer(player.getUniqueId());
        if (current != null && current != target) {
            arenaManager.removePlayerFromArena(player);
        }

        // Arena class already handles: ended/full/in-game spectator
        arenaManager.addPlayerToArena(player, target);
        return true;
    }

    private Arena pickRandomArena() {
        List<Arena> list = new ArrayList<>(arenaManager.getArenas());

        // Prefer WAITING and not full
        List<Arena> waiting = list.stream()
                .filter(a -> a.getState() == ArenaState.WAITING)
                .filter(a -> a.getNoPlayers() < a.getMax())
                .filter(a -> a.getWorld() != null)
                .toList();

        if (!waiting.isEmpty()) {
            return waiting.get(ThreadLocalRandom.current().nextInt(waiting.size()));
        }

        // Fallback: allow IN_GAME (spectator join handled in Arena.addPlayer)
        List<Arena> ingame = list.stream()
                .filter(a -> a.getState() == ArenaState.IN_GAME)
                .filter(a -> a.getWorld() != null)
                .toList();

        if (!ingame.isEmpty()) {
            return ingame.get(ThreadLocalRandom.current().nextInt(ingame.size()));
        }

        return null;
    }

    private void listArenas(Player player) {
        player.sendMessage(ChatColor.GOLD + "Arenas:");
        for (Arena a : arenaManager.getArenas()) {
            player.sendMessage(ChatColor.YELLOW + "- " + a.getId() + ChatColor.GRAY +
                    " (" + a.getNoPlayers() + "/" + a.getMax() + ") " + ChatColor.DARK_GRAY + a.getState());
        }
    }

    private Arena findArena(String name) {
        for (Arena a : arenaManager.getArenas()) {
            if (a.getId().equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    // ---------------- LEAVE ----------------

    private boolean cmdLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Usage: /cse leave");
            return true;
        }

        // Keep your rule: cannot leave while PLAYING
        if (plugin.getPlayerManager().getPlayerState(player) == me.cbhud.castlesiege.player.PlayerState.PLAYING) {
            player.sendMessage(ChatColor.RED + "You cannot leave right now!");
            return true;
        }

        Arena current = arenaManager.getArenaByPlayer(player.getUniqueId());
        if (current == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not in an arena.");
            return true;
        }

        arenaManager.removePlayerFromArena(player);
        player.sendMessage(ChatColor.GREEN + "You left the arena.");
        return true;
    }

    // ---------------- STATS ----------------

    private boolean cmdStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        String targetName = (args.length == 1) ? args[0] : viewer.getName();
        boolean isOwn = targetName.equalsIgnoreCase(viewer.getName());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID targetUuid;

                if (isOwn) {
                    targetUuid = viewer.getUniqueId();
                } else {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                    if (!off.hasPlayedBefore() && !off.isOnline()) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> viewer.sendMessage(ChatColor.RED + "That player has never played before."));
                        return;
                    }
                    targetUuid = off.getUniqueId();
                }

                int wins = plugin.getDataManager().getPlayerWins(targetUuid);
                int kills = plugin.getDataManager().getPlayerKills(targetUuid);
                int deaths = plugin.getDataManager().getPlayerDeaths(targetUuid);
                int coins = plugin.getDataManager().getPlayerCoins(targetUuid);

                double kdr = deaths > 0 ? (double) kills / deaths : kills;

                String finalTargetName = targetName;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (String line : plugin.getMsg().getMessage("stats-msg", viewer)) {
                        viewer.sendMessage(line
                                .replace("{cs_player_name}", finalTargetName)
                                .replace("{cs_wins}", String.valueOf(wins))
                                .replace("{cs_kills}", String.valueOf(kills))
                                .replace("{cs_deaths}", String.valueOf(deaths))
                                .replace("{cs_coins}", String.valueOf(coins))
                                .replace("{cs_kdr}", String.valueOf(kdr))
                        );
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().warning("Stats error for " + targetName + ": " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> viewer.sendMessage(ChatColor.RED + "Error retrieving stats."));
            }
        });

        return true;
    }

    // ---------------- COINS ----------------

    private boolean cmdCoins(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cs.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /cse coins <add|remove> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "That player has never played before.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (action.equals("add")) {
            plugin.getDataManager().addPlayerCoins(uuid, amount);
            sender.sendMessage(ChatColor.GREEN + "Added " + amount + " coins to " + target.getName() + ".");
        } else if (action.equals("remove")) {
            plugin.getDataManager().addPlayerCoins(uuid, -amount); // FIXED
            sender.sendMessage(ChatColor.GREEN + "Removed " + amount + " coins from " + target.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid action. Use add or remove.");
        }

        return true;
    }

    // ---------------- SETLOBBY ----------------
    private boolean cmdSetLobby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("cs.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Usage: /cse setlobby");
            return true;
        }

        try {
            File file = new File(plugin.getDataFolder(), "lobby-location.yml");

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            Location loc = player.getLocation();

            cfg.set("lobby.world", loc.getWorld().getName());
            cfg.set("lobby.x", loc.getX());
            cfg.set("lobby.y", loc.getY());
            cfg.set("lobby.z", loc.getZ());
            cfg.set("lobby.yaw", loc.getYaw());
            cfg.set("lobby.pitch", loc.getPitch());

            cfg.save(file);

            player.sendMessage(ChatColor.GREEN + "Lobby location set!");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save lobby-location.yml: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Failed to save lobby location. Check console.");
        }

        return true;
    }

    // ---------------- ARENA SETUP ----------------

    private boolean cmdArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("cs.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /cse arena <create|edit|setlobby|setking|setdefenders|setattackers|finish|cancel> ...");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /cse arena create <arenaName>");
                    return true;
                }

                String id = args[1];

                if (findArena(id) != null) {
                    player.sendMessage(ChatColor.RED + "An arena with that name already exists.");
                    return true;
                }

                // Enforce: 1 arena per world
                for (Arena a : arenaManager.getArenas()) {
                    if (a.getWorldName() != null && a.getWorldName().equalsIgnoreCase(player.getWorld().getName())) {
                        player.sendMessage(ChatColor.RED + "This world already has an arena (" + a.getId() + ").");
                        return true;
                    }
                }

                // Defaults (keep your old behavior)
                Arena arena = new Arena(plugin, id,
                        null, null, null, null,
                        16, 4,
                        60, 300,
                        player.getWorld().getName(),
                        false
                );

                setupSession.put(player.getUniqueId(), arena);
                player.sendMessage(ChatColor.GREEN + "Arena setup started for '" + id + "'.");
                player.sendMessage(ChatColor.YELLOW + "/cse arena setlobby | setking | setdefenders | setattackers | finish");
                return true;
            }
            case "setlobby" -> setSetupLocation(player, "lobby");
            case "setking" -> setSetupLocation(player, "king");
            case "setdefenders" -> setSetupLocation(player, "defenders");
            case "setattackers" -> setSetupLocation(player, "attackers");
            case "edit" -> {
                new me.cbhud.castlesiege.gui.ArenaEditorGui(plugin).open(player);
                return true;
            }
            case "cancel" -> {
                setupSession.remove(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Arena setup cancelled.");
                return true;
            }
            case "finish" -> {
                Arena arena = setupSession.get(player.getUniqueId());
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "You are not setting up an arena.");
                    return true;
                }

                if (arena.getLSpawn() == null || arena.getKingSpawn() == null
                        || arena.getDefendersSpawn() == null || arena.getAttackersSpawn() == null) {
                    player.sendMessage(ChatColor.RED + "You have not set all locations yet.");
                    return true;
                }

                arenaManager.addArena(arena, player);
                setupSession.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Arena saved to arenas.yml");
                return true;
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
            }
        }

        return true;
    }

    private void setSetupLocation(Player player, String type) {
        Arena arena = setupSession.get(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "You are not setting up an arena. Use /cse arena create <name>");
            return;
        }

        Location loc = player.getLocation();
        switch (type) {
            case "lobby" -> {
                arena.setLobbySpawn(loc);
                player.sendMessage(ChatColor.GREEN + "Lobby spawn set.");
            }
            case "king" -> {
                arena.setKingSpawn(loc);
                player.sendMessage(ChatColor.GREEN + "King spawn set.");
            }
            case "defenders" -> {
                arena.setDefendersSpawn(loc);
                player.sendMessage(ChatColor.GREEN + "Defenders spawn set.");
            }
            case "attackers" -> {
                arena.setAttackersSpawn(loc);
                player.sendMessage(ChatColor.GREEN + "Attackers spawn set.");
            }
        }
    }

    // ---------------- TAB COMPLETE (basic) ----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        String root = command.getName().toLowerCase();
        List<String> tokens = new ArrayList<>();
        if (!root.equals("cse")) tokens.add(root);
        tokens.addAll(Arrays.asList(args));

        // Example: /cse <sub>
        if (tokens.size() == 1) {
            return partial(tokens.get(0), List.of("join", "randomjoin", "leave", "stats", "arena", "coins", "setlobby"));
        }

        String sub = tokens.get(0).toLowerCase();

        // /cse join <arena>
        if (sub.equals("join") && tokens.size() == 2) {
            List<String> names = arenaManager.getArenas().stream().map(Arena::getId).toList();
            return partial(tokens.get(1), names);
        }

        // /cse arena <sub>
        if (sub.equals("arena") && tokens.size() == 2) {
            return partial(tokens.get(1), List.of("create", "setlobby", "setking", "setdefenders", "setattackers", "finish", "cancel"));
        }

        // /cse coins <add|remove>
        if (sub.equals("coins") && tokens.size() == 2) {
            return partial(tokens.get(1), List.of("add", "remove"));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> options) {
        String in = input == null ? "" : input.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(in)) out.add(opt);
        }
        return out;
    }
}
