package com.daytonjwatson.hardcore.jobs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;

public class JobsManager {

    public static final String ADMIN_PERMISSION = "hardcore.jobs.admin";
    private static final int MAX_RECENT_LOCATIONS = 20;
    private static final int MAX_RECENT_PAYOUTS = 20;

    private static JobsManager instance;

    private final JavaPlugin plugin;
    private final File jobFile;
    private final File playerDataFile;
    private final FileConfiguration jobConfig;
    private final FileConfiguration playerConfig;
    private final Map<Occupation, OccupationSettings> occupationSettings = new EnumMap<>(Occupation.class);
    private final Map<UUID, OccupationProfile> profiles = new HashMap<>();
    private final Set<String> placedBlockRegistry = new HashSet<>();
    private final Map<UUID, Occupation> pendingSelection = new HashMap<>();

    private JobsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.jobFile = new File(plugin.getDataFolder(), "jobs.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "jobs-data.yml");
        this.jobConfig = YamlConfiguration.loadConfiguration(jobFile);
        this.playerConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        createDefaults();
        loadSettings();
        loadProfiles();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new JobsManager(plugin);
        }
    }

    public static JobsManager get() {
        return instance;
    }

    public Map<Occupation, OccupationSettings> getOccupationSettings() {
        return occupationSettings;
    }

    public OccupationProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public Occupation getOccupation(UUID uuid) {
        OccupationProfile profile = profiles.get(uuid);
        return profile == null ? null : profile.getOccupation();
    }

    public void queueSelection(Player player, Occupation occupation) {
        pendingSelection.put(player.getUniqueId(), occupation);
    }

    public Occupation popPendingSelection(UUID uuid) {
        return pendingSelection.remove(uuid);
    }

    public void setOccupation(Player player, Occupation occupation, boolean adminOverride) {
        OccupationProfile existing = profiles.get(player.getUniqueId());
        if (existing != null && !adminOverride) {
            player.sendMessage(Util.color("&cYour occupation is permanent and cannot be changed."));
            return;
        }

        OccupationProfile profile = new OccupationProfile(occupation, System.currentTimeMillis());
        profiles.put(player.getUniqueId(), profile);
        save();
        MessageStyler.sendPanel(player, "Occupation Selected",
                "&7You are now a &f" + occupation.getDisplayName() + "&7. This choice is permanent.");
    }

    public void resetOccupation(UUID uuid) {
        profiles.remove(uuid);
        save();
    }

    public void reload() {
        try {
            jobConfig.load(jobFile);
            loadSettings();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to reload jobs.yml: " + ex.getMessage());
        }
    }

    public void handleJoin(Player player) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile != null) {
            profile.markActivity();
        }
    }

    public void markMovement(UUID uuid) {
        OccupationProfile profile = profiles.get(uuid);
        if (profile != null) {
            profile.markActivity();
        }
    }

    public void handleQuit(Player player) {
        save();
    }

    public void handleKill(Player player, Entity killed) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.WARRIOR) {
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.WARRIOR);
        String key;
        double reward;

        if (killed instanceof Player victim) {
            if (!allowNewVictim(profile, victim.getUniqueId(), settings.getCooldown("pvp-kill"))) {
                return;
            }
            key = "pvp-kill";
            reward = settings.getReward(key);
        } else if (killed instanceof Monster || isHostileOverride(killed)) {
            key = "hostile-kill";
            reward = settings.getReward(key);
        } else {
            return;
        }

        reward(player, profile, reward, "for combat victories", key, settings);
        profile.incrementCounter(key);
    }

    public void handleCropBreak(Player player, Block block) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.FARMER) {
            return;
        }
        if (!isMatureCrop(block) || isPlayerPlaced(block.getLocation())) {
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.FARMER);
        reward(player, profile, settings.getReward("harvest"), "for harvesting crops", "harvest", settings);
        profile.incrementCounter("crops");
    }

    public void handleFish(Player player, PlayerFishEvent event) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.FISHERMAN) {
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!profile.recentlyMoved()) {
            // basic AFK prevention
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.FISHERMAN);
        reward(player, profile, settings.getReward("catch"), "for catching fish", "catch", settings);
        profile.incrementCounter("catches");
    }

    public void handleLogBreak(Player player, Block block) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.LUMBERJACK) {
            return;
        }
        if (!isLog(block.getType()) || isPlayerPlaced(block.getLocation())) {
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.LUMBERJACK);
        reward(player, profile, settings.getReward("tree"), "for felling trees", "tree", settings);
        profile.incrementCounter("trees");
    }

    public void handleOreBreak(Player player, Block block) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.MINER) {
            return;
        }
        if (!isOre(block.getType()) || isPlayerPlaced(block.getLocation())) {
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.MINER);
        double reward = settings.getReward("ore");
        if (block.getLocation().getY() < settings.getDepthBonusThreshold()) {
            reward += settings.getReward("depth-bonus");
        }
        reward(player, profile, reward, "for mining ores", "ore", settings);
        profile.incrementCounter("ores");
    }

    public void handleTravel(Player player, Location from, Location to) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.EXPLORER) {
            return;
        }
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        profile.markActivity();
        if (from.getChunk().equals(to.getChunk())) {
            return;
        }
        OccupationSettings settings = occupationSettings.get(Occupation.EXPLORER);
        long now = System.currentTimeMillis();
        if (!profile.canRewardLocation(to.getChunk(), settings.getCooldown("chunk"), MAX_RECENT_LOCATIONS)) {
            return;
        }
        double reward = settings.getReward("chunk");
        reward(player, profile, reward, "for exploring new territory", "chunk", settings);
        profile.incrementCounter("chunks");
        profile.setLastLocationTime(now);
    }

    public void handleBuild(Player player, Block placed) {
        OccupationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.getOccupation() != Occupation.BUILDER) {
            return;
        }
        profile.addPlacedBlock(placed.getLocation());
        OccupationSettings settings = occupationSettings.get(Occupation.BUILDER);
        profile.recordBuildSession(placed.getType());
        if (profile.readyForBuildReward(settings.getSessionBlockThreshold(), settings.getSessionUniqueThreshold())) {
            reward(player, profile, settings.getReward("build-session"), "for sustained building", "build-session",
                    settings);
            profile.resetBuildSession();
        }
    }

    public void trackPlacement(UUID uuid, Block block) {
        OccupationProfile profile = profiles.get(uuid);
        if (profile == null) {
            return;
        }
        profile.addPlacedBlock(block.getLocation());
        placedBlockRegistry.add(serializeLocation(block.getLocation()));
    }

    public void backupOffers(Player player) {
        player.sendMessage(Util.color("&eUse the jobs menu to pick an occupation."));
    }

    public void save() {
        playerConfig.set("players", null);
        for (Map.Entry<UUID, OccupationProfile> entry : profiles.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            OccupationProfile profile = entry.getValue();
            playerConfig.set(base + "occupation", profile.getOccupation().name());
            playerConfig.set(base + "chosenAt", profile.getChosenAt());
            playerConfig.set(base + "lifetimeEarnings", profile.getLifetimeEarnings());
            playerConfig.set(base + "sessionEarnings", profile.getSessionEarnings());
            playerConfig.set(base + "lastPayoutAt", profile.getLastPayoutAt());
            playerConfig.set(base + "dailyEarnings", profile.getDailyEarnings());
            playerConfig.set(base + "dailyResetAt", profile.getDailyResetAt());
            playerConfig.set(base + "placed", new ArrayList<>(profile.getPlacedBlocks()));
            playerConfig.set(base + "recentVictims", serializeVictims(profile.getRecentVictims()));
            playerConfig.set(base + "recentLocations", new ArrayList<>(profile.getRecentLocations()));
            playerConfig.set(base + "counters", profile.getCounters());
            playerConfig.set(base + "bests", profile.getBests());
            playerConfig.set(base + "streaks", profile.getStreaks());
            playerConfig.set(base + "recentPayouts", serializePayouts(profile.getRecentPayouts()));
        }
        try {
            playerConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save jobs-data.yml: " + e.getMessage());
        }
    }

    private List<String> serializeVictims(Map<UUID, Long> victims) {
        List<String> serialized = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : victims.entrySet()) {
            serialized.add(entry.getKey() + ":" + entry.getValue());
        }
        return serialized;
    }

    private List<String> serializePayouts(Deque<PayoutRecord> payouts) {
        List<String> serialized = new ArrayList<>();
        for (PayoutRecord record : payouts) {
            serialized.add(record.reason() + "|" + record.amount() + "|" + record.timestamp());
        }
        return serialized;
    }

    private Map<UUID, Long> deserializeVictims(List<String> values) {
        Map<UUID, Long> victims = new HashMap<>();
        for (String value : values) {
            String[] parts = value.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                victims.put(UUID.fromString(parts[0]), Long.parseLong(parts[1]));
            } catch (Exception ignored) {
            }
        }
        return victims;
    }

    private Deque<PayoutRecord> deserializePayouts(List<String> values) {
        Deque<PayoutRecord> payouts = new ArrayDeque<>();
        for (String value : values) {
            String[] parts = value.split("\\|");
            if (parts.length != 3) {
                continue;
            }
            try {
                payouts.add(new PayoutRecord(parts[0], Double.parseDouble(parts[1]), Long.parseLong(parts[2])));
            } catch (Exception ignored) {
            }
        }
        return payouts;
    }

    private void reward(Player player, OccupationProfile profile, double amount, String reason, String key,
            OccupationSettings settings) {
        if (amount <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        profile.refreshDaily(now);
        if (!profile.canCollect(key, settings.getCooldown(key), now)) {
            return;
        }
        double available = Math.max(0, settings.getDailyCap() - profile.getDailyEarnings());
        if (available <= 0) {
            return;
        }
        double payout = Math.min(amount, available);
        BankManager bank = BankManager.get();
        if (bank != null) {
            bank.deposit(player.getUniqueId(), payout, "Occupation income: " + reason);
            player.sendMessage(Util.color("&a+" + bank.formatCurrency(payout) + " &7" + reason + "."));
        } else {
            player.sendMessage(Util.color("&a+" + payout + " &7" + reason + "."));
        }
        profile.bookkeepPayout(payout, reason, now, MAX_RECENT_PAYOUTS);
    }

    private void createDefaults() {
        boolean createdJobs = false;
        if (!jobFile.exists()) {
            plugin.saveResource("jobs.yml", false);
            createdJobs = true;
        }
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Unable to create jobs-data.yml: " + e.getMessage());
            }
        }

        if (createdJobs) {
            try {
                jobConfig.load(jobFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Unable to load default jobs.yml: " + e.getMessage());
            }
        }
    }

    private void loadSettings() {
        occupationSettings.clear();
        for (Occupation occupation : Occupation.values()) {
            String path = "occupations." + occupation.name() + ".";
            String display = jobConfig.getString(path + "display-name", occupation.getDisplayName());
            List<String> description = jobConfig.getStringList(path + "description");
            if (description.isEmpty()) {
                description = occupation.getDefaultDescription();
            }
            double dailyCap = jobConfig.getDouble(path + "daily-cap", 5000.0);
            Map<String, Double> rewards = new HashMap<>();
            ConfigurationSection rewardSection = jobConfig.getConfigurationSection(path + "rewards");
            if (rewardSection != null) {
                for (String key : rewardSection.getKeys(false)) {
                    rewards.put(key, rewardSection.getDouble(key));
                }
            }
            Map<String, Integer> cooldowns = new HashMap<>();
            ConfigurationSection cooldownSection = jobConfig.getConfigurationSection(path + "cooldowns");
            if (cooldownSection != null) {
                for (String key : cooldownSection.getKeys(false)) {
                    cooldowns.put(key, cooldownSection.getInt(key));
                }
            }
            int depthThreshold = jobConfig.getInt(path + "depth-threshold", 20);
            int sessionBlockThreshold = jobConfig.getInt(path + "build-session.block-threshold", 25);
            int sessionUniqueThreshold = jobConfig.getInt(path + "build-session.unique-threshold", 5);
            occupationSettings.put(occupation, new OccupationSettings(display, description, occupation.getIcon(),
                    dailyCap, rewards, cooldowns, depthThreshold, sessionBlockThreshold, sessionUniqueThreshold));
        }
    }

    private void loadProfiles() {
        profiles.clear();
        if (!playerConfig.isConfigurationSection("players")) {
            return;
        }
        for (String key : playerConfig.getConfigurationSection("players").getKeys(false)) {
            Occupation occupation = Occupation.fromString(playerConfig.getString("players." + key + ".occupation"));
            if (occupation == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                long chosenAt = playerConfig.getLong("players." + key + ".chosenAt", System.currentTimeMillis());
                OccupationProfile profile = new OccupationProfile(occupation, chosenAt);
                profile.setLifetimeEarnings(playerConfig.getDouble("players." + key + ".lifetimeEarnings", 0));
                profile.setSessionEarnings(0); // session earnings reset each startup
                profile.setLastPayoutAt(playerConfig.getLong("players." + key + ".lastPayoutAt", 0));
                profile.setDailyEarnings(playerConfig.getDouble("players." + key + ".dailyEarnings", 0));
                profile.setDailyResetAt(playerConfig.getLong("players." + key + ".dailyResetAt", 0));
                profile.setPlacedBlocks(new HashSet<>(playerConfig.getStringList("players." + key + ".placed")));
                placedBlockRegistry.addAll(profile.getPlacedBlocks());
                profile.setRecentVictims(deserializeVictims(playerConfig.getStringList("players." + key + ".recentVictims")));
                profile.setRecentLocations(new ArrayDeque<>(playerConfig.getStringList("players." + key + ".recentLocations")));
                ConfigurationSection counterSec = playerConfig.getConfigurationSection("players." + key + ".counters");
                if (counterSec != null) {
                    for (String cKey : counterSec.getKeys(false)) {
                        profile.getCounters().put(cKey, counterSec.getInt(cKey));
                    }
                }
                ConfigurationSection bestSec = playerConfig.getConfigurationSection("players." + key + ".bests");
                if (bestSec != null) {
                    for (String cKey : bestSec.getKeys(false)) {
                        profile.getBests().put(cKey, bestSec.getInt(cKey));
                    }
                }
                ConfigurationSection streakSec = playerConfig.getConfigurationSection("players." + key + ".streaks");
                if (streakSec != null) {
                    for (String cKey : streakSec.getKeys(false)) {
                        profile.getStreaks().put(cKey, streakSec.getInt(cKey));
                    }
                }
                profile.setRecentPayouts(deserializePayouts(playerConfig.getStringList("players." + key + ".recentPayouts")));
                profiles.put(uuid, profile);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private boolean isMatureCrop(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return switch (type) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, SWEET_BERRY_BUSH, COCOA ->
                    ageable.getAge() >= ageable.getMaximumAge();
            default -> false;
        };
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM");
    }

    private boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS") || name.equals("NETHER_QUARTZ_ORE");
    }

    private boolean isHostileOverride(Entity entity) {
        return switch (entity.getType()) {
            case PHANTOM, SLIME, MAGMA_CUBE, GHAST, ENDERMAN, PIGLIN, PIGLIN_BRUTE, HOGLIN, ZOGLIN -> true;
            default -> false;
        };
    }

    private boolean isPlayerPlaced(Location location) {
        return placedBlockRegistry.contains(serializeLocation(location));
    }

    public record OccupationSettings(String displayName, List<String> description, Material icon, double dailyCap,
            Map<String, Double> rewards, Map<String, Integer> cooldowns, int depthBonusThreshold,
            int sessionBlockThreshold, int sessionUniqueThreshold) {

        public double getReward(String key) {
            return rewards.getOrDefault(key, 0.0);
        }

        public int getCooldown(String key) {
            return cooldowns.getOrDefault(key, cooldowns.getOrDefault("default", 0));
        }

        public int getDepthBonusThreshold() {
            return depthBonusThreshold;
        }

        public int getSessionBlockThreshold() {
            return sessionBlockThreshold;
        }

        public int getSessionUniqueThreshold() {
            return sessionUniqueThreshold;
        }
    }

    public record PayoutRecord(String reason, double amount, long timestamp) {
    }

    public static final class OccupationProfile {
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
            return placedBlocks.contains(serializeLocation(location));
        }

        public void addPlacedBlock(Location location) {
            placedBlocks.add(serializeLocation(location));
        }

        public void setPlacedBlocks(Set<String> placedBlocks) {
            this.placedBlocks.clear();
            this.placedBlocks.addAll(placedBlocks);
        }

        public boolean canRewardLocation(Chunk chunk, int cooldownSeconds, int limit) {
            String key = chunkKey(chunk);
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

    private boolean allowNewVictim(OccupationProfile profile, UUID victim, int cooldownSeconds) {
        return profile.allowVictim(victim, cooldownSeconds);
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }

    private static String chunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}
