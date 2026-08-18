package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.RoadDao;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;

/** Provides validated, DAO-backed access to persisted campus locations and roads. */
public final class LocationService {

    private final LocationDao locationDao;
    private final RoadDao roadDao;

    public LocationService(DatabaseManager databaseManager) {
        this(
                new LocationDao(Objects.requireNonNull(
                        databaseManager, "databaseManager cannot be null")),
                new RoadDao(databaseManager));
    }

    public LocationService(LocationDao locationDao, RoadDao roadDao) {
        this.locationDao = Objects.requireNonNull(locationDao, "locationDao cannot be null");
        this.roadDao = Objects.requireNonNull(roadDao, "roadDao cannot be null");
    }

    public Location[] getAllLocations() throws SQLException {
        return locationDao.findAll();
    }

    public Optional<Location> findLocationById(int locationId) throws SQLException {
        return locationDao.findById(locationId);
    }

    public Location requireLocation(int locationId) throws SQLException {
        return findLocationById(locationId).orElseThrow(
                () -> new NoSuchElementException("Location does not exist: " + locationId));
    }

    public boolean locationExists(int locationId) throws SQLException {
        return findLocationById(locationId).isPresent();
    }

    public Road[] getAllRoads() throws SQLException {
        return roadDao.findAll();
    }

    public Optional<Road> findRoadById(int roadId) throws SQLException {
        return roadDao.findById(roadId);
    }

    public Road requireRoad(int roadId) throws SQLException {
        return findRoadById(roadId).orElseThrow(
                () -> new NoSuchElementException("Road does not exist: " + roadId));
    }

    public Location createLocation(Location location) throws SQLException {
        Objects.requireNonNull(location, "location cannot be null");
        locationDao.insert(location);
        return requireLocation(location.getLocationId());
    }

    public Road createRoad(Road road) throws SQLException {
        Objects.requireNonNull(road, "road cannot be null");
        requireExistingLocation(road.getFromLocationId(), "from");
        requireExistingLocation(road.getToLocationId(), "to");
        roadDao.insert(road);
        return requireRoad(road.getRoadId());
    }

    private void requireExistingLocation(int locationId, String role) throws SQLException {
        if (!locationExists(locationId)) {
            throw new IllegalArgumentException(
                    "Road " + role + " location does not exist: " + locationId);
        }
    }
}
