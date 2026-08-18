package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.ServiceRequestDao;
import org.ugoptimizer.model.ServiceRequest;

/** Provides validated request creation and query operations over the existing DAO. */
public final class RequestService {

    private final ServiceRequestDao requestDao;
    private final LocationDao locationDao;

    public RequestService(DatabaseManager databaseManager) {
        this(
                new ServiceRequestDao(Objects.requireNonNull(
                        databaseManager, "databaseManager cannot be null")),
                new LocationDao(databaseManager));
    }

    public RequestService(ServiceRequestDao requestDao, LocationDao locationDao) {
        this.requestDao = Objects.requireNonNull(requestDao, "requestDao cannot be null");
        this.locationDao = Objects.requireNonNull(locationDao, "locationDao cannot be null");
    }

    public ServiceRequest[] getAllRequests() throws SQLException {
        return requestDao.findAll();
    }

    public Optional<ServiceRequest> findRequestById(int requestId) throws SQLException {
        return requestDao.findById(requestId);
    }

    public ServiceRequest requireRequest(int requestId) throws SQLException {
        return findRequestById(requestId).orElseThrow(
                () -> new NoSuchElementException("Service request does not exist: " + requestId));
    }

    /** Creates a new incident. Lifecycle changes after creation belong to {@link WorkflowService}. */
    public ServiceRequest createRequest(ServiceRequest request) throws SQLException {
        Objects.requireNonNull(request, "request cannot be null");
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("A new service request must have status PENDING");
        }
        requireExistingLocation(request.getSourceLocationId(), "source");
        requireExistingLocation(request.getDestinationLocationId(), "destination");
        requestDao.insert(request);
        return requireRequest(request.getRequestId());
    }

    public ServiceRequest[] findByStatus(String status) throws SQLException {
        validateStatus(status);
        return filter(getAllRequests(), status, true);
    }

    public ServiceRequest[] findByCategory(String category) throws SQLException {
        validateCategory(category);
        return filter(getAllRequests(), category, false);
    }

    private void requireExistingLocation(int locationId, String role) throws SQLException {
        if (locationDao.findById(locationId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Request " + role + " location does not exist: " + locationId);
        }
    }

    private static ServiceRequest[] filter(
            ServiceRequest[] requests, String expected, boolean byStatus) {
        int count = 0;
        for (ServiceRequest request : requests) {
            String actual = byStatus ? request.getStatus() : request.getCategory();
            if (expected.equals(actual)) {
                count++;
            }
        }
        ServiceRequest[] matches = new ServiceRequest[count];
        int index = 0;
        for (ServiceRequest request : requests) {
            String actual = byStatus ? request.getStatus() : request.getCategory();
            if (expected.equals(actual)) {
                matches[index++] = request;
            }
        }
        return matches;
    }

    private static void validateStatus(String status) {
        Objects.requireNonNull(status, "status cannot be null");
        switch (status) {
            case "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED" -> {
                return;
            }
            default -> throw new IllegalArgumentException("Unsupported request status: " + status);
        }
    }

    private static void validateCategory(String category) {
        Objects.requireNonNull(category, "category cannot be null");
        switch (category) {
            case "ACCESS_CONTROL", "CCTV_FAULT", "CROWD_CONTROL", "EMERGENCY_TRANSPORT",
                    "FIRE_ALARM", "MEDICAL_EMERGENCY", "NIGHT_PATROL_REQUEST",
                    "ROAD_OBSTRUCTION", "SECURITY_ESCORT", "SUSPICIOUS_ACTIVITY",
                    "THEFT_REPORT", "WELFARE_CHECK" -> {
                return;
            }
            default -> throw new IllegalArgumentException("Unsupported request category: " + category);
        }
    }
}
