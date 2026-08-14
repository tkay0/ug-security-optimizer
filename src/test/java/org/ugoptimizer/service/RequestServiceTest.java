package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.model.ServiceRequest;

class RequestServiceTest {

    private static final Instant SUBMITTED = Instant.parse("2026-08-14T10:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-14T11:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private RequestService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new RequestService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "requests.db"));
    }

    @Test
    void queriesRequestsByIdStatusAndCanonicalCategory() throws Exception {
        assertEquals(300, service.getAllRequests().length);
        assertEquals("THEFT_REPORT", service.requireRequest(1).getCategory());
        assertEquals(98, service.findByStatus("PENDING").length);
        assertEquals(28, service.findByCategory("MEDICAL_EMERGENCY").length);
        assertTrue(service.findRequestById(999).isEmpty());
    }

    @Test
    void createsPendingRequestAfterLocationAndDaoValidation() throws Exception {
        ServiceRequest request = request(301, 1, 2, "PENDING", "PATROL_OFFICER");

        assertEquals(request, service.createRequest(request));
        assertEquals(request, service.requireRequest(301));
    }

    @Test
    void rejectsInvalidCreationAndFilterInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request(301, 1, 2, "ASSIGNED", "PATROL_OFFICER")));
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request(302, 999, 2, "PENDING", "PATROL_OFFICER")));
        assertThrows(SQLException.class,
                () -> service.createRequest(request(303, 1, 2, "PENDING", "UNKNOWN_TEAM")));
        assertThrows(IllegalArgumentException.class, () -> service.findByStatus("RESOLVED"));
        assertThrows(IllegalArgumentException.class, () -> service.findByCategory("UNKNOWN"));
        assertThrows(NoSuchElementException.class, () -> service.requireRequest(999));
    }

    private static ServiceRequest request(
            int id, int sourceId, int destinationId, String status, String resourceType) {
        return new ServiceRequest(
                id, sourceId, destinationId, "SECURITY_ESCORT", 3,
                SUBMITTED, DEADLINE, status, resourceType, "Service-layer test request");
    }
}
