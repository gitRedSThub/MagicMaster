package me.redst.magicmaster.config;

import me.redst.magicmaster.item.ToolMaterialGroup;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record MagicMasterConfig(MultiplyLevel multiplyLevel, Boost enchantmentPlus, Boost levelPlus) {

    public boolean anySystemEnabled() {
        return multiplyLevel.enabled() || enchantmentPlus.enabled() || levelPlus.enabled();
    }

    public static MagicMasterConfig defaults() {
        return new MagicMasterConfig(
                new MultiplyLevel(true, NumericSetting.MULTIPLIER.defaultValue(), Map.of(), true, Map.of()),
                Boost.defaults(NumericSetting.ADDITIONAL_ENCHANTMENTS),
                Boost.defaults(NumericSetting.LEVELS_PER_INTERVAL));
    }

    public record Override(boolean enabled, double multiplier) {
    }

    public record MultiplyLevel(boolean enabled,
                                double defaultMultiplier,
                                Map<Material, Override> items,
                                boolean materialModeEnabled,
                                Map<ToolMaterialGroup, Override> materials) {

        public double resolveMultiplier(Material material) {
            if (!enabled) {
                return 1.0D;
            }
            Override item = itemOverride(material);
            if (item != null && item.enabled()) {
                return item.multiplier();
            }
            if (materialModeEnabled) {
                ToolMaterialGroup group = ToolMaterialGroup.of(material);
                if (group != null) {
                    Override tool = materials.get(group);
                    if (tool != null && tool.enabled()) {
                        return tool.multiplier();
                    }
                }
            }
            return defaultMultiplier;
        }

        public @Nullable Override itemOverride(Material material) {
            Override override = items.get(material);
            if (override != null) {
                return override;
            }
            if (material == Material.BOOK) {
                return items.get(Material.ENCHANTED_BOOK);
            }
            if (material == Material.ENCHANTED_BOOK) {
                return items.get(Material.BOOK);
            }
            return null;
        }
    }

    public record Boost(boolean enabled, double thresholdLevel, double interval, double amount) {

        static Boost defaults(NumericSetting amountSetting) {
            return new Boost(true,
                    NumericSetting.THRESHOLD_LEVEL.defaultValue(),
                    NumericSetting.INTERVAL.defaultValue(),
                    amountSetting.defaultValue());
        }

        public int bonus(int effectiveLevel, int hardCap) {
            if (!enabled || effectiveLevel <= 0 || hardCap <= 0) {
                return 0;
            }
            if (!Double.isFinite(interval) || interval <= 0.0D) {
                return 0;
            }
            double above = (double) effectiveLevel - thresholdLevel;
            if (!(above >= 0.0D)) {
                return 0;
            }
            double intervals = Math.floor(above / interval);
            if (!Double.isFinite(intervals) || intervals <= 0.0D) {
                return 0;
            }
            double raw = intervals * amount;
            if (!Double.isFinite(raw) || raw <= 0.0D) {
                return 0;
            }
            long bonus = (long) Math.floor(raw);
            return (int) Math.min(bonus, (long) hardCap);
        }
    }
}
