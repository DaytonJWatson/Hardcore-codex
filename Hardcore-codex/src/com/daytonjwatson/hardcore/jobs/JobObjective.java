package com.daytonjwatson.hardcore.jobs;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

public class JobObjective {

    private final JobType type;
    private final String target;
    private final int minAmount;
    private final int maxAmount;
    private final boolean consumeItems;

    public JobObjective(JobType type, String target, int minAmount, int maxAmount, boolean consumeItems) {
        this.type = type;
        this.target = target.toUpperCase(Locale.ROOT);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.consumeItems = consumeItems;
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

    public boolean shouldConsumeItems() {
        return consumeItems;
    }

    public boolean matches(EntityType entityType) {
        return switch (type) {
            case KILL_MOB -> entityType.name().equalsIgnoreCase(target);
            case BREED_ANIMAL -> entityType.name().equalsIgnoreCase(target);
            case TAME_ENTITY -> entityType.name().equalsIgnoreCase(target);
            default -> false;
        };
    }

    public boolean matches(Material material) {
        return switch (type) {
            case COLLECT_ITEM, MINE_BLOCK, FISH_ITEM, CRAFT_ITEM, PLACE_BLOCK, SMELT_ITEM, ENCHANT_ITEM ->
                    material.name().equalsIgnoreCase(target);
            default -> false;
        };
    }

    public boolean matchesBiome(Biome biome) {
        return type == JobType.TRAVEL_BIOME && biome.name().equalsIgnoreCase(target);
    }
}
