package org.ugoptimizer.optimization;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.shared.Incident;
import org.ugoptimizer.shared.ResponsePlan;

import static org.junit.jupiter.api.Assertions.*;

class DynamicProgrammingIncidentSelectorTest {

    private final DynamicProgrammingIncidentSelector selector = new DynamicProgrammingIncidentSelector();

    @Test
    void emptyInputReturnsEmptyPlan() {
        ResponsePlan plan = selector.optimize(new Incident[0], 100);
        assertEquals(0, plan.getCount());
        assertEquals(0, plan.getTotalCost());
        assertEquals(0, plan.getTotalSeverity());
    }

    @Test
    void zeroCapacityReturnsEmptyPlan() {
        Incident[] incidents = {
            new Incident(1, "Theft", "Hall A", 50, 80)
        };
        ResponsePlan plan = selector.optimize(incidents, 0);
        assertEquals(0, plan.getCount());
    }

    @Test
    void simpleOptimalSelection() {
        Incident[] incidents = {
            new Incident(1, "Disturbance", "Gate", 2, 3),
            new Incident(2, "Theft", "Library", 3, 4),
            new Incident(3, "Fire Alarm", "Hostel", 4, 5),
            new Incident(4, "Vandalism", "Parking", 5, 6)
        };
        // Capacity 7: best is Theft(3,4) + Fire Alarm(4,5) = cost 7, severity 9
        ResponsePlan plan = selector.optimize(incidents, 7);
        assertEquals(9, plan.getTotalSeverity());
        assertEquals(7, plan.getTotalCost());
        assertEquals(2, plan.getCount());
    }

    @Test
    void singleIncidentFits() {
        Incident[] incidents = {
            new Incident(1, "Solo", "Quad", 5, 10)
        };
        ResponsePlan plan = selector.optimize(incidents, 5);
        assertEquals(1, plan.getCount());
        assertEquals(10, plan.getTotalSeverity());
    }

    @Test
    void noneFitWithinCapacity() {
        Incident[] incidents = {
            new Incident(1, "Major", "Field", 100, 90),
            new Incident(2, "Major2", "Pool", 200, 95)
        };
        ResponsePlan plan = selector.optimize(incidents, 50);
        assertEquals(0, plan.getCount());
        assertEquals(0, plan.getTotalSeverity());
    }

    @Test
    void allFitWithinCapacity() {
        Incident[] incidents = {
            new Incident(1, "A", "Z1", 2, 4),
            new Incident(2, "B", "Z2", 3, 5)
        };
        ResponsePlan plan = selector.optimize(incidents, 10);
        assertEquals(2, plan.getCount());
        assertEquals(9, plan.getTotalSeverity());
        assertEquals(5, plan.getTotalCost());
    }

    @Test
    void reconstructionOrderIsOriginal() {
        Incident[] incidents = {
            new Incident(1, "First", "A", 2, 3),
            new Incident(2, "Second", "B", 3, 4)
        };
        ResponsePlan plan = selector.optimize(incidents, 5);
        assertEquals("First", plan.getSelectedIncidents()[0].getDescription());
        assertEquals("Second", plan.getSelectedIncidents()[1].getDescription());
    }

    @Test
    void invalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> selector.optimize(null, 10));
        assertThrows(IllegalArgumentException.class, () -> selector.optimize(new Incident[0], -1));
    }
}
