package com.daytonjwatson.hardcore.jobs;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

public class JobObjective {

    private final JobType type;
    private final String target;
    private final Set<String> targetAliases;
    private final int minAmount;
    private final int maxAmount;
    private final boolean consumeItems;

    public JobObjective(JobType type, String target, List<String> aliases, int minAmount, int maxAmount,
            boolean consumeItems) {
        this.type = type;
        this.target = target.toUpperCase(Locale.ROOT);
        this.targetAliases = new LinkedHashSet<>();
        this.targetAliases.add(this.target);
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == null) {
                    continue;
                }
                this.targetAliases.add(alias.toUpperCase(Locale.ROOT));
            }
        }
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

    public Set<String> getTargets() {
        return targetAliases;
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
            case KILL_MOB -> targetAliases.contains(entityType.name().toUpperCase(Locale.ROOT));
            case BREED_ANIMAL -> targetAliases.contains(entityType.name().toUpperCase(Locale.ROOT));
            case TAME_ENTITY -> targetAliases.contains(entityType.name().toUpperCase(Locale.ROOT));
            default -> false;
        };
    }

    public boolean matches(Material material) {
        return switch (type) {
            case COLLECT_ITEM, MINE_BLOCK, FISH_ITEM, CRAFT_ITEM, PLACE_BLOCK, SMELT_ITEM, ENCHANT_ITEM ->
                    targetAliases.contains(material.name().toUpperCase(Locale.ROOT));
            default -> false;
        };
    }

    public boolean matchesBiome(Biome biome) {
        return type == JobType.TRAVEL_BIOME && targetAliases.contains(biome.name().toUpperCase(Locale.ROOT));
    }
}
