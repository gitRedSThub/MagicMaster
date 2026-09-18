package me.redst.magicmaster.util;

import java.util.Locale;

public final class Text {

    private Text() {
    }

    public static String pretty(String constantName) {
        String[] parts = constantName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(constantName.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        return builder.toString();
    }

    public static boolean startsWith(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
