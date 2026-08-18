package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;

/** Swing-facing location and road contract with no persistence-specific checked exceptions. */
public interface LocationService {
  List<Location> findAllLocations();

  int nextLocationId();

  Location addLocation(Location location);

  List<Road> findAllRoads();

  int nextRoadId();

  Road addRoad(Road road);
}
