package io.github.loliiiico;

import me.angeschossen.lands.api.LandsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class LandClaimRules extends JavaPlugin {

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Lands") == null) {
            getLogger().warning("Lands not found. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        LandsIntegration lands = LandsIntegration.of(this);

        lands.onLoad(() -> {
            Bukkit.getPluginManager().registerEvents(new LandsClaimListener(this), this);
            getLogger().info("LandsClaimListener registered.");
        });
    }
}
