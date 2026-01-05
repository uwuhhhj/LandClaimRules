package io.github.loliiiico.commands;

import io.github.loliiiico.LandClaimRules;
import io.github.loliiiico.cache.DisconnectedLandRegistry;
import io.github.loliiiico.config.PluginConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.List;

public final class LandClaimRulesCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 10;
    private final LandClaimRules plugin;

    public LandClaimRulesCommand(LandClaimRules plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginConfig config = plugin.getPluginConfig();
        if (!sender.hasPermission("landclaimrules.admin")) {
            sender.sendMessage(config.formatNoPermission());
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender, label, config);
            return true;
        }

        if ("scan".equalsIgnoreCase(args[0])) {
            plugin.scanDisconnectedLands(sender);
            return true;
        }

        if ("disconnected".equalsIgnoreCase(args[0]) || "list".equalsIgnoreCase(args[0])) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(config.formatInvalidPage());
                    return true;
                }
            }

            DisconnectedLandRegistry registry = plugin.getDisconnectedRegistry();
            if (registry.getLastScanEpochMs() == 0L) {
                sender.sendMessage(config.formatNoScanResults(label));
                return true;
            }

            int total = registry.size();
            int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            int pageIndex = Math.min(page, totalPages);
            int offset = (pageIndex - 1) * PAGE_SIZE;

            List<String> lines = registry.listLines(offset, PAGE_SIZE);
            sender.sendMessage(config.formatListHeader(total, pageIndex, totalPages));

            long ageMs = System.currentTimeMillis() - registry.getLastScanEpochMs();
            sender.sendMessage(config.formatListAge(formatAge(ageMs)));

            if (lines.isEmpty()) {
                sender.sendMessage(config.formatListEmpty());
                return true;
            }

            for (String line : lines) {
                sender.sendMessage("- " + line);
            }
            return true;
        }

        sender.sendMessage(config.formatUnknownSubcommand(label));
        return true;
    }

    private static void sendHelp(CommandSender sender, String label, PluginConfig config) {
        sender.sendMessage(config.formatHelpHeader());
        sender.sendMessage(config.formatHelpScan(label));
        sender.sendMessage(config.formatHelpList(label));
    }

    private static String formatAge(long ageMs) {
        Duration d = Duration.ofMillis(Math.max(0, ageMs));
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        if (minutes <= 0) {
            return seconds + "s";
        }
        return minutes + "m" + seconds + "s";
    }
}
