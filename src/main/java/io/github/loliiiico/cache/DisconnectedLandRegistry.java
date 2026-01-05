package io.github.loliiiico.cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DisconnectedLandRegistry {

    public static final class LandWorldKey {
        private final String landName;
        private final String worldName;

        public LandWorldKey(String landName, String worldName) {
            this.landName = landName;
            this.worldName = worldName;
        }

        public String getLandName() {
            return landName;
        }

        public String getWorldName() {
            return worldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LandWorldKey)) return false;
            LandWorldKey other = (LandWorldKey) o;
            return landName.equals(other.landName) && worldName.equals(other.worldName);
        }

        @Override
        public int hashCode() {
            return 31 * landName.hashCode() + worldName.hashCode();
        }
    }

    private final ConcurrentHashMap<LandWorldKey, Integer> disconnected = new ConcurrentHashMap<>();
    private volatile long lastScanEpochMs = 0L;

    public void update(Map<LandWorldKey, Integer> next) {
        disconnected.clear();
        disconnected.putAll(next);
        lastScanEpochMs = System.currentTimeMillis();
    }

    public int size() {
        return disconnected.size();
    }

    public long getLastScanEpochMs() {
        return lastScanEpochMs;
    }

    public List<String> listLines(int offset, int limit) {
        List<Map.Entry<LandWorldKey, Integer>> entries = new ArrayList<>(disconnected.entrySet());
        entries.sort(Comparator.comparing((Map.Entry<LandWorldKey, Integer> e) -> e.getKey().getLandName())
                .thenComparing(e -> e.getKey().getWorldName()));

        int start = Math.max(0, offset);
        int end = Math.min(entries.size(), start + Math.max(0, limit));
        List<String> lines = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Map.Entry<LandWorldKey, Integer> entry = entries.get(i);
            LandWorldKey key = entry.getKey();
            lines.add(key.getLandName() + " @ " + key.getWorldName() + " (chunks=" + entry.getValue() + ")");
        }
        return lines;
    }
}
