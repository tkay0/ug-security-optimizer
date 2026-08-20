package org.ugoptimizer.frontend;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.app.BackendContext;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.RequestStatusHistory;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.OptimizationComparison;
import org.ugoptimizer.result.RequestOptimizationCandidate;
import org.ugoptimizer.result.RequestOptimizationResult;
import org.ugoptimizer.result.TraversalResult;

class BackendFrontendServicesTest {

  @TempDir java.nio.file.Path directory;

  private BackendContext backend;
  private BackendFrontendServices frontend;

  @BeforeEach
  void setUp() throws Exception {
    backend =
        BackendContext.initializeWithDataset(directory.resolve("frontend.db"), java.nio.file.Path.of("data"));
    frontend = BackendFrontendServices.from(backend);
  }

  @Test
  void convertsBackendArraysToImmutableFrontendLists() {
    assertEquals(50, frontend.locations().findAllLocations().size());
    assertEquals(100, frontend.locations().findAllRoads().size());
    assertEquals(300, frontend.requests().findAll().size());
    assertEquals(30, frontend.resources().findAll().size());
    assertEquals(60, frontend.workflow().findAuditLog().size());
    assertEquals(30, frontend.reports().findAll().size());

    List<Location> locations = frontend.locations().findAllLocations();
    assertThrows(UnsupportedOperationException.class, () -> locations.add(locations.get(0)));
  }

  @Test
  void reservesIdsAndPersistsAllCreateOperationsUsedByTheUi() {
    BackendFrontendServices secondAdapter = BackendFrontendServices.from(backend);
    int locationId = frontend.locations().nextLocationId();
    int separatelyReservedLocationId = secondAdapter.locations().nextLocationId();
    assertEquals(51, locationId);
    assertEquals(52, separatelyReservedLocationId);

    Location location =
        new Location(locationId, "Frontend Post", "Legon", "SECURITY_POST", 1, 2, null, "N/A");
    assertEquals(location, frontend.locations().addLocation(location));

    int roadId = frontend.locations().nextRoadId();
    Road road =
        new Road(roadId, 1, locationId, 0.5, 2.0, 1.0, "Frontend Link", "ACCESS_ROAD", "LOW", false);
    assertEquals(road, frontend.locations().addRoad(road));

    int resourceId = frontend.resources().nextResourceId();
    Resource resource =
        new Resource(resourceId, "PATROL_OFFICER", locationId, 1, "AVAILABLE", locationId, null, null);
    assertEquals(resource, frontend.resources().add(resource));

    int requestId = frontend.requests().nextRequestId();
    Instant submitted = Instant.parse("2026-08-17T12:00:00Z");
    ServiceRequest request =
        new ServiceRequest(
            requestId,
            1,
            locationId,
            "ACCESS_CONTROL",
            3,
            submitted,
            submitted.plus(1, ChronoUnit.HOURS),
            "PENDING",
            "PATROL_OFFICER",
            "Frontend-created request");
    assertEquals(request, frontend.requests().add(request));

    assertEquals(51, frontend.locations().findAllLocations().size());
    assertEquals(101, frontend.locations().findAllRoads().size());
    assertEquals(31, frontend.resources().findAll().size());
    assertEquals(301, frontend.requests().findAll().size());
  }

  @Test
  void routesStatusChangesThroughAssignmentWorkflowAndUndoServices() throws Exception {
    int requestId = frontend.requests().nextRequestId();
    Instant submitted = Instant.parse("2026-08-17T13:00:00Z");
    frontend
        .requests()
        .add(
            new ServiceRequest(
                requestId,
                1,
                2,
                "NIGHT_PATROL_REQUEST",
                5,
                submitted,
                submitted.plus(1, ChronoUnit.HOURS),
                "PENDING",
                "MOTORCYCLE_PATROL",
                "Adapter workflow test"));

    assertTrue(frontend.priority().priorityOrder().stream().anyMatch(r -> r.getRequestId() == requestId));
    assertEquals("ASSIGNED", frontend.requests().updateStatus(requestId, "ASSIGNED").getStatus());
    assertTrue(backend.getAssignmentService().findActiveByRequestId(requestId).isPresent());
    assertEquals("IN_PROGRESS", frontend.requests().updateStatus(requestId, "IN_PROGRESS").getStatus());

    RequestStatusHistory[] history = backend.getUndoService().getHistory(requestId);
    assertEquals(RequestStatusHistory.ASSIGNMENT, history[0].getChangeType());
    assertEquals(RequestStatusHistory.STATUS_CHANGE, history[1].getChangeType());
    assertEquals("ASSIGNED", frontend.requests().updateStatus(requestId, "ASSIGNED").getStatus());
    assertEquals(RequestStatusHistory.UNDO,
        backend.getUndoService().getHistory(requestId)[2].getChangeType());
  }

  @Test
  void delegatesRoutingAuditAndAlgorithmRunsToCanonicalServices() throws Exception {
    TraversalResult bfs = frontend.routes().bfs(1);
    TraversalResult dfs = frontend.routes().dfs(1);
    PathResult path = frontend.routes().shortestPath(1, 2);
    assertArrayEquals(backend.getRouteService().bfs(1).getVisitOrder(), bfs.getVisitOrder());
    assertArrayEquals(backend.getRouteService().dfs(1).getVisitOrder(), dfs.getVisitOrder());
    PathResult backendPath = backend.getRouteService().shortestPath(1, 2);
    assertEquals(backendPath.getStatus(), path.getStatus());
    assertArrayEquals(backendPath.getVertexIds(), path.getVertexIds());
    assertEquals(backendPath.getTotalWeight(), path.getTotalWeight());

    int auditCount = frontend.workflow().findAuditLog().size();
    AuditEvent event = frontend.workflow().logEvent("GUI_VIEWED", 1, "Opened request details");
    assertEquals("SERVICE_REQUEST", event.getEntityType());
    assertEquals(auditCount + 1, frontend.workflow().findAuditLog().size());

    AlgorithmRun first = frontend.reports().record(measuredRun(1, "MergeSort", 100, 1));
    AlgorithmRun second = frontend.reports().record(measuredRun(1, "BFS", 50, 1));
    assertEquals(31, first.getRunId());
    assertEquals(1, first.getRunNumber());
    assertEquals(32, second.getRunId());
    assertEquals(2, second.getRunNumber());
    assertEquals(32, frontend.reports().findAll().size());
  }

  @Test
  void delegatesOptimizationAndResourceRecommendationToBackendServices() {
    ServiceRequest first = frontend.requests().findAll().get(3);
    ServiceRequest second = frontend.requests().findAll().get(11);
    List<RequestOptimizationCandidate> candidates = List.of(
        new RequestOptimizationCandidate(first, 3, first.getUrgency()),
        new RequestOptimizationCandidate(second, 2, second.getUrgency()));

    RequestOptimizationResult dp = frontend.optimization().runDynamicProgramming(candidates);
    RequestOptimizationResult brute = frontend.optimization().runBruteForce(candidates);
    OptimizationComparison comparison = frontend.optimization().compare(candidates);
    assertEquals("DYNAMIC_PROGRAMMING", dp.getAlgorithm());
    assertEquals("BRUTE_FORCE", brute.getAlgorithm());
    assertTrue(comparison.hasSameOptimum());
    assertEquals(80, dp.getObjective().getCapacity());

    assertNotNull(frontend.optimization().recommendResource(first.getRequestId()));
    assertTrue(frontend.optimization().pendingRequestCandidates().size() <= 24);
  }

  @Test
  void obtainsSystemReportThroughTheFrontendReportAdapter() throws Exception {
    assertEquals(
        backend.getReportService().generateSystemReport().getTotalRequests(),
        frontend.reports().generateSystemReport().getTotalRequests());
    assertEquals(
        backend.getReportService().generateSystemReport().getAuditEventCount(),
        frontend.reports().generateSystemReport().getAuditEventCount());
  }

  @Test
  void optimizationSwingScreenContainsNoDirectAlgorithmOrSyntheticMetricCalls() throws Exception {
    String source = java.nio.file.Files.readString(
        java.nio.file.Path.of("src/main/java/org/ugoptimizer/ui/menu/OptimizationMenu.java"));
    assertFalse(source.contains("GreedyAssignment"));
    assertFalse(source.contains("DynamicProgrammingIncidentSelector"));
    assertFalse(source.contains("BruteForceIncidentSelector"));
    assertFalse(source.contains("PlaceholderResponseMetrics"));
    assertTrue(source.contains("OptimizationService"));
  }

  @Test
  void translatesSqlFailuresButPreservesDomainValidationErrors() {
    Location duplicate = new Location(1, "Duplicate", "Legon", "OTHER", 0, 0, null, "N/A");
    FrontendServiceException persistenceFailure =
        assertThrows(FrontendServiceException.class, () -> frontend.locations().addLocation(duplicate));
    assertInstanceOf(SQLException.class, persistenceFailure.getCause());

    int roadId = frontend.locations().nextRoadId();
    Road invalidRoad =
        new Road(roadId, 1, 9999, 1.0, 1.0, 1.0, "Missing endpoint", null, null, false);
    assertThrows(IllegalArgumentException.class, () -> frontend.locations().addRoad(invalidRoad));
  }

  private static AlgorithmRun measuredRun(
      int requestedId, String algorithm, int inputSize, int requestedRunNumber) {
    return new AlgorithmRun(
        requestedId,
        algorithm,
        inputSize,
        100L,
        2.0,
        Instant.parse("2026-08-17T14:00:00Z"),
        "MEASURED",
        "GUI_MANUAL_RUN",
        requestedRunNumber);
  }
}
