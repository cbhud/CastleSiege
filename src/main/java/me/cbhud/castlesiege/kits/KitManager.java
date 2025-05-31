package me.cbhud.castlesiege.kits;

import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.gui.Manager;
import me.cbhud.castlesiege.team.Team;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class KitManager {
    private final CastleSiege plugin;
    private List<KitData> kits;

    public KitManager(CastleSiege plugin) {
        this.plugin = plugin;
        createKitsFileIfNotExists();
        kits = new ArrayList<>();
        kits = loadKits();
    }

    // Load kits from kits.yml
    public List<KitData> loadKits() {
        if (!kits.isEmpty()) return kits;

        File kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);

        for (String kitName : config.getConfigurationSection("kits").getKeys(false)) {
            String path = "kits." + kitName;

            List<String> kitItems = config.getStringList(path + ".kitItems");
            int price = config.getInt(path + ".price");
            Team team = Team.valueOf(config.getString(path + ".team"));
            Boolean isDefault = config.getBoolean(path + ".default", false);

            List<ItemStack> items = new ArrayList<>();

            for (String itemString : kitItems) {
                String[] itemData = itemString.split(":");
                Material material = Material.getMaterial(itemData[0].toUpperCase());

                if (material == null) {
                    String customItemName = itemData.length > 1 ? itemData[1] : itemData[0];
                    ItemStack customItem = createCustomItem(customItemName);

                    if (customItem != null && customItem.getType() != Material.AIR) {
                        items.add(customItem);
                    } else {
                        System.out.println("Failed to create custom item: " + customItemName);
                    }
                } else {
                    int amount = itemData.length > 1 ? Integer.parseInt(itemData[1]) : 1;
                    items.add(new ItemStack(material, amount));
                }
            }

            kits.add(new KitData(kitName, items, price, team, isDefault));
        }

        return kits;
    }

    private ItemStack createCustomItem(String customItemName) {
        switch (customItemName.toLowerCase()) {
            case "stew":
                return new ItemStack(Manager.stew);
            case "spear":
            return new ItemStack(Manager.spear);
            case "axe":
            return new ItemStack(Manager.axe);

            case "rage":
            return new ItemStack(Manager.rage);

            case "ragnarok":
            return new ItemStack(Manager.ragnarok);

            case "sight":
            return new ItemStack(Manager.sight);

            case "sword":
            return new ItemStack(Manager.sword);

            case "attack":
            return new ItemStack(Manager.attack);

            case "support":
            return new ItemStack(Manager.support);

            default:
                return new ItemStack(Material.STONE);
        }
    }

    private void createKitsFileIfNotExists() {
        File kitsFile = new File(plugin.getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("kits.yml")) {
                if (in != null) {
                    Files.copy(in, kitsFile.toPath());
                    plugin.getLogger().info("kits.yml created successfully.");
                } else {
                    plugin.getLogger().severe("kits.yml not found in resources!");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create kits.yml file.");
                e.printStackTrace();
            }
        }
    }

    public KitData getKitByName(String kitName) {
        for (KitData kit : kits) {
            if (kit.getName().equalsIgnoreCase(kitName)) {
                return kit;
            }
        }
        return null;
    }

    public static class KitData {
        private final String name;
        private final List<ItemStack> items;
        private final int price;
        private final Team team;
        private final Boolean isDefault;

        public KitData(String name, List<ItemStack> items, int price, Team team, Boolean isDefault) {
            this.name = name;
            this.items = items;
            this.price = price;
            this.team = team;
            this.isDefault = isDefault;
        }

        public String getName() {
            return name;
        }

        public List<ItemStack> getItems() {
            return items;
        }

        public int getPrice() {
            return price;
        }

        public Team getTeam() {
            return team;
        }
    }
}

