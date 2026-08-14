package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.structures.heap.BinaryHeap;
import org.ugoptimizer.util.ProjectParameters;

/** Centralizes the approved urgency-weighted response-queue policy. */
public final class PriorityService {

    private final RequestService requestService;

    public PriorityService(DatabaseManager databaseManager) {
        this(new RequestService(databaseManager));
    }

    public PriorityService(RequestService requestService) {
        this.requestService = Objects.requireNonNull(
                requestService, "requestService cannot be null");
    }

    /**
     * Calculates the project priority score. The repository defines only an
     * urgency field and the approved index-derived weight, so no unapproved
     * proximity or time values are folded into the numeric score.
     */
    public int calculatePriority(ServiceRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return Math.multiplyExact(request.getUrgency(), ProjectParameters.PRIORITY_WEIGHT);
    }

    /** Active work displayed in the response queue, including assigned/in-progress work. */
    public ServiceRequest[] getActiveQueue() throws SQLException {
        return orderActiveRequests(requestService.getAllRequests());
    }

    /** Requests that are still eligible for a new resource assignment. */
    public ServiceRequest[] getDispatchQueue() throws SQLException {
        return orderDispatchableRequests(requestService.getAllRequests());
    }

    public ServiceRequest[] orderActiveRequests(ServiceRequest[] requests) {
        return orderMatching(requests, true);
    }

    public ServiceRequest[] orderDispatchableRequests(ServiceRequest[] requests) {
        return orderMatching(requests, false);
    }

    public boolean isActive(ServiceRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return switch (request.getStatus()) {
            case "PENDING", "ASSIGNED", "IN_PROGRESS" -> true;
            case "COMPLETED", "CANCELLED" -> false;
            default -> throw new IllegalArgumentException(
                    "Unsupported request status: " + request.getStatus());
        };
    }

    public boolean isDispatchable(ServiceRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return "PENDING".equals(request.getStatus());
    }

    private ServiceRequest[] orderMatching(ServiceRequest[] requests, boolean includeAllActive) {
        Objects.requireNonNull(requests, "requests cannot be null");
        BinaryHeap<ServiceRequest> heap = new BinaryHeap<>(priorityComparator());
        for (ServiceRequest request : requests) {
            Objects.requireNonNull(request, "requests cannot contain null");
            boolean include = includeAllActive ? isActive(request) : isDispatchable(request);
            if (include) {
                heap.add(request);
            }
        }
        ServiceRequest[] ordered = new ServiceRequest[heap.size()];
        for (int index = 0; index < ordered.length; index++) {
            ordered[index] = heap.poll();
        }
        return ordered;
    }

    /** Highest weighted urgency first, then the existing earliest-submission rule. */
    private Comparator<ServiceRequest> priorityComparator() {
        return (first, second) -> {
            int scoreComparison = Integer.compare(
                    calculatePriority(second), calculatePriority(first));
            if (scoreComparison != 0) {
                return scoreComparison;
            }
            int submissionComparison = first.getTimeSubmitted()
                    .compareTo(second.getTimeSubmitted());
            return submissionComparison != 0
                    ? submissionComparison
                    : Integer.compare(first.getRequestId(), second.getRequestId());
        };
    }
}
