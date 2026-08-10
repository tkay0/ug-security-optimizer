package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.AuditEvent;

class AuditEventDaoTest {

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager manager;
    private AuditEventDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = DaoTestDatabase.create(temporaryDirectory, "audit.db");
        dao = new AuditEventDao(manager);
    }

    @Test
    void readsKnownSeededEvent() throws Exception {
        AuditEvent event = dao.findById(1).orElseThrow();

        assertEquals("REQUEST_CREATED", event.getEventType());
        assertEquals(Instant.parse("2026-08-01T07:37:00Z"), event.getTimestamp());
        assertEquals("SERVICE_REQUEST", event.getEntityType());
        assertEquals(18, event.getEntityId());
        assertEquals("DISPATCH_OPERATOR", event.getActorType());
    }

    @Test
    void validEventInsertionPersists() throws Exception {
        AuditEvent event = event(61, "RESOURCE", 1, "2026-08-10T12:00:00Z");

        dao.insert(event);

        assertEquals(event, dao.findById(61).orElseThrow());
    }

    @Test
    void missingTargetFailsAndTransactionLeavesNoEvent() throws Exception {
        int countBefore = auditCount();
        AuditEvent event = event(61, "RESOURCE", 999, "2026-08-10T12:00:00Z");

        SQLException exception = assertThrows(SQLException.class, () -> dao.insert(event));

        assertTrue(exception.getMessage().contains("Failed to insert audit event 61"));
        assertEquals(countBefore, auditCount());
        assertTrue(dao.findById(61).isEmpty());
    }

    @Test
    void unsupportedEntityTypeIsRejectedBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> dao.findByEntity("LOCATION", 1));
    }

    @Test
    void historyContainsOnlyRequestedEntityInTimestampThenIdOrder() throws Exception {
        dao.insert(event(61, "ROAD", 1, "2026-08-10T12:02:00Z"));
        dao.insert(event(62, "ROAD", 1, "2026-08-10T12:01:00Z"));
        dao.insert(event(63, "ROAD", 2, "2026-08-10T12:00:00Z"));

        AuditEvent[] history = dao.findByEntity("ROAD", 1);

        assertEquals(2, history.length);
        assertEquals(62, history[0].getEventId());
        assertEquals(61, history[1].getEventId());
        assertEquals(1, history[0].getEntityId());
        assertEquals(1, history[1].getEntityId());
    }

    @Test
    void missingAndInvalidIdsFollowPolicy() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(0));
        assertThrows(IllegalArgumentException.class,
                () -> dao.findByEntity("ROAD", 0));
    }

    private AuditEvent event(int id, String entityType, int entityId, String timestamp) {
        return new AuditEvent(
                id, "CHECKPOINT_TEST", Instant.parse(timestamp), entityType, entityId,
                "TEST", "DAO checkpoint test event");
    }

    private int auditCount() throws Exception {
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM audit_events")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
