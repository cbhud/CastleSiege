package me.cbhud.castlesiege.kit;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerKitManager {
    private final Map<UUID, KitManager.KitData> selectedKits;
    private final CastleSiege plugin;

    public PlayerKitManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.selectedKits = new HashMap<>();
    }

    public boolean hasSelectedKit(Player player){
        return player != null && selectedKits.containsKey(player.getUniqueId());
    }

    public void giveKit(Player player, KitManager.KitData kit) {
        player.getInventory().clear();
        for (ItemStack item : kit.getItems()) {
            player.getInventory().addItem(item);
        }

        equipArmor(player, kit.getItems());
        selectedKits.put(player.getUniqueId(), kit);
    }

    public void selectKit(Player player, KitManager.KitData kit) {
        if (kit == null) {
            player.sendMessage("§cKit not found.");
            return;
        }

        if (plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) == null
                || kit.getTeam() != plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player)) {
            player.sendMessage(plugin.getMsg().getMessage("opposingTeamKit", player).get(0));
            return;
        }

        if (kit.getPrice() == 0) {
            selectedKits.put(player.getUniqueId(), kit);
            plugin.updateScoreboard(player, "pre-game");
            String msg = plugin.getMsg().getMessage("selectedKit", player).get(0);
            msg = msg.replace("{kit}", kit.getName());
            player.sendMessage(msg);
            return;
        }

        plugin.getDataManager().hasPlayerKit(player.getUniqueId(), kit.getName()).thenAccept(hasKit -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasKit) {
                    selectedKits.put(player.getUniqueId(), kit);
                    plugin.updateScoreboard(player, "pre-game");
                    String msg = plugin.getMsg().getMessage("selectedKit", player).get(0);
                    msg = msg.replace("{kit}", kit.getName());
                    player.sendMessage(msg);
                } else {
                    player.sendMessage(plugin.getMsg().getMessage("lockedKit", player).get(0));
                }
            });
        });
    }


    public KitManager.KitData getSelectedKit(Player player) {
        return player == null ? null : selectedKits.get(player.getUniqueId());
    }

    public int getKitPrice(KitManager.KitData kit) {
        return kit.getPrice();
    }

    private void equipArmor(Player player, List<ItemStack> items) {
        ItemStack helmet = null, chestplate = null, leggings = null, boots = null;

        for (ItemStack item : items) {
            switch (item.getType()) {
                case LEATHER_HELMET: case CHAINMAIL_HELMET: case GOLDEN_HELMET: case IRON_HELMET: case DIAMOND_HELMET: case NETHERITE_HELMET:
                    helmet = item; break;
                case LEATHER_CHESTPLATE: case CHAINMAIL_CHESTPLATE: case GOLDEN_CHESTPLATE: case IRON_CHESTPLATE: case DIAMOND_CHESTPLATE: case NETHERITE_CHESTPLATE:
                    chestplate = item; break;
                case LEATHER_LEGGINGS: case CHAINMAIL_LEGGINGS: case GOLDEN_LEGGINGS: case IRON_LEGGINGS: case DIAMOND_LEGGINGS: case NETHERITE_LEGGINGS:
                    leggings = item; break;
                case LEATHER_BOOTS: case CHAINMAIL_BOOTS: case GOLDEN_BOOTS: case IRON_BOOTS: case DIAMOND_BOOTS: case NETHERITE_BOOTS:
                    boots = item; break;
                default: break;
            }
        }

        if (helmet != null) {
            player.getInventory().setHelmet(helmet);
            player.getInventory().remove(helmet);
        }
        if (chestplate != null) {
            player.getInventory().setChestplate(chestplate);
            player.getInventory().remove(chestplate);
        }
        if (leggings != null) {
            player.getInventory().setLeggings(leggings);
            player.getInventory().remove(leggings);
        }
        if (boots != null) {
            player.getInventory().setBoots(boots);
            player.getInventory().remove(boots);
        }
    }

    public void setDefaultKit(Player player) {
        Team playerTeam = plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()).getTeam(player);
        KitManager.KitData defaultKit = plugin.getKitManager().getDefaultKitForTeam(playerTeam);

        if (defaultKit == null) {
            player.sendMessage("§cDefault kit not found for your team contact admin!.");
            return;
        }

        selectedKits.put(player.getUniqueId(), defaultKit);
    }

    public void clearSelectedKit(Player player) {
        if (player != null) {
            selectedKits.remove(player.getUniqueId());
        }
    }



}
