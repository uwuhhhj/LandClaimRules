package io.github.loliiiico;

import io.github.loliiiico.rules.DualRingSupportRule;
import me.angeschossen.lands.api.events.ChunkPreClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class LandsClaimListener implements Listener {

    private final LandClaimRules plugin;
    private final DualRingSupportRule rule = new DualRingSupportRule(1, 5);

    public LandsClaimListener(LandClaimRules plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkPreClaim(ChunkPreClaimEvent event) {
        DualRingSupportRule.Result res = rule.check(event.getLand(), event.getWorld(), event.getX(), event.getZ());
        if (res.allowed) return;

        event.setCancelled(true);

        String detail = "ring1=" + res.ring1Claimed + " ring12=" + res.ring12Claimed;

        UUID uuid = event.getPlayerUUID();
        if (uuid != null) {
            Player p = Bukkit.getPlayer(uuid);

            // 一句话：3×3一圈 / 5×5两圈，用“当前/需要”和“差多少”
            int need3x3 = rule.getM() + 1; // 严格 >m
            int need5x5 = rule.getN() + 1; // 严格 >n
            int lack3x3 = Math.max(0, need3x3 - res.ring1Claimed);
            int lack5x5 = Math.max(0, need5x5 - res.ring12Claimed);
            
            String msg = String.format(
                    "§c圈地失败：双环支撑不足（3×3一圈 %d/%d 差%d；5×5两圈 %d/%d 差%d）",
                    res.ring1Claimed, need3x3, lack3x3,
                    res.ring12Claimed, need5x5, lack5x5
            );
            
            if (p != null) p.sendMessage(msg);
        }

        plugin.getLogger().info("[DENY DualRingSupport] land=" + event.getLand().getName()
                + " world=" + event.getWorld().getName()
                + " cx=" + event.getX() + " cz=" + event.getZ()
                + " " + detail);
    }
}
