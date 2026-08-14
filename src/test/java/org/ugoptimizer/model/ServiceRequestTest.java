package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ServiceRequestTest {

    private static final Instant SUBMITTED = Instant.parse("2026-08-04T16:17:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-04T19:46:00Z");

    @Test
    void createsRequestFromCanonicalDatasetValues() {
        ServiceRequest request = canonicalRequest("INVESTIGATION_TEAM", "Theft report request.");

        assertEquals(1, request.getRequestId());
        assertEquals(1, request.getSourceLocationId());
        assertEquals(13, request.getDestinationLocationId());
        assertEquals("THEFT_REPORT", request.getCategory());
        assertEquals(2, request.getUrgency());
        assertEquals(SUBMITTED, request.getTimeSubmitted());
        assertEquals(DEADLINE, request.getDeadline());
        assertEquals("IN_PROGRESS", request.getStatus());
        assertEquals("INVESTIGATION_TEAM", request.getRequiredResourceType());
        assertEquals("Theft report request.", request.getDescription());
    }

    @Test
    void permitsNullableOptionalFields() {
        ServiceRequest request = canonicalRequest(null, null);

        assertNull(request.getRequiredResourceType());
        assertNull(request.getDescription());
    }

    @Test
    void rejectsUrgencyOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(0, 1, 13, SUBMITTED, DEADLINE));
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(6, 1, 13, SUBMITTED, DEADLINE));
    }

    @Test
    void rejectsEqualOrInvalidLocationIds() {
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(2, 1, 1, SUBMITTED, DEADLINE));
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(2, 0, 13, SUBMITTED, DEADLINE));
    }

    @Test
    void rejectsDeadlineAtOrBeforeSubmission() {
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(2, 1, 13, SUBMITTED, SUBMITTED));
        assertThrows(IllegalArgumentException.class,
                () -> requestWith(2, 1, 13, SUBMITTED, SUBMITTED.minusSeconds(1)));
    }

    @Test
    void rejectsUnsupportedCategoryOrStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceRequest(
                        1, 1, 13, "UNKNOWN", 2, SUBMITTED, DEADLINE,
                        "PENDING", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceRequest(
                        1, 1, 13, "THEFT_REPORT", 2, SUBMITTED, DEADLINE,
                        "UNKNOWN", null, null));
    }

    @Test
    void equalRequestsHaveEqualHashCodes() {
        ServiceRequest first = canonicalRequest(null, null);
        ServiceRequest second = canonicalRequest(null, null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static ServiceRequest canonicalRequest(String requiredType, String description) {
        return new ServiceRequest(
                1, 1, 13, "THEFT_REPORT", 2, SUBMITTED, DEADLINE,
                "IN_PROGRESS", requiredType, description);
    }

    private static ServiceRequest requestWith(
            int urgency, int sourceId, int destinationId, Instant submitted, Instant deadline) {
        return new ServiceRequest(
                1, sourceId, destinationId, "THEFT_REPORT", urgency, submitted, deadline,
                "PENDING", null, null);
    }
}
