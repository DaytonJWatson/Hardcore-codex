package com.daytonjwatson.hardcore.jobs;

public class ActiveJob {

    private final JobDefinition job;
    private double progress;
    private org.bukkit.Location startLocation;

    public ActiveJob(JobDefinition job, double progress) {
        this.job = job;
        this.progress = progress;
    }

    public ActiveJob(JobDefinition job, double progress, org.bukkit.Location startLocation) {
        this.job = job;
        this.progress = progress;
        this.startLocation = startLocation;
    }

    public JobDefinition getJob() {
        return job;
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

    public boolean isComplete() {
        return progress >= job.getAmount();
    }

    public double addProgress(double amount) {
        progress = Math.min(job.getAmount(), progress + amount);
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.min(job.getAmount(), progress);
    }
}
