package ru.whitebeef.beefunlimitedenchants;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.whitebeef.beefunlimitedenchants.handlers.PrepareAnvilHandler;

import java.util.HashMap;
import java.util.Map;

public final class BeefUnlimitedEnchants extends JavaPlugin {

    private static BeefUnlimitedEnchants instance;

    private final Map<String, Integer> limitedEnchantments = new HashMap<>();

    public static BeefUnlimitedEnchants getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getPluginManager().registerEvents(new PrepareAnvilHandler(), this);

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        for (String enchantment : config.getConfigurationSection("limits").getKeys(false)) {
            limitedEnchantments.put(enchantment, config.getInt("limits." + enchantment));
        }
    }

    @Override
    public void onDisable() {
    }

    public int getLimitationForEnchant(String enchantment) {
        return limitedEnchantments.getOrDefault(enchantment, 1);
    }

}
