package me.redst.magicmaster.enchant;

import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class Enchantments {

    public static final int UNKNOWN = 0;

    private static final int FALLBACK_WEIGHT = 10;

    private Enchantments() {
    }

    public static int maxLevel(Enchantment enchantment) {
        try {
            int max = enchantment.getMaxLevel();
            return max > 0 ? max : UNKNOWN;
        } catch (RuntimeException unsupported) {
            return UNKNOWN;
        }
    }

    public static int startLevel(Enchantment enchantment) {
        try {
            int start = enchantment.getStartLevel();
            return start > 0 ? start : 1;
        } catch (RuntimeException unsupported) {
            return 1;
        }
    }

    public static int weight(Enchantment enchantment) {
        try {
            return Math.max(1, enchantment.getWeight());
        } catch (RuntimeException unsupported) {
            return FALLBACK_WEIGHT;
        }
    }

    public static boolean conflicts(Enchantment first, Enchantment second) {
        if (first.equals(second)) {
            return true;
        }
        try {
            return first.conflictsWith(second) || second.conflictsWith(first);
        } catch (RuntimeException unsupported) {
            return true;
        }
    }

    public static boolean conflictsWithAny(Enchantment enchantment, Collection<Enchantment> others) {
        for (Enchantment other : others) {
            if (conflicts(enchantment, other)) {
                return true;
            }
        }
        return false;
    }

    public static int levelFor(Enchantment enchantment, int enchantingLevel) {
        int max = maxLevel(enchantment);
        if (max == UNKNOWN) {
            return UNKNOWN;
        }
        int start = Math.min(startLevel(enchantment), max);
        for (int level = max; level >= start; level--) {
            int minimumCost;
            try {
                minimumCost = enchantment.getMinModifiedCost(level);
            } catch (RuntimeException unsupported) {
                return UNKNOWN;
            }
            if (enchantingLevel >= minimumCost) {
                return level;
            }
        }
        return UNKNOWN;
    }

    public static boolean applies(Enchantment enchantment, ItemStack item, @Nullable TypedKey<ItemType> itemKey) {
        if (itemKey != null) {
            try {
                RegistryKeySet<ItemType> primary = enchantment.getPrimaryItems();
                if (primary != null) {
                    return primary.contains(itemKey);
                }
                RegistryKeySet<ItemType> supported = enchantment.getSupportedItems();
                if (supported != null) {
                    return supported.contains(itemKey);
                }
            } catch (RuntimeException unsupported) {

            }
        }
        try {
            return enchantment.canEnchantItem(item);
        } catch (RuntimeException unsupported) {
            return false;
        }
    }
}
