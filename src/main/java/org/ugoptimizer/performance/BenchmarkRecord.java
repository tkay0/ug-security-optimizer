package org.ugoptimizer.performance;

import java.time.Instant;
import java.util.Objects;

/** One genuine raw benchmark trial, annotated with its group average. */
public record BenchmarkRecord(
        String experiment,
        String algorithm,
        int inputSize,
        int trial,
        long runtimeNs,
        long averageRuntimeNs,
        long memoryKb,
        long seed,
        String parameters,
        String resultMetric,
        Instant dateRun) {

    public BenchmarkRecord {
        experiment = required(experiment, "experiment");
        algorithm = required(algorithm, "algorithm");
        parameters = required(parameters, "parameters");
        resultMetric = required(resultMetric, "resultMetric");
        Objects.requireNonNull(dateRun, "dateRun cannot be null");
        if (inputSize < 0 || trial < 1 || runtimeNs < 0 || averageRuntimeNs < 0 || memoryKb < 0) {
            throw new IllegalArgumentException("Benchmark numeric values cannot be negative");
        }
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
