package com.daytonjwatson.hardcore.jobs;

import org.bukkit.Location;

public class ActiveObjective {

    private final JobObjective definition;
    private final int goalAmount;
    private double progress;
    private Location startLocation;

    public ActiveObjective(JobObjective definition, int goalAmount, double progress, Location startLocation) {
        this.definition = definition;
        this.goalAmount = goalAmount;
        this.progress = progress;
        this.startLocation = startLocation;
    }

    public JobObjective getDefinition() {
        return definition;
    }

    public int getGoalAmount() {
        return goalAmount;
    }

    public double getProgress() {
        return progress;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public boolean isComplete() {
        return progress >= goalAmount;
    }

    public double getRemaining() {
        return Math.max(0, goalAmount - progress);
    }

    public double addProgress(double amount) {
        progress = Math.min(goalAmount, progress + amount);
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.min(goalAmount, progress);
    }
}
