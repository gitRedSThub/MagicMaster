package me.redst.magicmaster.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class EnchantableItems {

    private volatile List<String> cachedNames;

    public List<String> names() {
        List<String> cached = cachedNames;
        if (cached != null) {
            return cached;
        }
        List<String> names = new ArrayList<>(128);
        for (Material material : Material.values()) {
            if (material.isLegacy() || material == Material.AIR || !material.isItem()) {
                continue;
            }
            if (isEnchantable(material)) {
                names.add(material.name());
            }
        }
        names.sort(null);
        cached = List.copyOf(names);
        cachedNames = cached;
        return cached;
    }

    public boolean isEnchantable(Material material) {
        if (material == Material.BOOK || material == Material.ENCHANTED_BOOK) {
            return true;
        }
        if (material.isLegacy() || material == Material.AIR || !material.isItem()) {
            return false;
        }
        try {
            Enchantable enchantable = new ItemStack(material).getData(DataComponentTypes.ENCHANTABLE);
            return enchantable != null && enchantable.value() > 0;
        } catch (RuntimeException unsupported) {
            return false;
        }
    }

    public void clear() {
        cachedNames = null;
    }
}
