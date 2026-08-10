package org.ugoptimizer.optimization;

import org.ugoptimizer.shared.IncidentOptimizer;
import org.ugoptimizer.shared.Incident;
import org.ugoptimizer.shared.ResponsePlan;
import org.ugoptimizer.util.DynamicArray;

/**
 * Brute-Force incident selector.
 * Enumerates all 2^n subsets via bit-mask to find the exact optimum.
 *
 * Time Complexity:  O(2^n × n)
 * Space Complexity: O(n) for the current subset
 *
 * No prohibited collections are used in the core algorithm.
 * Practical limit: n ≤ 30 (enforced to prevent runaway execution).
 */
public class BruteForceIncidentSelector implements IncidentOptimizer {

    @Override
    public ResponsePlan optimize(Incident[] incidents, int capacity) {
        if (incidents == null || capacity < 0) {
            throw new IllegalArgumentException("Incidents cannot be null and capacity must be non-negative");
        }

        int n = incidents.length;
        if (n == 0 || capacity == 0) {
            return new ResponsePlan(new Incident[0], 0, 0, capacity);
        }

        if (n > 30) {
            throw new IllegalArgumentException(
                "Brute-force supports max 30 incidents. Given: " + n);
        }

        int subsetCount = 1 << n;
        int bestSeverity = 0;
        int bestCost = 0;
        int bestMask = 0;

        for (int mask = 0; mask < subsetCount; mask++) {
            int currentCost = 0;
            int currentSeverity = 0;

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    Incident inc = incidents[i];
                    currentCost += inc.getResponseCost();
                    currentSeverity += inc.getSeverity();
                }
            }

            if (currentCost <= capacity) {
                if (currentSeverity > bestSeverity) {
                    bestSeverity = currentSeverity;
                    bestCost = currentCost;
                    bestMask = mask;
                } else if (currentSeverity == bestSeverity && currentCost < bestCost) {
                    bestCost = currentCost;
                    bestMask = mask;
                }
            }
        }

        DynamicArray<Incident> selected = new DynamicArray<>();
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1 << i)) != 0) {
                selected.add(incidents[i]);
            }
        }

        Incident[] result = new Incident[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            result[i] = selected.get(i);
        }

        return new ResponsePlan(result, bestCost, bestSeverity, capacity);
    }
}
