package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.AlgorithmRun;

class AlgorithmRunDaoTest {

    private static final Instant MEASURED_AT = Instant.parse("2026-08-10T12:30:00Z");

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager manager;
    private AlgorithmRunDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = DaoTestDatabase.create(temporaryDirectory, "runs.db");
        dao = new AlgorithmRunDao(manager);
    }

    @Test
    void plannedRunPreservesNullMeasurements() throws Exception {
        AlgorithmRun run = dao.findById(1).orElseThrow();

        assertEquals("BFS", run.getAlgorithmName());
        assertEquals("PLANNED", run.getStatus());
        assertNull(run.getTimeNs());
        assertNull(run.getMemoryKb());
        assertNull(run.getDateRun());
    }

    @Test
    void findsAlgorithmRunsInDeterministicOrder() throws Exception {
        AlgorithmRun[] runs = dao.findByAlgorithmName("BFS");

        assertEquals(12, runs.length);
        for (int index = 0; index < runs.length; index++) {
            assertEquals(index + 1, runs[index].getRunId());
            assertEquals("BFS", runs[index].getAlgorithmName());
        }
        assertEquals(0, dao.findByAlgorithmName("FUTURE_ALGORITHM").length);
    }

    @Test
    void markMeasuredPersistsAllFieldsAfterReopen() throws Exception {
        assertTrue(dao.markMeasured(1, 25_000L, 128.5d, MEASURED_AT));

        AlgorithmRunDao reopenedDao = new AlgorithmRunDao(
                new DatabaseManager(manager.getDatabasePath()));
        AlgorithmRun run = reopenedDao.findById(1).orElseThrow();
        assertEquals("MEASURED", run.getStatus());
        assertEquals(25_000L, run.getTimeNs());
        assertEquals(128.5d, run.getMemoryKb());
        assertEquals(MEASURED_AT, run.getDateRun());
    }

    @Test
    void invalidMeasurementsAreRejectedBeforeSql() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.markMeasured(1, -1L, 1.0d, MEASURED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> dao.markMeasured(1, 1L, -1.0d, MEASURED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> dao.markMeasured(1, 1L, Double.NaN, MEASURED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> dao.markMeasured(1, 1L, Double.POSITIVE_INFINITY, MEASURED_AT));
        assertThrows(NullPointerException.class,
                () -> dao.markMeasured(1, 1L, 1.0d, null));
    }

    @Test
    void missingOrAlreadyMeasuredRunReturnsFalse() throws Exception {
        assertFalse(dao.markMeasured(999, 1L, 1.0d, MEASURED_AT));
        assertTrue(dao.markMeasured(1, 1L, 1.0d, MEASURED_AT));
        assertFalse(dao.markMeasured(1, 2L, 2.0d, MEASURED_AT.plusSeconds(1)));
    }

    @Test
    void missingAndInvalidQueriesFollowPolicy() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(0));
        assertThrows(IllegalArgumentException.class,
                () -> dao.findByAlgorithmName("  "));
        assertThrows(NullPointerException.class,
                () -> dao.findByAlgorithmName(null));
    }
}
