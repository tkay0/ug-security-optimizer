package org.ugoptimizer.algorithms;

import org.ugoptimizer.service.GreedyAssignmentService;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link GreedyAssignment}.
 *
 * <p>Covers the required scenarios: best resource selection, unavailable and
 * wrong-type filtering, every tie-break rule, all null/empty edge cases,
 * single and multiple candidates, and a large dataset simulation. Also covers
 * the {@link GreedyAssignmentService} facade.</p>
 */
class GreedyAssignmentTest {

    private static ServiceRequest medicalIncident() {
        return new ServiceRequest("INC001", ServiceRequest.TYPE_MEDICAL, "HIGH", "Hall 1", "OPEN", "2026-08-06T06:00:00Z");
    }

    private static Resource ambulance(String id, boolean available, int responseTime, int workload) {
        return new Resource(id, Resource.TYPE_AMBULANCE, "IDLE", "Central", available, responseTime, workload);
    }

    private static Resource fireUnit(String id, boolean available, int responseTime, int workload) {
        return new Resource(id, Resource.TYPE_FIRE_RESPONSE_UNIT, "IDLE", "Central", available, responseTime, workload);
    }

    @Test
    void assignBestResourceSelectsFastestAvailableResource() {
        Resource[] resources = {
                ambulance("AMB001", true, 7, 3),
                ambulance("AMB002", true, 4, 5),
                ambulance("AMB003", false, 2, 0)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
    }

    @Test
    void assignBestResourceIgnoresUnavailableResources() {
        Resource[] resources = {
                ambulance("AMB001", false, 2, 0),
                ambulance("AMB002", true, 9, 1),
                ambulance("AMB003", true, 5, 2)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB003", assigned.getId());
    }

    @Test
    void assignBestResourceIgnoresWrongResourceType() {
        Resource[] resources = {
                fireUnit("FIRE001", true, 1, 0),
                ambulance("AMB001", true, 8, 1)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB001", assigned.getId());
    }

    @Test
    void assignBestResourceMatchesEveryCanonicalDatasetCategory() {
        String[][] canonicalMappings = {
                {"THEFT_REPORT", "INVESTIGATION_TEAM"},
                {"MEDICAL_EMERGENCY", "AMBULANCE"},
                {"WELFARE_CHECK", "PATROL_OFFICER"},
                {"NIGHT_PATROL_REQUEST", "MOTORCYCLE_PATROL"},
                {"CROWD_CONTROL", "CROWD_CONTROL_TEAM"},
                {"SUSPICIOUS_ACTIVITY", "PATROL_OFFICER"},
                {"ACCESS_CONTROL", "PATROL_OFFICER"},
                {"SECURITY_ESCORT", "PATROL_OFFICER"},
                {"ROAD_OBSTRUCTION", "PATROL_VEHICLE"},
                {"EMERGENCY_TRANSPORT", "RAPID_RESPONSE_TEAM"},
                {"CCTV_FAULT", "CCTV_TECHNICIAN"},
                {"FIRE_ALARM", "FIRE_RESPONSE_UNIT"}
        };
        for (String[] mapping : canonicalMappings) {
            String category = mapping[0];
            String resourceType = mapping[1];
            ServiceRequest request = new ServiceRequest("REQ-" + category, category, "HIGH", "Hall 1", "OPEN", "2026-08-06T06:00:00Z");
            Resource matching = new Resource("RES-" + resourceType, resourceType, "IDLE", "Central", true, 10, 0);
            Resource[] resources = {
                    matching,
                    new Resource("AID001", Resource.TYPE_FIRST_AID_TEAM, "IDLE", "Central", true, 1, 0)
            };
            Resource assigned = GreedyAssignment.assignBestResource(request, resources);
            assertNotNull(assigned, "No resource matched canonical category " + category);
            assertEquals("RES-" + resourceType, assigned.getId(),
                    "Category " + category + " must map to resource type " + resourceType);
        }
    }

    @Test
    void assignBestResourceBreaksResponseTimeTieByWorkload() {
        Resource[] resources = {
                ambulance("AMB001", true, 5, 4),
                ambulance("AMB002", true, 5, 2)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
    }

    @Test
    void assignBestResourceBreaksResponseAndWorkloadTieBySmallestId() {
        Resource[] resources = {
                ambulance("AMB010", true, 6, 3),
                ambulance("AMB002", true, 6, 3),
                ambulance("AMB005", true, 6, 3)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
    }

    @Test
    void assignBestResourceHandlesDuplicateResourceIdsWithoutCrashing() {
        Resource[] resources = {
                ambulance("AMB001", true, 4, 2),
                ambulance("AMB001", true, 4, 2)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB001", assigned.getId());
    }

    @Test
    void assignBestResourceReturnsNullWhenNoResourceIsAvailable() {
        Resource[] resources = {
                ambulance("AMB001", false, 2, 0),
                ambulance("AMB002", false, 3, 1)
        };
        assertNull(GreedyAssignment.assignBestResource(medicalIncident(), resources));
    }

    @Test
    void assignBestResourceReturnsNullForEmptyArray() {
        assertNull(GreedyAssignment.assignBestResource(medicalIncident(), new Resource[0]));
    }

    @Test
    void assignBestResourceReturnsNullForNullIncident() {
        Resource[] resources = {ambulance("AMB001", true, 4, 1)};
        assertNull(GreedyAssignment.assignBestResource(null, resources));
    }

    @Test
    void assignBestResourceReturnsNullForNullArray() {
        assertNull(GreedyAssignment.assignBestResource(medicalIncident(), null));
    }

    @Test
    void assignBestResourceSkipsNullElementsInArraySafely() {
        Resource[] resources = {
                null,
                ambulance("AMB002", true, 5, 1),
                null
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
    }

    @Test
    void assignBestResourceWithSingleMatchingResourceReturnsIt() {
        Resource[] resources = {ambulance("AMB001", true, 6, 0)};
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB001", assigned.getId());
    }

    @Test
    void assignBestResourceWithSingleUnavailableResourceReturnsNull() {
        Resource[] resources = {ambulance("AMB001", false, 6, 0)};
        assertNull(GreedyAssignment.assignBestResource(medicalIncident(), resources));
    }

    @Test
    void assignBestResourceWithMultipleValidResourcesPicksFastest() {
        Resource[] resources = {
                ambulance("AMB001", true, 12, 0),
                ambulance("AMB002", true, 7, 4),
                ambulance("AMB003", true, 3, 2),
                ambulance("AMB004", true, 9, 1)
        };
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB003", assigned.getId());
    }

    @Test
    void assignBestResourceOnLargeDatasetSelectsDeterministically() {
        int size = 1000;
        Resource[] resources = new Resource[size];
        for (int i = 0; i < size; i++) {
            resources[i] = ambulance("AMB" + String.format("%04d", i), true, 1000 - i, i % 5);
        }
        Resource assigned = GreedyAssignment.assignBestResource(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB0999", assigned.getId());
    }

    @Test
    void serviceAssignsBestResource() {
        GreedyAssignmentService service = new GreedyAssignmentService();
        Resource[] resources = {
                ambulance("AMB001", true, 7, 3),
                ambulance("AMB002", true, 4, 5)
        };
        Resource assigned = service.assign(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
    }

    @Test
    void serviceHandlesNullInputsGracefully() {
        GreedyAssignmentService service = new GreedyAssignmentService();
        assertNull(service.assign(null, new Resource[0]));
        assertNull(service.assign(medicalIncident(), null));
    }

    @Test
    void serviceUpdatesAssignedResourceState() {
        GreedyAssignmentService service = new GreedyAssignmentService();
        Resource[] resources = {
                ambulance("AMB001", true, 7, 3),
                ambulance("AMB002", true, 4, 5)
        };
        Resource assigned = service.assign(medicalIncident(), resources);
        assertNotNull(assigned);
        assertEquals("AMB002", assigned.getId());
        assertFalse(assigned.isAvailable());
        assertEquals(6, assigned.getCurrentWorkload());
        assertEquals("DISPATCHED", assigned.getStatus());
    }
}
