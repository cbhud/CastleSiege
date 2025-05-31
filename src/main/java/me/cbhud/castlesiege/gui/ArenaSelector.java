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

            String msg1 = plugin.getMsg().getGuiMessage("arena-hover-gui").get(1);
            msg1 = msg1.replace("{players}", String.valueOf(arena.getNoPlayers()));
            String msg2 = plugin.getMsg().getGuiMessage("arena-hover-gui").get(2);
            msg2 = msg2.replace("{status}", status);
            GuiItem arenaItem = ItemBuilder.from(woolMaterial)
                    .name(Component.text(statusColor + arena.getId()))
                    .lore(
                            Component.text(plugin.getMsg().getGuiMessage("arena-hover-gui").get(0)),
                            Component.text(msg1),
                            Component.text(msg2),
                            Component.text(plugin.getMsg().getGuiMessage("arena-hover-gui").get(3)),
                            Component.text(plugin.getMsg().getGuiMessage("arena-hover-gui").get(4))
                    )
                    .asGuiItem(event -> handleArenaSelection(event, arena));

            gui.setItem(slot++, arenaItem);
        }
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
