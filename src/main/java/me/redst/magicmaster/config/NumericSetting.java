package me.redst.magicmaster.config;

import me.redst.magicmaster.util.Numbers;

import java.math.BigDecimal;
import java.util.List;

public enum NumericSetting {

    MULTIPLIER(0.01D, 100.00D, 2.00D, List.of("1", "2", "3", "4")),
    THRESHOLD_LEVEL(1.00D, 1000.00D, 15.00D, List.of("10", "15", "20", "30")),
    INTERVAL(0.01D, 1000.00D, 15.00D, List.of("5", "10", "15", "20")),
    ADDITIONAL_ENCHANTMENTS(1.00D, 20.00D, 1.00D, List.of("1", "2", "3")),
    LEVELS_PER_INTERVAL(0.01D, 20.00D, 1.00D, List.of("1", "2", "3"));

    private final double minimum;
    private final double maximum;
    private final double defaultValue;
    private final List<String> suggestions;

    NumericSetting(double minimum, double maximum, double defaultValue, List<String> suggestions) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.defaultValue = defaultValue;
        this.suggestions = suggestions;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public List<String> suggestions() {
        return suggestions;
    }

    public boolean accepts(double value) {
        return Double.isFinite(value)
                && value >= minimum
                && value <= maximum
                && Numbers.hasAllowedPrecision(value);
    }

    public String limitDescription() {
        return Numbers.format(minimum) + " - " + Numbers.format(maximum);
    }

    public Double parse(String input) {
        BigDecimal decimal;
        try {
            decimal = new BigDecimal(input.trim());
        } catch (NumberFormatException expected) {
            return null;
        }
        if (decimal.stripTrailingZeros().scale() > 2) {
            return null;
        }
        double value = decimal.doubleValue();
        return accepts(value) ? value : null;
    }
}
