package me.redst.magicmaster.enchant;

import me.redst.magicmaster.config.ConfigManager;
import me.redst.magicmaster.config.MagicMasterConfig;
import me.redst.magicmaster.util.Numbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;

public final class EnchantingListener implements Listener {

    private final ConfigManager configManager;
    private final EnchantmentEngine engine;

    public EnchantingListener(ConfigManager configManager, EnchantmentEngine engine) {
        this.configManager = configManager;
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        MagicMasterConfig config = configManager.current();
        if (!config.levelPlus().enabled()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.isEmpty()) {
            return;
        }
        double multiplier = config.multiplyLevel().resolveMultiplier(item.getType());

        for (EnchantmentOffer offer : event.getOffers()) {
            if (offer == null) {
                continue;
            }
            int effectiveLevel = Numbers.effectiveLevel(offer.getCost(), multiplier);
            int extraLevels = config.levelPlus().bonus(effectiveLevel, EnchantmentEngine.MAX_LEVEL_BONUS);
            if (extraLevels <= 0) {
                continue;
            }
            Enchantment enchantment = offer.getEnchantment();
            if (enchantment == null) {
                continue;
            }
            int maximum = Enchantments.maxLevel(enchantment);
            int level = offer.getEnchantmentLevel();
            if (maximum == Enchantments.UNKNOWN || level >= maximum) {
                continue;
            }
            offer.setEnchantmentLevel(Math.min(maximum, level + extraLevels));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        MagicMasterConfig config = configManager.current();
        if (!config.anySystemEnabled()) {
            return;
        }
        engine.apply(event, config);
    }
}
