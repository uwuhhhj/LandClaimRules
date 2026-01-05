package io.github.loliiiico;

import io.github.loliiiico.rules.AntiHollowClaimRule;
import io.github.loliiiico.rules.DualRingSupportRule;
import io.github.loliiiico.config.PluginConfig;
import me.angeschossen.lands.api.events.ChunkDeleteEvent;
import me.angeschossen.lands.api.events.ChunkPostClaimEvent;
import me.angeschossen.lands.api.events.ChunkPreClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class LandsClaimListener implements Listener {

    private final LandClaimRules plugin;
    private final DualRingSupportRule rule;
    private final AntiHollowClaimRule hollowRule = new AntiHollowClaimRule();
    private final PluginConfig config;

    public LandsClaimListener(LandClaimRules plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.rule = new DualRingSupportRule(config.getDualRingM(), config.getDualRingN());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkPreClaim(ChunkPreClaimEvent event) {
        World world = event.getWorld().getWorld();
        if (world == null) {
            return;
        }

        UUID uuid = event.getPlayerUUID();
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);

        if (config.isAntiHollowEnabled()) {
            if (player != null && player.hasPermission("landclaimrules.bypass.antihollow")) {
                return;
            }
            AntiHollowClaimRule.CacheKey key = hollowRule.key(event.getLand(), world);
            AntiHollowClaimRule.Result hollowRes = hollowRule.checkCached(key, event.getX(), event.getZ());
            if (!hollowRes.cacheHit) {
                refreshHollowCache(event.getLand(), world);
            }

            if (!hollowRes.allowed) {
                event.setCancelled(true);

                if (player != null) {
                    player.sendMessage(config.formatAntiHollowDenied(hollowRes.holesCached));
                }

                plugin.getLogger().info("[DENY AntiHollowClaim] land=" + event.getLand().getName()
                        + " world=" + event.getWorld().getName()
                        + " cx=" + event.getX() + " cz=" + event.getZ()
                        + " holes=" + hollowRes.holesCached);
                return;
            }
        }

        if (player != null && player.hasPermission("landclaimrules.bypass.dualring")) {
            return;
        }

        DualRingSupportRule.Result res = rule.check(event.getLand(), event.getWorld(), event.getX(), event.getZ());
        if (res.allowed) return;

        event.setCancelled(true);

        String detail = "ring1=" + res.ring1Claimed + " ring12=" + res.ring12Claimed;

        if (uuid != null) {
            int need3x3 = rule.getM() + 1;
            int need5x5 = rule.getN() + 1;
            int lack3x3 = Math.max(0, need3x3 - res.ring1Claimed);
            int lack5x5 = Math.max(0, need5x5 - res.ring12Claimed);

            String msg = config.formatDualRingDenied(
                    res.ring1Claimed,
                    need3x3,
                    lack3x3,
                    res.ring12Claimed,
                    need5x5,
                    lack5x5
            );

            if (player != null) player.sendMessage(msg);
        }

        plugin.getLogger().info("[DENY DualRingSupport] land=" + event.getLand().getName()
                + " world=" + event.getWorld().getName()
                + " cx=" + event.getX() + " cz=" + event.getZ()
                + " " + detail);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPostClaim(ChunkPostClaimEvent event) {
        World world = event.getWorld().getWorld();
        if (world == null) {
            return;
        }
        refreshHollowCache(event.getLand(), world);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkDelete(ChunkDeleteEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        refreshHollowCache(event.getLand(), world);
    }

    private void refreshHollowCache(me.angeschossen.lands.api.land.Land land, World world) {
        AntiHollowClaimRule.CacheKey key = hollowRule.key(land, world);
        AntiHollowClaimRule.Snapshot snapshot = hollowRule.snapshot(land, world);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> hollowRule.updateCache(key, snapshot));
    }
}
