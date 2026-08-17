package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.model.*;

class AssignmentUndoServiceTest {
  @TempDir Path dir;
  DatabaseManager manager;
  RequestService requests;
  ResourceService resources;
  AssignmentService assignments;
  WorkflowService workflow;
  UndoService undo;

  @BeforeEach
  void setup() throws Exception {
    manager = ServiceTestDatabase.createSeeded(dir, "assignment.db");
    requests = new RequestService(manager);
    resources = new ResourceService(manager);
    assignments = new AssignmentService(manager);
    workflow = new WorkflowService(manager);
    undo = new UndoService(manager);
  }

  @Test
  void assignmentPersistsAndChangesBothStates() throws Exception {
    create(301);
    Assignment a = assignments.assignResource(301, 13, "OPERATOR");
    assertTrue(a.isActive());
    assertEquals("ASSIGNED", requests.requireRequest(301).getStatus());
    assertEquals("BUSY", resources.requireResource(13).getAvailabilityStatus());
    assertEquals(a, assignments.findActiveByRequestId(301).orElseThrow());
  }

  @Test
  void rejectsUnavailableWrongCapabilityDuplicateAndTerminal() throws Exception {
    create(301);
    assertThrows(
        IllegalArgumentException.class, () -> assignments.assignResource(301, 1, "OPERATOR"));
    assertThrows(
        IllegalStateException.class, () -> assignments.assignResource(301, 14, "OPERATOR"));
    assignments.assignResource(301, 13, "OPERATOR");
    create(302);
    assertThrows(
        IllegalStateException.class, () -> assignments.assignResource(302, 13, "OPERATOR"));
    assertThrows(IllegalStateException.class, () -> assignments.assignBestResource(2, "OPERATOR"));
    workflow.cancelRequest(302, "OPERATOR");
    assertThrows(
        IllegalStateException.class, () -> assignments.assignBestResource(302, "OPERATOR"));
  }

  @Test
  void completionAndCancellationReleaseResource() throws Exception {
    create(301);
    assignments.assignResource(301, 13, "OPERATOR");
    workflow.startWork(301, "OPERATOR");
    workflow.completeRequest(301, "OPERATOR");
    assertEquals("AVAILABLE", resources.requireResource(13).getAvailabilityStatus());
    assertTrue(assignments.findActiveByRequestId(301).isEmpty());
    create(302);
    assignments.assignResource(302, 13, "OPERATOR");
    workflow.cancelRequest(302, "OPERATOR");
    assertEquals("AVAILABLE", resources.requireResource(13).getAvailabilityStatus());
  }

  @Test
  void undoAssignmentRestoresStateAppendsHistoryAndAudit() throws Exception {
    create(301);
    assignments.assignResource(301, 13, "OPERATOR");
    int before = new AuditService(manager).getAuditLog().length;
    ServiceRequest restored = undo.undoLatest(301, "SUPERVISOR");
    assertEquals("PENDING", restored.getStatus());
    assertEquals("AVAILABLE", resources.requireResource(13).getAvailabilityStatus());
    assertEquals(2, undo.getHistory(301).length);
    assertEquals(RequestStatusHistory.UNDO, undo.getHistory(301)[1].getChangeType());
    assertTrue(new AuditService(manager).getAuditLog().length > before);
  }

  @Test
  void undoWithoutHistoryFails() throws Exception {
    create(301);
    assertThrows(IllegalStateException.class, () -> undo.undoLatest(301, "OPERATOR"));
  }

  @Test
  void undoCompletionReactivatesAssignmentAndResource() throws Exception {
    create(301);
    assignments.assignResource(301, 13, "OPERATOR");
    workflow.startWork(301, "OPERATOR");
    workflow.completeRequest(301, "OPERATOR");
    undo.undoLatest(301, "SUPERVISOR");
    assertEquals("IN_PROGRESS", requests.requireRequest(301).getStatus());
    assertEquals("BUSY", resources.requireResource(13).getAvailabilityStatus());
    assertTrue(assignments.findActiveByRequestId(301).isPresent());
  }

  private void create(int id) throws Exception {
    Instant t = Instant.parse("2026-08-14T10:00:00Z");
    requests.createRequest(
        new ServiceRequest(
            id,
            1,
            2,
            "NIGHT_PATROL_REQUEST",
            3,
            t,
            t.plusSeconds(3600),
            "PENDING",
            "MOTORCYCLE_PATROL",
            "test"));
  }
}
