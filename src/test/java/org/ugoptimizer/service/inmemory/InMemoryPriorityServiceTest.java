package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.PriorityService;
import org.ugoptimizer.frontend.RequestService;

class InMemoryPriorityServiceTest {

    @Test
    void onlyPendingRequestsAppear() {
        RequestService requestService = new InMemoryRequestService();
        PriorityService priorityService = new InMemoryPriorityService(requestService);

        // Seed has 4 requests; only the two PENDING ones (urgency 5 and urgency 4) should show.
        List<ServiceRequest> ordered = priorityService.priorityOrder();

        assertEquals(2, ordered.size());
        assertTrue(ordered.stream().allMatch(r -> "PENDING".equals(r.getStatus())));
    }

    @Test
    void ordersByUrgencyDescending() {
        RequestService requestService = new InMemoryRequestService();
        PriorityService priorityService = new InMemoryPriorityService(requestService);

        List<ServiceRequest> ordered = priorityService.priorityOrder();

        assertEquals(5, ordered.get(0).getUrgency());
        assertEquals(4, ordered.get(1).getUrgency());
    }

    @Test
    void tieBreaksByEarliestSubmissionTime() {
        RequestService requestService = new InMemoryRequestService();
        PriorityService priorityService = new InMemoryPriorityService(requestService);

        ServiceRequest laterUrgent = new ServiceRequest(
                requestService.nextRequestId(), 1, 2, "FIRE_ALARM", 5,
                Instant.now().plus(10, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "FIRE_TEAM", "later urgent");
        requestService.add(laterUrgent);

        List<ServiceRequest> ordered = priorityService.priorityOrder();

        assertEquals(3, ordered.size());
        assertEquals(5, ordered.get(0).getUrgency());
        assertEquals(5, ordered.get(1).getUrgency());
        assertTrue(ordered.get(0).getTimeSubmitted().isBefore(ordered.get(1).getTimeSubmitted()),
                "the earlier-submitted urgency-5 request must rank ahead of the later one");
    }

    @Test
    void reflectsRequestStatusChangesLive() {
        RequestService requestService = new InMemoryRequestService();
        PriorityService priorityService = new InMemoryPriorityService(requestService);

        int topId = priorityService.priorityOrder().get(0).getRequestId();
        requestService.updateStatus(topId, "ASSIGNED");

        List<ServiceRequest> afterAdvance = priorityService.priorityOrder();
        assertEquals(1, afterAdvance.size());
        assertFalse(afterAdvance.stream().anyMatch(r -> r.getRequestId() == topId));
    }
}
