package org.ugoptimizer.service;

import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Delegates one runtime resource-selection decision to {@link GreedyAssignment}.
 *
 * <p>This compatibility service does not mutate canonical domain objects or
 * perform persistence. Later workflow integration must transactionally change
 * resource availability from AVAILABLE to BUSY, change the request from
 * PENDING to ASSIGNED, and insert the corresponding audit event through the
 * persistence layer.</p>
 */
public final class GreedyAssignmentService {

    /**
     * Selects the best runtime candidate without changing the request or resource.
     *
     * @param request canonical service request; may be null
     * @param candidates validated runtime candidates; may be null
     * @return selected candidate, or null when no candidate is eligible
     */
    public AssignmentCandidate assign(
            ServiceRequest request, AssignmentCandidate[] candidates) {
        return GreedyAssignment.assignBestResource(request, candidates);
    }
}
