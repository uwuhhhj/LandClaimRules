package io.github.loliiiico.rules;

import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.nation.Nation;
import org.bukkit.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiHollowClaimRule {

    public static final class Result {
        public final boolean allowed;
        public final boolean cacheHit;
        public final int holesCached;

        public Result(boolean allowed, boolean cacheHit, int holesCached) {
            this.allowed = allowed;
            this.cacheHit = cacheHit;
            this.holesCached = holesCached;
        }
    }

    public static final class CacheKey {
        private final String landName;
        private final UUID worldId;

        private CacheKey(String landName, UUID worldId) {
            this.landName = landName;
            this.worldId = worldId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) o;
            return Objects.equals(landName, other.landName) && Objects.equals(worldId, other.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(landName, worldId);
        }
    }

    public static final class Snapshot {
        private final HashSet<Long> claimed;
        private final boolean empty;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private Snapshot(HashSet<Long> claimed, boolean empty, int minX, int maxX, int minZ, int maxZ) {
            this.claimed = claimed;
            this.empty = empty;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    private static final class CacheEntry {
        private final int holes;
        private final HashSet<Long> holeCells;

        private CacheEntry(int holes, HashSet<Long> holeCells) {
            this.holes = holes;
            this.holeCells = holeCells;
        }
    }

    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public CacheKey key(Land land, World world) {
        return new CacheKey(land.getName(), world.getUID());
    }

    public Result checkCached(CacheKey key, int cx, int cz) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return new Result(true, false, 0);
        }

        if (entry.holes == 0) {
            return new Result(true, true, 0);
        }

        if (entry.holeCells != null && entry.holeCells.contains(pack(cx, cz))) {
            return new Result(true, true, entry.holes);
        }

        return new Result(false, true, entry.holes);
    }

    public Snapshot snapshot(Land land, World world) {
        Collection<? extends ChunkCoordinate> chunks = land.getChunks(world);
        HashSet<Long> claimed = new HashSet<>(chunks == null ? 16 : Math.max(16, chunks.size() * 2));

        boolean first = true;
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;

        if (chunks != null) {
            for (ChunkCoordinate chunk : chunks) {
                int x = chunk.getX();
                int z = chunk.getZ();
                claimed.add(pack(x, z));
                if (first) {
                    minX = maxX = x;
                    minZ = maxZ = z;
                    first = false;
                } else {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (z < minZ) minZ = z;
                    if (z > maxZ) maxZ = z;
                }
            }
        }

        if (!first) {
            Nation nation = land.getNation();
            if (nation != null) {
                // Treat same-nation claims inside this land's bounds as filled to avoid false hollow hits.
                for (Land member : nation.getLands()) {
                    if (member == land) {
                        continue;
                    }
                    Collection<? extends ChunkCoordinate> memberChunks = member.getChunks(world);
                    if (memberChunks == null) {
                        continue;
                    }
                    for (ChunkCoordinate chunk : memberChunks) {
                        int x = chunk.getX();
                        int z = chunk.getZ();
                        if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                            claimed.add(pack(x, z));
                        }
                    }
                }
            }
        }

        return new Snapshot(claimed, first, minX, maxX, minZ, maxZ);
    }

    public void updateCache(CacheKey key, Snapshot snapshot) {
        if (snapshot == null || snapshot.empty) {
            cache.put(key, new CacheEntry(0, new HashSet<>()));
            return;
        }

        CacheEntry entry = computeHoles(snapshot.claimed, snapshot.minX, snapshot.maxX, snapshot.minZ, snapshot.maxZ);
        cache.put(key, entry);
    }

    private static CacheEntry computeHoles(HashSet<Long> claimed, int minX, int maxX, int minZ, int maxZ) {
        int minXExp = minX - 1;
        int maxXExp = maxX + 1;
        int minZExp = minZ - 1;
        int maxZExp = maxZ + 1;

        int width = maxXExp - minXExp + 1;
        int height = maxZExp - minZExp + 1;
        boolean[] outside = new boolean[width * height];
        long[] queue = new long[width * height];
        int head = 0;
        int tail = 0;

        for (int x = minXExp; x <= maxXExp; x++) {
            tail = tryEnqueue(x, minZExp, minXExp, minZExp, width, claimed, outside, queue, tail);
            tail = tryEnqueue(x, maxZExp, minXExp, minZExp, width, claimed, outside, queue, tail);
        }
        for (int z = minZExp + 1; z <= maxZExp - 1; z++) {
            tail = tryEnqueue(minXExp, z, minXExp, minZExp, width, claimed, outside, queue, tail);
            tail = tryEnqueue(maxXExp, z, minXExp, minZExp, width, claimed, outside, queue, tail);
        }

        while (head < tail) {
            long packed = queue[head++];
            int x = (int) (packed >> 32);
            int z = (int) packed;

            if (x > minXExp) {
                tail = tryEnqueue(x - 1, z, minXExp, minZExp, width, claimed, outside, queue, tail);
            }
            if (x < maxXExp) {
                tail = tryEnqueue(x + 1, z, minXExp, minZExp, width, claimed, outside, queue, tail);
            }
            if (z > minZExp) {
                tail = tryEnqueue(x, z - 1, minXExp, minZExp, width, claimed, outside, queue, tail);
            }
            if (z < maxZExp) {
                tail = tryEnqueue(x, z + 1, minXExp, minZExp, width, claimed, outside, queue, tail);
            }
        }

        int holes = 0;
        HashSet<Long> holeCells = new HashSet<>();
        for (int z = minZExp; z <= maxZExp; z++) {
            for (int x = minXExp; x <= maxXExp; x++) {
                long key = pack(x, z);
                if (!claimed.contains(key)) {
                    int idx = indexOf(x, z, minXExp, minZExp, width);
                    if (!outside[idx]) {
                        holes++;
                        holeCells.add(key);
                    }
                }
            }
        }

        return new CacheEntry(holes, holeCells);
    }

    private static int tryEnqueue(
            int x,
            int z,
            int minXExp,
            int minZExp,
            int width,
            HashSet<Long> claimed,
            boolean[] outside,
            long[] queue,
            int tail
    ) {
        if (claimed.contains(pack(x, z))) return tail;
        int idx = indexOf(x, z, minXExp, minZExp, width);
        if (outside[idx]) return tail;
        outside[idx] = true;
        queue[tail++] = pack(x, z);
        return tail;
    }

    private static int indexOf(int x, int z, int minXExp, int minZExp, int width) {
        return (x - minXExp) + (z - minZExp) * width;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
