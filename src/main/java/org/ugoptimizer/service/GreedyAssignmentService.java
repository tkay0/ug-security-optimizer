package org.ugoptimizer.service;

import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Service facade for the greedy resource assignment flow.
 *
 * <p>This class is the stable entry point used by the rest of the system. It
 * is responsible only for <b>validating inputs</b>, delegating the decision to
 * {@link GreedyAssignment}, and returning the assigned resource. Once a
 * resource is selected it is marked as dispatched using the shared
 * {@link Resource} model's state fields. It deliberately contains
 * <b>no algorithm logic</b> — all selection rules live in
 * {@link GreedyAssignment}.</p>
 */
public class GreedyAssignmentService {

    /**
     * Assigns the best available resource to {@code incident}.
     *
     * <p>Invalid inputs (a {@code null} incident or a {@code null} resource
     * array) are handled gracefully by returning {@code null}. A {@code null}
     * return also means no suitable resource exists.</p>
     *
     * <p>When a resource is selected, its state is updated to reflect the
     * assignment: it is marked unavailable, its workload is incremented, and
     * its status is set to {@code DISPATCHED}.</p>
     *
     * @param incident  the incident requiring a resource; may be {@code null}
     * @param resources the candidate resources; may be {@code null}
     * @return the assigned resource with its state updated, or {@code null} if
     *         none could be assigned
     */
    public Resource assign(ServiceRequest incident, Resource[] resources) {
        if (incident == null || resources == null) {
            return null;
        }
        Resource assigned = GreedyAssignment.assignBestResource(incident, resources);
        if (assigned != null) {
            assigned.setAvailable(false);
            assigned.setCurrentWorkload(assigned.getCurrentWorkload() + 1);
            assigned.setStatus("DISPATCHED");
        }
        return assigned;
    }
}
