package org.ugoptimizer.shared;

/**
 * Contract for incident selection optimizers.
 * All implementations must be deterministic for the same input.
 */
public interface IncidentOptimizer {
    /**
     * Selects the optimal subset of incidents that maximizes total severity
     * without exceeding the given capacity constraint.
     *
     * @param incidents array of incidents to consider
     * @param capacity  maximum allowable response cost (budget / fuel / time)
     * @return ResponsePlan containing the optimal subset
     */
    ResponsePlan optimize(Incident[] incidents, int capacity);
}
