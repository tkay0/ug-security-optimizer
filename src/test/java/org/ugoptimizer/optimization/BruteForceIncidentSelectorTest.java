package org.ugoptimizer.optimization;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.shared.Incident;
import org.ugoptimizer.shared.ResponsePlan;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceIncidentSelectorTest {

    private final BruteForceIncidentSelector selector = new BruteForceIncidentSelector();

    @Test
    void emptyInputReturnsEmptyPlan() {
        ResponsePlan plan = selector.optimize(new Incident[0], 100);
        assertEquals(0, plan.getCount());
        assertEquals(0, plan.getTotalSeverity());
    }

    @Test
    void zeroCapacityReturnsEmptyPlan() {
        Incident[] incidents = {
            new Incident(1, "Fire", "Lab", 20, 50)
        };
        ResponsePlan plan = selector.optimize(incidents, 0);
        assertEquals(0, plan.getCount());
    }

    @Test
    void matchesDpOnSmallInput() {
        Incident[] incidents = {
            new Incident(1, "Disturbance", "Gate", 2, 3),
            new Incident(2, "Theft", "Library", 3, 4),
            new Incident(3, "Fire Alarm", "Hostel", 4, 5),
            new Incident(4, "Vandalism", "Parking", 5, 6)
        };
        ResponsePlan plan = selector.optimize(incidents, 7);
        assertEquals(9, plan.getTotalSeverity());
        assertEquals(7, plan.getTotalCost());
        assertEquals(2, plan.getCount());
    }

    @Test
    void allFit() {
        Incident[] incidents = {
            new Incident(1, "A", "Z1", 1, 2),
            new Incident(2, "B", "Z2", 2, 3),
            new Incident(3, "C", "Z3", 3, 4)
        };
        ResponsePlan plan = selector.optimize(incidents, 10);
        assertEquals(3, plan.getCount());
        assertEquals(9, plan.getTotalSeverity());
    }

    @Test
    void exceeds30Throws() {
        Incident[] incidents = new Incident[31];
        for (int i = 0; i < 31; i++) {
            incidents[i] = new Incident(i, "I" + i, "Z" + i, 1, 1);
        }
        assertThrows(IllegalArgumentException.class, () -> selector.optimize(incidents, 50));
    }

    @Test
    void tieBreakerPrefersLowerCost() {
        Incident[] incidents = {
            new Incident(1, "Cheap", "A", 2, 5),
            new Incident(2, "Expensive", "B", 4, 5)
        };
        ResponsePlan plan = selector.optimize(incidents, 4);
        assertEquals(5, plan.getTotalSeverity());
        assertEquals(2, plan.getTotalCost()); // picks the cheaper one
    }

    @Test
    void invalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> selector.optimize(null, 10));
        assertThrows(IllegalArgumentException.class, () -> selector.optimize(new Incident[0], -5));
    }
}
