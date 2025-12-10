package com.daytonjwatson.hardcore.jobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class JobDefinition {

    private final String id;
    private final String displayName;
    private final JobType type;
    private final String target;
    private final int amount;
    private final double difficulty;
    private final double reward;
    private final String[] description;

    public JobDefinition(String id, String displayName, JobType type, String target, int amount, double difficulty,
                         double reward, String[] description) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.difficulty = difficulty;
        this.reward = reward;
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

    public int getAmount() {
        return amount;
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

    public boolean matches(EntityType entityType) {
        return type == JobType.KILL_MOB && entityType.name().equalsIgnoreCase(target);
    }

    public boolean matches(Material material) {
        return type == JobType.COLLECT_ITEM && material.name().equalsIgnoreCase(target);
    }
}
