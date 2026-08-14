package org.ugoptimizer.algorithms;

import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Selects the best currently available resource for one service request.
 *
 * <p>The manual best-so-far scan first filters candidates by canonical
 * availability and resource-type compatibility. It then minimizes response
 * time, current workload, and numeric resource ID in that order. Response time
 * and workload are request-specific runtime inputs held by
 * {@link AssignmentCandidate}; they are deliberately not fields of the
 * persisted {@link Resource} model.</p>
 *
 * <p>For {@code n} candidates, best-, average-, and worst-case running time are
 * {@code O(n)} because every candidate must be inspected to prove that no later
 * candidate is better. The algorithm retains one candidate reference, so its
 * auxiliary selection space is {@code O(1)}.</p>
 *
 * <p><b>Correctness sketch:</b> after each array position, the retained
 * candidate is the minimum eligible candidate among all positions processed so
 * far under the ordered key (response time, workload, resource ID). An
 * ineligible candidate preserves the invariant; an eligible candidate replaces
 * the retained value exactly when its key is smaller. Therefore, after the last
 * position, the retained candidate is the minimum over the complete eligible
 * set.</p>
 *
 * <p><b>Greedy limitation:</b> this method optimizes one assignment using only
 * current information. If two medical requests arrive sequentially, assigning
 * the fastest ambulance to the first may be worse for the pair than reserving
 * it for a later, more urgent request. Coordinating multiple known requests is
 * a separate global scheduling problem.</p>
 *
 * <p><b>Example trace:</b> for a medical request, candidate 1 is AVAILABLE with
 * response time 7, candidate 2 is AVAILABLE with response time 4, and candidate
 * 3 is BUSY with response time 2. Candidate 1 becomes the initial best,
 * candidate 2 replaces it, and candidate 3 is skipped. Candidate 2 is returned.</p>
 */
public final class GreedyAssignment {

    private static final String AVAILABLE = "AVAILABLE";

    private GreedyAssignment() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    /**
     * Selects the best eligible candidate for a canonical service request.
     *
     * @param request request to assign; may be null
     * @param candidates runtime candidates; may be null and may contain null entries
     * @return best candidate, or null when the inputs contain no eligible candidate
     */
    public static AssignmentCandidate assignBestResource(
            ServiceRequest request, AssignmentCandidate[] candidates) {
        if (request == null || candidates == null) {
            return null;
        }

        AssignmentCandidate best = null;
        for (AssignmentCandidate candidate : candidates) {
            if (isEligible(request, candidate)
                    && (best == null || isBetter(candidate, best))) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isEligible(
            ServiceRequest request, AssignmentCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        Resource resource = candidate.getResource();
        return AVAILABLE.equals(resource.getAvailabilityStatus())
                && isCompatible(request, resource);
    }

    private static boolean isCompatible(ServiceRequest request, Resource resource) {
        String requiredResourceType = request.getRequiredResourceType();
        if (requiredResourceType != null) {
            return requiredResourceType.equals(resource.getResourceType());
        }
        return fallbackResourceType(request.getCategory()).equals(resource.getResourceType());
    }

    private static String fallbackResourceType(String category) {
        return switch (category) {
            case "MEDICAL_EMERGENCY" -> "AMBULANCE";
            case "FIRE_ALARM" -> "FIRE_RESPONSE_UNIT";
            case "THEFT_REPORT" -> "INVESTIGATION_TEAM";
            case "WELFARE_CHECK", "SUSPICIOUS_ACTIVITY", "ACCESS_CONTROL",
                    "SECURITY_ESCORT" -> "PATROL_OFFICER";
            case "NIGHT_PATROL_REQUEST" -> "MOTORCYCLE_PATROL";
            case "CROWD_CONTROL" -> "CROWD_CONTROL_TEAM";
            case "ROAD_OBSTRUCTION" -> "PATROL_VEHICLE";
            case "EMERGENCY_TRANSPORT" -> "RAPID_RESPONSE_TEAM";
            case "CCTV_FAULT" -> "CCTV_TECHNICIAN";
            default -> throw new IllegalArgumentException(
                    "Unsupported request category: " + category);
        };
    }

    private static boolean isBetter(
            AssignmentCandidate candidate, AssignmentCandidate currentBest) {
        int responseComparison = Double.compare(
                candidate.getResponseTime(), currentBest.getResponseTime());
        if (responseComparison != 0) {
            return responseComparison < 0;
        }
        if (candidate.getCurrentWorkload() != currentBest.getCurrentWorkload()) {
            return candidate.getCurrentWorkload() < currentBest.getCurrentWorkload();
        }
        return candidate.getResource().getResourceId()
                < currentBest.getResource().getResourceId();
    }
}
