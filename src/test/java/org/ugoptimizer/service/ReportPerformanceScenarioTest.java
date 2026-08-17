package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.ugoptimizer.app.BackendContext;
import org.ugoptimizer.database.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.result.*;
import org.ugoptimizer.structures.graph.*;

class ReportPerformanceScenarioTest {
  @TempDir Path dir;
  DatabaseManager manager;

  @BeforeEach
  void setup() throws Exception {
    manager = ServiceTestDatabase.createSeeded(dir, "reports.db");
  }

  @Test
  void reportAggregatesCanonicalData() throws Exception {
    SystemReport r = new ReportService(manager).generateSystemReport();
    assertEquals(300, r.getTotalRequests());
    assertEquals(30, r.getTotalResources());
    assertEquals(50, r.getLocationCount());
    assertEquals(100, r.getRoadCount());
    assertEquals(60, r.getAuditEventCount());
    assertEquals(30, r.getAlgorithmRunCount());
  }

  @Test
  void recordsRealTimingAndRetrievesRun() throws Exception {
    PerformanceService p = new PerformanceService(manager);
    PerformanceMeasurement<Integer> m =
        p.measureAndRecord(
            1,
            "BFS",
            50,
            () -> {
              int sum = 0;
              for (int i = 0; i < 100; i++) sum += i;
              return sum;
            });
    assertEquals(4950, m.getResult());
    assertEquals("MEASURED", m.getAlgorithmRun().getStatus());
    assertNotNull(m.getAlgorithmRun().getTimeNs());
    assertEquals(1, p.getRunsByStatus("MEASURED").length);
    assertEquals(30, p.getAllRuns().length);
  }

  @Test
  void validatesPlannedRunMetadataBeforeExecutingOrOverwriting() throws Exception {
    PerformanceService performance = new PerformanceService(manager);
    AtomicBoolean executed = new AtomicBoolean();

    assertThrows(
        IllegalArgumentException.class,
        () -> performance.measureAndRecord(1, "DIJKSTRA", 50, () -> executed.getAndSet(true)));
    assertThrows(
        IllegalArgumentException.class,
        () -> performance.measureAndRecord(1, "BFS", 100, () -> executed.getAndSet(true)));
    assertThrows(
        IllegalArgumentException.class,
        () -> performance.measureAndRecord(999, "BFS", 50, () -> executed.getAndSet(true)));
    assertFalse(executed.get());
    assertEquals("PLANNED", performance.requireRun(1).getStatus());

    assertThrows(
        Exception.class,
        () ->
            performance.measureAndRecord(
                1,
                "BFS",
                50,
                () -> {
                  throw new Exception("operation failed");
                }));
    assertEquals("PLANNED", performance.requireRun(1).getStatus());

    performance.measureAndRecord(1, "BFS", 50, () -> 1);
    assertThrows(
        IllegalStateException.class,
        () -> performance.measureAndRecord(1, "BFS", 50, () -> executed.getAndSet(true)));
    assertFalse(executed.get());
  }

  @Test
  void scenarioLoaderBlocksOverridesWithoutBreakingBaseline() throws Exception {
    RouteService routes = new RouteService(manager);
    Road road = new LocationService(manager).requireRoad(6);
    WeightedGraph baseline = routes.loadCampusGraph();
    WeightedGraph scenario = routes.loadCampusGraph("ACCESS_BLOCKAGE_DRILL");
    assertTrue(baseline.containsEdge(road.getFromLocationId(), road.getToLocationId()));
    assertFalse(scenario.containsEdge(road.getFromLocationId(), road.getToLocationId()));
    assertTrue(routes.findShortestRoute(1, 2).isReachable());
    assertEquals(3, routes.getRoadScenarioNames().length);
    assertThrows(IllegalArgumentException.class, () -> routes.loadCampusGraph("UNKNOWN"));
  }

  @Test
  void backendContextBootstrapsSeededServicesWithoutExposingDaos() throws Exception {
    BackendContext context =
        BackendContext.initializeWithDataset(dir.resolve("bootstrap.db"), Path.of("data"));
    assertEquals(300, context.getRequestService().getAllRequests().length);
    assertNotNull(context.getAssignmentService());
    assertNotNull(context.getReportService());
  }
}
