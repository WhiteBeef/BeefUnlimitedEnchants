package ru.whitebeef.beefunlimitedenchants;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.whitebeef.beefunlimitedenchants.handlers.PrepareAnvilHandler;

public final class BeefUnlimitedEnchants extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new PrepareAnvilHandler(), this);
    }

    @Override
    public void onDisable() {
    }
}
