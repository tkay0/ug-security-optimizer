package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.AuditEvent;

class InMemoryWorkflowServiceTest {

    @Test
    void logEventAssignsIncrementingEventIds() {
        InMemoryWorkflowService service = new InMemoryWorkflowService();

        AuditEvent first = service.logEvent("STATUS_CHANGE", 1, "first");
        AuditEvent second = service.logEvent("STATUS_CHANGE", 1, "second");

        assertNotEquals(first.getEventId(), second.getEventId());
        assertEquals(first.getEventId() + 1, second.getEventId());
    }

    @Test
    void findAuditLogReturnsEveryLoggedEventInOrder() {
        InMemoryWorkflowService service = new InMemoryWorkflowService();

        service.logEvent("STATUS_CHANGE", 1, "a");
        service.logEvent("CANCELLATION", 2, "b");
        service.logEvent("UNDO", 1, "c");

        assertEquals(3, service.findAuditLog().size());
        assertEquals("STATUS_CHANGE", service.findAuditLog().get(0).getEventType());
        assertEquals("CANCELLATION", service.findAuditLog().get(1).getEventType());
        assertEquals("UNDO", service.findAuditLog().get(2).getEventType());
    }
}
