package org.ugoptimizer.performance;

import java.util.Objects;

/** Immutable benchmark records plus the environment in which they were measured. */
public final class BenchmarkReport {

    private final BenchmarkRecord[] records;
    private final String environment;

    public BenchmarkReport(BenchmarkRecord[] records, String environment) {
        Objects.requireNonNull(records, "records cannot be null");
        for (int index = 0; index < records.length; index++) {
            Objects.requireNonNull(records[index], "records[" + index + "] cannot be null");
        }
        this.records = records.clone();
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
    }

    public BenchmarkRecord[] getRecords() {
        return records.clone();
    }

    public String getEnvironment() {
        return environment;
    }
}
