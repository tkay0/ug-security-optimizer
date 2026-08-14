package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Single source of truth for service requests. A real implementation wraps
 * {@code ServiceRequestDao}; {@code InMemoryRequestService} exists for
 * development before that lands. Every screen that reads or writes requests
 * (Requests &amp; Resources, Search &amp; Sort, Dispatch Workflow, Optimization)
 * shares the same injected instance, so a change made in one tab is visible
 * in the others.
 */
public interface RequestService {

    List<ServiceRequest> findAll();

    /** Returns the ID the next added request should use (mirrors DB auto-increment). */
    int nextRequestId();

    ServiceRequest add(ServiceRequest request);

    /** Replaces the stored request with {@code updated} status, keyed by request ID. */
    ServiceRequest updateStatus(int requestId, String newStatus);
}
