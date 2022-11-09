package ru.whitebeef.beefunlimitedenchants.handlers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class PrepareAnvilHandler implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvilRecipe(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();

        if (inventory.getFirstItem() == null || inventory.getSecondItem() == null) {
            return;
        }
        ItemStack firstItem = inventory.getFirstItem().clone();
        ItemStack secondItem = inventory.getSecondItem().clone();
        ItemStack result = inventory.getResult();
        Map<Enchantment, Integer> toSet = new HashMap<>();

        boolean ignoreConflicts = false;
        ItemMeta meta = null;
        if (result == null && secondItem.getType() == Material.ENCHANTED_BOOK) {
            result = firstItem.clone();
            meta = result.getItemMeta();
            if (inventory.getRenameText() != null && !inventory.getRenameText().isEmpty()) {
                meta.displayName(Component.text(inventory.getRenameText()));
            }
            for (Enchantment bookEnchantment : secondItem.getEnchantments().keySet()) {
                boolean conflict = false;
                for (Enchantment itemEnchantment : firstItem.getEnchantments().keySet()) {
                    if (!bookEnchantment.conflictsWith(itemEnchantment)) {
                        conflict = true;
                    }
                }
                if (!conflict) {
                    result.addUnsafeEnchantment(bookEnchantment, 1);
                }
            }
        }
        if (result == null) {
            return;
        }
        sumEnchants(secondItem, firstItem, result, toSet);
        sumEnchants(firstItem, secondItem, result, toSet);
        if (meta == null) {
            meta = result.getItemMeta();
        }
        for (var entry : toSet.entrySet()) {
            if (entry.getValue() >= 6) {
                inventory.setRepairCost(39);
            }
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        result.setItemMeta(meta);

        inventory.setResult(result);
        event.getViewers().forEach(player -> ((Player) player).updateInventory());
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
