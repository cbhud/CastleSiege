package me.cbhud.castlesiege.kit;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final ItemStack itemStack;
    private final List<PotionEffect> effects;
    private final long cooldown;
    private final Map<Enchantment, Integer> enchantments;

    public CustomItem(String id, ItemStack itemStack, List<PotionEffect> effects, long cooldown, Map<Enchantment, Integer> enchantments) {
        this.id = id;
        this.itemStack = itemStack;
        this.effects = effects;
        this.cooldown = cooldown;
        this.enchantments = enchantments;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

    public long getCooldown() {
        return cooldown;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
}
