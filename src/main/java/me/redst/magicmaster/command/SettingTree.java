package me.redst.magicmaster.command;

import me.redst.magicmaster.config.ConfigManager;
import me.redst.magicmaster.config.MagicMasterConfig;
import me.redst.magicmaster.config.NumericSetting;
import me.redst.magicmaster.item.EnchantableItems;
import me.redst.magicmaster.item.ToolMaterialGroup;
import me.redst.magicmaster.util.Messages;
import me.redst.magicmaster.util.Numbers;
import me.redst.magicmaster.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SettingTree {

    static final List<String> SECTIONS = List.of(
            ConfigManager.MULTIPLY_LEVEL, ConfigManager.ENCHANTMENT_PLUS, ConfigManager.LEVEL_PLUS);

    private static final List<String> BOOLEANS = List.of("true", "false");
    private static final List<String> MULTIPLY_LEVEL_KEYS = List.of(
            ConfigManager.DEFAULT_MULTIPLIER, ConfigManager.ITEMS, ConfigManager.MATERIAL_MODE);
    private static final List<String> MATERIAL_MODE_KEYS = List.of(
            ConfigManager.ENABLED, ConfigManager.MATERIALS);
    private static final List<String> OVERRIDE_KEYS = List.of(
            ConfigManager.ENABLED, ConfigManager.MULTIPLIER);

    private final ConfigManager config;
    private final EnchantableItems enchantableItems;

    public SettingTree(ConfigManager config, EnchantableItems enchantableItems) {
        this.config = config;
        this.enchantableItems = enchantableItems;
    }

    public Component apply(String[] arguments) {
        if (arguments.length == 0) {
            return usage("/magicmaster set <" + String.join(" | ", SECTIONS) + "> ...");
        }
        String section = arguments[0].toLowerCase(Locale.ROOT);
        return switch (section) {
            case ConfigManager.MULTIPLY_LEVEL -> applyMultiplyLevel(arguments);
            case ConfigManager.ENCHANTMENT_PLUS -> applyBoost(arguments, section,
                    ConfigManager.ADDITIONAL_ENCHANTMENTS, NumericSetting.ADDITIONAL_ENCHANTMENTS);
            case ConfigManager.LEVEL_PLUS -> applyBoost(arguments, section,
                    ConfigManager.LEVELS_PER_INTERVAL, NumericSetting.LEVELS_PER_INTERVAL);
            default -> Messages.error("Unknown setting: " + arguments[0]);
        };
    }

    private Component applyMultiplyLevel(String[] arguments) {
        if (arguments.length < 2) {
            return usage("/magicmaster set multiply-level <"
                    + String.join(" | ", MULTIPLY_LEVEL_KEYS) + "> ...");
        }
        String key = arguments[1].toLowerCase(Locale.ROOT);
        switch (key) {
            case ConfigManager.ENABLED:
                return mainSystemRejection(ConfigManager.MULTIPLY_LEVEL);
            case ConfigManager.DEFAULT_MULTIPLIER:
                if (arguments.length < 3) {
                    return usage("/magicmaster set multiply-level default-multiplier <value>");
                }
                return writeNumber(ConfigManager.MULTIPLY_LEVEL + "." + ConfigManager.DEFAULT_MULTIPLIER,
                        NumericSetting.MULTIPLIER, arguments[2], "Default multiplier");
            case ConfigManager.ITEMS:
                return applyItem(arguments);
            case ConfigManager.MATERIAL_MODE:
                return applyMaterialMode(arguments);
            default:
                return Messages.error("Unknown setting: " + arguments[1]);
        }
    }

    private Component applyItem(String[] arguments) {
        if (arguments.length < 3) {
            return usage("/magicmaster set multiply-level items <item> <enabled | multiplier> <value>");
        }
        Material material = ConfigManager.matchItem(arguments[2]);
        if (material == null) {
            return Messages.error("Unknown item: " + arguments[2]);
        }
        if (arguments.length < 5) {
            return usage("/magicmaster set multiply-level items " + material.name()
                    + " <enabled | multiplier> <value>");
        }
        String leaf = arguments[3].toLowerCase(Locale.ROOT);
        String label = Text.pretty(material.name());
        if (leaf.equals(ConfigManager.ENABLED)) {
            return writeBoolean(config.itemPath(material, ConfigManager.ENABLED), arguments[4],
                    label + " override");
        }
        if (!leaf.equals(ConfigManager.MULTIPLIER)) {
            return Messages.error("Unknown setting: " + arguments[3]);
        }
        Double value = NumericSetting.MULTIPLIER.parse(arguments[4]);
        if (value == null) {
            return rejected(arguments[4], NumericSetting.MULTIPLIER);
        }

        Map<String, Object> values = new LinkedHashMap<>(2);
        String enabledPath = config.itemPath(material, ConfigManager.ENABLED);
        if (!config.isSet(enabledPath)) {
            values.put(enabledPath, true);
        }
        values.put(config.itemPath(material, ConfigManager.MULTIPLIER), value);
        if (!config.write(values)) {
            return saveFailure();
        }
        Component message = Messages.success(label + " multiplier set to " + Numbers.format(value) + "x");
        message = note(message, overrideNote(config.current().multiplyLevel().itemOverride(material),
                "/magicmaster set multiply-level items " + material.name() + " enabled true"));
        if (!enchantableItems.isEnchantable(material)) {
            message = note(message,
                    Messages.warn("This item cannot be enchanted at an enchanting table."));
        }
        return message;
    }

    private Component applyMaterialMode(String[] arguments) {
        if (arguments.length < 3) {
            return usage("/magicmaster set multiply-level material-mode <"
                    + String.join(" | ", MATERIAL_MODE_KEYS) + "> ...");
        }
        String key = arguments[2].toLowerCase(Locale.ROOT);
        if (key.equals(ConfigManager.ENABLED)) {
            if (arguments.length < 4) {
                return usage("/magicmaster set multiply-level material-mode enabled <true | false>");
            }
            return writeBoolean(ConfigManager.MULTIPLY_LEVEL + "." + ConfigManager.MATERIAL_MODE
                    + "." + ConfigManager.ENABLED, arguments[3], "Material mode");
        }
        if (!key.equals(ConfigManager.MATERIALS)) {
            return Messages.error("Unknown setting: " + arguments[2]);
        }
        if (arguments.length < 4) {
            return usage("/magicmaster set multiply-level material-mode materials <"
                    + groupNames() + "> <enabled | multiplier> <value>");
        }
        ToolMaterialGroup group = ToolMaterialGroup.parse(arguments[3]);
        if (group == null) {
            return Messages.error("Unknown tool material: " + arguments[3]);
        }
        if (arguments.length < 6) {
            return usage("/magicmaster set multiply-level material-mode materials " + group.name()
                    + " <enabled | multiplier> <value>");
        }
        String leaf = arguments[4].toLowerCase(Locale.ROOT);
        if (leaf.equals(ConfigManager.ENABLED)) {
            return writeBoolean(config.materialPath(group, ConfigManager.ENABLED), arguments[5],
                    group.displayName() + " tools");
        }
        if (leaf.equals(ConfigManager.MULTIPLIER)) {
            Double value = NumericSetting.MULTIPLIER.parse(arguments[5]);
            if (value == null) {
                return rejected(arguments[5], NumericSetting.MULTIPLIER);
            }
            if (!config.write(config.materialPath(group, ConfigManager.MULTIPLIER), value)) {
                return saveFailure();
            }
            Component message = Messages.success(group.displayName()
                    + " multiplier set to " + Numbers.format(value) + "x");
            return note(message, overrideNote(config.current().multiplyLevel().materials().get(group),
                    "/magicmaster set multiply-level material-mode materials " + group.name() + " enabled true"));
        }
        return Messages.error("Unknown setting: " + arguments[4]);
    }

    private Component applyBoost(String[] arguments, String section, String amountKey,
                                 NumericSetting amountSetting) {
        if (arguments.length < 2) {
            return usage("/magicmaster set " + section + " <"
                    + String.join(" | ", boostKeys(amountKey)) + "> <value>");
        }
        String key = arguments[1].toLowerCase(Locale.ROOT);
        if (key.equals(ConfigManager.ENABLED)) {
            return mainSystemRejection(section);
        }
        NumericSetting setting;
        String label;
        if (key.equals(ConfigManager.THRESHOLD_LEVEL)) {
            setting = NumericSetting.THRESHOLD_LEVEL;
            label = "Threshold";
        } else if (key.equals(ConfigManager.INTERVAL)) {
            setting = NumericSetting.INTERVAL;
            label = "Interval";
        } else if (key.equals(amountKey)) {
            setting = amountSetting;
            label = Text.pretty(amountKey.replace('-', '_'));
        } else {
            return Messages.error("Unknown setting: " + arguments[1]);
        }
        if (arguments.length < 3) {
            return usage("/magicmaster set " + section + " " + key + " <value>");
        }
        return writeNumber(section + "." + key, setting, arguments[2], label);
    }

    private Component writeNumber(String path, NumericSetting setting, String input, String label) {
        Double value = setting.parse(input);
        if (value == null) {
            return rejected(input, setting);
        }
        if (!config.write(path, value)) {
            return saveFailure();
        }
        return Messages.success(label + " set to " + Numbers.format(value));
    }

    private Component writeBoolean(String path, String input, String label) {
        String normalised = input.toLowerCase(Locale.ROOT);
        if (!normalised.equals("true") && !normalised.equals("false")) {
            return Messages.error("Value must be true or false.");
        }
        boolean value = normalised.equals("true");
        if (!config.write(path, value)) {
            return saveFailure();
        }
        return Messages.success(label + " is now " + (value ? "ON" : "OFF"));
    }

    private static Component rejected(String input, NumericSetting setting) {
        if (input.contains(".") && input.substring(input.indexOf('.') + 1).length() > 2) {
            return Messages.error("Values may have at most 2 decimal places.");
        }
        return Messages.error("Value must be a number between " + setting.limitDescription() + ".");
    }

    private static Component mainSystemRejection(String section) {
        return Messages.error("Use /magicmaster toggle " + section + " to turn this system on or off.");
    }

    private static Component usage(String usage) {
        return Messages.info("Usage: " + usage);
    }

    public List<String> complete(String[] arguments) {
        int depth = arguments.length;
        String partial = arguments[depth - 1];
        if (depth == 1) {
            return filter(SECTIONS, partial);
        }
        String section = arguments[0].toLowerCase(Locale.ROOT);
        return switch (section) {
            case ConfigManager.MULTIPLY_LEVEL -> completeMultiplyLevel(arguments, depth, partial);
            case ConfigManager.ENCHANTMENT_PLUS -> completeBoost(arguments, depth, partial,
                    ConfigManager.ADDITIONAL_ENCHANTMENTS, NumericSetting.ADDITIONAL_ENCHANTMENTS);
            case ConfigManager.LEVEL_PLUS -> completeBoost(arguments, depth, partial,
                    ConfigManager.LEVELS_PER_INTERVAL, NumericSetting.LEVELS_PER_INTERVAL);
            default -> List.of();
        };
    }

    private List<String> completeMultiplyLevel(String[] arguments, int depth, String partial) {
        if (depth == 2) {
            return filter(MULTIPLY_LEVEL_KEYS, partial);
        }
        String key = arguments[1].toLowerCase(Locale.ROOT);
        switch (key) {
            case ConfigManager.DEFAULT_MULTIPLIER:
                return depth == 3 ? filter(NumericSetting.MULTIPLIER.suggestions(), partial) : List.of();
            case ConfigManager.ITEMS:
                if (depth == 3) {
                    return filter(enchantableItems.names(), partial);
                }
                if (depth == 4) {
                    return filter(OVERRIDE_KEYS, partial);
                }
                if (depth == 5) {
                    return completeOverrideValue(arguments[3], partial);
                }
                return List.of();
            case ConfigManager.MATERIAL_MODE:
                if (depth == 3) {
                    return filter(MATERIAL_MODE_KEYS, partial);
                }
                String leaf = arguments[2].toLowerCase(Locale.ROOT);
                if (leaf.equals(ConfigManager.ENABLED)) {
                    return depth == 4 ? filter(BOOLEANS, partial) : List.of();
                }
                if (leaf.equals(ConfigManager.MATERIALS)) {
                    if (depth == 4) {
                        return filter(groupNameList(), partial);
                    }
                    if (depth == 5) {
                        return filter(OVERRIDE_KEYS, partial);
                    }
                    if (depth == 6) {
                        return completeOverrideValue(arguments[4], partial);
                    }
                }
                return List.of();
            default:
                return List.of();
        }
    }

    private List<String> completeBoost(String[] arguments, int depth, String partial, String amountKey,
                                       NumericSetting amountSetting) {
        if (depth == 2) {
            return filter(boostKeys(amountKey), partial);
        }
        if (depth != 3) {
            return List.of();
        }
        String key = arguments[1].toLowerCase(Locale.ROOT);
        if (key.equals(ConfigManager.THRESHOLD_LEVEL)) {
            return filter(NumericSetting.THRESHOLD_LEVEL.suggestions(), partial);
        }
        if (key.equals(ConfigManager.INTERVAL)) {
            return filter(NumericSetting.INTERVAL.suggestions(), partial);
        }
        if (key.equals(amountKey)) {
            return filter(amountSetting.suggestions(), partial);
        }
        return List.of();
    }

    private List<String> completeOverrideValue(String leaf, String partial) {
        if (leaf.equalsIgnoreCase(ConfigManager.ENABLED)) {
            return filter(BOOLEANS, partial);
        }
        if (leaf.equalsIgnoreCase(ConfigManager.MULTIPLIER)) {
            return filter(NumericSetting.MULTIPLIER.suggestions(), partial);
        }
        return List.of();
    }

    private static List<String> boostKeys(String amountKey) {
        return List.of(ConfigManager.THRESHOLD_LEVEL, ConfigManager.INTERVAL, amountKey);
    }

    private static List<String> groupNameList() {
        List<String> names = new ArrayList<>(ToolMaterialGroup.values().length);
        for (ToolMaterialGroup group : ToolMaterialGroup.values()) {
            names.add(group.name());
        }
        return names;
    }

    private static String groupNames() {
        return String.join(" | ", groupNameList());
    }

    static List<String> filter(Collection<String> values, String partial) {
        if (partial.isEmpty()) {
            return values instanceof List<String> list ? list : new ArrayList<>(values);
        }
        List<String> matches = new ArrayList<>(Math.min(values.size(), 32));
        for (String value : values) {
            if (Text.startsWith(value, partial)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private static Component note(Component message, @Nullable Component note) {
        return note == null ? message : message.append(Component.newline()).append(note);
    }

    private static @Nullable Component overrideNote(@Nullable MagicMasterConfig.Override override,
                                                    String command) {
        if (override == null || override.enabled()) {
            return null;
        }
        return Messages.warn("This override is switched off. Run " + command + " to apply it.");
    }

    private static Component saveFailure() {
        return Messages.error("Could not save config.yml. The setting was not changed.");
    }
}
