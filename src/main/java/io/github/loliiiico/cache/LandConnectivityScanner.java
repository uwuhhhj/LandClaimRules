package io.github.loliiiico.cache;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class LandConnectivityScanner {

    public static final class Snapshot {
        private final String landName;
        private final String worldName;
        private final HashSet<Long> claimed;

        private Snapshot(String landName, String worldName, HashSet<Long> claimed) {
            this.landName = landName;
            this.worldName = worldName;
            this.claimed = claimed;
        }
    }

    public List<Snapshot> snapshotAll(LandsIntegration lands) {
        List<Snapshot> snapshots = new ArrayList<>();
        for (Land land : lands.getLands()) {
            for (Container container : land.getContainers()) {
                if (container == null || container.getWorld() == null || container.getWorld().getWorld() == null) {
                    continue;
                }
                String worldName = container.getWorld().getWorld().getName();
                Collection<? extends ChunkCoordinate> chunks = container.getChunks();
                HashSet<Long> claimed = new HashSet<>(chunks == null ? 16 : Math.max(16, chunks.size() * 2));
                if (chunks != null) {
                    for (ChunkCoordinate chunk : chunks) {
                        claimed.add(pack(chunk.getX(), chunk.getZ()));
                    }
                }
                snapshots.add(new Snapshot(land.getName(), worldName, claimed));
            }
        }
        return snapshots;
    }

    public Map<DisconnectedLandRegistry.LandWorldKey, Integer> findDisconnected(List<Snapshot> snapshots) {
        Map<DisconnectedLandRegistry.LandWorldKey, Integer> result = new HashMap<>();
        for (Snapshot snap : snapshots) {
            int size = snap.claimed.size();
            if (size <= 1) {
                continue;
            }
            if (!isConnected(snap.claimed)) {
                DisconnectedLandRegistry.LandWorldKey key =
                        new DisconnectedLandRegistry.LandWorldKey(snap.landName, snap.worldName);
                result.put(key, size);
            }
        }
        return result;
    }

    private static boolean isConnected(HashSet<Long> claimed) {
        long start = claimed.iterator().next();
        HashSet<Long> visited = new HashSet<>(Math.max(16, claimed.size() * 2));
        ArrayDeque<Long> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            long packed = queue.poll();
            int x = (int) (packed >> 32);
            int z = (int) packed;
            long n1 = pack(x + 1, z);
            long n2 = pack(x - 1, z);
            long n3 = pack(x, z + 1);
            long n4 = pack(x, z - 1);
            tryVisit(n1, claimed, visited, queue);
            tryVisit(n2, claimed, visited, queue);
            tryVisit(n3, claimed, visited, queue);
            tryVisit(n4, claimed, visited, queue);
        }

        return visited.size() == claimed.size();
    }

    private static void tryVisit(long key, HashSet<Long> claimed, HashSet<Long> visited, ArrayDeque<Long> queue) {
        if (!claimed.contains(key) || visited.contains(key)) return;
        visited.add(key);
        queue.add(key);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
