package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlgorithmRunTest {

    private static final Instant RUN_TIME = Instant.parse("2026-08-09T12:00:00Z");

    @Test
    void createsPlannedRunFromCanonicalDatasetValues() {
        AlgorithmRun run = new AlgorithmRun(
                1, "BFS", 50, null, null, null, "PLANNED", "BFS_50", 1);

        assertEquals(1, run.getRunId());
        assertEquals("BFS", run.getAlgorithmName());
        assertEquals(50, run.getInputSize());
        assertNull(run.getTimeNs());
        assertNull(run.getMemoryKb());
        assertNull(run.getDateRun());
        assertEquals("PLANNED", run.getStatus());
        assertEquals("BFS_50", run.getExperimentGroup());
        assertEquals(1, run.getRunNumber());
    }

    @Test
    void createsMeasuredRunAndDoesNotRestrictAlgorithmName() {
        AlgorithmRun run = new AlgorithmRun(
                31, "FUTURE_ALGORITHM", 50, 25_000L, 128.5d, RUN_TIME,
                "MEASURED", "FUTURE_50", 1);

        assertEquals("FUTURE_ALGORITHM", run.getAlgorithmName());
        assertEquals(25_000L, run.getTimeNs());
        assertEquals(128.5d, run.getMemoryKb());
        assertEquals(RUN_TIME, run.getDateRun());
    }

    @Test
    void rejectsPlannedRunWithMeasurements() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, 1L, null, null, "PLANNED", "BFS_50", 1));
    }

    @Test
    void rejectsMeasuredRunWithIncompleteMeasurements() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, 1L, 2.0d, null,
                        "MEASURED", "BFS_50", 1));
    }

    @Test
    void rejectsNegativeOrNonFiniteMeasurements() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, -1L, 2.0d, RUN_TIME,
                        "MEASURED", "BFS_50", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, 1L, -0.1d, RUN_TIME,
                        "MEASURED", "BFS_50", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, 1L, Double.NaN, RUN_TIME,
                        "MEASURED", "BFS_50", 1));
    }

    @Test
    void rejectsInvalidIdentityAndStatusValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        0, "BFS", 50, null, null, null, "PLANNED", "BFS_50", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 0, null, null, null, "PLANNED", "BFS_50", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AlgorithmRun(
                        1, "BFS", 50, null, null, null, "UNKNOWN", "BFS_50", 1));
    }

    @Test
    void equalRunsHaveEqualHashCodes() {
        AlgorithmRun first = new AlgorithmRun(
                1, "BFS", 50, null, null, null, "PLANNED", "BFS_50", 1);
        AlgorithmRun second = new AlgorithmRun(
                1, "BFS", 50, null, null, null, "PLANNED", "BFS_50", 1);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
