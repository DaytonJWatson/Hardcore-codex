package com.daytonjwatson.hardcore.jobs;

public class ActiveJob {

    private final JobDefinition job;
    private int progress;

    public ActiveJob(JobDefinition job, int progress) {
        this.job = job;
        this.progress = progress;
    }

    public JobDefinition getJob() {
        return job;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isComplete() {
        return progress >= job.getAmount();
    }

    public int addProgress(int amount) {
        progress = Math.min(job.getAmount(), progress + amount);
        return progress;
    }
}
