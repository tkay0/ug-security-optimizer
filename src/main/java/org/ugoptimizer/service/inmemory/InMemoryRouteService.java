package org.ugoptimizer.service.inmemory;

import java.util.Objects;
import org.ugoptimizer.algorithms.shortestpath.Dijkstra;
import org.ugoptimizer.algorithms.traversal.BreadthFirstSearch;
import org.ugoptimizer.algorithms.traversal.DepthFirstSearch;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

/**
 * In-memory {@link RouteService} that builds its graph on every call from an
 * injected {@link LocationService}, the same way {@code RoutingMenu} used to
 * build it inline from {@code LocationRoadMenu}. Blocked roads are excluded,
 * so results always reflect whatever locations/roads currently exist.
 * Replace with a real implementation backed by {@code DatabaseGraphLoader}
 * once the database team's work lands.
 */
public final class InMemoryRouteService implements RouteService {

    private final LocationService locationService;

    public InMemoryRouteService(LocationService locationService) {
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");
    }

    @Override
    public TraversalResult bfs(int startLocationId) {
        return new BreadthFirstSearch().traverse(buildGraph(), startLocationId);
    }

    @Override
    public TraversalResult dfs(int startLocationId) {
        return new DepthFirstSearch().traverse(buildGraph(), startLocationId);
    }

    @Override
    public PathResult shortestPath(int sourceLocationId, int destinationLocationId) {
        return new Dijkstra().shortestPath(buildGraph(), sourceLocationId, destinationLocationId);
    }

    private WeightedGraph buildGraph() {
        WeightedGraph graph = new AdjacencyListGraph();
        for (Location location : locationService.findAllLocations()) {
            graph.addVertex(location.getLocationId());
        }
        for (Road road : locationService.findAllRoads()) {
            if (!road.isBlocked()) {
                graph.addEdge(road.getFromLocationId(), road.getToLocationId(), road.getTravelTimeMin());
            }
        }
        return graph;
    }
}
