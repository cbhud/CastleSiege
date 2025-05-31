package me.cbhud.castlesiege.kit;

import me.cbhud.castlesiege.CastleSiege;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import dev.triumphteam.gui.builder.item.ItemBuilder;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {

    private final CastleSiege plugin;
    private final Map<String, CustomItem> customItems = new HashMap<>();

    public ItemManager(CastleSiege plugin) {
        this.plugin = plugin;
        loadCustomItems();
    }

    private void loadCustomItems() {
        File file = new File(plugin.getDataFolder(), "custom_items.yml");
        if (!file.exists()) plugin.saveResource("custom_items.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(key);
            if (section == null) continue;

            Material material = Material.getMaterial(section.getString("material", "STONE"));
            if (material == null) continue;

            String name = ChatColor.translateAlternateColorCodes('&', section.getString("name", key));
            List<String> rawLore = section.getStringList("lore");
            List<String> lore = rawLore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());

            long cooldown = section.getLong("cooldown", 0);
            int amount = section.getInt("amount", 1);

            ItemBuilder builder = ItemBuilder.from(material)
                    .name(Component.text(name))
                    .amount(amount)
                    .flags(ItemFlag.HIDE_ATTRIBUTES);

            for (String line : lore) {
                builder.lore(Component.text(line));
            }

            // Parse potion effects
            List<PotionEffect> effects = new ArrayList<>();
            if (section.isList("effects")) {
                for (Map<?, ?> map : section.getMapList("effects")) {
                    try {
                        String type = map.get("type").toString();
                        int amplifier = Integer.parseInt(map.get("amplifier").toString());
                        int duration = Integer.parseInt(map.get("duration").toString());

                        PotionEffectType effectType = PotionEffectType.getByName(type.toUpperCase());
                        if (effectType != null) {
                            effects.add(new PotionEffect(effectType, duration, amplifier));
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("Invalid potion effect in item: " + key);
                    }
                }
            }

            // Parse enchantments
            Map<Enchantment, Integer> enchantmentsMap = new HashMap<>();
            if (section.isConfigurationSection("enchantments")) {
                ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
                for (String enchKey : enchSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                    int level = enchSection.getInt(enchKey, 1);
                    if (enchantment != null) {
                        enchantmentsMap.put(enchantment, level);
                        builder.enchant(enchantment, level); // Also apply enchantment visually to item
                    }
                }
            }

            CustomItem customItem = new CustomItem(key, builder.build(), effects, cooldown * 1000, enchantmentsMap);
            customItems.put(key, customItem);
        }
    }




    public Optional<CustomItem> matchCustomItem(ItemStack item) {
        return customItems.values().stream()
                .filter(ci -> ci.getItemStack().isSimilar(item))
                .findFirst();
    }

    public Map<String, CustomItem> getCustomItems() {
        return customItems;
    }

    public CustomItem getById(String id) {
        return customItems.get(id);
    }
}
