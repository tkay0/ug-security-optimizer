package org.ugoptimizer.ui.menu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

class OperationalDisplayTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void assignmentOutcomeUsesPersistedAssignmentAndRequestData() {
        ServiceRequest pending = request("PENDING");
        ServiceRequest assigned = request("ASSIGNED");
        Assignment assignment = new Assignment(
                7, 21, 12, Instant.parse("2026-08-21T12:00:00Z"), null, "ACTIVE", 8.25);

        String display = DispatchWorkflowMenu.formatOutcome(
                pending, assigned, Optional.of(assignment), locations());

        assertTrue(display.contains("Incident #21 moved from Pending to Assigned"));
        assertTrue(display.contains("Assignment #7 was created"));
        assertTrue(display.contains("Selected resource: #12"));
        assertTrue(display.contains("8.25 minutes"));
        assertTrue(display.contains("Incident location: Balme Library"));
        assertTrue(display.contains("Response destination: Legon Hall"));
        assertTrue(display.contains("Assignment status: Active"));
    }

    @Test
    void recommendationDisplayClearlyStatesThatItIsNotPersisted() {
        ServiceRequest request = request("PENDING");
        Resource resource = new Resource(
                12, "MEDICAL_TEAM", 3, 2, "AVAILABLE", 4, null, null);

        String display = OptimizationMenu.formatRecommendation(
                request, new AssignmentCandidate(resource, 6.5, 0));

        assertTrue(display.contains("Recommendation only — no assignment has been persisted"));
        assertTrue(display.contains("Selected resource: 12 (MEDICAL_TEAM)"));
        assertTrue(display.contains("Resource location: 4"));
        assertTrue(display.contains("Current resource state: AVAILABLE"));
        assertTrue(display.contains("6.50 minutes"));
    }

    private static ServiceRequest request(String status) {
        Instant submitted = Instant.parse("2026-08-21T11:00:00Z");
        return new ServiceRequest(
                21,
                1,
                9,
                "MEDICAL_EMERGENCY",
                5,
                submitted,
                submitted.plusSeconds(3600),
                status,
                "MEDICAL_TEAM",
                "Medical response requested");
    }

    private static List<Location> locations() {
        return List.of(
                new Location(1, "Balme Library", "Legon", "LIBRARY", 1, 1,
                        "Open", "test source"),
                new Location(9, "Legon Hall", "Legon", "HALL", 9, 9,
                        "Open", "test source"));
    }
}
