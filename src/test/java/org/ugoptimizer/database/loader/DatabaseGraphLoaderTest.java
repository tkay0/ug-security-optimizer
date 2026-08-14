package org.ugoptimizer.database.loader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.RoadDao;
import org.ugoptimizer.database.importers.CsvDatasetImporter;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class DatabaseGraphLoaderTest {

    private static final Path CANONICAL_DATA_DIRECTORY = Path.of("data");

    @TempDir
    Path temporaryDirectory;

    @Test
    void canonicalReloadProducesEquivalentListAndMatrixGraphs() throws Exception {
        DatabaseManager manager = canonicalDatabase("equivalent.db");
        RoadDao roadDao = new RoadDao(manager);
        DatabaseGraphLoader loader = loader(manager);
        WeightedGraph listGraph = new AdjacencyListGraph();
        WeightedGraph matrixGraph = new AdjacencyMatrixGraph();

        loader.loadInto(listGraph);
        loader.loadInto(matrixGraph);

        assertEquals(50, listGraph.getVertexCount());
        assertEquals(50, matrixGraph.getVertexCount());
        assertEquals(100, listGraph.getEdgeCount());
        assertEquals(100, matrixGraph.getEdgeCount());
        assertArrayEquals(listGraph.getVertexIds(), matrixGraph.getVertexIds());
        assertArrayEquals(listGraph.getEdges(), matrixGraph.getEdges());

        Road road = roadDao.findById(1).orElseThrow();
        double expectedCost = road.getTravelTimeMin() * road.getConditionWeight();
        assertEquals(
                expectedCost,
                listGraph.getEdgeWeight(
                        road.getFromLocationId(), road.getToLocationId()).orElseThrow());
        assertEquals(
                expectedCost,
                matrixGraph.getEdgeWeight(
                        road.getFromLocationId(), road.getToLocationId()).orElseThrow());
        assertTrue(listGraph.containsEdge(1, 2));
        assertArrayEquals(listGraph.getNeighborIds(1), matrixGraph.getNeighborIds(1));
    }

    @Test
    void reloadWorksAfterDatabaseIsClosedAndReopened() throws Exception {
        DatabaseManager originalManager = canonicalDatabase("reopened.db");
        Path databasePath = originalManager.getDatabasePath();
        DatabaseManager reopenedManager = new DatabaseManager(databasePath);
        WeightedGraph graph = new AdjacencyListGraph();

        loader(reopenedManager).loadInto(graph);

        assertEquals(50, graph.getVertexCount());
        assertEquals(100, graph.getEdgeCount());
    }

    @Test
    void loadsNonContiguousIdsAndKeepsIsolatedVertex() throws Exception {
        DatabaseManager manager = initializedDatabase("non-contiguous.db");
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO locations VALUES "
                            + "(10, 'Ten', 'Test', 'GATE', -4, 2, NULL, 'https://example.edu/10'),"
                            + "(30, 'Thirty', 'Test', 'SECURITY', 0, 0, NULL, 'https://example.edu/30'),"
                            + "(99, 'Ninety Nine', 'Test', 'HALL', 8, -3, NULL, 'https://example.edu/99')");
            statement.executeUpdate(
                    "INSERT INTO roads VALUES "
                            + "(1, 10, 99, 1.0, 2.0, 1.5, 'Ten - Ninety Nine', "
                            + "NULL, NULL, 0)");
        }
        WeightedGraph graph = new AdjacencyMatrixGraph();

        loader(manager).loadInto(graph);

        assertArrayEquals(new int[] {10, 30, 99}, graph.getVertexIds());
        assertEquals(3, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertEquals(3.0d, graph.getEdgeWeight(10, 99).orElseThrow());
        assertEquals(0, graph.getDegree(30).orElseThrow());
    }

    @Test
    void missingRoadEndpointFailsBeforeGraphMutation() throws Exception {
        DatabaseManager manager = canonicalDatabase("missing-endpoint.db");
        executeWithoutForeignKeys(
                manager, "UPDATE roads SET to_location_id = 999 WHERE road_id = 1");
        WeightedGraph graph = new AdjacencyListGraph();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> loader(manager).loadInto(graph));

        assertTrue(exception.getMessage().contains("Road 1"));
        assertTrue(exception.getMessage().contains("1 -> 999"));
        assertTrue(graph.isEmpty());
    }

    @Test
    void nonFiniteDerivedCostFailsBeforeGraphMutation() throws Exception {
        DatabaseManager manager = canonicalDatabase("invalid-cost.db");
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            assertEquals(
                    1,
                    statement.executeUpdate(
                            "UPDATE roads SET travel_time_min = 1.0e308, "
                                    + "condition_weight = 2.0 WHERE road_id = 1"));
        }
        WeightedGraph graph = new AdjacencyMatrixGraph();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> loader(manager).loadInto(graph));

        assertTrue(exception.getMessage().contains("Road 1"));
        assertTrue(exception.getMessage().contains("invalid routing cost"));
        assertTrue(graph.isEmpty());
    }

    @Test
    void blockedBaselineRoadIsExcluded() throws Exception {
        DatabaseManager manager = canonicalDatabase("blocked.db");
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            assertEquals(
                    1,
                    statement.executeUpdate(
                            "UPDATE roads SET is_blocked = 1 WHERE road_id = 1"));
        }
        WeightedGraph graph = new AdjacencyListGraph();

        loader(manager).loadInto(graph);

        assertEquals(50, graph.getVertexCount());
        assertEquals(99, graph.getEdgeCount());
        assertFalse(graph.containsEdge(1, 2));
    }

    @Test
    void loadingDoesNotModifyDatabaseRecords() throws Exception {
        DatabaseManager manager = canonicalDatabase("unchanged.db");
        LocationDao locationDao = new LocationDao(manager);
        RoadDao roadDao = new RoadDao(manager);
        Location[] locationsBefore = locationDao.findAll();
        Road[] roadsBefore = roadDao.findAll();

        new DatabaseGraphLoader(locationDao, roadDao).loadInto(new AdjacencyListGraph());

        assertArrayEquals(locationsBefore, locationDao.findAll());
        assertArrayEquals(roadsBefore, roadDao.findAll());
    }

    @Test
    void nullOrNonEmptyTargetGraphIsRejected() throws Exception {
        DatabaseManager manager = canonicalDatabase("invalid-target.db");
        DatabaseGraphLoader loader = loader(manager);
        WeightedGraph nonEmptyGraph = new AdjacencyListGraph();
        nonEmptyGraph.addVertex(999);

        assertThrows(NullPointerException.class, () -> loader.loadInto(null));
        assertThrows(IllegalArgumentException.class, () -> loader.loadInto(nonEmptyGraph));
        assertArrayEquals(new int[] {999}, nonEmptyGraph.getVertexIds());
    }

    private DatabaseManager canonicalDatabase(String fileName) throws Exception {
        DatabaseManager manager = initializedDatabase(fileName);
        new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();
        return manager;
    }

    private DatabaseManager initializedDatabase(String fileName) throws Exception {
        DatabaseManager manager = new DatabaseManager(temporaryDirectory.resolve(fileName));
        manager.initializeSchema();
        return manager;
    }

    private static DatabaseGraphLoader loader(DatabaseManager manager) {
        return new DatabaseGraphLoader(new LocationDao(manager), new RoadDao(manager));
    }

    private static void executeWithoutForeignKeys(DatabaseManager manager, String sql)
            throws Exception {
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            assertEquals(1, statement.executeUpdate(sql));
        }
    }
}
