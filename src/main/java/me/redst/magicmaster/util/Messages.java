package me.redst.magicmaster.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Messages {

    private static final Component PREFIX = Component.text("MagicMaster", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(" \u00BB ", NamedTextColor.DARK_GRAY));

    private Messages() {
    }

    public static Component info(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GRAY));
    }

    public static Component success(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GREEN));
    }

    public static Component warn(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.YELLOW));
    }

    public static Component error(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.RED));
    }
}
