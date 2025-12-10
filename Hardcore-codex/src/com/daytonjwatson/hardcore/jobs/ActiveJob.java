package com.daytonjwatson.hardcore.jobs;

public class ActiveJob {

    private final JobDefinition job;
    private final int goalAmount;
    private double progress;
    private final int selectedSlot;
    private org.bukkit.Location startLocation;

    public ActiveJob(JobDefinition job, int goalAmount, double progress) {
        this(job, goalAmount, progress, null, -1);
    }

    public ActiveJob(JobDefinition job, int goalAmount, double progress, org.bukkit.Location startLocation,
            int selectedSlot) {
        this.job = job;
        this.goalAmount = goalAmount;
        this.progress = progress;
        this.selectedSlot = selectedSlot;
        this.startLocation = startLocation;
    }

    public JobDefinition getJob() {
        return job;
    }

    public int getGoalAmount() {
        return goalAmount;
    }

    public double getProgress() {
        return progress;
    }

    public org.bukkit.Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(org.bukkit.Location startLocation) {
        this.startLocation = startLocation;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isComplete() {
        return progress >= goalAmount;
    }

    public double addProgress(double amount) {
        progress = Math.min(goalAmount, progress + amount);
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.min(goalAmount, progress);
    }
}
