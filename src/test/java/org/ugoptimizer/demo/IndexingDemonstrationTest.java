package org.ugoptimizer.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.ServiceRequest;

class IndexingDemonstrationTest {

    @Test
    void searchesRealTreeImplementationsAndReportsOrderedIndexEvidence() {
        ServiceRequest[] requests = new ServiceRequest[12];
        for (int index = 0; index < requests.length; index++) {
            requests[index] = request(index + 1);
        }

        String report = new IndexingDemonstration().demonstrate(requests, 9);

        assertTrue(report.contains("Records indexed: 12"));
        assertTrue(report.contains("BST search path: 1 -> 2 -> 3"));
        assertTrue(report.contains("Red-black search path:"));
        assertTrue(report.contains("B-tree search path:"));
        assertTrue(report.contains("Match agreement: YES (request 9)"));
        assertTrue(report.contains("root split occurred: true"));
        assertTrue(report.contains("Red-black balancing events: left rotations="));
        assertTrue(report.contains("insertion recolours="));
        assertTrue(report.contains("BST in-order IDs: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12"));
    }

    @Test
    void missingRequestIsReportedConsistentlyByEveryIndex() {
        String report = new IndexingDemonstration().demonstrate(
                new ServiceRequest[]{request(5), request(1), request(9), request(3)}, 7);
        assertTrue(report.contains("Match agreement: YES (request 7)"));
        assertTrue(report.contains("Found: false"));
    }

    private static ServiceRequest request(int id) {
        Instant submitted = Instant.parse("2026-01-01T00:00:00Z");
        return new ServiceRequest(id, 1, 2, "SECURITY_ESCORT", 3, submitted,
                submitted.plusSeconds(600), "PENDING", "PATROL_OFFICER", "Request " + id);
    }
}
