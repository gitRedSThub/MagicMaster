package me.redst.magicmaster.command;

import me.redst.magicmaster.config.MagicMasterConfig;
import me.redst.magicmaster.item.ToolMaterialGroup;
import me.redst.magicmaster.util.Numbers;
import me.redst.magicmaster.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InfoRenderer {

    private static final String BRANCH = "\u251C\u2500 ";
    private static final String LAST = "\u2514\u2500 ";
    private static final String PIPE = "\u2502  ";
    private static final String GAP = "   ";
    private static final String SPACER = "\u2502";

    private static final Component ON = Component.text("ON", NamedTextColor.GREEN);
    private static final Component OFF = Component.text("OFF", NamedTextColor.RED);

    private InfoRenderer() {
    }

    public static List<Component> render(MagicMasterConfig config) {
        List<Component> lines = new ArrayList<>(24);
        lines.add(Component.text("MagicMaster", NamedTextColor.LIGHT_PURPLE));

        MagicMasterConfig.MultiplyLevel multiply = config.multiplyLevel();
        lines.add(node(BRANCH, "Multiply Level", state(multiply.enabled())));
        lines.add(node(PIPE + BRANCH, "Default Multiplier", multiplier(multiply.defaultMultiplier())));
        addItemOverrides(lines, multiply.items());
        addMaterialMode(lines, multiply);

        lines.add(tree(SPACER));
        addBoost(lines, BRANCH, "Enchantment Plus", config.enchantmentPlus(), "Additional Enchantments", PIPE);

        lines.add(tree(SPACER));
        addBoost(lines, LAST, "Level Plus", config.levelPlus(), "Levels Per Interval", GAP);
        return lines;
    }

    private static void addItemOverrides(List<Component> lines, Map<Material, MagicMasterConfig.Override> items) {
        if (items.isEmpty()) {
            lines.add(node(PIPE + BRANCH, "Item Overrides", Component.text("none", NamedTextColor.DARK_GRAY)));
            return;
        }
        lines.add(tree(PIPE + BRANCH).append(Component.text("Item Overrides", NamedTextColor.GRAY)));
        int remaining = items.size();
        for (Map.Entry<Material, MagicMasterConfig.Override> entry : items.entrySet()) {
            String prefix = PIPE + PIPE + (--remaining == 0 ? LAST : BRANCH);
            lines.add(node(prefix, Text.pretty(entry.getKey().name()), value(entry.getValue())));
        }
    }

    private static void addMaterialMode(List<Component> lines, MagicMasterConfig.MultiplyLevel multiply) {
        lines.add(node(PIPE + LAST, "Material Mode", state(multiply.materialModeEnabled())));
        Map<ToolMaterialGroup, MagicMasterConfig.Override> materials = multiply.materials();
        if (materials.isEmpty()) {
            lines.add(node(PIPE + GAP + LAST, "Tool Materials",
                    Component.text("none", NamedTextColor.DARK_GRAY)));
            return;
        }
        int remaining = materials.size();
        for (Map.Entry<ToolMaterialGroup, MagicMasterConfig.Override> entry : materials.entrySet()) {
            String prefix = PIPE + GAP + (--remaining == 0 ? LAST : BRANCH);
            lines.add(node(prefix, entry.getKey().displayName(), value(entry.getValue())));
        }
    }

    private static void addBoost(List<Component> lines, String prefix, String title,
                                 MagicMasterConfig.Boost boost, String amountLabel, String indent) {
        lines.add(node(prefix, title, state(boost.enabled())));
        lines.add(node(indent + BRANCH, "Threshold", number(boost.thresholdLevel())));
        lines.add(node(indent + BRANCH, "Interval", number(boost.interval())));
        lines.add(node(indent + LAST, amountLabel, number(boost.amount())));
    }

    private static Component node(String prefix, String label, Component value) {
        return tree(prefix)
                .append(Component.text(label + ": ", NamedTextColor.GRAY))
                .append(value);
    }

    private static Component tree(String prefix) {
        return Component.text(prefix, NamedTextColor.DARK_GRAY);
    }

    private static Component state(boolean enabled) {
        return enabled ? ON : OFF;
    }

    private static Component value(MagicMasterConfig.Override override) {
        return override.enabled() ? multiplier(override.multiplier()) : OFF;
    }

    private static Component multiplier(double value) {
        return Component.text(Numbers.format(value) + "x", NamedTextColor.AQUA);
    }

    private static Component number(double value) {
        return Component.text(Numbers.format(value), NamedTextColor.AQUA);
    }
}
