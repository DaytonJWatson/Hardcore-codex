package com.daytonjwatson.hardcore.jobs;

public class JobOffer {

    private final JobDefinition definition;
    private final java.util.List<Integer> amounts;

    public JobOffer(JobDefinition definition, java.util.List<Integer> amounts) {
        this.definition = definition;
        this.amounts = amounts;
    }

    public JobDefinition getDefinition() {
        return definition;
    }

    public java.util.List<Integer> getAmounts() {
        return amounts;
    }

    public int getAmount(int index) {
        if (index < 0 || index >= amounts.size()) {
            return 0;
        }
        return amounts.get(index);
    }
}
