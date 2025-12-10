package com.daytonjwatson.hardcore.jobs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;

public class JobsManager {

    private static final double DEFAULT_REWARD_MULTIPLIER = 125.0;
    private static final long SLOT_COOLDOWN_MILLIS = 15 * 60 * 1000L;

    private static JobsManager instance;

    private final JavaPlugin plugin;
    private final File jobFile;
    private final File playerDataFile;
    private final FileConfiguration jobConfig;
    private final FileConfiguration playerConfig;

    private final Map<String, JobDefinition> jobPool = new LinkedHashMap<>();
    private final Map<UUID, ActiveJob> activeJobs = new HashMap<>();
    private final Map<UUID, List<JobOffer>> offeredJobs = new HashMap<>();
    private final Map<UUID, Map<Integer, Long>> slotCooldowns = new HashMap<>();
    private final Random random = new Random();

    private JobsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.jobFile = new File(plugin.getDataFolder(), "jobs.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "jobs-data.yml");
        this.jobConfig = YamlConfiguration.loadConfiguration(jobFile);
        this.playerConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        createDefaults();
        loadJobs();
        loadActiveJobs();
        loadCooldowns();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new JobsManager(plugin);
        }
    }

    public static JobsManager get() {
        return instance;
    }

    public List<JobDefinition> getJobPool() {
        return List.copyOf(jobPool.values());
    }

    public List<JobOffer> getOfferedJobs(UUID playerId) {
        refreshOffers(playerId);
        return new ArrayList<>(offeredJobs.get(playerId));
    }

    public void rerollOffers(UUID playerId) {
        List<JobOffer> offers = offeredJobs.computeIfAbsent(playerId,
                ignored -> new ArrayList<>(Collections.nCopies(3, null)));
        ensureOfferSize(offers);
        for (int i = 0; i < offers.size(); i++) {
            if (isSlotCoolingDown(playerId, i)) {
                offers.set(i, null);
            } else {
                offers.set(i, pickRandomOffer());
            }
        }
    }

    public ActiveJob getActiveJob(UUID uuid) {
        return activeJobs.get(uuid);
    }

    public void assignJob(Player player, JobOffer offer, int slotIndex) {
        JobDefinition definition = offer.getDefinition();
        Location start = definition.getType() == JobType.TRAVEL_DISTANCE ? player.getLocation().clone() : null;
        ActiveJob active = new ActiveJob(definition, offer.getAmount(), 0, start, slotIndex);
        activeJobs.put(player.getUniqueId(), active);
        startCooldownForSlot(player.getUniqueId(), slotIndex);
        offeredJobs.remove(player.getUniqueId());
        saveActiveJobs();
        player.sendMessage(Util.color("&aAccepted job '&f" + definition.getDisplayName() + "&a'. Good luck!"));
        player.sendMessage(Util.color("&7Goal: &f" + offer.getAmount() + "x " + definition.getTarget() + " (&e"
                + definition.getType().name().replace('_', ' ') + "&7)."));
    }

    public void abandonJob(Player player) {
        ActiveJob active = activeJobs.remove(player.getUniqueId());
        if (active != null) {
            saveActiveJobs();
        }
        player.sendMessage(Util.color("&eYou abandoned your active job."));
    }

    public void handleKill(Player player, EntityType type) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null || !active.getJob().matches(type)) {
            return;
        }
        incrementProgress(player, active, 1);
    }

    public void handleCollection(Player player, Material material, int amount) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null || !active.getJob().matches(material)) {
            return;
        }
        int counted = amount;
        if (active.getJob().shouldConsumeItems()) {
            counted = consumeItems(player, material, amount);
        }
        if (counted <= 0) {
            return;
        }
        incrementProgress(player, active, counted);
    }

    public void handleMine(Player player, Material material, int amount) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null || !active.getJob().matchesMine(material)) {
            return;
        }
        incrementProgress(player, active, amount);
    }

    public void handleFish(Player player, Material material, int amount) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null || !active.getJob().matchesFish(material)) {
            return;
        }
        int counted = amount;
        if (active.getJob().shouldConsumeItems()) {
            counted = consumeItems(player, material, amount);
        }
        if (counted <= 0) {
            return;
        }
        incrementProgress(player, active, counted);
    }

    public void handleCraft(Player player, Material material, int amount) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null || !active.getJob().matchesCraft(material)) {
            return;
        }
        int counted = amount;
        if (active.getJob().shouldConsumeItems()) {
            counted = consumeItems(player, material, amount);
        }
        if (counted <= 0) {
            return;
        }
        incrementProgress(player, active, counted);
    }

    public void handleTravel(Player player, Location from, Location to) {
        ActiveJob active = activeJobs.get(player.getUniqueId());
        if (active == null) {
            return;
        }

        JobDefinition job = active.getJob();
        switch (job.getType()) {
            case TRAVEL_BIOME -> {
                Biome biome = to.getBlock().getBiome();
                if (job.matchesBiome(biome)) {
                    setProgressIfHigher(player, active, active.getGoalAmount());
                }
            }
            case TRAVEL_DISTANCE -> {
                Location start = active.getStartLocation();
                if (start == null) {
                    start = from.clone();
                    active.setStartLocation(start);
                }

                if (start.getWorld() == null || to.getWorld() == null || !start.getWorld().equals(to.getWorld())) {
                    return;
                }

                double distance = start.distance(to);
                setProgressIfHigher(player, active, distance);
            }
            default -> {
            }
        }
    }

    private void incrementProgress(Player player, ActiveJob active, double amount) {
        double previous = active.getProgress();
        double updated = active.addProgress(amount);
        if (updated == previous) {
            return;
        }

        JobDefinition job = active.getJob();
        if (active.isComplete()) {
            reward(player, job);
            finalizeJob(player, active);
            return;
        }
        player.sendMessage(Util.color("&6Job Progress &8» &7" + formatNumber(updated) + "/" +
                formatNumber(active.getGoalAmount()) + " completed."));
        saveActiveJobs();
    }

    private void setProgressIfHigher(Player player, ActiveJob active, double newProgress) {
        double previous = active.getProgress();
        double clamped = Math.min(active.getGoalAmount(), newProgress);
        if (clamped <= previous) {
            return;
        }
        active.setProgress(clamped);

        JobDefinition job = active.getJob();
        if (active.isComplete()) {
            reward(player, job);
            finalizeJob(player, active);
            return;
        }
        boolean isTravelDistance = job.getType() == JobType.TRAVEL_DISTANCE;
        if (!isTravelDistance || reachedNextQuarter(previous, clamped, active.getGoalAmount())) {
            player.sendMessage(Util.color("&6Job Progress &8» &7" + formatNumber(clamped) + "/" +
                    formatNumber(active.getGoalAmount()) + " completed."));
        }
        saveActiveJobs();
    }

    private boolean reachedNextQuarter(double previous, double current, double goal) {
        double divisor = goal <= 0 ? 1 : goal;
        int previousQuarter = (int) Math.floor((previous / divisor) * 4);
        int currentQuarter = (int) Math.floor((current / divisor) * 4);
        return currentQuarter > previousQuarter;
    }

    private void reward(Player player, JobDefinition job) {
        double rewardAmount = job.getReward();
        BankManager bank = BankManager.get();
        String formatted = rewardAmount + "";
        if (bank != null) {
            bank.deposit(player.getUniqueId(), rewardAmount,
                    "Completed job: " + job.getDisplayName());
            formatted = bank.formatCurrency(rewardAmount);
        }

        MessageStyler.sendPanel(player, "Job Complete",
                "&7" + job.getDisplayName(),
                "&7Reward: &a" + formatted);
    }

    private void finalizeJob(Player player, ActiveJob active) {
        activeJobs.remove(player.getUniqueId());
        saveActiveJobs();
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

    private void loadJobs() {
        jobPool.clear();
        ConfigurationSection section = jobConfig.getConfigurationSection("jobs");
        if (section == null) {
            plugin.getLogger().warning("No jobs found in jobs.yml.");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(id);
            if (node == null) continue;

            JobType type = JobType.fromString(node.getString("type"));
            if (type == null) {
                plugin.getLogger().warning("Unknown job type for job '" + id + "'. Skipping.");
                continue;
            }

            String target = node.getString("target", "").toUpperCase(Locale.ROOT);
            int minAmount = Math.max(1, node.getInt("amount-min", node.getInt("amount", 1)));
            int maxAmount = Math.max(minAmount, node.getInt("amount-max", minAmount));
            double difficulty = Math.max(0.1, node.getDouble("difficulty", 1.0));
            double reward = node.getDouble("reward", difficulty * DEFAULT_REWARD_MULTIPLIER);
            String name = node.getString("name", id);
            List<String> lore = node.getStringList("description");
            String[] description = lore.toArray(new String[0]);
            boolean consumeItems = node.getBoolean("consume-items", false);

            if (!isValidTarget(type, target)) {
                plugin.getLogger().warning("Invalid target '" + target + "' for job '" + id + "'. Skipping.");
                continue;
            }

            JobDefinition definition = new JobDefinition(id, name, type, target, minAmount, maxAmount, difficulty, reward,
                    consumeItems, description);
            jobPool.put(id, definition);
        }
    }

    private void loadActiveJobs() {
        activeJobs.clear();
        ConfigurationSection section = playerConfig.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String jobId = playerConfig.getString("players." + key + ".job");
                double progress = playerConfig.getDouble("players." + key + ".progress", 0);
                int goal = playerConfig.getInt("players." + key + ".goal", -1);
                Location start = readLocation(playerConfig, "players." + key + ".start");
                int slot = playerConfig.getInt("players." + key + ".slot", -1);
                JobDefinition definition = jobPool.get(jobId);
                if (definition != null) {
                    if (goal < 0) {
                        goal = Math.max(definition.getMinAmount(), definition.getMaxAmount());
                    }
                    activeJobs.put(uuid, new ActiveJob(definition, goal, progress, start, slot));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveActiveJobs() {
        playerConfig.set("players", null);
        for (Map.Entry<UUID, ActiveJob> entry : activeJobs.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            ActiveJob job = entry.getValue();
            playerConfig.set(base + "job", job.getJob().getId());
            playerConfig.set(base + "progress", job.getProgress());
            playerConfig.set(base + "goal", job.getGoalAmount());
            playerConfig.set(base + "slot", job.getSelectedSlot());
            writeLocation(job.getStartLocation(), base + "start");
        }
        writeCooldowns();
        try {
            playerConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save jobs-data.yml: " + e.getMessage());
        }
    }

    private boolean isValidTarget(JobType type, String target) {
        try {
            return switch (type) {
                case KILL_MOB -> EntityType.valueOf(target) != null;
                case COLLECT_ITEM, MINE_BLOCK, FISH_ITEM, CRAFT_ITEM -> Material.valueOf(target) != null;
                case TRAVEL_BIOME -> Biome.valueOf(target) != null;
                case TRAVEL_DISTANCE -> true;
            };
        } catch (Exception ex) {
            return false;
        }
    }

    public void reload() {
        try {
            jobConfig.load(jobFile);
            loadJobs();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reload jobs.yml: " + e.getMessage());
        }
    }

    public void clearOffers(UUID uuid) {
        offeredJobs.remove(uuid);
    }

    public void completeTrackingFor(UUID uuid) {
        activeJobs.remove(uuid);
        saveActiveJobs();
    }

    public void save() {
        saveActiveJobs();
    }

    public void backupOffers(Player player) {
        List<JobOffer> offers = getOfferedJobs(player.getUniqueId());
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            JobOffer offer = offers.get(i);
            if (isSlotCoolingDown(player.getUniqueId(), i)) {
                long seconds = getCooldownRemainingMillis(player.getUniqueId(), i) / 1000;
                lines.add(MessageStyler.bulletLine("Option " + (i + 1), org.bukkit.ChatColor.GRAY,
                        "Cooling down (" + seconds + "s remaining)"));
                continue;
            }
            if (offer == null) {
                lines.add(MessageStyler.bulletLine("Option " + (i + 1), org.bukkit.ChatColor.RED,
                        "Unavailable"));
                continue;
            }
            JobDefinition job = offer.getDefinition();
            lines.add(MessageStyler.bulletLine("Option " + (i + 1), org.bukkit.ChatColor.GOLD,
                    job.getDisplayName() + " &7(" + job.getTarget() + " x" + offer.getAmount() + ")"));
        }
        MessageStyler.sendPanel(player, "Job Offers", lines.toArray(new String[0]));
    }

    public boolean isSlotCoolingDown(UUID playerId, int slotIndex) {
        Long until = getCooldownMap(playerId).get(slotIndex);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            getCooldownMap(playerId).remove(slotIndex);
            return false;
        }
        return true;
    }

    public long getCooldownRemainingMillis(UUID playerId, int slotIndex) {
        Long until = getCooldownMap(playerId).get(slotIndex);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    private void refreshOffers(UUID playerId) {
        List<JobOffer> offers = offeredJobs.computeIfAbsent(playerId,
                ignored -> new ArrayList<>(Collections.nCopies(3, null)));
        ensureOfferSize(offers);

        for (int i = 0; i < offers.size(); i++) {
            if (isSlotCoolingDown(playerId, i)) {
                offers.set(i, null);
            } else if (offers.get(i) == null) {
                offers.set(i, pickRandomOffer());
            }
        }
    }

    private JobOffer pickRandomOffer() {
        if (jobPool.isEmpty()) {
            return null;
        }
        List<JobDefinition> pool = new ArrayList<>(jobPool.values());
        Collections.shuffle(pool, random);
        JobDefinition def = pool.get(0);
        return new JobOffer(def, def.rollAmount(random));
    }

    private void ensureOfferSize(List<JobOffer> offers) {
        while (offers.size() < 3) {
            offers.add(null);
        }
    }

    private void startCooldownForSlot(UUID uuid, int slotIndex) {
        if (slotIndex < 0) {
            return;
        }
        getCooldownMap(uuid).put(slotIndex, System.currentTimeMillis() + SLOT_COOLDOWN_MILLIS);
    }

    private Map<Integer, Long> getCooldownMap(UUID uuid) {
        return slotCooldowns.computeIfAbsent(uuid, ignored -> new HashMap<>());
    }

    private void loadCooldowns() {
        slotCooldowns.clear();
        ConfigurationSection section = playerConfig.getConfigurationSection("cooldowns");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) {
                    continue;
                }
                Map<Integer, Long> slots = getCooldownMap(uuid);
                for (String slotKey : child.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotKey);
                        long until = child.getLong(slotKey);
                        if (until > System.currentTimeMillis()) {
                            slots.put(slot, until);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void writeCooldowns() {
        playerConfig.set("cooldowns", null);
        for (Map.Entry<UUID, Map<Integer, Long>> entry : slotCooldowns.entrySet()) {
            String base = "cooldowns." + entry.getKey() + ".";
            for (Map.Entry<Integer, Long> slot : entry.getValue().entrySet()) {
                if (slot.getValue() > System.currentTimeMillis()) {
                    playerConfig.set(base + slot.getKey(), slot.getValue());
                }
            }
        }
    }

    private void writeLocation(Location location, String path) {
        if (location == null) {
            playerConfig.set(path, null);
            return;
        }
        playerConfig.set(path + ".world", location.getWorld().getName());
        playerConfig.set(path + ".x", location.getX());
        playerConfig.set(path + ".y", location.getY());
        playerConfig.set(path + ".z", location.getZ());
    }

    private Location readLocation(FileConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    private String formatNumber(double value) {
        if (value % 1 == 0) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private int consumeItems(Player player, Material material, int requested) {
        int remaining = Math.max(0, requested);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
        return requested - remaining;
    }
}
