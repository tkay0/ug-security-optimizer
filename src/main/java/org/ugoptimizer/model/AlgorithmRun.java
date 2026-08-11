package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable planned or measured algorithm benchmark run. */
public final class AlgorithmRun {

    private final int runId;
    private final String algorithmName;
    private final int inputSize;
    private final Long timeNs;
    private final Double memoryKb;
    private final Instant dateRun;
    private final String status;
    private final String experimentGroup;
    private final int runNumber;

    /** Creates a run whose measurement fields must agree with its status. */
    public AlgorithmRun(
            int runId,
            String algorithmName,
            int inputSize,
            Long timeNs,
            Double memoryKb,
            Instant dateRun,
            String status,
            String experimentGroup,
            int runNumber) {
        if (runId <= 0) {
            throw new IllegalArgumentException("runId must be positive");
        }
        if (inputSize <= 0) {
            throw new IllegalArgumentException("inputSize must be positive");
        }
        if (runNumber <= 0) {
            throw new IllegalArgumentException("runNumber must be positive");
        }
        if (timeNs != null && timeNs < 0L) {
            throw new IllegalArgumentException("timeNs cannot be negative");
        }
        if (memoryKb != null && (!Double.isFinite(memoryKb) || memoryKb < 0.0d)) {
            throw new IllegalArgumentException("memoryKb must be finite and non-negative");
        }

        this.runId = runId;
        this.algorithmName = requiredText(algorithmName, "algorithmName");
        this.inputSize = inputSize;
        this.timeNs = timeNs;
        this.memoryKb = memoryKb;
        this.dateRun = dateRun;
        this.status = validateStatus(status, timeNs, memoryKb, dateRun);
        this.experimentGroup = requiredText(experimentGroup, "experimentGroup");
        this.runNumber = runNumber;
    }

    public int getRunId() {
        return runId;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int getInputSize() {
        return inputSize;
    }

    /** Returns elapsed nanoseconds, or {@code null} for a planned run. */
    public Long getTimeNs() {
        return timeNs;
    }

    /** Returns measured memory in kilobytes, or {@code null} for a planned run. */
    public Double getMemoryKb() {
        return memoryKb;
    }

    /** Returns the measurement time, or {@code null} for a planned run. */
    public Instant getDateRun() {
        return dateRun;
    }

    public String getStatus() {
        return status;
    }

    public String getExperimentGroup() {
        return experimentGroup;
    }

    public int getRunNumber() {
        return runNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AlgorithmRun run)) {
            return false;
        }
        return runId == run.runId
                && inputSize == run.inputSize
                && runNumber == run.runNumber
                && algorithmName.equals(run.algorithmName)
                && Objects.equals(timeNs, run.timeNs)
                && Objects.equals(memoryKb, run.memoryKb)
                && Objects.equals(dateRun, run.dateRun)
                && status.equals(run.status)
                && experimentGroup.equals(run.experimentGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                runId, algorithmName, inputSize, timeNs, memoryKb, dateRun,
                status, experimentGroup, runNumber);
    }

    @Override
    public String toString() {
        return "AlgorithmRun{"
                + "runId=" + runId
                + ", algorithmName='" + algorithmName + '\''
                + ", inputSize=" + inputSize
                + ", timeNs=" + timeNs
                + ", memoryKb=" + memoryKb
                + ", dateRun=" + dateRun
                + ", status='" + status + '\''
                + ", experimentGroup='" + experimentGroup + '\''
                + ", runNumber=" + runNumber
                + '}';
    }

    private static String validateStatus(
            String value, Long timeNs, Double memoryKb, Instant dateRun) {
        requiredText(value, "status");
        if ("PLANNED".equals(value)) {
            if (timeNs != null || memoryKb != null || dateRun != null) {
                throw new IllegalArgumentException(
                        "PLANNED runs cannot contain measurement values");
            }
            return value;
        }
        if ("MEASURED".equals(value)) {
            if (timeNs == null || memoryKb == null || dateRun == null) {
                throw new IllegalArgumentException(
                        "MEASURED runs require timeNs, memoryKb, and dateRun");
            }
            return value;
        }
        throw new IllegalArgumentException("Unsupported status: " + value);
    }

    private static String requiredText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
