package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.ServiceRequest;

class ServiceRequestDaoTest {

    private static final Instant SUBMITTED = Instant.parse("2026-08-10T10:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-10T11:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager manager;
    private ServiceRequestDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = DaoTestDatabase.create(temporaryDirectory, "requests.db");
        dao = new ServiceRequestDao(manager);
    }

    @Test
    void readsKnownRequestAndAllRequestsInIdOrder() throws Exception {
        ServiceRequest request = dao.findById(1).orElseThrow();
        assertEquals("THEFT_REPORT", request.getCategory());
        assertEquals(Instant.parse("2026-08-04T16:17:00Z"), request.getTimeSubmitted());
        assertEquals("IN_PROGRESS", request.getStatus());
        assertEquals("INVESTIGATION_TEAM", request.getRequiredResourceType());

        ServiceRequest[] requests = dao.findAll();
        assertEquals(300, requests.length);
        for (int index = 0; index < requests.length; index++) {
            assertEquals(index + 1, requests[index].getRequestId());
        }
    }

    @Test
    void missingAndInvalidIdsFollowPolicy() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(0));
    }

    @Test
    void insertsRequestAndReopenedDaoReadsPersistedNullableFields() throws Exception {
        ServiceRequest request = request(301, 1, 2, null, null);
        dao.insert(request);

        ServiceRequestDao reopenedDao = new ServiceRequestDao(
                new DatabaseManager(manager.getDatabasePath()));
        ServiceRequest stored = reopenedDao.findById(301).orElseThrow();
        assertEquals(request, stored);
        assertNull(stored.getRequiredResourceType());
        assertNull(stored.getDescription());
    }

    @Test
    void duplicatePrimaryKeyIsRejected() {
        assertThrows(SQLException.class, () -> dao.insert(request(1, 1, 2, null, null)));
    }

    @Test
    void invalidForeignKeyIsRejected() {
        ServiceRequest request = request(301, 999, 2, null, null);

        assertThrows(SQLException.class, () -> dao.insert(request));
    }

    @Test
    void rejectsUnknownRequiredResourceTypeWithoutInsertingButAcceptsKnownType()
            throws Exception {
        ServiceRequest invalid = request(301, 1, 2, "UNKNOWN_TEAM", "Invalid type test");

        SQLException exception = assertThrows(SQLException.class, () -> dao.insert(invalid));

        assertTrue(exception.getMessage().contains("UNKNOWN_TEAM"));
        assertTrue(dao.findById(301).isEmpty());

        ServiceRequest valid = request(302, 1, 2, "PATROL_OFFICER", "Known type test");
        dao.insert(valid);
        assertEquals(valid, dao.findById(302).orElseThrow());
    }

    @Test
    void updateStatusPersists() throws Exception {
        assertTrue(dao.updateStatus(1, "COMPLETED"));
        assertEquals("COMPLETED", dao.findById(1).orElseThrow().getStatus());
    }

    @Test
    void missingUpdateReturnsFalseAndInvalidStatusIsRejected() throws Exception {
        assertFalse(dao.updateStatus(999, "COMPLETED"));
        assertThrows(IllegalArgumentException.class, () -> dao.updateStatus(1, "UNKNOWN"));
        assertThrows(NullPointerException.class, () -> dao.updateStatus(1, null));
    }

    private static ServiceRequest request(
            int id, int sourceId, int destinationId, String resourceType, String description) {
        return new ServiceRequest(
                id, sourceId, destinationId, "SECURITY_ESCORT", 3, SUBMITTED, DEADLINE,
                "PENDING", resourceType, description);
    }
}
