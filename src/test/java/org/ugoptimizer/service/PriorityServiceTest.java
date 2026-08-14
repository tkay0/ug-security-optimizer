package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.util.ProjectParameters;

class PriorityServiceTest {

    @TempDir
    Path temporaryDirectory;

    private PriorityService service;

    @BeforeEach
    void setUp() throws Exception {
        RequestService requestService = new RequestService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "priority.db"));
        service = new PriorityService(requestService);
    }

    @Test
    void priorityUsesApprovedIndexDerivedWeight() {
        ServiceRequest critical = request(401, 5, "PENDING", "2026-08-14T10:00:00Z");

        assertEquals(3, ProjectParameters.PRIORITY_WEIGHT);
        assertEquals(15, service.calculatePriority(critical));
    }

    @Test
    void queueOrderingIsDeterministicAndExcludesResolvedWork() {
        ServiceRequest assigned = request(401, 3, "ASSIGNED", "2026-08-14T08:00:00Z");
        ServiceRequest laterCritical = request(402, 5, "PENDING", "2026-08-14T10:00:00Z");
        ServiceRequest earlierCritical = request(403, 5, "PENDING", "2026-08-14T09:00:00Z");
        ServiceRequest completed = request(404, 5, "COMPLETED", "2026-08-14T07:00:00Z");

        ServiceRequest[] active = service.orderActiveRequests(
                new ServiceRequest[]{assigned, laterCritical, completed, earlierCritical});
        assertEquals(3, active.length);
        assertEquals(403, active[0].getRequestId());
        assertEquals(402, active[1].getRequestId());
        assertEquals(401, active[2].getRequestId());

        ServiceRequest[] dispatchable = service.orderDispatchableRequests(
                new ServiceRequest[]{assigned, laterCritical, completed, earlierCritical});
        assertEquals(2, dispatchable.length);
        assertEquals(403, dispatchable[0].getRequestId());
        assertEquals(402, dispatchable[1].getRequestId());
        assertFalse(service.isActive(completed));
        assertTrue(service.isDispatchable(earlierCritical));
    }

    @Test
    void persistedQueuesContainOnlyTheirCanonicalLifecycleStates() throws Exception {
        for (ServiceRequest request : service.getActiveQueue()) {
            assertTrue(service.isActive(request));
        }
        for (ServiceRequest request : service.getDispatchQueue()) {
            assertEquals("PENDING", request.getStatus());
        }
    }

    private static ServiceRequest request(int id, int urgency, String status, String submitted) {
        Instant submittedAt = Instant.parse(submitted);
        return new ServiceRequest(
                id, 1, 2, "SECURITY_ESCORT", urgency, submittedAt,
                submittedAt.plusSeconds(3600), status, "PATROL_OFFICER", "Priority test");
    }
}
