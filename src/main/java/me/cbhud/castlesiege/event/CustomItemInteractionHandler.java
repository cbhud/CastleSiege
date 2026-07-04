package me.cbhud.castlesiege.event;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.kit.CustomItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class CustomItemInteractionHandler {

    private final CastleSiege plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    CustomItemInteractionHandler(CastleSiege plugin) {
        this.plugin = plugin;
    }

    boolean handle(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Optional<CustomItem> match = plugin.getItemManager().matchCustomItem(item);
        if (match.isEmpty()) return false;

        CustomItem customItem = match.get();

        if (item.getType() == Material.MUSHROOM_STEW) {
            removeOneItem(player, item);
            applyEffects(player, customItem);
            return true;
        }

        if (!canUse(player, customItem)) {
            return false;
        }

        applyEffects(player, customItem);
        return true;
    }

    void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }

    private boolean canUse(Player player, CustomItem customItem) {
        long cooldown = customItem.getCooldown();
        if (cooldown <= 0) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUsed = cooldowns
                .computeIfAbsent(uuid, key -> new HashMap<>())
                .getOrDefault(customItem.getId(), 0L);

        if (now - lastUsed < cooldown) {
            int seconds = (int) ((cooldown - (now - lastUsed)) / 1000);
            String msg = plugin.getMsg().getGuiMessage("customitem-cooldown").get(0);
            msg = msg.replace("{seconds}", String.valueOf(seconds));
            player.sendMessage(msg);
            return false;
        }

        cooldowns.get(uuid).put(customItem.getId(), now);
        return true;
    }

    private void applyEffects(Player player, CustomItem customItem) {
        for (PotionEffect effect : customItem.getEffects()) {
            player.addPotionEffect(effect);
        }
    }

    private void removeOneItem(Player player, ItemStack item) {
        ItemStack clone = item.clone();
        clone.setAmount(1);
        player.getInventory().removeItem(clone);
    }
}
