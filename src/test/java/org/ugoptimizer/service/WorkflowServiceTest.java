package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.AuditEventDao;
import org.ugoptimizer.database.dao.WorkflowDao;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.ServiceRequest;

class WorkflowServiceTest {

    private static final Instant SUBMITTED = Instant.parse("2026-08-14T10:00:00Z");
    private static final Instant TRANSITION_TIME = Instant.parse("2026-08-14T10:05:00Z");

    @TempDir
    Path temporaryDirectory;

    private RequestService requestService;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager manager = ServiceTestDatabase.createSeeded(
                temporaryDirectory, "workflow.db");
        requestService = new RequestService(manager);
        workflowService = new WorkflowService(
                requestService,
                new WorkflowDao(manager),
                new AuditEventDao(manager),
                Clock.fixed(TRANSITION_TIME, ZoneOffset.UTC));
    }

    @Test
    void exposesCanonicalTransitionRules() {
        assertTrue(workflowService.canTransition("PENDING", "ASSIGNED"));
        assertTrue(workflowService.canTransition("ASSIGNED", "IN_PROGRESS"));
        assertTrue(workflowService.canTransition("IN_PROGRESS", "COMPLETED"));
        assertTrue(workflowService.canTransition("PENDING", "CANCELLED"));
        assertFalse(workflowService.canTransition("COMPLETED", "PENDING"));
        assertFalse(workflowService.canTransition("CANCELLED", "ASSIGNED"));
        assertArrayEquals(
                new String[]{"ASSIGNED", "CANCELLED"},
                workflowService.getAllowedTransitions("PENDING"));
        assertEquals(0, workflowService.getAllowedTransitions("COMPLETED").length);
    }

    @Test
    void persistsValidLifecycleAndAtomicAuditHistory() throws Exception {
        createPendingRequest(301);
        assertEquals(60, workflowService.getAuditLog().length);

        assertEquals("ASSIGNED",
                workflowService.markAssigned(301, "DISPATCH_OPERATOR").getStatus());
        assertEquals("IN_PROGRESS",
                workflowService.startWork(301, "DISPATCH_OPERATOR").getStatus());
        assertEquals("COMPLETED",
                workflowService.completeRequest(301, "DISPATCH_OPERATOR").getStatus());

        AuditEvent[] history = workflowService.getRequestHistory(301);
        assertEquals(3, history.length);
        assertEquals("REQUEST_STATUS_CHANGED", history[0].getEventType());
        assertEquals(TRANSITION_TIME, history[0].getTimestamp());
        assertEquals("DISPATCH_OPERATOR", history[0].getActorType());
        assertTrue(history[2].getDetails().contains("IN_PROGRESS to COMPLETED"));
        assertEquals(63, workflowService.getAuditLog().length);
    }

    @Test
    void rejectsSkippingStagesAndLeavesPersistenceUnchanged() throws Exception {
        createPendingRequest(301);

        assertThrows(IllegalStateException.class,
                () -> workflowService.completeRequest(301, "DISPATCH_OPERATOR"));

        assertEquals("PENDING", requestService.requireRequest(301).getStatus());
        assertEquals(0, workflowService.getRequestHistory(301).length);
    }

    @Test
    void completedAndCancelledRequestsAreTerminal() throws Exception {
        createPendingRequest(301);
        workflowService.cancelRequest(301, "DISPATCH_OPERATOR");
        assertThrows(IllegalStateException.class,
                () -> workflowService.markAssigned(301, "DISPATCH_OPERATOR"));

        createPendingRequest(302);
        workflowService.markAssigned(302, "DISPATCH_OPERATOR");
        workflowService.startWork(302, "DISPATCH_OPERATOR");
        workflowService.completeRequest(302, "DISPATCH_OPERATOR");
        assertThrows(IllegalStateException.class,
                () -> workflowService.cancelRequest(302, "DISPATCH_OPERATOR"));
    }

    @Test
    void rejectsUnknownStatusAndBlankActor() throws Exception {
        createPendingRequest(301);
        assertThrows(IllegalArgumentException.class,
                () -> workflowService.transitionStatus(301, "RESOLVED", "OPERATOR"));
        assertThrows(IllegalArgumentException.class,
                () -> workflowService.transitionStatus(301, "ASSIGNED", " "));
    }

    private void createPendingRequest(int requestId) throws Exception {
        ServiceRequest request = new ServiceRequest(
                requestId, 1, 2, "SECURITY_ESCORT", 3,
                SUBMITTED, SUBMITTED.plusSeconds(3600), "PENDING",
                "PATROL_OFFICER", "Workflow test request");
        requestService.createRequest(request);
    }
}
