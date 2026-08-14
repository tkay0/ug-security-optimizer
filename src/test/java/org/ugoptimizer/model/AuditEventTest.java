package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEventTest {

    private static final Instant EVENT_TIME = Instant.parse("2026-08-01T07:37:00Z");

    @Test
    void createsAuditEventFromCanonicalDatasetValues() {
        AuditEvent event = new AuditEvent(
                1, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 18,
                "DISPATCH_OPERATOR", "Request 18 created.");

        assertEquals(1, event.getEventId());
        assertEquals("REQUEST_CREATED", event.getEventType());
        assertEquals(EVENT_TIME, event.getTimestamp());
        assertEquals("SERVICE_REQUEST", event.getEntityType());
        assertEquals(18, event.getEntityId());
        assertEquals("DISPATCH_OPERATOR", event.getActorType());
        assertEquals("Request 18 created.", event.getDetails());
    }

    @Test
    void permitsNullableActorAndDetails() {
        AuditEvent event = new AuditEvent(
                1, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 18, null, null);

        assertNull(event.getActorType());
        assertNull(event.getDetails());
    }

    @Test
    void rejectsInvalidEventAndEntityIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditEvent(
                        0, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 18, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditEvent(
                        1, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 0, null, null));
    }

    @Test
    void rejectsUnsupportedEntityType() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditEvent(
                        1, "REQUEST_CREATED", EVENT_TIME, "LOCATION", 18, null, null));
    }

    @Test
    void equalEventsHaveEqualHashCodes() {
        AuditEvent first = new AuditEvent(
                1, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 18, null, null);
        AuditEvent second = new AuditEvent(
                1, "REQUEST_CREATED", EVENT_TIME, "SERVICE_REQUEST", 18, null, null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
