package org.ugoptimizer.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.UiOption;

class OperationalUsabilityTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void readableCodesPreserveTheirStoredValues() {
        UiOption<String> category = UiFormat.codeOption("MEDICAL_EMERGENCY");
        UiOption<String> response = UiFormat.codeOption("FIRST_AID_TEAM");
        UiOption<String> status = UiFormat.codeOption("IN_PROGRESS");

        assertEquals("Medical Emergency", category.label());
        assertEquals("MEDICAL_EMERGENCY", category.value());
        assertEquals("First Aid Team", response.label());
        assertEquals("FIRST_AID_TEAM", response.value());
        assertEquals("In Progress", status.label());
        assertEquals("IN_PROGRESS", status.value());
    }

    @Test
    void readableUrgencyLabelsPreserveIntegerValues() {
        assertEquals("1 - Low", UiFormat.urgencyOption(1).label());
        assertEquals(1, UiFormat.urgencyOption(1).value());
        assertEquals("5 - Critical", UiFormat.urgencyOption(5).label());
        assertEquals(5, UiFormat.urgencyOption(5).value());
    }

    @Test
    void incidentNamedLocationsMapToCanonicalNumericIds() {
        Instant submitted = Instant.parse("2026-08-25T18:00:00Z");
        ServiceRequest request = IncidentMenu.buildRequest(
                301,
                new UiOption<>(29, "Commonwealth Hall"),
                new UiOption<>(45, "University Hospital"),
                UiFormat.codeOption("MEDICAL_EMERGENCY"),
                UiFormat.urgencyOption(5),
                UiFormat.codeOption("AMBULANCE"),
                "Medical response requested",
                submitted);

        assertEquals(29, request.getSourceLocationId());
        assertEquals(45, request.getDestinationLocationId());
        assertEquals("MEDICAL_EMERGENCY", request.getCategory());
        assertEquals(5, request.getUrgency());
        assertEquals("AMBULANCE", request.getRequiredResourceType());
        assertEquals("PENDING", request.getStatus());
    }

    @Test
    void resourceNamedHomeLocationMapsToCanonicalNumericId() {
        Resource resource = ResourceMenu.buildResource(
                31,
                UiFormat.codeOption("PATROL_OFFICER"),
                new UiOption<>(7, "Main University Gate"),
                2,
                UiFormat.codeOption("AVAILABLE"));

        assertEquals(7, resource.getHomeLocationId());
        assertEquals("PATROL_OFFICER", resource.getResourceType());
        assertEquals("AVAILABLE", resource.getAvailabilityStatus());
        assertEquals(2, resource.getCapacity());
    }

    @Test
    void incidentFilteringUsesLoadedDataWithoutMutatingIt() {
        List<Location> locations = locations();
        List<ServiceRequest> requests = requests();

        List<ServiceRequest> byLocation = IncidentMenu.filterIncidents(
                requests, "University Hospital", null, null, locations);
        List<ServiceRequest> byStatusAndUrgency = IncidentMenu.filterIncidents(
                requests, "", "PENDING", 5, locations);

        assertEquals(List.of(requests.get(0)), byLocation);
        assertEquals(List.of(requests.get(0)), byStatusAndUrgency);
        assertEquals(2, requests.size());
        assertEquals("PENDING", requests.get(0).getStatus());
    }

    @Test
    void dispatchNextActionMatchesCanonicalWorkflowLifecycle() {
        assertEquals("ASSIGNED", DispatchWorkflowMenu.nextStatus("PENDING"));
        assertEquals("IN_PROGRESS", DispatchWorkflowMenu.nextStatus("ASSIGNED"));
        assertEquals("COMPLETED", DispatchWorkflowMenu.nextStatus("IN_PROGRESS"));
        assertNull(DispatchWorkflowMenu.nextStatus("COMPLETED"));
        assertNull(DispatchWorkflowMenu.nextStatus("CANCELLED"));
    }

    @Test
    void operationalSummaryContainsOnlySupportedStatusTotals() {
        List<Resource> resources = List.of(
                new Resource(1, "AMBULANCE", 29, 2, "AVAILABLE", 29, null, null),
                new Resource(2, "PATROL_OFFICER", 7, 1, "BUSY", 7, null, null));

        String summary = OperationalReportMenu.formatSummary(requests(), resources);

        assertTrue(summary.contains("Total: 2"));
        assertTrue(summary.contains("Active: 1"));
        assertTrue(summary.contains("Pending: 1"));
        assertTrue(summary.contains("Completed: 1"));
        assertTrue(summary.contains("Critical (active urgency 5): 1"));
        assertTrue(summary.contains("Available: 1"));
        assertTrue(summary.contains("Busy: 1"));
    }

    private static List<Location> locations() {
        return List.of(
                new Location(7, "Main University Gate", "Legon", "GATE", 7, 7,
                        "Open", "test source"),
                new Location(29, "Commonwealth Hall", "Legon", "HALL", 29, 29,
                        "Open", "test source"),
                new Location(45, "University Hospital", "Legon", "HOSPITAL", 45, 45,
                        "Open", "test source"));
    }

    private static List<ServiceRequest> requests() {
        Instant submitted = Instant.parse("2026-08-25T18:00:00Z");
        return List.of(
                new ServiceRequest(301, 29, 45, "MEDICAL_EMERGENCY", 5,
                        submitted, submitted.plusSeconds(3600), "PENDING", "AMBULANCE",
                        "Medical response at Commonwealth Hall"),
                new ServiceRequest(302, 7, 29, "SECURITY_ESCORT", 2,
                        submitted, submitted.plusSeconds(7200), "COMPLETED", "PATROL_OFFICER",
                        "Escort completed"));
    }
}
