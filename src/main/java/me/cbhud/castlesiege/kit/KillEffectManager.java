package me.cbhud.castlesiege.kit;

import me.cbhud.castlesiege.CastleSiege;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class KillEffectManager {
    private final CastleSiege plugin;
    private final Map<String, List<KillEffect>> effectMap = new HashMap<>();

    public KillEffectManager(CastleSiege plugin) {
        this.plugin = plugin;
        loadKillEffects();
    }

    private void loadKillEffects() {
        File file = new File(plugin.getDataFolder(), "killeffects.yml");

        // Create file if missing
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("killeffects.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    plugin.getLogger().warning("killeffects.yml not found in resources!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error creating killeffects.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("kits")) return;

        for (String kit : config.getConfigurationSection("kits").getKeys(false)) {
            List<KillEffect> effects = new ArrayList<>();
            List<?> rawList = config.getList("kits." + kit + ".effects");

            if (rawList != null) {
                for (Object obj : rawList) {
                    if (!(obj instanceof Map)) continue;

                    Map<?, ?> entry = (Map<?, ?>) obj;

                    // Potion effect
                    if (entry.containsKey("type")) {
                        String typeStr = String.valueOf(entry.get("type"));
                        PotionEffectType type = PotionEffectType.getByName(typeStr.toUpperCase());
                        if (type != null) {
                            int duration = getIntValue(entry.get("duration"), 100);
                            int amplifier = getIntValue(entry.get("amplifier"), 0);
                            effects.add(new KillEffect(type, duration, amplifier));
                        }
                    }

                    // Item reward
                    else if (entry.containsKey("give_item")) {
                        String itemStr = String.valueOf(entry.get("give_item"));
                        Material mat = Material.getMaterial(itemStr.toUpperCase());
                        if (mat != null) {
                            int amount = getIntValue(entry.get("amount"), 1);
                            effects.add(new KillEffect(mat, amount));
                        }
                    }
                }
            }

            effectMap.put(kit.toLowerCase(), effects);
        }
    }

    private int getIntValue(Object obj, int defaultValue) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void applyKillEffects(Player player, KitManager.KitData kit) {
        if (kit == null) return;
        List<KillEffect> effects = effectMap.getOrDefault(kit.getName().toLowerCase(), Collections.emptyList());
        for (KillEffect effect : effects) {
            effect.applyTo(player);
        }
    }

    private static class KillEffect {
        private final PotionEffectType potionType;
        private final int duration;
        private final int amplifier;
        private final ItemStack item;

        public KillEffect(PotionEffectType type, int duration, int amplifier) {
            this.potionType = type;
            this.duration = duration;
            this.amplifier = amplifier;
            this.item = null;
        }

        public KillEffect(Material material, int amount) {
            this.item = new ItemStack(material, amount);
            this.potionType = null;
            this.duration = 0;
            this.amplifier = 0;
        }

        public void applyTo(Player player) {
            if (potionType != null) {
                player.addPotionEffect(new PotionEffect(potionType, duration, amplifier));
            } else if (item != null) {
                player.getInventory().addItem(item);
            }
        }
    }
}
