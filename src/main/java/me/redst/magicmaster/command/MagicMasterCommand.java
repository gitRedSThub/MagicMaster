package me.redst.magicmaster.command;

import me.redst.magicmaster.config.ConfigManager;
import me.redst.magicmaster.config.ValidationReport;
import me.redst.magicmaster.item.EnchantableItems;
import me.redst.magicmaster.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MagicMasterCommand implements TabExecutor {

    public static final String PERMISSION = "magicmaster.admin";

    private static final List<String> SUBCOMMANDS = List.of("set", "toggle", "info", "reload", "help");

    private final ConfigManager config;
    private final SettingTree settings;

    public MagicMasterCommand(ConfigManager config, EnchantableItems enchantableItems) {
        this.config = config;
        this.settings = new SettingTree(config, enchantableItems);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] arguments) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Messages.error("You do not have permission to use this command."));
            return true;
        }
        if (arguments.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "set" -> sender.sendMessage(
                    settings.apply(Arrays.copyOfRange(arguments, 1, arguments.length)));
            case "toggle" -> toggle(sender, arguments);
            case "info" -> InfoRenderer.render(config.current()).forEach(sender::sendMessage);
            case "reload" -> reload(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Messages.error("Unknown command: " + arguments[0]));
                sendHelp(sender);
            }
        }
        return true;
    }

    private void toggle(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            sender.sendMessage(Messages.info("Usage: /magicmaster toggle <"
                    + String.join(" | ", SettingTree.SECTIONS) + ">"));
            return;
        }
        String section = arguments[1].toLowerCase(Locale.ROOT);
        Boolean enabled = switch (section) {
            case ConfigManager.MULTIPLY_LEVEL -> config.current().multiplyLevel().enabled();
            case ConfigManager.ENCHANTMENT_PLUS -> config.current().enchantmentPlus().enabled();
            case ConfigManager.LEVEL_PLUS -> config.current().levelPlus().enabled();
            default -> null;
        };
        if (enabled == null) {
            sender.sendMessage(Messages.error("Unknown system: " + arguments[1]));
            return;
        }
        boolean next = !enabled;
        if (!config.write(section + "." + ConfigManager.ENABLED, next)) {
            sender.sendMessage(Messages.error("Could not save config.yml. Nothing was changed."));
            return;
        }
        sender.sendMessage(Messages.success(displayName(section) + " is now " + (next ? "ON" : "OFF")));
    }

    private void reload(CommandSender sender) {
        ValidationReport report = config.reload();
        sender.sendMessage(Messages.success("MagicMaster configuration reloaded."));
        if (report.count() > 0) {
            sender.sendMessage(Messages.warn(report.count() == 1
                    ? "1 invalid value was reset to its default."
                    : report.count() + " invalid values were reset to their defaults."));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("MagicMaster", NamedTextColor.LIGHT_PURPLE));
        sendHelpLine(sender, "/magicmaster set", "Change a configurable setting.");
        sendHelpLine(sender, "/magicmaster toggle", "Turn a main system on or off.");
        sendHelpLine(sender, "/magicmaster info", "Show current MagicMaster settings.");
        sendHelpLine(sender, "/magicmaster reload", "Reload config.yml.");
        sendHelpLine(sender, "/magicmaster help", "Show this help message.");
    }

    private void sendHelpLine(CommandSender sender, String usage, String description) {
        sender.sendMessage(Component.text(usage, NamedTextColor.AQUA)
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(description, NamedTextColor.GRAY)));
    }

    private static String displayName(String section) {
        return switch (section) {
            case ConfigManager.MULTIPLY_LEVEL -> "Multiply Level";
            case ConfigManager.ENCHANTMENT_PLUS -> "Enchantment Plus";
            default -> "Level Plus";
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, String[] arguments) {
        if (!sender.hasPermission(PERMISSION) || arguments.length == 0) {
            return List.of();
        }
        if (arguments.length == 1) {
            return SettingTree.filter(SUBCOMMANDS, arguments[0]);
        }
        String subcommand = arguments[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("toggle")) {
            return arguments.length == 2
                    ? SettingTree.filter(SettingTree.SECTIONS, arguments[1])
                    : List.of();
        }
        if (subcommand.equals("set")) {
            return settings.complete(Arrays.copyOfRange(arguments, 1, arguments.length));
        }
        return List.of();
    }
}
