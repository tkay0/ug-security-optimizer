package org.ugoptimizer.service.inmemory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.RequestService;

/**
 * In-memory {@link RequestService} shared by every screen that touches
 * requests (Requests &amp; Resources, Search &amp; Sort, Dispatch Workflow,
 * Optimization). Seeded with the sample requests those screens used to hold
 * separately, merged into one list so a request added on one tab is visible
 * on the others. Replace with a real DAO-backed implementation once the
 * database team's work lands.
 */
public final class InMemoryRequestService implements RequestService {

    private final List<ServiceRequest> requests = new ArrayList<>();
    private int nextRequestId = 1;

    public InMemoryRequestService() {
        seedSampleData();
    }

    @Override
    public List<ServiceRequest> findAll() {
        return Collections.unmodifiableList(requests);
    }

    @Override
    public int nextRequestId() {
        return nextRequestId;
    }

    @Override
    public ServiceRequest add(ServiceRequest request) {
        requests.add(request);
        nextRequestId++;
        return request;
    }

    @Override
    public ServiceRequest updateStatus(int requestId, String newStatus) {
        for (int i = 0; i < requests.size(); i++) {
            ServiceRequest current = requests.get(i);
            if (current.getRequestId() == requestId) {
                ServiceRequest updated = withStatus(current, newStatus);
                requests.set(i, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("No service request with ID " + requestId);
    }

    private static ServiceRequest withStatus(ServiceRequest original, String newStatus) {
        return new ServiceRequest(
                original.getRequestId(),
                original.getSourceLocationId(),
                original.getDestinationLocationId(),
                original.getCategory(),
                original.getUrgency(),
                original.getTimeSubmitted(),
                original.getDeadline(),
                newStatus,
                original.getRequiredResourceType(),
                original.getDescription());
    }

    private void seedSampleData() {
        requests.add(new ServiceRequest(nextRequestId++, 1, 2, "MEDICAL_EMERGENCY", 5,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "MEDICAL_TEAM", "Sample seeded medical emergency"));
        requests.add(new ServiceRequest(nextRequestId++, 3, 4, "THEFT_REPORT", 2,
                Instant.now(), Instant.now().plus(4, ChronoUnit.HOURS),
                "ASSIGNED", "PATROL_TEAM", "Sample seeded theft report"));
        requests.add(new ServiceRequest(nextRequestId++, 5, 6, "FIRE_ALARM", 4,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "FIRE_TEAM", "Sample fire alarm"));
        requests.add(new ServiceRequest(nextRequestId++, 2, 7, "CROWD_CONTROL", 3,
                Instant.now(), Instant.now().plus(3, ChronoUnit.HOURS),
                "IN_PROGRESS", "PATROL_TEAM", "Sample crowd control"));
    }
}
