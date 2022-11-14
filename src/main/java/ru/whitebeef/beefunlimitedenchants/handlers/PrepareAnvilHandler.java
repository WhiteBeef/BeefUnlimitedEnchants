package ru.whitebeef.beefunlimitedenchants.handlers;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import ru.whitebeef.beefunlimitedenchants.BeefUnlimitedEnchants;
import ru.whitebeef.beefunlimitedenchants.enums.MergeType;

import java.util.Map;
import java.util.TreeSet;

public class PrepareAnvilHandler implements Listener {

    @EventHandler
    public void onAnvilEvent(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack leftItem = inventory.getItem(0);
        ItemStack rightItem = inventory.getItem(1);
        if (leftItem == null || rightItem == null) {
            return;
        }
        ItemStack mergeResult = getMergeResult(leftItem, rightItem);
        if (mergeResult == null) {
            event.setResult(null);
            return;
        }
        event.setResult(mergeResult);
        ItemMeta meta = mergeResult.getItemMeta();

        TreeSet<Integer> levels = new TreeSet<>();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            levels.addAll(bookMeta.getStoredEnchants().values());
        }
        levels.addAll(meta.getEnchants().values());

        int repairCost = levels.last() * 3;

        if (inventory.getRenameText() != null && !inventory.getRenameText().isEmpty()) {
            meta.displayName(Component.text(inventory.getRenameText()));
            repairCost += 1;
        }
        mergeResult.setItemMeta(meta);

        inventory.setRepairCost(Math.min(repairCost, 39));
    }

    private ItemStack getMergeResult(ItemStack leftItem, ItemStack rightItem) {
        return switch (getMergeType(leftItem, rightItem)) {
            case BOOK_ON_BOOK -> mergeEnchantedBooks(leftItem, rightItem);
            case BOOK_ON_ITEM -> mergeBookAndItem(leftItem, rightItem);
            case ITEM_ON_ITEM -> mergeEnchantedItems(leftItem, rightItem);
            case NONE -> null;
        };
    }

    private MergeType getMergeType(ItemStack leftItem, ItemStack rightItem) {
        if (leftItem.getType() != Material.ENCHANTED_BOOK && rightItem.getType() != Material.ENCHANTED_BOOK) {
            return MergeType.ITEM_ON_ITEM;
        }
        if (leftItem.getType() == Material.ENCHANTED_BOOK && rightItem.getType() == Material.ENCHANTED_BOOK) {
            return MergeType.BOOK_ON_BOOK;
        }
        if (leftItem.getType() != Material.ENCHANTED_BOOK && rightItem.getType() == Material.ENCHANTED_BOOK) {
            return MergeType.BOOK_ON_ITEM;
        }
        return MergeType.NONE;
    }

    private ItemStack mergeEnchantedItems(ItemStack leftItem, ItemStack rightItem) {
        ItemStack resultItem = leftItem.clone();
        ItemMeta leftMeta = leftItem.getItemMeta();
        ItemMeta rightMeta = rightItem.getItemMeta();
        addLoreToResultItemIfExists(leftMeta, resultItem);
        addAttributeModifiersToResultItemIfExists(leftMeta, resultItem);
        leftMeta.getEnchants().forEach((enchant, level) -> {
            int resultLevel = getResultLevel(level, enchant, rightMeta.getEnchants());
            resultItem.addUnsafeEnchantment(enchant, resultLevel);
        });
        rightMeta.getEnchants().forEach((enchant, level) -> {
            if (!resultItem.getEnchantments().containsKey(enchant) && !resultItem.getItemMeta().hasConflictingEnchant(enchant)) {
                resultItem.addUnsafeEnchantment(enchant, level);
            }
        });
        return resultItem;
    }

    private ItemStack mergeBookAndItem(ItemStack item, ItemStack book) {
        ItemStack resultItem = item.clone();
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        addLoreToResultItemIfExists(item.getItemMeta(), resultItem);
        addAttributeModifiersToResultItemIfExists(item.getItemMeta(), resultItem);
        item.getEnchantments().forEach((enchant, level) -> {
            int resultLevel = getResultLevel(level, enchant, bookMeta.getStoredEnchants());
            resultItem.addUnsafeEnchantment(enchant, resultLevel);
        });
        bookMeta.getStoredEnchants().forEach((enchant, level) -> {
            if (enchant.canEnchantItem(resultItem) && !resultItem.getEnchantments().containsKey(enchant) && !resultItem.getItemMeta().hasConflictingEnchant(enchant)) {
                resultItem.addUnsafeEnchantment(enchant, level);
            }
        });
        return resultItem;
    }

    private ItemStack mergeEnchantedBooks(ItemStack leftBook, ItemStack rightBook) {
        ItemStack resultItem = leftBook.clone();
        EnchantmentStorageMeta resultMeta = (EnchantmentStorageMeta) resultItem.getItemMeta();
        EnchantmentStorageMeta leftMeta = (EnchantmentStorageMeta) leftBook.getItemMeta();
        EnchantmentStorageMeta rightMeta = (EnchantmentStorageMeta) rightBook.getItemMeta();
        leftMeta.getStoredEnchants().forEach((enchant, level) -> {
            int resultLevel = getResultLevel(level, enchant, rightMeta.getStoredEnchants());
            resultMeta.addStoredEnchant(enchant, resultLevel, true);
        });
        rightMeta.getStoredEnchants().forEach((enchant, level) -> {
            if (!resultMeta.getStoredEnchants().containsKey(enchant) && !resultMeta.hasConflictingStoredEnchant(enchant)) {
                resultMeta.addStoredEnchant(enchant, level, true);
            }
        });
        resultItem.setItemMeta(resultMeta);
        return resultItem;
    }

    private int getResultLevel(int value, Enchantment enchant, Map<Enchantment, Integer> storedEnchants) {
        int resultLevel = value;
        if (storedEnchants.containsKey(enchant) && enchant.getMaxLevel() != 1) {
            int rightLevel = storedEnchants.get(enchant);
            if (rightLevel == value) {
                resultLevel++;
            } else if (rightLevel > value) {
                resultLevel = rightLevel;
            }
        }
        int enchantLimitation = BeefUnlimitedEnchants.getInstance().getLimitationForEnchant(enchant.getKey().getKey());
        return (enchantLimitation == -1) ?
                resultLevel :
                Math.min(resultLevel, enchantLimitation);
    }

    private void addLoreToResultItemIfExists(ItemMeta leftMeta, ItemStack resultItem) {
        if (!leftMeta.hasLore()) {
            return;
        }
        ItemMeta resultMeta = resultItem.getItemMeta();
        resultMeta.setLore(leftMeta.getLore());
        resultItem.setItemMeta(resultMeta);
    }

    private void addAttributeModifiersToResultItemIfExists(ItemMeta leftMeta, ItemStack resultItem) {
        if (!leftMeta.hasAttributeModifiers()) {
            return;
        }
        ItemMeta resultMeta = resultItem.getItemMeta();
        resultMeta.setAttributeModifiers(leftMeta.getAttributeModifiers());
        resultItem.setItemMeta(resultMeta);
    }

}
