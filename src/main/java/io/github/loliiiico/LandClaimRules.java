package io.github.loliiiico;

import io.github.loliiiico.config.PluginConfig;
import io.github.loliiiico.cache.DisconnectedLandRegistry;
import io.github.loliiiico.cache.LandConnectivityScanner;
import io.github.loliiiico.commands.LandClaimRulesCommand;
import me.angeschossen.lands.api.LandsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class LandClaimRules extends JavaPlugin {

    private PluginConfig pluginConfig;
    private LandsClaimListener listener;
    private LandsIntegration lands;
    private final DisconnectedLandRegistry disconnectedRegistry = new DisconnectedLandRegistry();
    private final LandConnectivityScanner connectivityScanner = new LandConnectivityScanner();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("Lands") == null) {
            getLogger().warning("Lands not found. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.pluginConfig = new PluginConfig(this);
        this.lands = LandsIntegration.of(this);

        lands.onLoad(() -> {
            this.listener = new LandsClaimListener(this, pluginConfig);
            Bukkit.getPluginManager().registerEvents(listener, this);
            if (getCommand("landclaimrules") != null) {
                getCommand("landclaimrules").setExecutor(new LandClaimRulesCommand(this));
            }
            getLogger().info("LandsClaimListener registered.");
            scanDisconnectedLands(null);
        });
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public DisconnectedLandRegistry getDisconnectedRegistry() {
        return disconnectedRegistry;
    }

    public void scanDisconnectedLands(CommandSender sender) {
        if (lands == null) {
            if (sender != null) {
                sender.sendMessage(pluginConfig.formatScanNotReady());
            }
            return;
        }

        if (sender != null) {
            sender.sendMessage(pluginConfig.formatScanStart());
        }

        Bukkit.getScheduler().runTask(this, () -> {
            var snapshots = connectivityScanner.snapshotAll(lands);
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                var result = connectivityScanner.findDisconnected(snapshots);
                disconnectedRegistry.update(result);
                if (sender != null) {
                    Bukkit.getScheduler().runTask(this, () -> sender.sendMessage(
                            pluginConfig.formatScanComplete(disconnectedRegistry.size())
                    ));
                }
            });
        });
    }
}
