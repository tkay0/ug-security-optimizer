package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.AlgorithmRun;

class InMemoryReportServiceTest {

    @Test
    void startsEmpty() {
        InMemoryReportService service = new InMemoryReportService();
        assertEquals(0, service.findAll().size());
    }

    @Test
    void recordAppendsAndReturnsTheSameRun() {
        InMemoryReportService service = new InMemoryReportService();
        AlgorithmRun run = new AlgorithmRun(1, "MergeSort", 1000, 12345L, 4.5d,
                Instant.now(), "MEASURED", "GUI_MANUAL_RUN", 1);

        AlgorithmRun recorded = service.record(run);

        assertSame(run, recorded);
        assertEquals(1, service.findAll().size());
        assertEquals("MergeSort", service.findAll().get(0).getAlgorithmName());
    }

    @Test
    void recordsAccumulateInOrder() {
        InMemoryReportService service = new InMemoryReportService();
        service.record(new AlgorithmRun(1, "MergeSort", 1000, 100L, 1.0d,
                Instant.now(), "MEASURED", "GUI_MANUAL_RUN", 1));
        service.record(new AlgorithmRun(2, "QuickSort", 1000, 90L, 1.0d,
                Instant.now(), "MEASURED", "GUI_MANUAL_RUN", 1));

        assertEquals(2, service.findAll().size());
        assertEquals("MergeSort", service.findAll().get(0).getAlgorithmName());
        assertEquals("QuickSort", service.findAll().get(1).getAlgorithmName());
    }
}
