package org.ugoptimizer.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.service.GreedyAssignmentService;

/** Tests greedy selection against the canonical immutable domain models. */
class GreedyAssignmentTest {

    private static final Instant SUBMITTED = Instant.parse("2026-08-06T06:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-06T06:30:00Z");

    @Test
    void selectsFastestEligibleCandidate() {
        AssignmentCandidate slow = candidate(1, "AMBULANCE", "AVAILABLE", 7.0d, 1);
        AssignmentCandidate fastest = candidate(2, "AMBULANCE", "AVAILABLE", 4.0d, 5);
        AssignmentCandidate medium = candidate(3, "AMBULANCE", "AVAILABLE", 6.0d, 0);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {slow, fastest, medium});

        assertSame(fastest, selected);
    }

    @Test
    void filtersBusyResourceEvenWhenItWouldOtherwiseWin() {
        AssignmentCandidate busy = candidate(1, "AMBULANCE", "BUSY", 1.0d, 0);
        AssignmentCandidate available = candidate(2, "AMBULANCE", "AVAILABLE", 8.0d, 3);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {busy, available});

        assertSame(available, selected);
        assertEquals("BUSY", busy.getResource().getAvailabilityStatus());
        assertEquals("AVAILABLE", available.getResource().getAvailabilityStatus());
    }

    @Test
    void filtersWrongResourceType() {
        AssignmentCandidate wrong = candidate(
                1, "FIRE_RESPONSE_UNIT", "AVAILABLE", 1.0d, 0);
        AssignmentCandidate matching = candidate(
                2, "AMBULANCE", "AVAILABLE", 9.0d, 2);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {wrong, matching});

        assertSame(matching, selected);
    }

    @Test
    void fallbackMappingCoversEveryCanonicalCategory() {
        String[][] mappings = {
            {"ACCESS_CONTROL", "PATROL_OFFICER"},
            {"CCTV_FAULT", "CCTV_TECHNICIAN"},
            {"CROWD_CONTROL", "CROWD_CONTROL_TEAM"},
            {"EMERGENCY_TRANSPORT", "RAPID_RESPONSE_TEAM"},
            {"FIRE_ALARM", "FIRE_RESPONSE_UNIT"},
            {"MEDICAL_EMERGENCY", "AMBULANCE"},
            {"NIGHT_PATROL_REQUEST", "MOTORCYCLE_PATROL"},
            {"ROAD_OBSTRUCTION", "PATROL_VEHICLE"},
            {"SECURITY_ESCORT", "PATROL_OFFICER"},
            {"SUSPICIOUS_ACTIVITY", "PATROL_OFFICER"},
            {"THEFT_REPORT", "INVESTIGATION_TEAM"},
            {"WELFARE_CHECK", "PATROL_OFFICER"}
        };

        for (int index = 0; index < mappings.length; index++) {
            String category = mappings[index][0];
            String resourceType = mappings[index][1];
            AssignmentCandidate matching = candidate(
                    index + 1, resourceType, "AVAILABLE", 10.0d, 0);
            AssignmentCandidate decoy = candidate(
                    index + 101, "FIRST_AID_TEAM", "AVAILABLE", 1.0d, 0);

            AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                    request(category, null), new AssignmentCandidate[] {decoy, matching});

            assertSame(matching, selected, "Incorrect fallback for " + category);
        }
    }

    @Test
    void explicitRequiredResourceTypeIsAuthoritative() {
        AssignmentCandidate ambulance = candidate(
                1, "AMBULANCE", "AVAILABLE", 1.0d, 0);
        AssignmentCandidate officer = candidate(
                2, "PATROL_OFFICER", "AVAILABLE", 5.0d, 0);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "PATROL_OFFICER"),
                new AssignmentCandidate[] {ambulance, officer});

        assertSame(officer, selected);
    }

    @Test
    void breaksResponseTimeTieByWorkload() {
        AssignmentCandidate busy = candidate(1, "AMBULANCE", "AVAILABLE", 5.0d, 4);
        AssignmentCandidate lessBusy = candidate(2, "AMBULANCE", "AVAILABLE", 5.0d, 2);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {busy, lessBusy});

        assertSame(lessBusy, selected);
    }

    @Test
    void breaksResponseAndWorkloadTieByNumericResourceId() {
        AssignmentCandidate higherId = candidate(
                8, "AMBULANCE", "AVAILABLE", 5.0d, 2);
        AssignmentCandidate lowerId = candidate(
                3, "AMBULANCE", "AVAILABLE", 5.0d, 2);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {higherId, lowerId});

        assertSame(lowerId, selected);
    }

    @Test
    void numericIdTwoRanksBeforeTen() {
        AssignmentCandidate ten = candidate(10, "AMBULANCE", "AVAILABLE", 5.0d, 2);
        AssignmentCandidate two = candidate(2, "AMBULANCE", "AVAILABLE", 5.0d, 2);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {ten, two});

        assertSame(two, selected);
    }

    @Test
    void returnsNullForEmptyCandidateArray() {
        assertNull(GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[0]));
    }

    @Test
    void returnsNullForNullRequest() {
        AssignmentCandidate candidate = candidate(
                1, "AMBULANCE", "AVAILABLE", 5.0d, 0);
        assertNull(GreedyAssignment.assignBestResource(
                null, new AssignmentCandidate[] {candidate}));
    }

    @Test
    void returnsNullForNullCandidateArray() {
        assertNull(GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"), null));
    }

    @Test
    void safelySkipsNullCandidateEntries() {
        AssignmentCandidate valid = candidate(2, "AMBULANCE", "AVAILABLE", 5.0d, 0);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {null, valid, null});

        assertSame(valid, selected);
    }

    @Test
    void returnsSingleEligibleCandidate() {
        AssignmentCandidate only = candidate(1, "AMBULANCE", "AVAILABLE", 5.0d, 0);

        assertSame(only, GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {only}));
    }

    @Test
    void multipleCandidatesRemainDeterministic() {
        AssignmentCandidate first = candidate(1, "AMBULANCE", "AVAILABLE", 9.0d, 0);
        AssignmentCandidate expected = candidate(2, "AMBULANCE", "AVAILABLE", 2.0d, 4);
        AssignmentCandidate last = candidate(3, "AMBULANCE", "AVAILABLE", 6.0d, 1);
        AssignmentCandidate[] candidates = {first, expected, last};

        assertSame(expected, GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"), candidates));
        assertSame(expected, GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"), candidates));
    }

    @Test
    void returnsNullWhenAllCandidatesAreIncompatible() {
        AssignmentCandidate fire = candidate(
                1, "FIRE_RESPONSE_UNIT", "AVAILABLE", 1.0d, 0);
        AssignmentCandidate officer = candidate(
                2, "PATROL_OFFICER", "AVAILABLE", 2.0d, 0);

        assertNull(GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {fire, officer}));
    }

    @Test
    void candidateRejectsNullResource() {
        assertThrows(NullPointerException.class,
                () -> new AssignmentCandidate(null, 1.0d, 0));
    }

    @Test
    void candidateRejectsNegativeResponseTime() {
        Resource resource = resource(1, "AMBULANCE", "AVAILABLE");
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentCandidate(resource, -0.01d, 0));
    }

    @Test
    void candidateRejectsNonFiniteResponseTime() {
        Resource resource = resource(1, "AMBULANCE", "AVAILABLE");
        double[] invalidValues = {
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };

        for (double invalidValue : invalidValues) {
            assertThrows(IllegalArgumentException.class,
                    () -> new AssignmentCandidate(resource, invalidValue, 0));
        }
    }

    @Test
    void candidateRejectsNegativeWorkload() {
        Resource resource = resource(1, "AMBULANCE", "AVAILABLE");
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentCandidate(resource, 1.0d, -1));
    }

    @Test
    void exactDuplicateKeyRetainsFirstCandidateDeterministically() {
        AssignmentCandidate first = new AssignmentCandidate(
                resource(5, "AMBULANCE", "AVAILABLE"), 4.0d, 1);
        AssignmentCandidate duplicate = new AssignmentCandidate(
                new Resource(5, "AMBULANCE", 2, 2, "AVAILABLE", 2, null, null),
                4.0d,
                1);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {first, duplicate});

        assertSame(first, selected);
    }

    @Test
    void coreDoesNotMutateCanonicalResource() {
        Resource resource = resource(1, "AMBULANCE", "AVAILABLE");
        Resource snapshot = resource(1, "AMBULANCE", "AVAILABLE");
        AssignmentCandidate candidate = new AssignmentCandidate(resource, 4.0d, 2);

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {candidate});

        assertSame(candidate, selected);
        assertSame(resource, selected.getResource());
        assertEquals(snapshot, resource);
        assertEquals("AVAILABLE", resource.getAvailabilityStatus());
        assertEquals(4.0d, candidate.getResponseTime());
        assertEquals(2, candidate.getCurrentWorkload());
    }

    @Test
    void serviceDelegatesWithoutMutatingCanonicalResource() {
        Resource resource = resource(2, "AMBULANCE", "AVAILABLE");
        Resource snapshot = resource(2, "AMBULANCE", "AVAILABLE");
        AssignmentCandidate expected = new AssignmentCandidate(resource, 3.0d, 1);
        AssignmentCandidate slower = candidate(3, "AMBULANCE", "AVAILABLE", 8.0d, 0);

        AssignmentCandidate selected = new GreedyAssignmentService().assign(
                request("MEDICAL_EMERGENCY", "AMBULANCE"),
                new AssignmentCandidate[] {slower, expected});

        assertSame(expected, selected);
        assertEquals(snapshot, resource);
        assertEquals("AVAILABLE", resource.getAvailabilityStatus());
        assertEquals("AVAILABLE", slower.getResource().getAvailabilityStatus());
    }

    @Test
    void serviceHandlesNullInputsByDelegation() {
        GreedyAssignmentService service = new GreedyAssignmentService();
        AssignmentCandidate candidate = candidate(
                1, "AMBULANCE", "AVAILABLE", 5.0d, 0);

        assertNull(service.assign(null, new AssignmentCandidate[] {candidate}));
        assertNull(service.assign(
                request("MEDICAL_EMERGENCY", "AMBULANCE"), null));
        assertEquals("AVAILABLE", candidate.getResource().getAvailabilityStatus());
    }

    @Test
    void largeCandidateArrayUsesSameSelectionPolicy() {
        AssignmentCandidate[] candidates = new AssignmentCandidate[1_000];
        for (int index = 0; index < candidates.length; index++) {
            candidates[index] = candidate(
                    index + 1, "AMBULANCE", "AVAILABLE", 1_000.0d - index, index % 5);
        }

        AssignmentCandidate selected = GreedyAssignment.assignBestResource(
                request("MEDICAL_EMERGENCY", "AMBULANCE"), candidates);

        assertSame(candidates[999], selected);
        assertEquals(1_000, selected.getResource().getResourceId());
    }

    private static AssignmentCandidate candidate(
            int resourceId,
            String resourceType,
            String availabilityStatus,
            double responseTime,
            int workload) {
        return new AssignmentCandidate(
                resource(resourceId, resourceType, availabilityStatus),
                responseTime,
                workload);
    }

    private static Resource resource(
            int resourceId, String resourceType, String availabilityStatus) {
        return new Resource(
                resourceId,
                resourceType,
                1,
                1,
                availabilityStatus,
                1,
                null,
                null);
    }

    private static ServiceRequest request(String category, String requiredResourceType) {
        return new ServiceRequest(
                1,
                1,
                2,
                category,
                3,
                SUBMITTED,
                DEADLINE,
                "PENDING",
                requiredResourceType,
                "Compatibility test request");
    }
}
