package me.redst.magicmaster.config;

import me.redst.magicmaster.item.ToolMaterialGroup;
import me.redst.magicmaster.util.Numbers;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class ConfigManager {

    public static final String MULTIPLY_LEVEL = "multiply-level";
    public static final String ENCHANTMENT_PLUS = "enchantment-plus";
    public static final String LEVEL_PLUS = "level-plus";
    public static final String ENABLED = "enabled";
    public static final String DEFAULT_MULTIPLIER = "default-multiplier";
    public static final String ITEMS = "items";
    public static final String MATERIAL_MODE = "material-mode";
    public static final String MATERIALS = "materials";
    public static final String MULTIPLIER = "multiplier";
    public static final String THRESHOLD_LEVEL = "threshold-level";
    public static final String INTERVAL = "interval";
    public static final String ADDITIONAL_ENCHANTMENTS = "additional-enchantments";
    public static final String LEVELS_PER_INTERVAL = "levels-per-interval";

    private final Plugin plugin;
    private volatile MagicMasterConfig current = MagicMasterConfig.defaults();

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public MagicMasterConfig current() {
        return current;
    }

    public ValidationReport reload() {
        plugin.reloadConfig();
        ValidationReport report = new ValidationReport();
        MagicMasterConfig parsed = parse(plugin.getConfig(), report);
        current = parsed;
        for (String problem : report.problems()) {
            plugin.getLogger().warning("Reset to default: " + problem);
        }
        return report;
    }

    public boolean write(String path, Object value) {
        return write(Map.of(path, value));
    }

    public boolean write(Map<String, Object> values) {
        FileConfiguration configuration = plugin.getConfig();
        for (Map.Entry<String, Object> value : values.entrySet()) {
            configuration.set(value.getKey(), value.getValue());
        }
        try {
            plugin.saveConfig();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", failure);
            return false;
        }
        current = parse(configuration, new ValidationReport());
        return true;
    }

    public boolean isSet(String path) {
        return plugin.getConfig().get(path) != null;
    }

    public String itemPath(Material material, String leaf) {
        String base = MULTIPLY_LEVEL + "." + ITEMS;
        return base + "." + existingKey(base, material.name()) + "." + leaf;
    }

    public String materialPath(ToolMaterialGroup group, String leaf) {
        String base = MULTIPLY_LEVEL + "." + MATERIAL_MODE + "." + MATERIALS;
        return base + "." + existingKey(base, group.name()) + "." + leaf;
    }

    private String existingKey(String sectionPath, String canonical) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key.equalsIgnoreCase(canonical)) {
                    return key;
                }
            }
        }
        return canonical;
    }

    public void clear() {
        current = MagicMasterConfig.defaults();
    }

    private MagicMasterConfig parse(FileConfiguration configuration, ValidationReport report) {
        return new MagicMasterConfig(
                parseMultiplyLevel(configuration, report),
                parseBoost(configuration, ENCHANTMENT_PLUS, ADDITIONAL_ENCHANTMENTS,
                        NumericSetting.ADDITIONAL_ENCHANTMENTS, report),
                parseBoost(configuration, LEVEL_PLUS, LEVELS_PER_INTERVAL,
                        NumericSetting.LEVELS_PER_INTERVAL, report));
    }

    private MagicMasterConfig.MultiplyLevel parseMultiplyLevel(FileConfiguration configuration,
                                                               ValidationReport report) {
        boolean enabled = readBoolean(configuration, MULTIPLY_LEVEL + "." + ENABLED, true, report);
        double defaultMultiplier = readNumber(configuration, MULTIPLY_LEVEL + "." + DEFAULT_MULTIPLIER,
                NumericSetting.MULTIPLIER, report);
        boolean materialMode = readBoolean(configuration,
                MULTIPLY_LEVEL + "." + MATERIAL_MODE + "." + ENABLED, true, report);

        Map<Material, MagicMasterConfig.Override> items = new EnumMap<>(Material.class);
        String itemsPath = MULTIPLY_LEVEL + "." + ITEMS;
        ConfigurationSection itemSection = configuration.getConfigurationSection(itemsPath);
        if (itemSection != null) {
            for (String key : itemSection.getKeys(false)) {
                Material material = matchItem(key);
                if (material == null) {
                    report.reject(itemsPath + "." + key, "unknown item");
                    continue;
                }
                items.put(material, readOverride(itemSection, key, report));
            }
        }

        Map<ToolMaterialGroup, MagicMasterConfig.Override> materials = new EnumMap<>(ToolMaterialGroup.class);
        String materialsPath = MULTIPLY_LEVEL + "." + MATERIAL_MODE + "." + MATERIALS;
        ConfigurationSection materialSection = configuration.getConfigurationSection(materialsPath);
        if (materialSection != null) {
            for (String key : materialSection.getKeys(false)) {
                ToolMaterialGroup group = ToolMaterialGroup.parse(key);
                if (group == null) {
                    report.reject(materialsPath + "." + key, "unknown tool material");
                    continue;
                }
                materials.put(group, readOverride(materialSection, key, report));
            }
        }

        return new MagicMasterConfig.MultiplyLevel(enabled, defaultMultiplier,
                Collections.unmodifiableMap(items), materialMode, Collections.unmodifiableMap(materials));
    }

    private MagicMasterConfig.Boost parseBoost(FileConfiguration configuration, String section,
                                               String amountKey, NumericSetting amountSetting,
                                               ValidationReport report) {
        return new MagicMasterConfig.Boost(
                readBoolean(configuration, section + "." + ENABLED, true, report),
                readNumber(configuration, section + "." + THRESHOLD_LEVEL, NumericSetting.THRESHOLD_LEVEL, report),
                readNumber(configuration, section + "." + INTERVAL, NumericSetting.INTERVAL, report),
                readNumber(configuration, section + "." + amountKey, amountSetting, report));
    }

    private MagicMasterConfig.Override readOverride(ConfigurationSection parent, String key,
                                                    ValidationReport report) {
        return new MagicMasterConfig.Override(
                readBoolean(parent, key + "." + ENABLED, true, report),
                readNumber(parent, key + "." + MULTIPLIER, NumericSetting.MULTIPLIER, report));
    }

    private double readNumber(ConfigurationSection section, String path, NumericSetting setting,
                              ValidationReport report) {
        Object raw = section.get(path);
        if (raw == null) {
            return setting.defaultValue();
        }
        if (!(raw instanceof Number number)) {
            report.reject(absolute(section, path), "not a number");
            return setting.defaultValue();
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            report.reject(absolute(section, path), "not a usable number");
            return setting.defaultValue();
        }
        if (!Numbers.hasAllowedPrecision(value)) {
            report.reject(absolute(section, path), "more than 2 decimal places");
            return setting.defaultValue();
        }
        if (!setting.accepts(value)) {
            report.reject(absolute(section, path), "outside " + setting.limitDescription());
            return setting.defaultValue();
        }
        return value;
    }

    private boolean readBoolean(ConfigurationSection section, String path, boolean fallback,
                                ValidationReport report) {
        Object raw = section.get(path);
        if (raw == null) {
            return fallback;
        }
        if (!(raw instanceof Boolean value)) {
            report.reject(absolute(section, path), "not true or false");
            return fallback;
        }
        return value;
    }

    private String absolute(ConfigurationSection section, String path) {
        String parent = section.getCurrentPath();
        return parent == null || parent.isEmpty() ? path : parent + "." + path;
    }

    public static @Nullable Material matchItem(String name) {
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (material == null || material.isLegacy() || material == Material.AIR || !material.isItem()) {
            return null;
        }
        return material;
    }
}
