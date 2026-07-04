package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class RightClickEffects implements Listener {

    private final CastleSiege plugin;
    private final MenuInteractionHandler menuHandler;
    private final ThrowableAxeHandler axeHandler;
    private final WizardSpellHandler wizardSpellHandler;
    private final CustomItemInteractionHandler customItemHandler;

    public RightClickEffects(CastleSiege plugin) {
        this.plugin = plugin;
        this.menuHandler = new MenuInteractionHandler(plugin);
        this.axeHandler = new ThrowableAxeHandler(plugin);
        this.wizardSpellHandler = new WizardSpellHandler(plugin);
        this.customItemHandler = new CustomItemInteractionHandler(plugin);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack clickedItem = event.getItem();
        if (clickedItem == null) {
            return;
        }

        if (menuHandler.handleLobbyItem(event)) {
            return;
        }

        Arena arena = plugin.getArenaManager().getArenaByPlayer(event.getPlayer().getUniqueId());
        if (arena == null) {
            return;
        }

        if (axeHandler.handle(event, arena)) {
            return;
        }

        if (wizardSpellHandler.handle(event, arena)) {
            return;
        }

        if (menuHandler.handleWaitingArenaItem(event, arena)) {
            return;
        }

        if (arena.getState() == ArenaState.ENDED) {
            return;
        }

        if (menuHandler.handleLeaveItem(event, arena)) {
            return;
        }

        customItemHandler.handle(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wizardSpellHandler.clearCooldowns(event.getPlayer().getUniqueId());
        customItemHandler.clearCooldowns(event.getPlayer().getUniqueId());
    }
}
