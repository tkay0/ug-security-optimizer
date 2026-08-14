package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;

/**
 * Provides campus locations and roads to the UI without exposing how they are
 * stored. A real implementation wraps {@code LocationDao} and {@code RoadDao};
 * {@code InMemoryLocationService} exists for development before that lands.
 */
public interface LocationService {

    List<Location> findAllLocations();

    /** Returns the ID the next added location should use (mirrors DB auto-increment). */
    int nextLocationId();

    Location addLocation(Location location);

    List<Road> findAllRoads();

    /** Returns the ID the next added road should use (mirrors DB auto-increment). */
    int nextRoadId();

    Road addRoad(Road road);
}
