package me.redst.magicmaster;

import me.redst.magicmaster.command.MagicMasterCommand;
import me.redst.magicmaster.config.ConfigManager;
import me.redst.magicmaster.config.ValidationReport;
import me.redst.magicmaster.enchant.EnchantingListener;
import me.redst.magicmaster.enchant.EnchantmentEngine;
import me.redst.magicmaster.enchant.EnchantmentSource;
import me.redst.magicmaster.item.EnchantableItems;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class MagicMasterPlugin extends JavaPlugin {

    private final EnchantableItems enchantableItems = new EnchantableItems();
    private final EnchantmentSource enchantmentSource = new EnchantmentSource();
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        ValidationReport report = configManager.reload();
        if (report.count() > 0) {
            getLogger().warning(report.count() + " invalid value(s) in config.yml were reset to their defaults.");
        }

        EnchantmentEngine engine = new EnchantmentEngine(enchantmentSource, getLogger());
        getServer().getPluginManager().registerEvents(new EnchantingListener(configManager, engine), this);

        PluginCommand command = getCommand("magicmaster");
        if (command == null) {
            getLogger().severe("The magicmaster command is missing, so no commands are available.");
            return;
        }
        MagicMasterCommand executor = new MagicMasterCommand(configManager, enchantableItems);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        PluginCommand command = getCommand("magicmaster");
        if (command != null) {
            command.setExecutor(null);
            command.setTabCompleter(null);
        }
        enchantableItems.clear();
        enchantmentSource.clear();
        if (configManager != null) {
            configManager.clear();
        }
    }
}
