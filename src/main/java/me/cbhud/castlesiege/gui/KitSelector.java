package me.cbhud.castlesiege.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.kit.KitManager;
import me.cbhud.castlesiege.kit.KitManager.KitData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KitSelector {

    private final Gui gui;
    private final CastleSiege plugin;

    public KitSelector(CastleSiege plugin) {
        this.plugin = plugin;
        this.gui = Gui.gui()
                .title(Component.text(plugin.getMsg().getGuiMessage("kitgui").get(0)))
                .rows(3)
                .create();

        init();
    }

    private void init() {
        KitManager kitManager = plugin.getKitManager();
        int slot = 9;

        for (KitData kit : kitManager.loadKits()) {
            if (slot == 13) {
                gui.setItem(slot++, new GuiItem(Material.AIR));
            }

            ItemStack displayItem = getDisplayItem(kit);
            String priceDisplay = kit.getPrice() <= 0
                    ? plugin.getMsg().getGuiMessage("kit-price-free").get(0)
                    : plugin.getMsg().getGuiMessage("kit-price-coins").get(0).replace("{price}", String.valueOf(kit.getPrice()));

            String teamDisplay = plugin.getMsg().getGuiMessage("kit-team-display").get(0).replace("{team}", kit.getTeam().toString());

            GuiItem guiItem = ItemBuilder.from(displayItem)
                    .name(Component.text(plugin.getMsg().getGuiMessage("kit-name-display").get(0).replace("{kit}", kit.getName())))
                    .lore(
                            Component.text(teamDisplay),
                            Component.text(plugin.getMsg().getGuiMessage("kit-lore-space-1").get(0)),
                            Component.text(priceDisplay),
                            Component.text(plugin.getMsg().getGuiMessage("kit-lore-space-2").get(0)),
                            Component.text(plugin.getMsg().getGuiMessage("kit-left-click").get(0)),
                            Component.text(plugin.getMsg().getGuiMessage("kit-right-click").get(0))
                    )
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .asGuiItem(event -> handleKitSelection(event, kit));

            gui.setItem(slot++, guiItem);
        }
    }



    private ItemStack getDisplayItem(KitData kit) {
        if (!kit.getKitIcon().isEmpty()) {
            return new ItemStack(Material.valueOf(kit.getKitIcon()));
        }
        return new ItemStack(Material.CHEST);
    }

    public void open(Player player) {
        gui.open(player);
    }

    private void handleKitSelection(InventoryClickEvent event, KitData selectedKit) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;

        event.setCancelled(true);

        if (event.isRightClick()) {
            attemptToPurchaseKit(player, selectedKit);
        } else if (event.isLeftClick()) {
            selectKit(player, selectedKit);
            gui.close(player);
        }
    }

    private void attemptToPurchaseKit(Player player, KitData selectedKit) {
        UUID uuid = player.getUniqueId();
        String kitname = selectedKit.getName();
        if (selectedKit.getPrice() == 0){
            plugin.getPlayerKitManager().selectKit(player, selectedKit);
            return;
        }
        if (plugin.getDataManager().hasPlayerKit(uuid, kitname)){
            player.sendMessage(plugin.getMsg().getMessage("alredayOwnKit", player).get(0));
            selectKit(player, selectedKit);
            return;
        }

        if (plugin.getDataManager().unlockPlayerKit(uuid, kitname, selectedKit.getPrice())) {
                String msg1 = plugin.getMsg().getGuiMessage("purchaseKitSuccess").get(0);
                msg1 = msg1.replace("{kitname}", String.valueOf(kitname));
                player.sendMessage(msg1);
            } else {
            player.sendMessage(plugin.getMsg().getMessage("purchaseKitUnsuccess", player).get(0));
            }
        }

    private void selectKit(Player player, KitData selectedKit) {
        plugin.getPlayerKitManager().selectKit(player, selectedKit);
    }
}
