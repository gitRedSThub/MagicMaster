package me.redst.magicmaster.enchant;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import me.redst.magicmaster.util.Numbers;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public final class EnchantmentSource {

    private static final int MAX_ROLLED = 32;

    private volatile Registry<Enchantment> registry;

    @SuppressWarnings("deprecation")
    private Collection<Enchantment> tablePool() {
        Registry<Enchantment> enchantments = registry();
        try {
            if (enchantments.hasTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE)) {
                return enchantments.getTagValues(EnchantmentTagKeys.IN_ENCHANTING_TABLE);
            }
        } catch (RuntimeException unsupported) {

        }
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment enchantment : enchantments) {
            try {
                if (!enchantment.isTreasure()) {
                    pool.add(enchantment);
                }
            } catch (RuntimeException unsupported) {

            }
        }
        return pool;
    }

    public @Nullable EnchantmentContext context(ItemStack item) {
        Collection<Enchantment> pool = tablePool();
        if (pool.isEmpty()) {
            return null;
        }
        Material material = item.getType();

        boolean acceptsAnything = material == Material.BOOK || material == Material.ENCHANTED_BOOK;
        TypedKey<ItemType> itemKey = itemKey(material);

        List<Enchantment> applicable = new ArrayList<>(pool.size());
        for (Enchantment enchantment : pool) {
            if (acceptsAnything || Enchantments.applies(enchantment, item, itemKey)) {
                applicable.add(enchantment);
            }
        }
        if (applicable.isEmpty()) {
            return null;
        }
        return new EnchantmentContext(enchantmentValue(item), applicable);
    }

    public List<EnchantmentCandidate> roll(EnchantmentContext context, int enchantingLevel, Random random) {
        int enchantability = context.enchantmentValue();
        if (enchantability <= 0) {
            return List.of();
        }
        int spread = enchantability / 4 + 1;
        long boosted = (long) enchantingLevel + 1L + random.nextInt(spread) + random.nextInt(spread);
        double variance = (random.nextFloat() + random.nextFloat() - 1.0D) * 0.15D;
        boosted = Math.round(boosted + boosted * variance);
        int level = (int) Math.max(1L, Math.min(boosted, (long) Numbers.MAX_EFFECTIVE_LEVEL));

        List<EnchantmentCandidate> candidates = context.candidates(level);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<EnchantmentCandidate> rolled = new ArrayList<>(4);
        EnchantmentCandidate picked = pickWeighted(candidates, random);
        rolled.add(picked);
        while (rolled.size() < MAX_ROLLED && random.nextInt(50) <= level) {
            removeIncompatible(candidates, picked.enchantment());
            if (candidates.isEmpty()) {
                break;
            }
            picked = pickWeighted(candidates, random);
            rolled.add(picked);
            level /= 2;
        }
        return rolled;
    }

    static EnchantmentCandidate pickWeighted(List<EnchantmentCandidate> candidates, Random random) {
        int total = 0;
        for (int index = 0; index < candidates.size(); index++) {
            total += candidates.get(index).weight();
        }
        if (total <= 0) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        int roll = random.nextInt(total);
        for (int index = 0; index < candidates.size(); index++) {
            roll -= candidates.get(index).weight();
            if (roll < 0) {
                return candidates.get(index);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    static void removeIncompatible(List<EnchantmentCandidate> candidates, Enchantment chosen) {
        candidates.removeIf(candidate -> Enchantments.conflicts(candidate.enchantment(), chosen));
    }

    @SuppressWarnings("deprecation")
    private Registry<Enchantment> registry() {
        Registry<Enchantment> cached = registry;
        if (cached != null) {
            return cached;
        }
        try {
            cached = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        } catch (RuntimeException unsupported) {
            cached = Registry.ENCHANTMENT;
        }
        registry = cached;
        return cached;
    }

    private static @Nullable TypedKey<ItemType> itemKey(Material material) {
        try {
            return TypedKey.create(RegistryKey.ITEM, material.getKey());
        } catch (RuntimeException unsupported) {
            return null;
        }
    }

    private static int enchantmentValue(ItemStack item) {
        try {
            Enchantable enchantable = item.getData(DataComponentTypes.ENCHANTABLE);
            return enchantable == null ? 0 : Math.max(0, enchantable.value());
        } catch (RuntimeException unsupported) {
            return 0;
        }
    }

    public void clear() {
        registry = null;
    }
}
