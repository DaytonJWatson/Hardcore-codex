package com.daytonjwatson.hardcore.jobs;

public enum JobType {
    KILL_MOB,
    COLLECT_ITEM;

    public static JobType fromString(String raw) {
        for (JobType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }
}
