package org.ugoptimizer.algorithms;

import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Greedy assignment algorithm used by Team 2 to automatically assign the most
 * suitable available emergency resource to an incident.
 *
 * <p><b>Purpose</b></p>
 * <p>
 * When an incident (medical emergency, fire alarm, theft report, welfare
 * check, night patrol request, crowd control, suspicious activity, access
 * control, security escort, road obstruction, emergency transport or CCTV
 * fault) is reported, a response resource must be chosen immediately. A greedy
 * algorithm is used because it makes the locally best
 * decision at each step without exploring every possible future combination —
 * which is exactly what emergency dispatch needs: a fast, deterministic answer
 * the moment an incident arrives.
 * </p>
 *
 * <p><b>Greedy Strategy</b></p>
 * <p>Candidates are evaluated one by one and the current best is replaced only
 * when a strictly better resource is found, using this priority order:</p>
 * <ol>
 *   <li><b>Rule 1 — Availability:</b> only resources with
 *       {@code available == true} are considered. Unavailable resources are
 *       never assigned.</li>
 *   <li><b>Rule 2 — Type match:</b> the resource type must be able to respond
 *       to the incident (see {@link ServiceRequest#matchesResourceType(String)}),
 *       e.g. MEDICAL_EMERGENCY → AMBULANCE, FIRE_ALARM → FIRE_RESPONSE_UNIT,
 *       THEFT_REPORT → INVESTIGATION_TEAM.</li>
 *   <li><b>Rule 3 — Response time:</b> among valid resources, the one with the
 *       smallest estimated response time wins.</li>
 *   <li><b>Rule 4 — Workload tie-break:</b> if response times are equal, the
 *       resource with the lowest current workload wins.</li>
 *   <li><b>Rule 5 — Identifier tie-break:</b> if both are equal, the resource
 *       with the smallest resource id wins.</li>
 * </ol>
 * <p>The scan is implemented manually with explicit loops and comparisons —
 * no Streams, no {@code PriorityQueue}, no {@code Collections.min()}, no
 * {@code Comparator} utilities and no external libraries.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of resources. Every candidate is visited
 * exactly once and each visit does a fixed amount of work (availability check,
 * type check, and at most three comparisons):</p>
 * <ul>
 *   <li><b>Best Case: {@code O(n)}</b> — even when the best resource is found
 *       immediately, the full input must still be scanned to guarantee no
 *       better candidate exists later.</li>
 *   <li><b>Average Case: {@code O(n)}</b> — on average the scan still visits
 *       all {@code n} resources because the last element may always hold a
 *       better candidate.</li>
 *   <li><b>Worst Case: {@code O(n)}</b> — every resource must be inspected; the
 *       current best may be replaced many times but each replacement is a
 *       constant-time comparison.</li>
 *   <li><b>Space Complexity: {@code O(1)}</b> — only a single reference to the
 *       current best resource is retained; no data structure proportional to
 *       the input is allocated.</li>
 * </ul>
 *
 * <p><b>Correctness Proof Sketch</b></p>
 * <p>Let {@code V} be the set of valid resources (available and type-matching)
 * and {@code b} the resource selected when the scan ends. The algorithm
 * maintains the invariant: <i>after processing the first {@code k} elements,
 * the retained resource is the greedy-best among the valid resources seen so
 * far</i>. The invariant holds for {@code k = 1} (the first valid resource is
 * trivially the best so far) and every later replacement is applied only when
 * the incoming resource beats the current best under the ordered keys
 * (response time, then workload, then id). By induction, after the final
 * element {@code b} is the greedy-best among <b>all</b> of {@code V}. Because
 * the triple (response time, workload, id) is a total order, no later resource
 * can be better than {@code b} without having been compared against it — and
 * any such comparison would have triggered a replacement. Hence the selected
 * resource minimizes response time, then workload, then id, as required.</p>
 *
 * <p><b>Greedy Counterexample (Why Greedy Is Not Globally Optimal)</b></p>
 * <p>Greedy algorithms do not solve every optimization problem optimally. In
 * this domain, consider two incidents arriving in sequence: a medical emergency
 * followed seconds later by a mass-casualty incident. The greedy pass assigns
 * the fastest ambulance (say {@code AMB001}, 2 minutes) to the first incident,
 * making it unavailable for the second, more severe one. A globally optimized
 * schedule that knew both incidents in advance could have reserved
 * {@code AMB001} for the second incident and dispatched a slower unit to the
 * first. The greedy choice is therefore locally optimal but not necessarily
 * globally optimal. The project intentionally accepts this trade-off: immediate
 * response to an incident that has already been reported is more important than
 * computing a globally optimal schedule, which would require knowing future
 * incidents in advance.
 * </p>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Incident: MEDICAL_EMERGENCY. Resources:</p>
 * <pre>
 * AMB001  available   responseTime = 7   workload = 3
 * AMB002  available   responseTime = 4   workload = 5
 * AMB003  unavailable responseTime = 2   workload = 0
 *
 * Scan:
 *   AMB001: valid -> current best = AMB001
 *   AMB002: valid -> 4 &lt; 7        -> replace best = AMB002
 *   AMB003: unavailable -> skip
 * End of scan
 * Assigned: AMB002
 * </pre>
 */
public final class GreedyAssignment {

    private GreedyAssignment() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Selects the best available resource for {@code incident} using the
     * greedy strategy described in the class documentation.
     *
     * @param incident  the incident to assign a resource to; may be
     *                  {@code null}
     * @param resources the candidate resources; may be {@code null}
     * @return the best resource, or {@code null} if no suitable resource
     *         exists (null incident, null or empty array, no available
     *         resource, or no type-matching resource)
     */
    public static Resource assignBestResource(ServiceRequest incident, Resource[] resources) {
        if (incident == null || resources == null) {
            return null;
        }
        Resource best = null;
        for (Resource resource : resources) {
            if (isEligible(incident, resource)) {
                if (best == null || isBetter(resource, best)) {
                    best = resource;
                }
            }
        }
        return best;
    }

    /**
     * Determines whether {@code resource} is a valid candidate for
     * {@code incident} under Rule 1 (available) and Rule 2 (type match).
     *
     * @param incident the incident to respond to; never {@code null}
     * @param resource the resource to test; may be {@code null}
     * @return {@code true} if the resource is available and type-matching
     */
    private static boolean isEligible(ServiceRequest incident, Resource resource) {
        return resource != null
                && resource.isAvailable()
                && incident.matchesResourceType(resource.getType());
    }

    /**
     * Compares two valid resources under Rules 3-5 and reports whether
     * {@code candidate} is strictly better than {@code currentBest}.
     *
     * <p>Comparison order: lower response time wins; if equal, lower workload
     * wins; if equal, smaller resource id wins. A {@code null} resource id is
     * treated as the largest, so non-null ids are always preferred.</p>
     *
     * @param candidate    the incoming resource
     * @param currentBest  the best resource found so far
     * @return {@code true} if the candidate should replace the current best
     */
    private static boolean isBetter(Resource candidate, Resource currentBest) {
        if (candidate.getResponseTime() != currentBest.getResponseTime()) {
            return candidate.getResponseTime() < currentBest.getResponseTime();
        }
        if (candidate.getCurrentWorkload() != currentBest.getCurrentWorkload()) {
            return candidate.getCurrentWorkload() < currentBest.getCurrentWorkload();
        }
        return compareResourceIds(candidate.getId(), currentBest.getId()) < 0;
    }

    /**
     * Compares two resource ids lexicographically, treating {@code null} as
     * larger than any non-null value.
     *
     * @param left  the first id; may be {@code null}
     * @param right the second id; may be {@code null}
     * @return a negative, zero, or positive value as {@code left} is smaller,
     *         equal, or larger than {@code right}
     */
    private static int compareResourceIds(String left, String right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }
}
