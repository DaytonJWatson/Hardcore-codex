package com.daytonjwatson.hardcore.jobs;

public class ActiveJob {

    private final JobDefinition job;
    private final java.util.List<ActiveObjective> objectives;
    private final int selectedSlot;

    public ActiveJob(JobDefinition job, java.util.List<ActiveObjective> objectives, int selectedSlot) {
        this.job = job;
        this.objectives = objectives;
        this.selectedSlot = selectedSlot;
    }

    public JobDefinition getJob() {
        return job;
    }

    public java.util.List<ActiveObjective> getObjectives() {
        return java.util.Collections.unmodifiableList(objectives);
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isComplete() {
        for (ActiveObjective objective : objectives) {
            if (!objective.isComplete()) {
                return false;
            }
        }
        return true;
    }

    public java.util.List<ActiveObjective> getPendingObjectives() {
        java.util.List<ActiveObjective> pending = new java.util.ArrayList<>();
        for (ActiveObjective objective : objectives) {
            if (!objective.isComplete()) {
                pending.add(objective);
                if (job.isOrdered()) {
                    break;
                }
            }
        }
        return pending;
    }
}
