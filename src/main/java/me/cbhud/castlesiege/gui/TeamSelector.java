package me.cbhud.castlesiege.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cbhud.castlesiege.team.Team;
import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TeamSelector {

    private final Gui gui;
    private final CastleSiege plugin;

    public TeamSelector(CastleSiege plugin) {
        this.plugin = plugin;
        gui = Gui.gui()
                .title(Component.text(plugin.getMsg().getGuiMessage("teamgui").get(0)))
                .rows(1)
                .create();

        init();
    }

    private void init() {
        GuiItem vikingTeamItem = ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                .name(Component.text(ChatColor.RED + plugin.getConfigManager().getAttacker()))
                .lore(
                        Component.text(plugin.getMsg().getGuiMessage("team-selector-attackers").get(0)))
                .asGuiItem(event -> handleTeamSelection(event, Team.Attackers));

        GuiItem franksTeamItem = ItemBuilder.from(Material.CYAN_STAINED_GLASS_PANE)
                .name(Component.text(ChatColor.AQUA + plugin.getConfigManager().getDefender()))
                .lore(
                        Component.text(plugin.getMsg().getGuiMessage("team-selector-defenders").get(0)))
                .asGuiItem(event -> handleTeamSelection(event, Team.Defenders));

        gui.setItem(3, franksTeamItem);
        gui.setItem(5, vikingTeamItem);
    }

    private void handleTeamSelection(InventoryClickEvent event, Team team) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;

        event.setCancelled(true);

        if (event.isRightClick()) {
            plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).joinTeam(player, team);
            gui.close(player);
        }
        else if (event.isLeftClick()) {
            plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).joinTeam(player, team);
            gui.close(player);

        }

    }

    public void open(Player player) {
        gui.open(player);
    }
}