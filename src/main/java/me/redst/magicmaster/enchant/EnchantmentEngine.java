package me.redst.magicmaster.enchant;

import me.redst.magicmaster.config.MagicMasterConfig;
import me.redst.magicmaster.util.Numbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.EnchantmentView;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EnchantmentEngine {

    public static final int MAX_ADDITIONAL_ENCHANTMENTS = 20;

    public static final int MAX_LEVEL_BONUS = 255;

    private final EnchantmentSource source;
    private final Logger logger;
    private boolean reportedWriteFailure;

    public EnchantmentEngine(EnchantmentSource source, Logger logger) {
        this.source = source;
        this.logger = logger;
    }

    public void apply(EnchantItemEvent event, MagicMasterConfig config) {
        ItemStack item = event.getItem();
        if (item == null || item.isEmpty()) {
            return;
        }

        int tableLevel = event.getExpLevelCost();
        if (tableLevel <= 0) {
            return;
        }

        double multiplier = config.multiplyLevel().resolveMultiplier(item.getType());
        int effectiveLevel = Numbers.effectiveLevel(tableLevel, multiplier);

        boolean improveResult = config.multiplyLevel().enabled() && effectiveLevel > tableLevel;
        int extraEnchantments = config.enchantmentPlus().bonus(effectiveLevel, MAX_ADDITIONAL_ENCHANTMENTS);
        int extraLevels = config.levelPlus().bonus(effectiveLevel, MAX_LEVEL_BONUS);
        if (!improveResult && extraEnchantments <= 0 && extraLevels <= 0) {
            return;
        }

        Map<Enchantment, Integer> table = event.getEnchantsToAdd();
        if (table == null) {
            return;
        }

        Map<Enchantment, Integer> produced = new LinkedHashMap<>(table);
        Map<Enchantment, Integer> alreadyOnItem = item.getEnchantments();
        Map<Enchantment, Integer> result = new LinkedHashMap<>(produced);

        EnchantmentContext context = improveResult || extraEnchantments > 0 ? source.context(item) : null;
        Random random = randomFor(event, effectiveLevel);

        if (improveResult && context != null) {
            List<EnchantmentCandidate> rolled = source.roll(context, effectiveLevel, random);
            for (EnchantmentCandidate candidate : rolled) {
                merge(result, alreadyOnItem, candidate);
            }
        }
        if (extraEnchantments > 0 && context != null) {
            addEnchantments(result, alreadyOnItem, context, effectiveLevel, extraEnchantments, random);
        }
        if (extraLevels > 0) {
            raiseLevels(result, extraLevels);
        }
        commit(table, produced, alreadyOnItem, result, context);
    }

    private void merge(Map<Enchantment, Integer> result, Map<Enchantment, Integer> alreadyOnItem,
                       EnchantmentCandidate candidate) {
        Enchantment enchantment = candidate.enchantment();
        Integer current = result.get(enchantment);
        if (current != null) {
            if (candidate.level() > current) {
                result.put(enchantment, candidate.level());
            }
            return;
        }
        if (alreadyOnItem.containsKey(enchantment)
                || Enchantments.conflictsWithAny(enchantment, result.keySet())
                || Enchantments.conflictsWithAny(enchantment, alreadyOnItem.keySet())) {
            return;
        }
        result.put(enchantment, candidate.level());
    }

    private void addEnchantments(Map<Enchantment, Integer> result, Map<Enchantment, Integer> alreadyOnItem,
                                 EnchantmentContext context, int effectiveLevel, int amount, Random random) {
        List<EnchantmentCandidate> candidates = context.candidates(effectiveLevel);
        candidates.removeIf(candidate -> result.containsKey(candidate.enchantment())
                || alreadyOnItem.containsKey(candidate.enchantment())
                || Enchantments.conflictsWithAny(candidate.enchantment(), result.keySet())
                || Enchantments.conflictsWithAny(candidate.enchantment(), alreadyOnItem.keySet()));

        for (int added = 0; added < amount && !candidates.isEmpty(); added++) {
            EnchantmentCandidate picked = EnchantmentSource.pickWeighted(candidates, random);
            result.put(picked.enchantment(), picked.level());
            EnchantmentSource.removeIncompatible(candidates, picked.enchantment());
        }
    }

    private void raiseLevels(Map<Enchantment, Integer> result, int extraLevels) {
        for (Map.Entry<Enchantment, Integer> entry : result.entrySet()) {
            int maximum = Enchantments.maxLevel(entry.getKey());
            if (maximum == Enchantments.UNKNOWN) {
                continue;
            }
            int level = entry.getValue();
            if (level >= maximum) {
                continue;
            }
            entry.setValue(Math.min(maximum, level + extraLevels));
        }
    }

    private void commit(Map<Enchantment, Integer> table, Map<Enchantment, Integer> produced,
                        Map<Enchantment, Integer> alreadyOnItem, Map<Enchantment, Integer> result,
                        @Nullable EnchantmentContext context) {
        try {
            for (Map.Entry<Enchantment, Integer> entry : result.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                int maximum = Enchantments.maxLevel(enchantment);
                Integer original = produced.get(enchantment);

                if (original != null) {
                    if (maximum == Enchantments.UNKNOWN || level <= original) {
                        continue;
                    }

                    int raised = Math.min(level, maximum);
                    if (raised > original) {
                        table.put(enchantment, raised);
                    }
                    continue;
                }
                if (maximum == Enchantments.UNKNOWN
                        || alreadyOnItem.containsKey(enchantment)
                        || context == null
                        || !context.canApply(enchantment)
                        || Enchantments.conflictsWithAny(enchantment, table.keySet())
                        || Enchantments.conflictsWithAny(enchantment, alreadyOnItem.keySet())) {
                    continue;
                }
                int start = Math.min(Enchantments.startLevel(enchantment), maximum);
                table.put(enchantment, Numbers.clamp(level, start, maximum));
            }
        } catch (RuntimeException failure) {
            if (!reportedWriteFailure) {
                reportedWriteFailure = true;
                logger.log(Level.WARNING,
                        "Could not improve an enchanting result; the normal result was kept", failure);
            }
        }
    }

    private static Random randomFor(EnchantItemEvent event, int effectiveLevel) {
        InventoryView view = event.getView();
        if (view instanceof EnchantmentView enchantmentView) {
            long seed = (long) enchantmentView.getEnchantmentSeed() * 31L
                    + (long) event.whichButton() * 7L
                    + effectiveLevel;
            return new Random(seed);
        }
        return new Random(ThreadLocalRandom.current().nextLong());
    }
}
