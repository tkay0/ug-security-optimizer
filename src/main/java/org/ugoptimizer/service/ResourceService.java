package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.ResourceDao;
import org.ugoptimizer.model.Resource;

/** Provides validated resource queries and state updates through {@link ResourceDao}. */
public final class ResourceService {

    private final ResourceDao resourceDao;
    private final LocationDao locationDao;

    public ResourceService(DatabaseManager databaseManager) {
        this(
                new ResourceDao(Objects.requireNonNull(
                        databaseManager, "databaseManager cannot be null")),
                new LocationDao(databaseManager));
    }

    public ResourceService(ResourceDao resourceDao, LocationDao locationDao) {
        this.resourceDao = Objects.requireNonNull(resourceDao, "resourceDao cannot be null");
        this.locationDao = Objects.requireNonNull(locationDao, "locationDao cannot be null");
    }

    public Resource[] getAllResources() throws SQLException {
        return resourceDao.findAll();
    }

    public Optional<Resource> findResourceById(int resourceId) throws SQLException {
        return resourceDao.findById(resourceId);
    }

    public Resource requireResource(int resourceId) throws SQLException {
        return findResourceById(resourceId).orElseThrow(
                () -> new NoSuchElementException("Resource does not exist: " + resourceId));
    }

    public Resource[] getAvailableResources() throws SQLException {
        return availableResources(null);
    }

    public Resource[] getAvailableResourcesByType(String resourceType) throws SQLException {
        requireText(resourceType, "resourceType");
        return availableResources(resourceType);
    }

    public Resource updateAvailability(int resourceId, String availabilityStatus)
            throws SQLException {
        validateAvailability(availabilityStatus);
        requireResource(resourceId);
        if (!resourceDao.updateAvailability(resourceId, availabilityStatus)) {
            throw new IllegalStateException("Resource disappeared during update: " + resourceId);
        }
        return requireResource(resourceId);
    }

    public Resource updateCurrentLocation(int resourceId, Integer currentLocationId)
            throws SQLException {
        requireResource(resourceId);
        if (currentLocationId != null && locationDao.findById(currentLocationId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Current location does not exist: " + currentLocationId);
        }
        if (!resourceDao.updateCurrentLocation(resourceId, currentLocationId)) {
            throw new IllegalStateException("Resource disappeared during update: " + resourceId);
        }
        return requireResource(resourceId);
    }

    public Resource createResource(Resource resource) throws SQLException {
        Objects.requireNonNull(resource, "resource cannot be null");
        requireExistingLocation(resource.getHomeLocationId(), "home");
        if (resource.getCurrentLocationId() != null) {
            requireExistingLocation(resource.getCurrentLocationId(), "current");
        }
        resourceDao.insert(resource);
        return requireResource(resource.getResourceId());
    }

    private Resource[] availableResources(String resourceType) throws SQLException {
        Resource[] resources = getAllResources();
        int count = 0;
        for (Resource resource : resources) {
            if (isAvailable(resource, resourceType)) {
                count++;
            }
        }
        Resource[] available = new Resource[count];
        int index = 0;
        for (Resource resource : resources) {
            if (isAvailable(resource, resourceType)) {
                available[index++] = resource;
            }
        }
        return available;
    }

    private static boolean isAvailable(Resource resource, String resourceType) {
        return "AVAILABLE".equals(resource.getAvailabilityStatus())
                && (resourceType == null || resourceType.equals(resource.getResourceType()));
    }

    private static void validateAvailability(String status) {
        Objects.requireNonNull(status, "availabilityStatus cannot be null");
        switch (status) {
            case "AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY" -> {
                return;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported availabilityStatus: " + status);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }

    private void requireExistingLocation(int locationId, String role) throws SQLException {
        if (locationDao.findById(locationId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Resource " + role + " location does not exist: " + locationId);
        }
    }
}
