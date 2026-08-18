package org.ugoptimizer.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.app.BackendContext;
import org.ugoptimizer.app.Main;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.RequestStatusHistory;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.LabelCount;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.SystemReport;

class BackendWorkflowEndToEndTest {

  @TempDir Path directory;

  @Test
  void dispatchWorkflowPersistsAcrossPublicServicesAndApplicationRestart() throws Exception {
    Path database = directory.resolve("workflow.db");
    AtomicReference<BackendContext> handoff = new AtomicReference<>();
    Main.start(database, Path.of("data"), handoff::set);
    BackendContext backend = handoff.get();
    assertNotNull(backend);

    SystemReport before = backend.getReportService().generateSystemReport();
    Instant submitted = Instant.parse("2026-08-17T10:00:00Z");
    ServiceRequest created =
        backend
            .getRequestService()
            .createRequest(
                new ServiceRequest(
                    301,
                    1,
                    2,
                    "NIGHT_PATROL_REQUEST",
                    4,
                    submitted,
                    submitted.plus(2, ChronoUnit.HOURS),
                    "PENDING",
                    "MOTORCYCLE_PATROL",
                    "End-to-end dispatch verification"));
    assertEquals("PENDING", created.getStatus());

    AssignmentCandidate recommendation =
        backend.getAssignmentService().recommendResource(created.getRequestId());
    Resource recommendedResource = recommendation.getResource();
    int resourceLocation =
        recommendedResource.getCurrentLocationId() == null
            ? recommendedResource.getHomeLocationId()
            : recommendedResource.getCurrentLocationId();
    PathResult route =
        backend
            .getRouteService()
            .findShortestRoute(resourceLocation, created.getSourceLocationId());
    assertTrue(route.isReachable());
    assertTrue(route.getVertexCount() >= 1);

    Assignment assigned =
        backend.getAssignmentService().assignBestResource(created.getRequestId(), "E2E_TEST");
    assertEquals(recommendedResource.getResourceId(), assigned.getResourceId());
    assertTrue(assigned.isActive());
    assertEquals(
        "ASSIGNED", backend.getRequestService().requireRequest(created.getRequestId()).getStatus());
    assertEquals(
        "BUSY",
        backend
            .getResourceService()
            .requireResource(assigned.getResourceId())
            .getAvailabilityStatus());
    assertEquals(
        assigned,
        backend.getAssignmentService().findActiveByRequestId(created.getRequestId()).orElseThrow());

    assertEquals(
        "IN_PROGRESS",
        backend
            .getWorkflowService()
            .startWork(created.getRequestId(), "E2E_TEST")
            .getStatus());
    assertEquals(
        "COMPLETED",
        backend
            .getWorkflowService()
            .completeRequest(created.getRequestId(), "E2E_TEST")
            .getStatus());

    RequestStatusHistory[] history = backend.getUndoService().getHistory(created.getRequestId());
    assertEquals(3, history.length);
    assertEquals(RequestStatusHistory.ASSIGNMENT, history[0].getChangeType());
    assertEquals("IN_PROGRESS", history[1].getNewStatus());
    assertEquals("COMPLETED", history[2].getNewStatus());

    AuditEvent[] requestAudit =
        backend.getAuditService().getEntityHistory("SERVICE_REQUEST", created.getRequestId());
    assertEquals(3, requestAudit.length);
    assertTrue(hasEvent(requestAudit, "REQUEST_ASSIGNED"));
    assertTrue(hasEvent(requestAudit, "REQUEST_STATUS_CHANGED"));

    Assignment persisted = backend.getAssignmentService().getRequestAssignments(301)[0];
    assertEquals(Assignment.RELEASED, persisted.getStatus());
    assertNotNull(persisted.getReleasedAt());
    assertTrue(backend.getAssignmentService().findActiveByRequestId(301).isEmpty());
    assertEquals(
        "AVAILABLE",
        backend
            .getResourceService()
            .requireResource(assigned.getResourceId())
            .getAvailabilityStatus());

    SystemReport after = backend.getReportService().generateSystemReport();
    assertEquals(before.getTotalRequests() + 1, after.getTotalRequests());
    assertEquals(
        count(before.getRequestsByStatus(), "COMPLETED") + 1,
        count(after.getRequestsByStatus(), "COMPLETED"));
    assertEquals(before.getAuditEventCount() + 5, after.getAuditEventCount());
    assertEquals(0, after.getActiveAssignmentCount());

    AtomicReference<BackendContext> reopened = new AtomicReference<>();
    Main.start(database, Path.of("data"), reopened::set);
    assertEquals(
        "COMPLETED", reopened.get().getRequestService().requireRequest(301).getStatus());
    assertEquals(301, reopened.get().getReportService().generateSystemReport().getTotalRequests());
  }

  @Test
  void startupFailureDoesNotInvokeFrontendHandoff() {
    AtomicReference<BackendContext> handoff = new AtomicReference<>();

    assertThrows(
        IOException.class,
        () ->
            Main.start(
                directory.resolve("failed-startup.db"),
                directory.resolve("missing-dataset"),
                handoff::set));
    assertNull(handoff.get());
  }

  private static boolean hasEvent(AuditEvent[] events, String eventType) {
    for (AuditEvent event : events) {
      if (eventType.equals(event.getEventType())) return true;
    }
    return false;
  }

  private static int count(LabelCount[] counts, String label) {
    for (LabelCount count : counts) {
      if (label.equals(count.getLabel())) return count.getCount();
    }
    throw new AssertionError("Missing report label: " + label);
  }
}
