package com.daytonjwatson.hardcore.jobs;

public class JobOffer {

    private final JobDefinition definition;
    private final int amount;

    public JobOffer(JobDefinition definition, int amount) {
        this.definition = definition;
        this.amount = amount;
    }

    public JobDefinition getDefinition() {
        return definition;
    }

    public int getAmount() {
        return amount;
    }
}
