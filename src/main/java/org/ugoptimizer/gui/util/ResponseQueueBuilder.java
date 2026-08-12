package org.ugoptimizer.gui.util;

import java.util.Comparator;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.structures.heap.BinaryHeap;

/**
 * Builds the emergency response ordering for open requests using the project's
 * existing {@link BinaryHeap}. Open requests (PENDING, ASSIGNED, IN_PROGRESS)
 * are prioritised by urgency (5 highest) and then by earliest submission time,
 * which drives the dispatch order shown to the operator.
 */
public final class ResponseQueueBuilder {

    private ResponseQueueBuilder() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static boolean isOpen(ServiceRequest request) {
        String status = request.getStatus();
        return "PENDING".equals(status)
                || "ASSIGNED".equals(status)
                || "IN_PROGRESS".equals(status);
    }

    /** Heap ordering: most urgent first, earliest submission first on ties. */
    public static Comparator<ServiceRequest> openComparator() {
        return Comparator.comparingInt(ServiceRequest::getUrgency).reversed()
                .thenComparing(ServiceRequest::getTimeSubmitted);
    }

    /**
     * Returns open requests ordered by the response priority queue. The order
     * is produced by inserting every open request into a {@link BinaryHeap}
     * using {@link #openComparator()} and then draining it.
     */
    public static ServiceRequest[] orderedOpenRequests(ServiceRequest[] requests) {
        if (requests == null) {
            return new ServiceRequest[0];
        }
        BinaryHeap<ServiceRequest> heap = new BinaryHeap<>(openComparator());
        for (ServiceRequest request : requests) {
            if (isOpen(request)) {
                heap.add(request);
            }
        }
        ServiceRequest[] ordered = new ServiceRequest[heap.size()];
        for (int index = 0; index < ordered.length; index++) {
            ordered[index] = heap.poll();
        }
        return ordered;
    }
}
