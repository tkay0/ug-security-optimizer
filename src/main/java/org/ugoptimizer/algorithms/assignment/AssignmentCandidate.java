package org.ugoptimizer.algorithms.assignment;

import java.util.Objects;
import org.ugoptimizer.model.Resource;

/**
 * Immutable runtime input for greedy resource selection.
 *
 * <p>Response time is specific to the request being assigned and may later be
 * calculated from the resource location and a routed path. Workload is runtime
 * assignment context. Neither value is persisted as part of {@link Resource}.</p>
 */
public final class AssignmentCandidate {

    private final Resource resource;
    private final double responseTime;
    private final int currentWorkload;

    /**
     * Creates a validated candidate for one assignment decision.
     *
     * @param resource canonical persisted resource
     * @param responseTime finite non-negative response time for the current request
     * @param currentWorkload current number of active assignments
     * @throws NullPointerException if {@code resource} is null
     * @throws IllegalArgumentException if response time or workload is invalid
     */
    public AssignmentCandidate(Resource resource, double responseTime, int currentWorkload) {
        this.resource = Objects.requireNonNull(resource, "resource cannot be null");
        if (!Double.isFinite(responseTime) || responseTime < 0.0d) {
            throw new IllegalArgumentException("responseTime must be finite and non-negative");
        }
        if (currentWorkload < 0) {
            throw new IllegalArgumentException("currentWorkload cannot be negative");
        }
        this.responseTime = responseTime == 0.0d ? 0.0d : responseTime;
        this.currentWorkload = currentWorkload;
    }

    public Resource getResource() {
        return resource;
    }

    public double getResponseTime() {
        return responseTime;
    }

    public int getCurrentWorkload() {
        return currentWorkload;
    }
}
