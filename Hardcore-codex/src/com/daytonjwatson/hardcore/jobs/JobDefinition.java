package com.daytonjwatson.hardcore.jobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.block.Biome;

public class JobDefinition {

    private final String id;
    private final String displayName;
    private final JobType type;
    private final String target;
    private final int minAmount;
    private final int maxAmount;
    private final double difficulty;
    private final double reward;
    private final String[] description;
    private final boolean consumeItems;

    public JobDefinition(String id, String displayName, JobType type, String target, int minAmount, int maxAmount, double difficulty,
                         double reward, boolean consumeItems, String[] description) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.target = target;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.difficulty = difficulty;
        this.reward = reward;
        this.consumeItems = consumeItems;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JobType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public int rollAmount(java.util.Random random) {
        if (maxAmount <= minAmount) {
            return minAmount;
        }
        return random.nextInt((maxAmount - minAmount) + 1) + minAmount;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public double getReward() {
        return reward;
    }

    public String[] getDescription() {
        return description;
    }

    public boolean shouldConsumeItems() {
        return consumeItems;
    }

    public boolean matches(EntityType entityType) {
        return type == JobType.KILL_MOB && entityType.name().equalsIgnoreCase(target);
    }

    public boolean matches(Material material) {
        return type == JobType.COLLECT_ITEM && material.name().equalsIgnoreCase(target);
    }

    public boolean matchesMine(Material material) {
        return type == JobType.MINE_BLOCK && material.name().equalsIgnoreCase(target);
    }

    public boolean matchesFish(Material material) {
        return type == JobType.FISH_ITEM && material.name().equalsIgnoreCase(target);
    }

    public boolean matchesCraft(Material material) {
        return type == JobType.CRAFT_ITEM && material.name().equalsIgnoreCase(target);
    }

    public boolean matchesBiome(Biome biome) {
        return type == JobType.TRAVEL_BIOME && biome.name().equalsIgnoreCase(target);
    }
}
