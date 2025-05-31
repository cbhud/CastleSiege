package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class CoinsCommand implements CommandExecutor {

    private final CastleSiege plugin;

    public CoinsCommand(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cs.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage("§cUsage: /coins <add|remove> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a positive number.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();


            if (action.equals("add")) {
                plugin.getDataManager().addPlayerCoins(uuid, amount);
                sender.sendMessage("§aAdded " + amount + " coins to " + target.getName() + ".");
            } else if (action.equals("remove")) {
                plugin.getDataManager().addPlayerCoins(uuid, amount);
                sender.sendMessage("§aRemoved " + amount + " coins from " + target.getName() + ".");
            } else {
                sender.sendMessage("§cInvalid action. Use add or remove.");
            }

        return true;
    }
}
