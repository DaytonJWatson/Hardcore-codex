package com.daytonjwatson.hardcore.jobs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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

    private static JobsManager instance;

    private final JavaPlugin plugin;
    private final File jobFile;
    private final File playerDataFile;
    private final FileConfiguration jobConfig;
    private final FileConfiguration playerConfig;
    private final Map<Occupation, OccupationSettings> occupationSettings = new EnumMap<>(Occupation.class);
    private final Map<UUID, OccupationProgress> progressMap = new HashMap<>();

    private JobsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.jobFile = new File(plugin.getDataFolder(), "jobs.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "jobs-data.yml");
        this.jobConfig = YamlConfiguration.loadConfiguration(jobFile);
        this.playerConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        createDefaults();
        loadSettings();
        loadProgress();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new JobsManager(plugin);
        }
    }

    public static JobsManager get() {
        return instance;
    }

    public void reload() {
        try {
            jobConfig.load(jobFile);
            loadSettings();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to reload jobs.yml: " + ex.getMessage());
        }
    }

    public Occupation getOccupation(UUID uuid) {
        OccupationProgress progress = progressMap.get(uuid);
        return progress == null ? null : progress.occupation();
    }

    public void setOccupation(Player player, Occupation occupation) {
        progressMap.put(player.getUniqueId(), new OccupationProgress(occupation, new HashSet<>()));
        save();
        MessageStyler.sendPanel(player, "Occupation Selected",
                "&7You are now a &f" + occupation.getDisplayName() + "&7.");
    }

    public void clearOccupation(Player player) {
        progressMap.remove(player.getUniqueId());
        save();
        player.sendMessage(Util.color("&cYou cleared your occupation. Use /jobs to pick a new one."));
    }

    public Map<Occupation, OccupationSettings> getOccupationSettings() {
        return occupationSettings;
    }

    public void handleKill(Player player, Entity killed) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.WARRIOR) {
            return;
        }
        if (!(killed instanceof Monster) && !isHostileOverride(killed)) {
            return;
        }

        double reward = occupationSettings.get(Occupation.WARRIOR).killHostileReward();
        reward(player, reward, "for slaying a hostile mob");
    }

    public void handleCropBreak(Player player, Block block) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.FARMER) {
            return;
        }
        if (!isMatureCrop(block)) {
            return;
        }
        double reward = occupationSettings.get(Occupation.FARMER).harvestReward();
        reward(player, reward, "for harvesting crops");
    }

    public void handleFish(Player player, PlayerFishEvent event) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.FISHERMAN) {
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        double reward = occupationSettings.get(Occupation.FISHERMAN).catchReward();
        reward(player, reward, "for catching a fish");
    }

    public void handleLogBreak(Player player, Material material) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.LUMBERJACK) {
            return;
        }
        if (!isLog(material)) {
            return;
        }
        double reward = occupationSettings.get(Occupation.LUMBERJACK).logReward();
        reward(player, reward, "for chopping logs");
    }

    public void handleOreBreak(Player player, Material material) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.MINER) {
            return;
        }
        if (!isOre(material)) {
            return;
        }
        double reward = occupationSettings.get(Occupation.MINER).oreReward();
        reward(player, reward, "for mining valuable blocks");
    }

    public void handleTravel(Player player, Location from, Location to) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.EXPLORER) {
            return;
        }
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        double distance = from.distance(to);
        if (distance <= 0) {
            return;
        }

        OccupationSettings settings = occupationSettings.get(Occupation.EXPLORER);
        double rewardPerBlock = settings.travelRewardPerBlock();
        double payout = distance * rewardPerBlock;
        reward(player, payout, "for exploring the world");
    }

    public void handleBuild(Player player, Material material) {
        OccupationProgress progress = progressMap.get(player.getUniqueId());
        if (progress == null || progress.occupation() != Occupation.BUILDER) {
            return;
        }
        if (material == Material.AIR) {
            return;
        }
        Set<Material> seen = new HashSet<>(progress.uniqueBlocks());
        if (!seen.add(material)) {
            return;
        }
        progressMap.put(player.getUniqueId(), new OccupationProgress(progress.occupation(), seen));
        double reward = occupationSettings.get(Occupation.BUILDER).uniqueBlockReward();
        reward(player, reward, "for placing a new block type");
        save();
    }

    public void backupOffers(Player player) {
        player.sendMessage(Util.color("&eUse the jobs menu to pick an occupation."));
    }

    public void save() {
        playerConfig.set("players", null);
        for (Map.Entry<UUID, OccupationProgress> entry : progressMap.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            OccupationProgress progress = entry.getValue();
            playerConfig.set(base + "occupation", progress.occupation().name());
            List<String> placed = new ArrayList<>();
            for (Material material : progress.uniqueBlocks()) {
                placed.add(material.name());
            }
            playerConfig.set(base + "unique-blocks", placed);
        }
        try {
            playerConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save jobs-data.yml: " + e.getMessage());
        }
    }

    private void reward(Player player, double amount, String reason) {
        if (amount <= 0) {
            return;
        }
        BankManager bank = BankManager.get();
        if (bank != null) {
            bank.deposit(player.getUniqueId(), amount, "Occupation income: " + reason);
            player.sendMessage(Util.color("&a+" + bank.formatCurrency(amount) + " &7" + reason + "."));
        } else {
            player.sendMessage(Util.color("&a+" + amount + " &7" + reason + "."));
        }
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
            double killReward = jobConfig.getDouble(path + "rewards.kill-hostile", 20.0);
            double harvestReward = jobConfig.getDouble(path + "rewards.harvest-crop", 4.0);
            double catchReward = jobConfig.getDouble(path + "rewards.catch-fish", 6.0);
            double logReward = jobConfig.getDouble(path + "rewards.chop-log", 3.0);
            double oreReward = jobConfig.getDouble(path + "rewards.mine-ore", 5.0);
            double travelReward = jobConfig.getDouble(path + "rewards.travel-per-block", 0.1);
            double uniqueReward = jobConfig.getDouble(path + "rewards.unique-block", 2.5);
            occupationSettings.put(occupation,
                    new OccupationSettings(display, description, occupation.getIcon(), killReward, harvestReward,
                            catchReward, logReward, oreReward, travelReward, uniqueReward));
        }
    }

    private void loadProgress() {
        progressMap.clear();
        if (!playerConfig.isConfigurationSection("players")) {
            return;
        }
        for (String key : playerConfig.getConfigurationSection("players").getKeys(false)) {
            Occupation occupation = Occupation.fromString(playerConfig.getString("players." + key + ".occupation"));
            if (occupation == null) {
                continue;
            }
            List<String> placed = playerConfig.getStringList("players." + key + ".unique-blocks");
            Set<Material> unique = new HashSet<>();
            for (String raw : placed) {
                try {
                    unique.add(Material.valueOf(raw.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            try {
                UUID uuid = UUID.fromString(key);
                progressMap.put(uuid, new OccupationProgress(occupation, unique));
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

    public record OccupationSettings(String displayName, List<String> description, Material icon,
            double killHostileReward, double harvestReward, double catchReward, double logReward, double oreReward,
            double travelRewardPerBlock, double uniqueBlockReward) {
    }

    public record OccupationProgress(Occupation occupation, Set<Material> uniqueBlocks) {
    }
}
