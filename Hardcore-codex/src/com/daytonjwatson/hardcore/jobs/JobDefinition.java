package com.daytonjwatson.hardcore.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobDefinition {

    private final String id;
    private final String displayName;
    private final List<JobObjective> objectives;
    private final boolean ordered;
    private final double difficulty;
    private final double reward;
    private final String[] description;

    public JobDefinition(String id, String displayName, List<JobObjective> objectives, boolean ordered,
            double difficulty, double reward, String[] description) {
        this.id = id;
        this.displayName = displayName;
        this.objectives = new ArrayList<>(objectives);
        this.ordered = ordered;
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

    public List<JobObjective> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }

    public boolean isOrdered() {
        return ordered;
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

    public JobObjective getPrimaryObjective() {
        if (objectives.isEmpty()) {
            return null;
        }
        return objectives.get(0);
    }
}
