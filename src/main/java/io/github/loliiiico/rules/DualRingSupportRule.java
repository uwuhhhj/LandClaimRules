package io.github.loliiiico.rules;

import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;

public final class DualRingSupportRule {

    public static final class Result {
        public final boolean allowed;
        public final int ring1Claimed;
        public final int ring12Claimed;

        public Result(boolean allowed, int ring1Claimed, int ring12Claimed) {
            this.allowed = allowed;
            this.ring1Claimed = ring1Claimed;
            this.ring12Claimed = ring12Claimed;
        }
    }

    private final int m;
    private final int n;

    public int getM() { return m; }
    public int getN() { return n; }

    public DualRingSupportRule(int m, int n) {
        this.m = m;
        this.n = n;
    }

    public Result check(Land land, LandWorld landWorld, int cx, int cz) {
        Container container = land.getContainer(landWorld.getWorld());

        // 第一次在该世界圈地：放行（否则永远无法开始）
        if (container == null) {
            return new Result(true, 0, 0);
        }

        int ring1Claimed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (container.hasChunk(cx + dx, cz + dz)) ring1Claimed++;
            }
        }

        int ring12Claimed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (container.hasChunk(cx + dx, cz + dz)) ring12Claimed++;
            }
        }

        boolean ok = ring1Claimed > m && ring12Claimed > n;
        return new Result(ok, ring1Claimed, ring12Claimed);
    }
}
