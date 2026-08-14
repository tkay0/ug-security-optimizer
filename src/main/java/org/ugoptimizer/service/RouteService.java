package org.ugoptimizer.service;

import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;

/**
 * Runs campus routing traversals over the current location/road graph. A real
 * implementation builds its graph the way {@code DatabaseGraphLoader} already
 * does for persisted data; {@code InMemoryRouteService} builds it from an
 * injected {@link LocationService} instead.
 */
public interface RouteService {

    TraversalResult bfs(int startLocationId);

    TraversalResult dfs(int startLocationId);

    /** Minimum-weight path using {@code Dijkstra}; edge weight is road travel time. */
    PathResult shortestPath(int sourceLocationId, int destinationLocationId);
}
