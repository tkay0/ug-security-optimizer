package org.ugoptimizer.optimization;

import org.ugoptimizer.shared.IncidentOptimizer;
import org.ugoptimizer.shared.Incident;
import org.ugoptimizer.shared.ResponsePlan;
import org.ugoptimizer.util.DynamicArray;

/**
 * Dynamic Programming incident selector.
 * Solves the 0/1 Knapsack problem using bottom-up tabulation.
 *
 * Time Complexity:  O(n × capacity)
 * Space Complexity: O(n × capacity)  (full table kept for reconstruction)
 *
 * No prohibited collections are used in the core algorithm.
 */
public class DynamicProgrammingIncidentSelector implements IncidentOptimizer {

    @Override
    public ResponsePlan optimize(Incident[] incidents, int capacity) {
        if (incidents == null || capacity < 0) {
            throw new IllegalArgumentException("Incidents cannot be null and capacity must be non-negative");
        }

        int n = incidents.length;
        if (n == 0 || capacity == 0) {
            return new ResponsePlan(new Incident[0], 0, 0, capacity);
        }

        // dp[i][w] = max severity achievable using first i incidents with cost limit w
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            Incident inc = incidents[i - 1];
            int cost = inc.getResponseCost();
            int severity = inc.getSeverity();

            for (int w = 0; w <= capacity; w++) {
                if (cost > w) {
                    dp[i][w] = dp[i - 1][w];
                } else {
                    int include = severity + dp[i - 1][w - cost];
                    int exclude = dp[i - 1][w];
                    dp[i][w] = Math.max(include, exclude);
                }
            }
        }

        // Reconstruct selected incidents by backtracking
        DynamicArray<Incident> selected = new DynamicArray<>();
        int w = capacity;
        for (int i = n; i > 0 && w > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                Incident inc = incidents[i - 1];
                selected.add(inc);
                w -= inc.getResponseCost();
            }
        }

        Incident[] result = reverse(selected);
        int totalCost = capacity - w;
        int totalSeverity = dp[n][capacity];

        return new ResponsePlan(result, totalCost, totalSeverity, capacity);
    }

    private Incident[] reverse(DynamicArray<Incident> arr) {
        int len = arr.size();
        Incident[] rev = new Incident[len];
        for (int i = 0; i < len; i++) {
            rev[i] = arr.get(len - 1 - i);
        }
        return rev;
    }
}
