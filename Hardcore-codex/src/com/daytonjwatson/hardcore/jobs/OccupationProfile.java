package com.daytonjwatson.hardcore.jobs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import com.daytonjwatson.hardcore.jobs.JobsManager.PayoutRecord;

public final class OccupationProfile {
    private final Occupation occupation;
    private final long chosenAt;
    private double lifetimeEarnings;
    private double sessionEarnings;
    private double dailyEarnings;
    private long lastPayoutAt;
    private long lastMoveAt;
    private long dailyResetAt;
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Integer> bests = new HashMap<>();
    private final Map<String, Integer> streaks = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> recentVictims = new HashMap<>();
    private final Set<String> placedBlocks = new HashSet<>();
    private final Deque<String> recentLocations = new ArrayDeque<>();
    private final Deque<PayoutRecord> recentPayouts = new ArrayDeque<>();
    private final Map<Material, Integer> buildSessionCounts = new HashMap<>();

    public OccupationProfile(Occupation occupation, long chosenAt) {
        this.occupation = occupation;
        this.chosenAt = chosenAt;
        this.dailyResetAt = System.currentTimeMillis();
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public long getChosenAt() {
        return chosenAt;
    }

    public double getLifetimeEarnings() {
        return lifetimeEarnings;
    }

    public void setLifetimeEarnings(double lifetimeEarnings) {
        this.lifetimeEarnings = lifetimeEarnings;
    }

    public double getSessionEarnings() {
        return sessionEarnings;
    }

    public void setSessionEarnings(double sessionEarnings) {
        this.sessionEarnings = sessionEarnings;
    }

    public double getDailyEarnings() {
        return dailyEarnings;
    }

    public void setDailyEarnings(double dailyEarnings) {
        this.dailyEarnings = dailyEarnings;
    }

    public long getLastPayoutAt() {
        return lastPayoutAt;
    }

    public void setLastPayoutAt(long lastPayoutAt) {
        this.lastPayoutAt = lastPayoutAt;
    }

    public long getDailyResetAt() {
        return dailyResetAt;
    }

    public void setDailyResetAt(long dailyResetAt) {
        this.dailyResetAt = dailyResetAt;
    }

    public Map<String, Integer> getCounters() {
        return counters;
    }

    public Map<String, Integer> getBests() {
        return bests;
    }

    public Map<String, Integer> getStreaks() {
        return streaks;
    }

    public Map<UUID, Long> getRecentVictims() {
        return recentVictims;
    }

    public Deque<PayoutRecord> getRecentPayouts() {
        return recentPayouts;
    }

    public Set<String> getPlacedBlocks() {
        return placedBlocks;
    }

    public Deque<String> getRecentLocations() {
        return recentLocations;
    }

    public boolean canCollect(String key, int cooldownSeconds, long now) {
        long last = cooldowns.getOrDefault(key, 0L);
        if (cooldownSeconds > 0 && now - last < cooldownSeconds * 1000L) {
            return false;
        }
        cooldowns.put(key, now);
        return true;
    }

    public void bookkeepPayout(double amount, String reason, long now, int limit) {
        lifetimeEarnings += amount;
        sessionEarnings += amount;
        dailyEarnings += amount;
        lastPayoutAt = now;
        recentPayouts.addFirst(new PayoutRecord(reason, amount, now));
        while (recentPayouts.size() > limit) {
            recentPayouts.removeLast();
        }
    }

    public void incrementCounter(String key) {
        int next = counters.getOrDefault(key, 0) + 1;
        counters.put(key, next);
        int best = bests.getOrDefault(key, 0);
        if (next > best) {
            bests.put(key, next);
        }
    }

    public boolean isPlayerPlaced(Location location) {
        return placedBlocks.contains(JobsManager.serializeLocation(location));
    }

    public void addPlacedBlock(Location location) {
        placedBlocks.add(JobsManager.serializeLocation(location));
    }

    public void setPlacedBlocks(Set<String> placedBlocks) {
        this.placedBlocks.clear();
        this.placedBlocks.addAll(placedBlocks);
    }

    public boolean canRewardLocation(Chunk chunk, int cooldownSeconds, int limit) {
        String key = JobsManager.chunkKey(chunk);
        if (recentLocations.contains(key)) {
            return false;
        }
        recentLocations.addFirst(key);
        while (recentLocations.size() > limit) {
            recentLocations.removeLast();
        }
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault("location", 0L);
        if (cooldownSeconds > 0 && now - last < cooldownSeconds * 1000L) {
            return false;
        }
        cooldowns.put("location", now);
        return true;
    }

    public void setRecentLocations(Deque<String> locations) {
        recentLocations.clear();
        recentLocations.addAll(locations);
    }

    public void setRecentPayouts(Deque<PayoutRecord> payouts) {
        recentPayouts.clear();
        recentPayouts.addAll(payouts);
    }

    public boolean recentlyMoved() {
        return System.currentTimeMillis() - lastMoveAt < 30000L;
    }

    public void markActivity() {
        lastMoveAt = System.currentTimeMillis();
    }

    public void setLastLocationTime(long time) {
        cooldowns.put("location", time);
    }

    public boolean readyForBuildReward(int blockThreshold, int uniqueThreshold) {
        int total = 0;
        for (int count : buildSessionCounts.values()) {
            total += count;
        }
        return total >= blockThreshold && buildSessionCounts.size() >= uniqueThreshold;
    }

    public void recordBuildSession(Material material) {
        buildSessionCounts.merge(material, 1, Integer::sum);
    }

    public void resetBuildSession() {
        buildSessionCounts.clear();
    }

    public Map<UUID, Long> getRecentVictimsCopy() {
        return new HashMap<>(recentVictims);
    }

    public void setRecentVictims(Map<UUID, Long> victims) {
        recentVictims.clear();
        recentVictims.putAll(victims);
    }

    public boolean allowVictim(UUID uuid, int cooldownSeconds) {
        long now = System.currentTimeMillis();
        long last = recentVictims.getOrDefault(uuid, 0L);
        if (cooldownSeconds > 0 && now - last < cooldownSeconds * 1000L) {
            return false;
        }
        recentVictims.put(uuid, now);
        return true;
    }

    public void refreshDaily(long now) {
        if (now - dailyResetAt >= 86_400_000L) {
            dailyResetAt = now;
            dailyEarnings = 0;
        }
    }
}
