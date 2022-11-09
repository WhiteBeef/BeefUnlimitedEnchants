package ru.whitebeef.beefunlimitedenchants.handlers;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class PrepareAnvilHandler implements Listener {

    @EventHandler
    public void onPrepareAnvilRecipe(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();

        if (inventory.getFirstItem() == null || inventory.getSecondItem() == null || inventory.getResult() == null) {
            return;
        }

        ItemStack firstItem = inventory.getFirstItem();
        ItemStack secondItem = inventory.getSecondItem();
        ItemStack result = inventory.getResult();
        Map<Enchantment, Integer> toSet = new HashMap<>();

        sumEnchants(secondItem, firstItem, result, toSet);

        sumEnchants(firstItem, secondItem, result, toSet);

        ItemMeta meta = event.getResult().getItemMeta();

        for (var entry : toSet.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        result.setItemMeta(meta);
        event.setResult(result);
    }

    private void sumEnchants(ItemStack firstItem, ItemStack secondItem, ItemStack result, Map<Enchantment, Integer> toSet) {
        for (var entry : secondItem.getEnchantments().entrySet()) {
            if (!firstItem.hasItemMeta() || !result.getItemMeta().hasEnchant(entry.getKey())) {
                continue;
            }
            int level = Math.max(entry.getValue(), result.getEnchantmentLevel(entry.getKey()));
            toSet.put(entry.getKey(), Math.max(toSet.getOrDefault(entry.getKey(), 0), level));
            if (firstItem.getEnchantmentLevel(entry.getKey()) == entry.getValue()) {
                toSet.put(entry.getKey(), Math.max(toSet.getOrDefault(entry.getKey(), 0), entry.getValue() + 1));
            }
        }
    }

}
