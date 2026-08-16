package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Orders pending service requests by dispatch priority using the project's
 * own {@code BinaryHeap} (highest urgency first, earliest submission time as
 * the tiebreaker). This is a derived view over {@link RequestService}, not
 * separately persisted state, so a real implementation can be as thin as
 * {@code InMemoryPriorityService} -- just build the heap from whatever
 * {@code RequestService} returns.
 */
public interface PriorityService {

    /** PENDING requests only, ordered highest dispatch priority first. */
    List<ServiceRequest> priorityOrder();
}
