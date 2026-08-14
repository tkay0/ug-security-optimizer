package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;

class RouteServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void delegatesShortestPathAndTraversalsOverPersistedCampusGraph() throws Exception {
        RouteService service = new RouteService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "routes.db"));

        PathResult route = service.findShortestRoute(1, 2);
        assertEquals(PathResult.Status.FOUND, route.getStatus());
        assertEquals(1, route.getVertexIds()[0]);
        assertEquals(2, route.getVertexIds()[route.getVertexCount() - 1]);
        assertTrue(route.getTotalWeight().orElseThrow() > 0.0d);

        TraversalResult bfs = service.breadthFirstTraversal(1);
        TraversalResult dfs = service.depthFirstTraversal(1);
        assertTrue(bfs.containsVertex(1));
        assertTrue(dfs.containsVertex(1));
        assertEquals(service.loadCampusGraph().getVertexCount(), bfs.getTotalVertexCount());
    }

    @Test
    void reportsMissingPositiveEndpointAndRejectsInvalidIds() throws Exception {
        RouteService service = new RouteService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "missing-route.db"));

        assertEquals(
                PathResult.Status.MISSING_DESTINATION,
                service.shortestPath(1, 999).getStatus());
        assertEquals(
                TraversalResult.Status.MISSING_START,
                service.bfs(999).getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.findShortestRoute(0, 1));
        assertThrows(IllegalArgumentException.class, () -> service.dfs(-1));
    }

    @Test
    void reportsUnreachableRouteFromDisconnectedPersistedLocations() throws Exception {
        DatabaseManager manager = ServiceTestDatabase.createEmpty(
                temporaryDirectory, "unreachable.db");
        insertDisconnectedLocations(manager);

        PathResult route = new RouteService(manager).findShortestRoute(1, 2);

        assertEquals(PathResult.Status.UNREACHABLE, route.getStatus());
    }

    private static void insertDisconnectedLocations(DatabaseManager manager) throws Exception {
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO locations VALUES "
                            + "(1, 'A', 'TEST', 'GATE', 0, 0, NULL, 'https://example.com/a')");
            statement.executeUpdate(
                    "INSERT INTO locations VALUES "
                            + "(2, 'B', 'TEST', 'GATE', 10, 0, NULL, 'https://example.com/b')");
        }
    }
}
