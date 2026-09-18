package me.redst.magicmaster.item;

import me.redst.magicmaster.util.Text;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public enum ToolMaterialGroup {

    WOOD("WOODEN"),
    STONE("STONE"),
    IRON("IRON"),
    GOLD("GOLDEN"),
    DIAMOND("DIAMOND"),
    NETHERITE("NETHERITE");

    private static final String[] TOOL_TYPES = {"SWORD", "PICKAXE", "AXE", "SHOVEL", "HOE"};
    private static final Map<Material, ToolMaterialGroup> INDEX = buildIndex();

    private final String materialPrefix;
    private final String displayName;

    ToolMaterialGroup(String materialPrefix) {
        this.materialPrefix = materialPrefix;
        this.displayName = Text.pretty(name());
    }

    private static Map<Material, ToolMaterialGroup> buildIndex() {
        Map<Material, ToolMaterialGroup> index = new EnumMap<>(Material.class);
        for (ToolMaterialGroup group : values()) {
            for (String toolType : TOOL_TYPES) {
                Material material = Material.getMaterial(group.materialPrefix + "_" + toolType);
                if (material != null) {
                    index.put(material, group);
                }
            }
        }
        return Collections.unmodifiableMap(index);
    }

    public static @Nullable ToolMaterialGroup of(Material material) {
        return INDEX.get(material);
    }

    public static @Nullable ToolMaterialGroup parse(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException expected) {
            return null;
        }
    }

    public String displayName() {
        return displayName;
    }
}
