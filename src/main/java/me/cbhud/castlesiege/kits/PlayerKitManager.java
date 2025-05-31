package me.cbhud.castlesiege.kits;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerKitManager {
    private final Map<Player, KitManager.KitData> selectedKits;
    private final CastleSiege plugin;

    public PlayerKitManager(CastleSiege plugin) {
        this.plugin = plugin;
        this.selectedKits = new HashMap<>();
    }

    public boolean hasSelectedKit(Player player){
        if (selectedKits.containsKey(player)){
            return true;
        }
        return false;
    }

    public void giveKit(Player player, KitManager.KitData kit) {
        player.getInventory().clear();

        for (ItemStack item : kit.getItems()) {
            player.getInventory().addItem(item);
        }

        equipArmor(player, kit.getItems());
        selectedKits.put(player, kit);
    }

    public boolean selectKit(Player player, String kitName) {
        KitManager.KitData kit = plugin.getKitManager().getKitByName(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found.");
            return false;
        }

        if (kit.getTeam() != plugin.getTeamManager().getTeam(player)) {
            player.sendMessage("§cYou cannot select a kit from the opposing team!");
            return false;
        }

        if (plugin.getDbConnection().checkPlayerKit(player.getUniqueId(), kit.getName())) {
            selectedKits.put(player, kit);
            plugin.getScoreboardManager().updateScoreboard(player);
            player.sendMessage("§aYou have selected the " + kit.getName() + " kit!");
            return true;
        } else {
            player.sendMessage("§cYou do not have this kit unlocked. Right-click to purchase it.");
            return false;
        }
    }

    public KitManager.KitData getSelectedKit(Player player) {
        return selectedKits.get(player);
    }

    public int getKitPrice(KitManager.KitData kit) {
        return kit.getPrice();
    }

    private void equipArmor(Player player, List<ItemStack> items) {
        ItemStack helmet = null, chestplate = null, leggings = null, boots = null;

        for (ItemStack item : items) {
            switch (item.getType()) {
                case LEATHER_HELMET: case CHAINMAIL_HELMET: case IRON_HELMET: case DIAMOND_HELMET: case NETHERITE_HELMET:
                    helmet = item; break;
                case LEATHER_CHESTPLATE: case CHAINMAIL_CHESTPLATE: case IRON_CHESTPLATE: case DIAMOND_CHESTPLATE: case NETHERITE_CHESTPLATE:
                    chestplate = item; break;
                case LEATHER_LEGGINGS: case CHAINMAIL_LEGGINGS: case IRON_LEGGINGS: case DIAMOND_LEGGINGS: case NETHERITE_LEGGINGS:
                    leggings = item; break;
                case LEATHER_BOOTS: case CHAINMAIL_BOOTS: case IRON_BOOTS: case DIAMOND_BOOTS: case NETHERITE_BOOTS:
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

    //default kit treba maknut iz hardcoda
    public void setDefaultKit(Player player) {
        Team playerTeam = plugin.getTeamManager().getTeam(player);
        String defaultKitName;

        if (playerTeam == Team.Attackers) {
            defaultKitName = "Skald";
        } else if (playerTeam == Team.Defenders) {
            defaultKitName = "Marksman";
        } else {
            player.sendMessage("§cYou are not part of a valid team.");
            return;
        }

        KitManager.KitData kit = plugin.getKitManager().getKitByName(defaultKitName);
        if (kit == null) {
            player.sendMessage("§cDefault kit not found.");
            return;
        }

        selectedKits.put(player, kit);
    }


}
