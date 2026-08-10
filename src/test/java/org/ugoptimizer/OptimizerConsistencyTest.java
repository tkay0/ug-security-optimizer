package org.ugoptimizer.optimization;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.shared.Incident;
import org.ugoptimizer.shared.ResponsePlan;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OptimizerConsistencyTest {

    @Test
    void dpAndBruteForceAgreeOnRandomSmallCases() {
        DynamicProgrammingIncidentSelector dp = new DynamicProgrammingIncidentSelector();
        BruteForceIncidentSelector bf = new BruteForceIncidentSelector();
        Random rand = new Random(2026);

        for (int trial = 0; trial < 100; trial++) {
            int n = rand.nextInt(12) + 1;      // 1–12 incidents
            int capacity = rand.nextInt(50) + 5; // capacity 5–54
            Incident[] incidents = new Incident[n];
            for (int i = 0; i < n; i++) {
                incidents[i] = new Incident(
                    i, "Inc" + i, "Zone" + i,
                    rand.nextInt(20) + 1,
                    rand.nextInt(50) + 1
                );
            }

            ResponsePlan dpPlan = dp.optimize(incidents, capacity);
            ResponsePlan bfPlan = bf.optimize(incidents, capacity);

            assertEquals(dpPlan.getTotalSeverity(), bfPlan.getTotalSeverity(),
                "Severity mismatch on trial " + trial);
            assertTrue(dpPlan.getTotalCost() <= capacity);
            assertTrue(bfPlan.getTotalCost() <= capacity);
        }
    }

    @Test
    void bothOptimizersReturnSameResultForClassicExample() {
        DynamicProgrammingIncidentSelector dp = new DynamicProgrammingIncidentSelector();
        BruteForceIncidentSelector bf = new BruteForceIncidentSelector();

        Incident[] incidents = {
            new Incident(1, "Disturbance", "Main Gate", 2, 3),
            new Incident(2, "Theft", "Library", 3, 4),
            new Incident(3, "Fire Alarm", "Hostel B", 4, 5),
            new Incident(4, "Vandalism", "Car Park", 5, 6)
        };

        ResponsePlan dpPlan = dp.optimize(incidents, 7);
        ResponsePlan bfPlan = bf.optimize(incidents, 7);

        assertEquals(dpPlan.getTotalSeverity(), bfPlan.getTotalSeverity());
        assertEquals(dpPlan.getTotalCost(), bfPlan.getTotalCost());
        assertEquals(dpPlan.getCount(), bfPlan.getCount());
    }
}
