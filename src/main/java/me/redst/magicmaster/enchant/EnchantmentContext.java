package me.redst.magicmaster.enchant;

import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentContext {

    private final int enchantmentValue;
    private final List<Enchantment> applicable;

    EnchantmentContext(int enchantmentValue, List<Enchantment> applicable) {
        this.enchantmentValue = enchantmentValue;
        this.applicable = applicable;
    }

    public int enchantmentValue() {
        return enchantmentValue;
    }

    public boolean canApply(Enchantment enchantment) {
        return applicable.contains(enchantment);
    }

    public List<EnchantmentCandidate> candidates(int enchantingLevel) {
        List<EnchantmentCandidate> candidates = new ArrayList<>(applicable.size());
        for (Enchantment enchantment : applicable) {
            int level = Enchantments.levelFor(enchantment, enchantingLevel);
            if (level != Enchantments.UNKNOWN) {
                candidates.add(new EnchantmentCandidate(enchantment, level, Enchantments.weight(enchantment)));
            }
        }
        return candidates;
    }
}
