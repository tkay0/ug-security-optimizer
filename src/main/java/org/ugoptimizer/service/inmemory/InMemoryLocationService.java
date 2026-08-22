package org.ugoptimizer.service.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.frontend.LocationService;

/**
 * In-memory {@link LocationService} seeded with the same sample campus data
 * {@code LocationRoadMenu} used to hold itself, so swapping this in changes
 * nothing about how the app looks or behaves. Replace with a real
 * DAO-backed implementation once the database team's work lands.
 */
public final class InMemoryLocationService implements LocationService {

    private final List<Location> locations = new ArrayList<>();
    private final List<Road> roads = new ArrayList<>();
    private int nextLocationId = 1;
    private int nextRoadId = 1;

    public InMemoryLocationService() {
        seedSampleData();
    }

    @Override
    public List<Location> findAllLocations() {
        return Collections.unmodifiableList(locations);
    }

    @Override
    public int nextLocationId() {
        return nextLocationId;
    }

    @Override
    public Location addLocation(Location location) {
        locations.add(location);
        nextLocationId++;
        return location;
    }

    @Override
    public List<Road> findAllRoads() {
        return Collections.unmodifiableList(roads);
    }

    @Override
    public int nextRoadId() {
        return nextRoadId;
    }

    @Override
    public Road addRoad(Road road) {
        roads.add(road);
        nextRoadId++;
        return road;
    }

    private void seedSampleData() {
        locations.add(new Location(nextLocationId++, "Balme Library", "Central Campus", "LIBRARY",
                10, 20, "07:00-22:00", "https://ug.edu.gh/library"));
        locations.add(new Location(nextLocationId++, "Legon Hall", "North Campus", "RESIDENCE",
                30, 45, "24 HOURS", "https://ug.edu.gh/legon-hall"));
        locations.add(new Location(nextLocationId++, "Great Hall", "Central Campus", "EVENT_VENUE",
                15, 25, "08:00-18:00", "https://ug.edu.gh/great-hall"));

        roads.add(new Road(nextRoadId++, 1, 2, 1.2, 6.0, 1.0,
                "Library Link Road", "CAMPUS_ROAD", "LOW", false));
        roads.add(new Road(nextRoadId++, 2, 3, 0.8, 4.0, 1.2,
                "Legon Hall Avenue", "CAMPUS_ROAD", "MODERATE", false));
        roads.add(new Road(nextRoadId++, 1, 3, 0.5, 3.0, 1.0,
                "Great Hall Walk", "ACCESS_ROAD", "LOW", true));
    }
}
