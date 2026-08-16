package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.service.LocationService;
import org.ugoptimizer.service.RouteService;

/**
 * Seeded graph: 1&lt;-&gt;2 unblocked (6.0 min), 2&lt;-&gt;3 unblocked (4.0 min),
 * 1&lt;-&gt;3 BLOCKED (3.0 min, excluded from routing).
 */
class InMemoryRouteServiceTest {

    private RouteService newRouteService() {
        LocationService locationService = new InMemoryLocationService();
        return new InMemoryRouteService(locationService);
    }

    @Test
    void bfsReachesAllLocationsViaTheUnblockedDetour() {
        TraversalResult result = newRouteService().bfs(1);

        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertEquals(3, result.getVisitedCount());
        assertTrue(result.containsVertex(3), "location 3 must be reachable via 1->2->3");
    }

    @Test
    void dfsAlsoReachesAllLocations() {
        TraversalResult result = newRouteService().dfs(1);

        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertEquals(3, result.getVisitedCount());
    }

    @Test
    void bfsFromMissingLocationReportsMissingStart() {
        TraversalResult result = newRouteService().bfs(9999);
        assertEquals(TraversalResult.Status.MISSING_START, result.getStatus());
    }

    @Test
    void shortestPathAvoidsTheBlockedDirectRoad() {
        PathResult result = newRouteService().shortestPath(1, 3);

        assertEquals(PathResult.Status.FOUND, result.getStatus());
        assertArrayEquals(new int[] {1, 2, 3}, result.getVertexIds());
        assertEquals(10.0d, result.getTotalWeight().getAsDouble(), 1.0e-9d);
    }

    @Test
    void shortestPathToSameVertexIsTrivial() {
        PathResult result = newRouteService().shortestPath(1, 1);

        assertEquals(PathResult.Status.FOUND, result.getStatus());
        assertEquals(1, result.getVertexCount());
        assertEquals(0.0d, result.getTotalWeight().getAsDouble(), 1.0e-9d);
    }

    @Test
    void shortestPathReportsMissingEndpoints() {
        RouteService routeService = newRouteService();

        assertEquals(PathResult.Status.MISSING_SOURCE, routeService.shortestPath(999, 1).getStatus());
        assertEquals(PathResult.Status.MISSING_DESTINATION, routeService.shortestPath(1, 999).getStatus());
        assertEquals(PathResult.Status.MISSING_BOTH, routeService.shortestPath(998, 999).getStatus());
    }
}
