package org.ugoptimizer.service.inmemory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.service.PriorityService;
import org.ugoptimizer.service.RequestService;
import org.ugoptimizer.structures.heap.BinaryHeap;

/**
 * In-memory {@link PriorityService} built directly over an injected
 * {@link RequestService}. Priority order is a derived view recomputed on
 * every call, not separately persisted state, so there is no seed data here
 * -- whatever {@code RequestService} currently holds is the source of truth.
 */
public final class InMemoryPriorityService implements PriorityService {

    private static final Comparator<ServiceRequest> DISPATCH_PRIORITY =
            Comparator.comparingInt(ServiceRequest::getUrgency).reversed()
                    .thenComparing(ServiceRequest::getTimeSubmitted);
    private static final String PENDING = "PENDING";

    private final RequestService requestService;

    public InMemoryPriorityService(RequestService requestService) {
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
    }

    @Override
    public List<ServiceRequest> priorityOrder() {
        BinaryHeap<ServiceRequest> heap = new BinaryHeap<>(DISPATCH_PRIORITY);
        for (ServiceRequest request : requestService.findAll()) {
            if (PENDING.equals(request.getStatus())) {
                heap.add(request);
            }
        }

        List<ServiceRequest> ordered = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            ordered.add(heap.poll());
        }
        return ordered;
    }
}
