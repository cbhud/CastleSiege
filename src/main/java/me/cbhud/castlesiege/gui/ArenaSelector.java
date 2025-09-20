package me.cbhud.castlesiege.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaManager;
import me.cbhud.castlesiege.arena.ArenaState;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;


public class ArenaSelector {

    private final Gui gui;
    private final CastleSiege plugin;
    private final ArenaManager arenaManager;

    public ArenaSelector(CastleSiege plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        gui = Gui.gui()
                .title(Component.text(plugin.getMsg().getGuiMessage("select-arena-in-gui").get(0)))
                .rows(4)
                .create();
    }

    // Removed synchronization
    private void init() {
        int slot = 0;
        for (Arena arena : arenaManager.getArenas()) {
            ArenaState state = arena.getState();
            Material woolMaterial;
            ChatColor statusColor;
            String status;

            switch (state) {
                case IN_GAME:
                    woolMaterial = Material.GREEN_WOOL;
                    statusColor = ChatColor.GREEN;
                    status = plugin.getMsg().getGuiMessage("in-game-status").get(0);
                    break;
                case WAITING:
                    woolMaterial = Material.LIME_WOOL;
                    statusColor = ChatColor.YELLOW;
                    status = plugin.getMsg().getGuiMessage("waiting-status").get(0);
                    break;
                case ENDED:
                    woolMaterial = Material.RED_WOOL;
                    statusColor = ChatColor.DARK_RED;
                    status = plugin.getMsg().getGuiMessage("restarting-status").get(0);
                    break;
                default:
                    woolMaterial = Material.WHITE_WOOL;
                    statusColor = ChatColor.GRAY;
                    status = plugin.getMsg().getGuiMessage("unknown-status").get(0);
                    break;
            }

            // Get all lines from the config and process them dynamically
            List<String> configLines = plugin.getMsg().getGuiMessage("arena-hover-gui");
            List<Component> loreComponents = new ArrayList<>();

            for (String line : configLines) {
                // Replace all placeholders in each line
                String processedLine = replacePlaceholders(line, arena, status);
                loreComponents.add(Component.text(processedLine));
            }

            GuiItem arenaItem = ItemBuilder.from(woolMaterial)
                    .name(Component.text(statusColor + arena.getId()))
                    .lore(loreComponents)
                    .asGuiItem(event -> handleArenaSelection(event, arena));

            gui.setItem(slot++, arenaItem);
        }
    }

    // Method to replace all placeholders - works with your Messages class
    private String replacePlaceholders(String text, Arena arena, String status) {
        // Note: The Messages.getGuiMessage() already handles ChatColor.translateAlternateColorCodes
        // so we don't need to worry about color codes here, just placeholder replacement
        return text
                .replace("{players}", String.valueOf(arena.getNoPlayers()))
                .replace("{status}", status)
                .replace("{arena}", arena.getId())
                .replace("{map}", arena.getWorldName() != null
                        ? arena.getWorldName().replaceAll("\\d", "")
                        : "Unknown")
                .replace("{type}", arena.isHardcore() ? "Hardcore" : "Normal")
                // Add any other placeholders you need based on your Arena class methods
                ;
    }

    // Removed synchronization here as well
    private void handleArenaSelection(InventoryClickEvent event, Arena arena) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;

        event.setCancelled(true);

            plugin.getArenaManager().addPlayerToArena(player, arena);

        gui.close(player);
    }

    // Open GUI with sync task
    public void open(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            init();
            gui.open(player);
        });
    }
}
