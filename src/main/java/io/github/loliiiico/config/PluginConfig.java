package io.github.loliiiico.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {

    private final int dualRingM;
    private final int dualRingN;
    private final boolean antiHollowEnabled;
    private final String dualRingDeniedMessage;
    private final String antiHollowDeniedMessage;
    private final String noPermissionMessage;
    private final String scanStartMessage;
    private final String scanCompleteMessage;
    private final String scanNotReadyMessage;
    private final String noScanResultsMessage;
    private final String listHeaderMessage;
    private final String listAgeMessage;
    private final String listEmptyMessage;
    private final String helpHeaderMessage;
    private final String helpScanMessage;
    private final String helpListMessage;
    private final String invalidPageMessage;
    private final String unknownSubcommandMessage;

    public PluginConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.dualRingM = config.getInt("rules.dualRing.m", 1);
        this.dualRingN = config.getInt("rules.dualRing.n", 5);
        this.antiHollowEnabled = config.getBoolean("rules.antiHollow.enabled", true);
        this.dualRingDeniedMessage = colorize(config.getString(
                "messages.dualRingDenied",
                "&cClaim denied: insufficient support (3x3 {ring1}/{need3x3} need {lack3x3}, 5x5 {ring12}/{need5x5} need {lack5x5})."
        ));
        this.antiHollowDeniedMessage = colorize(config.getString(
                "messages.antiHollowDenied",
                "&cClaim denied: hollow areas exist (cached holes={holes}). Fill them first."
        ));
        this.noPermissionMessage = colorize(config.getString(
                "messages.noPermission",
                "&c你没有权限执行此命令。"
        ));
        this.scanStartMessage = colorize(config.getString(
                "messages.scanStart",
                "&7开始扫描断裂领地..."
        ));
        this.scanCompleteMessage = colorize(config.getString(
                "messages.scanComplete",
                "&a扫描完成，断裂领地数量：{count}"
        ));
        this.scanNotReadyMessage = colorize(config.getString(
                "messages.scanNotReady",
                "&cLands 仍未就绪。"
        ));
        this.noScanResultsMessage = colorize(config.getString(
                "messages.noScanResults",
                "&e暂无扫描结果，请先执行 /{label} scan。"
        ));
        this.listHeaderMessage = colorize(config.getString(
                "messages.listHeader",
                "&6断裂领地：{total}（第 {page}/{pages} 页）"
        ));
        this.listAgeMessage = colorize(config.getString(
                "messages.listAge",
                "&7上次扫描：{age} 前。"
        ));
        this.listEmptyMessage = colorize(config.getString(
                "messages.listEmpty",
                "&a没有发现断裂领地。"
        ));
        this.helpHeaderMessage = colorize(config.getString(
                "messages.helpHeader",
                "&eLandClaimRules 命令："
        ));
        this.helpScanMessage = colorize(config.getString(
                "messages.helpScan",
                "&7/{label} scan &f- 扫描断裂领地"
        ));
        this.helpListMessage = colorize(config.getString(
                "messages.helpList",
                "&7/{label} disconnected [page] &f- 列出断裂领地"
        ));
        this.invalidPageMessage = colorize(config.getString(
                "messages.invalidPage",
                "&c页码无效。"
        ));
        this.unknownSubcommandMessage = colorize(config.getString(
                "messages.unknownSubcommand",
                "&c未知子命令，请输入 /{label} help。"
        ));
    }

    public int getDualRingM() {
        return dualRingM;
    }

    public int getDualRingN() {
        return dualRingN;
    }

    public boolean isAntiHollowEnabled() {
        return antiHollowEnabled;
    }

    public String formatDualRingDenied(
            int ring1,
            int need3x3,
            int lack3x3,
            int ring12,
            int need5x5,
            int lack5x5
    ) {
        return formatMessage(dualRingDeniedMessage,
                "{ring1}", Integer.toString(ring1),
                "{need3x3}", Integer.toString(need3x3),
                "{lack3x3}", Integer.toString(lack3x3),
                "{ring12}", Integer.toString(ring12),
                "{need5x5}", Integer.toString(need5x5),
                "{lack5x5}", Integer.toString(lack5x5)
        );
    }

    public String formatAntiHollowDenied(int holes) {
        return formatMessage(antiHollowDeniedMessage, "{holes}", Integer.toString(holes));
    }

    public String formatNoPermission() {
        return noPermissionMessage;
    }

    public String formatScanStart() {
        return scanStartMessage;
    }

    public String formatScanComplete(int count) {
        return formatMessage(scanCompleteMessage, "{count}", Integer.toString(count));
    }

    public String formatScanNotReady() {
        return scanNotReadyMessage;
    }

    public String formatNoScanResults(String label) {
        return formatMessage(noScanResultsMessage, "{label}", label);
    }

    public String formatListHeader(int total, int page, int pages) {
        return formatMessage(listHeaderMessage,
                "{total}", Integer.toString(total),
                "{page}", Integer.toString(page),
                "{pages}", Integer.toString(pages)
        );
    }

    public String formatListAge(String age) {
        return formatMessage(listAgeMessage, "{age}", age);
    }

    public String formatListEmpty() {
        return listEmptyMessage;
    }

    public String formatHelpHeader() {
        return helpHeaderMessage;
    }

    public String formatHelpScan(String label) {
        return formatMessage(helpScanMessage, "{label}", label);
    }

    public String formatHelpList(String label) {
        return formatMessage(helpListMessage, "{label}", label);
    }

    public String formatInvalidPage() {
        return invalidPageMessage;
    }

    public String formatUnknownSubcommand(String label) {
        return formatMessage(unknownSubcommandMessage, "{label}", label);
    }

    private static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String formatMessage(String message, String... pairs) {
        String result = message;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace(pairs[i], pairs[i + 1]);
        }
        return result;
    }
}
