package org.ugoptimizer.database.loader;

import java.sql.SQLException;
import java.util.Objects;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.RoadDao;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.structures.graph.WeightedGraph;
import org.ugoptimizer.structures.graph.WeightedGraph.EdgeUpdate;

/**
 * Reloads persisted campus locations and baseline roads into an empty weighted
 * graph without exposing JDBC to graph implementations or algorithms.
 *
 * <p>Every location becomes a vertex. Baseline roads marked as blocked are
 * excluded, while each unblocked road is added once as an undirected edge with
 * routing cost {@code travelTimeMin * conditionWeight}. Road scenarios are not
 * applied by this loader.</p>
 */
public final class DatabaseGraphLoader {

    private final LocationDao locationDao;
    private final RoadDao roadDao;

    public DatabaseGraphLoader(LocationDao locationDao, RoadDao roadDao) {
        this.locationDao = Objects.requireNonNull(locationDao, "locationDao cannot be null");
        this.roadDao = Objects.requireNonNull(roadDao, "roadDao cannot be null");
    }

    /**
     * Loads the complete baseline graph into an empty graph instance.
     *
     * <p>All database rows and derived costs are validated before the supplied
     * graph is mutated. Location identifiers are treated as values and are not
     * assumed to be contiguous.</p>
     *
     * @param graph empty graph receiving the database snapshot
     * @throws NullPointerException if {@code graph} is null
     * @throws IllegalArgumentException if {@code graph} is not empty
     * @throws IllegalStateException if persisted roads reference missing
     *         locations, produce invalid routing costs, or cannot be represented
     *         consistently by the supplied graph
     * @throws SQLException if location or road loading fails
     */
    public void loadInto(WeightedGraph graph) throws SQLException {
        Objects.requireNonNull(graph, "graph cannot be null");
        if (!graph.isEmpty()) {
            throw new IllegalArgumentException("graph must be empty before database loading");
        }

        Location[] locations = locationDao.findAll();
        Road[] roads = roadDao.findAll();
        double[] routingCosts = validateAndCalculateRoutingCosts(locations, roads);

        for (Location location : locations) {
            if (!graph.addVertex(location.getLocationId())) {
                throw new IllegalStateException(
                        "Duplicate location ID while loading graph: "
                                + location.getLocationId());
            }
        }

        for (int index = 0; index < roads.length; index++) {
            Road road = roads[index];
            if (road.isBlocked()) {
                continue;
            }
            EdgeUpdate result = graph.addEdge(
                    road.getFromLocationId(), road.getToLocationId(), routingCosts[index]);
            if (result != EdgeUpdate.ADDED) {
                throw edgeLoadFailure(road, result);
            }
        }
    }

    private static double[] validateAndCalculateRoutingCosts(
            Location[] locations, Road[] roads) {
        double[] routingCosts = new double[roads.length];
        for (int index = 0; index < roads.length; index++) {
            Road road = roads[index];
            if (!containsLocation(locations, road.getFromLocationId())
                    || !containsLocation(locations, road.getToLocationId())) {
                throw new IllegalStateException(
                        "Road "
                                + road.getRoadId()
                                + " references a location missing from the loaded locations: "
                                + road.getFromLocationId()
                                + " -> "
                                + road.getToLocationId());
            }

            if (!road.isBlocked()) {
                double routingCost = road.getTravelTimeMin() * road.getConditionWeight();
                if (!Double.isFinite(routingCost) || routingCost < 0.0d) {
                    throw new IllegalStateException(
                            "Road " + road.getRoadId() + " has invalid routing cost: "
                                    + routingCost);
                }
                routingCosts[index] = routingCost == 0.0d ? 0.0d : routingCost;
            }
        }
        return routingCosts;
    }

    private static boolean containsLocation(Location[] locations, int locationId) {
        for (Location location : locations) {
            if (location.getLocationId() == locationId) {
                return true;
            }
        }
        return false;
    }

    private static IllegalStateException edgeLoadFailure(Road road, EdgeUpdate result) {
        return new IllegalStateException(
                "Could not load road "
                        + road.getRoadId()
                        + " ("
                        + road.getFromLocationId()
                        + " -> "
                        + road.getToLocationId()
                        + "): graph returned "
                        + result);
    }
}
