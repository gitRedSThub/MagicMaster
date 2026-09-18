package me.redst.magicmaster.util;

import java.math.BigDecimal;
import java.util.Locale;

public final class Numbers {

    public static final int MAX_EFFECTIVE_LEVEL = 10_000;

    private Numbers() {
    }

    public static boolean hasAllowedPrecision(double value) {
        if (!Double.isFinite(value)) {
            return false;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().scale() <= 2;
    }

    public static int effectiveLevel(int baseLevel, double multiplier) {
        if (baseLevel <= 0) {
            return 0;
        }
        if (!Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return baseLevel;
        }
        double raw = (double) baseLevel * multiplier;
        if (!Double.isFinite(raw)) {
            return MAX_EFFECTIVE_LEVEL;
        }
        long rounded = Math.round(raw);
        if (rounded < 1L) {
            return 1;
        }
        return (int) Math.min(rounded, (long) MAX_EFFECTIVE_LEVEL);
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
