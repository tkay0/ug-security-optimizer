package org.ugoptimizer.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.ServiceRequest;

class SchedulingDemonstrationTest {

    @Test
    void demonstratesAllFourStructuresWithDeterministicOrdersWithoutMutation() {
        ServiceRequest[] requests = {
            request(1, 2, 400), request(2, 5, 300),
            request(3, 3, 200), request(4, 5, 100)
        };
        ServiceRequest[] snapshot = requests.clone();

        String trace = new SchedulingDemonstration().demonstrate(requests);

        assertTrue(trace.contains("FIFO arrival -> dispatch: R1, R2, R3, R4"));
        assertTrue(trace.contains(
                "removed R1, enqueued R4 after rear wrapped (front 0->1, rear 2->0); remaining -> R2, R3, R4"));
        assertTrue(trace.contains("front R2"));
        assertTrue(trace.contains("rear R3"));
        assertTrue(trace.contains("R4[u=5], R2[u=5], R3[u=3], R1[u=2]"));
        assertEquals(snapshot[0], requests[0]);
        assertEquals(snapshot[3], requests[3]);
    }

    @Test
    void requiresFourRequestsForWrapAroundEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulingDemonstration().demonstrate(new ServiceRequest[0]));
    }

    private static ServiceRequest request(int id, int urgency, long deadlineOffset) {
        Instant submitted = Instant.parse("2026-01-01T00:00:00Z");
        return new ServiceRequest(id, 1, 2, "SECURITY_ESCORT", urgency, submitted,
                submitted.plusSeconds(deadlineOffset), "PENDING", "PATROL_OFFICER", "Request " + id);
    }
}
