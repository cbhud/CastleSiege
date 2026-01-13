package me.cbhud.castlesiege.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class ArenaEditorGui {

    private final CastleSiege plugin;

    public ArenaEditorGui(CastleSiege plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text("Edit Arenas"))
                .rows(4)
                .create();

        int slot = 0;

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            Material mat;
            ChatColor color;
            switch (arena.getState()) {
                case WAITING -> { mat = Material.LIME_WOOL; color = ChatColor.YELLOW; }
                case IN_GAME -> { mat = Material.GREEN_WOOL; color = ChatColor.GREEN; }
                case ENDED -> { mat = Material.RED_WOOL; color = ChatColor.DARK_RED; }
                default -> { mat = Material.GRAY_WOOL; color = ChatColor.GRAY; }
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(ChatColor.GRAY + "State: " + arena.getState()));
            lore.add(Component.text(ChatColor.GRAY + "Players: " + arena.getNoPlayers() + "/" + arena.getMax()));
            lore.add(Component.text(ChatColor.DARK_GRAY + "Click to edit settings"));

            GuiItem item = ItemBuilder.from(mat)
                    .name(Component.text(color + arena.getId()))
                    .lore(lore)
                    .asGuiItem(e -> onPickArena(e, arena.getId()));

            gui.setItem(slot++, item);
            if (slot >= 36) break;
        }

        gui.open(player);
    }

    private void onPickArena(InventoryClickEvent event, String arenaId) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        // Open settings GUI
        ArenaEditChatListener chat = plugin.getArenaEditChatListener(); // see note below
        new ArenaSettingsGui(plugin, chat, arenaId).open(player);
    }
}