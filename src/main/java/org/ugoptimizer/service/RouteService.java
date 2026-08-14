package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.Objects;
import org.ugoptimizer.algorithms.shortestpath.Dijkstra;
import org.ugoptimizer.algorithms.traversal.BreadthFirstSearch;
import org.ugoptimizer.algorithms.traversal.DepthFirstSearch;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.RoadDao;
import org.ugoptimizer.database.loader.DatabaseGraphLoader;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

/** Loads the persisted campus graph and delegates routing to reusable algorithms. */
public final class RouteService {

    private final DatabaseGraphLoader graphLoader;
    private final Dijkstra dijkstra;
    private final BreadthFirstSearch breadthFirstSearch;
    private final DepthFirstSearch depthFirstSearch;

    public RouteService(DatabaseManager databaseManager) {
        this(new DatabaseGraphLoader(
                new LocationDao(Objects.requireNonNull(
                        databaseManager, "databaseManager cannot be null")),
                new RoadDao(databaseManager)));
    }

    public RouteService(DatabaseGraphLoader graphLoader) {
        this.graphLoader = Objects.requireNonNull(graphLoader, "graphLoader cannot be null");
        this.dijkstra = new Dijkstra();
        this.breadthFirstSearch = new BreadthFirstSearch();
        this.depthFirstSearch = new DepthFirstSearch();
    }

    /**
     * Finds the minimum routing-cost path. Database graph weights are
     * {@code travelTimeMin * conditionWeight}; blocked roads are excluded.
     */
    public PathResult findShortestRoute(int startLocationId, int destinationLocationId)
            throws SQLException {
        requirePositiveLocationId(startLocationId, "startLocationId");
        requirePositiveLocationId(destinationLocationId, "destinationLocationId");
        return dijkstra.shortestPath(loadCampusGraph(), startLocationId, destinationLocationId);
    }

    /** Compatibility-friendly name for future UI integration. */
    public PathResult shortestPath(int startLocationId, int destinationLocationId)
            throws SQLException {
        return findShortestRoute(startLocationId, destinationLocationId);
    }

    public TraversalResult breadthFirstTraversal(int startLocationId) throws SQLException {
        requirePositiveLocationId(startLocationId, "startLocationId");
        return breadthFirstSearch.traverse(loadCampusGraph(), startLocationId);
    }

    public TraversalResult depthFirstTraversal(int startLocationId) throws SQLException {
        requirePositiveLocationId(startLocationId, "startLocationId");
        return depthFirstSearch.traverse(loadCampusGraph(), startLocationId);
    }

    /** Compatibility-friendly aliases for the current frontend vocabulary. */
    public TraversalResult bfs(int startLocationId) throws SQLException {
        return breadthFirstTraversal(startLocationId);
    }

    public TraversalResult dfs(int startLocationId) throws SQLException {
        return depthFirstTraversal(startLocationId);
    }

    public WeightedGraph loadCampusGraph() throws SQLException {
        WeightedGraph graph = new AdjacencyListGraph();
        graphLoader.loadInto(graph);
        return graph;
    }

    private static void requirePositiveLocationId(int locationId, String fieldName) {
        if (locationId <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
