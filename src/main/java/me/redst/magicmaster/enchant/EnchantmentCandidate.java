package me.redst.magicmaster.enchant;

import org.bukkit.enchantments.Enchantment;

public record EnchantmentCandidate(Enchantment enchantment, int level, int weight) {
}
