package me.cbhud.castlesiege.cmd;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveArenaCommand implements CommandExecutor {
    private final CastleSiege plugin;

    public LeaveArenaCommand(CastleSiege plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Usage: /leave");
            return true;
        }
        if (plugin.getPlayerManager().getPlayerState(player) == me.cbhud.castlesiege.player.PlayerState.PLAYING) {
            player.sendMessage(ChatColor.RED + "You cannot leave now!");
            return true;
        }

        plugin.getArenaManager().removePlayerFromArena(player);


        return true;
    }
}
