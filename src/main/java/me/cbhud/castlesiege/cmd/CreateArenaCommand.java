package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class CreateArenaCommand implements CommandExecutor {

    private final CastleSiege plugin;
    private final ArenaManager arenaManager;
    private final HashMap<String, Arena> arenaSetup = new HashMap<>();
    private final HashMap<UUID, String> playerSetup = new HashMap<>();

    public CreateArenaCommand(CastleSiege plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUsage: /arena <create/setlobby/setking/setdefenders/setattackers/finish>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /arena create <arenaName>");
                    return true;
                }
                String arenaName = args[1];
                if (arenaSetup.containsKey(arenaName)) {
                    player.sendMessage("§cThis arena is already being set up.");
                    return true;
                }
                arenaSetup.put(arenaName, new Arena(plugin, arenaName, null, null, null, null, 16, 4, 60, 300, player.getWorld().getName()));
                playerSetup.put(player.getUniqueId(), arenaName);
                player.sendMessage("§aArena setup started! Now set the locations:");
                player.sendMessage("§a/arena setlobby, /arena setking");
                player.sendMessage("§a/arena setdefenders, /arena setattackers");
                break;

            case "setlobby":
                setLocation(player, "lobby");
                break;
            case "setking":
                setLocation(player, "king");
                break;
            case "setdefenders":
                setLocation(player, "defenders");
                break;
            case "setattackers":
                setLocation(player, "attackers");
                break;
            case "finish":
                finishSetup(player);
                break;
            default:
                player.sendMessage("§cUnknown subcommand");
                player.sendMessage("§cUsage: /arena <create/setlobby/setking/setdefenders/setattackers/finish>");
        }
        return true;
    }

    private void setLocation(Player player, String type) {
        Arena arena = getPlayerArena(player);
        if (arena == null) return;

        Location loc = player.getLocation();
        switch (type) {
            case "lobby":
                arena.setLobbySpawn(loc);
                player.sendMessage("§aLobby spawn set!");
                break;
            case "king":
                arena.setKingSpawn(loc);
                player.sendMessage("§aKing spawn set!");
                break;
            case "defenders":
                arena.setDefendersSpawn(loc);
                player.sendMessage("§aDefenders spawn set!");
                break;
            case "attackers":
                arena.setAttackersSpawn(loc);
                player.sendMessage("§aAttackers spawn set!");
                break;
        }
    }

    private void finishSetup(Player player) {
        Arena arena = getPlayerArena(player);
        if (arena == null) return;

        if (arena.getLSpawn() != null &&
                arena.getKingSpawn() != null &&
                arena.getDefendersSpawn() != null &&
                arena.getAttackersSpawn() != null) {

            arenaManager.addArena(arena, player);
            arenaSetup.remove(arena.getId());
            playerSetup.remove(player.getUniqueId());
            player.sendMessage("§aArena setup complete! Arena saved to arenas.yml");
        } else {
            player.sendMessage("§cYou have not set all necessary locations!");
        }
    }

    private Arena getPlayerArena(Player player) {
        String arenaName = playerSetup.get(player.getUniqueId());
        if (arenaName == null) {
            player.sendMessage("§cYou are not setting up an arena. Use /arena create <name>");
            return null;
        }
        return arenaSetup.get(arenaName);
    }
}