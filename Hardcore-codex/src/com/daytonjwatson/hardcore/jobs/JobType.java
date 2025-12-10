package com.daytonjwatson.hardcore.jobs;

public enum JobType {
    KILL_MOB,
    COLLECT_ITEM,
    MINE_BLOCK,
    FISH_ITEM,
    CRAFT_ITEM,
    PLACE_BLOCK,
    SMELT_ITEM,
    ENCHANT_ITEM,
    BREED_ANIMAL,
    TAME_ENTITY,
    TRAVEL_BIOME,
    TRAVEL_DISTANCE;

    public static JobType fromString(String raw) {
        for (JobType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }
}
