package org.ugoptimizer.gui;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.AuditEventDao;
import org.ugoptimizer.database.dao.LocationDao;
import org.ugoptimizer.database.dao.ResourceDao;
import org.ugoptimizer.database.dao.RoadDao;
import org.ugoptimizer.database.dao.ServiceRequestDao;
import org.ugoptimizer.database.loader.DatabaseGraphLoader;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.model.RequestStatus;
import org.ugoptimizer.model.ResourceAvailability;
import org.ugoptimizer.service.GreedyAssignmentService;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;
import org.ugoptimizer.structures.hash.CustomMap;

/**
 * Integration facade between the Swing GUI and the existing system layers.
 *
 * <p>This class is the only GUI-facing entry point to the persistence layer
 * (Team 1 DAOs), the algorithm layer (linear/binary search, sorting, greedy
 * assignment, binary heap, graph traversal) and the canonical datasets. It
 * deliberately contains no business algorithms: every operation delegates to
 * an existing implementation.</p>
 *
 * <p>Atomic workflow writes (status change, availability change, assignment)
 * are performed on one connection so a GUI action cannot leave the database
 * half-updated. Request status and resource availability writes reuse the same
 * canonical values enforced by the domain models and schema checks.</p>
 */
public final class AppContext {

    /** Assumed campus response speed used only as greedy-assignment runtime input. */
    private static final double RESPONSE_SPEED_M_PER_MIN = 200.0d;

    private static final String ACTOR_DISPATCH_OPERATOR = "DISPATCH_OPERATOR";

    private final DatabaseManager databaseManager;
    private final LocationDao locationDao;
    private final RoadDao roadDao;
    private final ResourceDao resourceDao;
    private final ServiceRequestDao requestDao;
    private final AuditEventDao auditDao;
    private final GreedyAssignmentService assignmentService;

    private final CustomMap<Integer, Location> locationById = new CustomMap<>();
    private final CustomMap<Integer, Resource> resourceById = new CustomMap<>();
    private final CustomMap<Integer, Road> roadById = new CustomMap<>();

    private boolean locationsLoaded;
    private boolean resourcesLoaded;
    private boolean roadsLoaded;

    public AppContext(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
        this.locationDao = new LocationDao(databaseManager);
        this.roadDao = new RoadDao(databaseManager);
        this.resourceDao = new ResourceDao(databaseManager);
        this.requestDao = new ServiceRequestDao(databaseManager);
        this.auditDao = new AuditEventDao(databaseManager);
        this.assignmentService = new GreedyAssignmentService();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /** Default runtime database file inside the project's database directory. */
    public static Path defaultDatabasePath() {
        return Path.of("database", "ug_security_gui.db");
    }

    // ------------------------------------------------------------------
    // Team 1 data loading
    // ------------------------------------------------------------------

    public Location[] loadLocations() throws SQLException {
        Location[] locations = locationDao.findAll();
        locationById.clear();
        for (Location location : locations) {
            locationById.put(location.getLocationId(), location);
        }
        locationsLoaded = true;
        return locations;
    }

    public Resource[] loadResources() throws SQLException {
        Resource[] resources = resourceDao.findAll();
        resourceById.clear();
        for (Resource resource : resources) {
            resourceById.put(resource.getResourceId(), resource);
        }
        resourcesLoaded = true;
        return resources;
    }

    public Road[] loadRoads() throws SQLException {
        Road[] roads = roadDao.findAll();
        roadById.clear();
        for (Road road : roads) {
            roadById.put(road.getRoadId(), road);
        }
        roadsLoaded = true;
        return roads;
    }

    public ServiceRequest[] loadRequests() throws SQLException {
        return requestDao.findAll();
    }

    public AuditEvent[] loadAuditEvents() throws SQLException {
        return auditDao.findAll();
    }

    public AuditEvent[] loadAuditEventsFor(String entityType, int entityId) throws SQLException {
        return auditDao.findByEntity(entityType, entityId);
    }

    /**
     * Reloads the campus network graph from persisted locations and roads,
     * routing cost being {@code travelTimeMin * conditionWeight} exactly as
     * {@link DatabaseGraphLoader} defines it.
     */
    public WeightedGraph loadCampusGraph() throws SQLException {
        ensureLocationsLoaded();
        ensureRoadsLoaded();
        WeightedGraph graph = new AdjacencyListGraph();
        new DatabaseGraphLoader(locationDao, roadDao).loadInto(graph);
        return graph;
    }

    // ------------------------------------------------------------------
    // Canonical lookup helpers (Hash Table / CustomMap indexed)
    // ------------------------------------------------------------------

    public String locationName(int locationId) {
        Location location = locationById.get(locationId);
        return location == null ? "#" + locationId : location.getName();
    }

    public Location location(int locationId) {
        return locationById.get(locationId);
    }

    public Resource resource(int resourceId) {
        return resourceById.get(resourceId);
    }

    public Road road(int roadId) {
        return roadById.get(roadId);
    }

    // ------------------------------------------------------------------
    // Greedy resource assignment
    // ------------------------------------------------------------------

    /**
     * Builds runtime candidates for every currently AVAILABLE resource and lets
     * the existing greedy algorithm select the best one for the request.
     *
     * <p>Response time is request-specific runtime input (distance estimate
     * from the resource location to the request destination); the greedy
     * selection itself is delegated to {@link GreedyAssignmentService} /
     * {@link GreedyAssignment}.</p>
     */
    public AssignmentRecommendation recommendAssignment(ServiceRequest request)
            throws SQLException {
        Objects.requireNonNull(request, "request cannot be null");
        ensureResourcesLoaded();
        ensureLocationsLoaded();

        Resource[] allResources = resourceDao.findAll();
        int availableCount = 0;
        for (Resource resource : allResources) {
            if (ResourceAvailability.AVAILABLE.name().equals(resource.getAvailabilityStatus())) {
                availableCount++;
            }
        }

        AssignmentCandidate[] candidates = new AssignmentCandidate[availableCount];
        int index = 0;
        for (Resource resource : allResources) {
            if (!ResourceAvailability.AVAILABLE.name().equals(resource.getAvailabilityStatus())) {
                continue;
            }
            candidates[index++] = new AssignmentCandidate(
                    resource,
                    estimateResponseTimeMin(resource, request),
                    0);
        }

        AssignmentCandidate best = assignmentService.assign(request, candidates);
        return new AssignmentRecommendation(candidates, best);
    }

    /**
     * Estimated travel minutes from a resource's current (or home) location to
     * the request destination, using the schematic coordinate distance. This is
     * presentation-level runtime input for the greedy selection, not a stored
     * or algorithm-produced route time.
     */
    public double estimateResponseTimeMin(Resource resource, ServiceRequest request) {
        Location from = locationById.get(
                resource.getCurrentLocationId() != null
                        ? resource.getCurrentLocationId()
                        : resource.getHomeLocationId());
        Location to = locationById.get(request.getDestinationLocationId());
        if (from == null || to == null) {
            return 0.0d;
        }
        double deltaX = from.getXCoord() - to.getXCoord();
        double deltaY = from.getYCoord() - to.getYCoord();
        double distanceMeters = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return distanceMeters / RESPONSE_SPEED_M_PER_MIN;
    }

    /**
     * Atomically commits a confirmed assignment: resource becomes BUSY, the
     * request becomes ASSIGNED, and a REQUEST_ASSIGNED audit event is recorded.
     */
    public void confirmAssignment(ServiceRequest request, Resource resource)
            throws SQLException {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateResourceAvailabilityTx(connection, resource.getResourceId(), ResourceAvailability.BUSY.name());
                updateRequestStatusTx(connection, request.getRequestId(), RequestStatus.ASSIGNED.name());
                insertAuditTx(
                        connection,
                        nextAuditEventId(connection),
                        "REQUEST_ASSIGNED",
                        "SERVICE_REQUEST",
                        request.getRequestId(),
                        "Resource " + resource.getResourceId()
                                + " (" + resource.getResourceType() + ") assigned");
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    // ------------------------------------------------------------------
    // Atomic status writes
    // ------------------------------------------------------------------

    /**
     * Atomically updates a request status and records a REQUEST_STATUS_CHANGED
     * audit event using the canonical request status values.
     */
    public void updateRequestStatus(int requestId, String newStatus) throws SQLException {
        validateRequestStatus(newStatus);
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateRequestStatusTx(connection, requestId, newStatus);
                insertAuditTx(
                        connection,
                        nextAuditEventId(connection),
                        "REQUEST_STATUS_CHANGED",
                        "SERVICE_REQUEST",
                        requestId,
                        "Status changed to " + newStatus);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    /**
     * Atomically updates a resource availability state and records a
     * RESOURCE_STATUS_CHANGED audit event using canonical availability values.
     */
    public void updateResourceAvailability(int resourceId, String availabilityStatus)
            throws SQLException {
        validateAvailabilityStatus(availabilityStatus);
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateResourceAvailabilityTx(connection, resourceId, availabilityStatus);
                insertAuditTx(
                        connection,
                        nextAuditEventId(connection),
                        "RESOURCE_STATUS_CHANGED",
                        "RESOURCE",
                        resourceId,
                        "Availability changed to " + availabilityStatus);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    // ------------------------------------------------------------------
    // Transactional statement helpers
    // ------------------------------------------------------------------

    private void updateRequestStatusTx(Connection connection, int requestId, String status)
            throws SQLException {
        String sql = "UPDATE service_requests SET status = ? WHERE request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, requestId);
            int affected = statement.executeUpdate();
            if (affected != 1) {
                throw new SQLException(
                        "No service request with id " + requestId + " was updated");
            }
        }
    }

    private void updateResourceAvailabilityTx(
            Connection connection, int resourceId, String availabilityStatus)
            throws SQLException {
        String sql = "UPDATE resources SET availability_status = ? WHERE resource_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, availabilityStatus);
            statement.setInt(2, resourceId);
            int affected = statement.executeUpdate();
            if (affected != 1) {
                throw new SQLException(
                        "No resource with id " + resourceId + " was updated");
            }
        }
    }

    private void insertAuditTx(
            Connection connection,
            int eventId,
            String eventType,
            String entityType,
            int entityId,
            String details) throws SQLException {
        String sql = "INSERT INTO audit_events (event_id, event_type, event_timestamp, "
                + "entity_type, entity_id, actor_type, details) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, eventType);
            statement.setString(3, java.time.Instant.now().toString());
            statement.setString(4, entityType);
            statement.setInt(5, entityId);
            statement.setString(6, ACTOR_DISPATCH_OPERATOR);
            if (details == null) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, details);
            }
            int affected = statement.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Audit insert affected " + affected + " rows");
            }
        }
    }

    private int nextAuditEventId(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(event_id), 0) + 1 FROM audit_events";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void ensureLocationsLoaded() throws SQLException {
        if (!locationsLoaded) {
            loadLocations();
        }
    }

    private void ensureResourcesLoaded() throws SQLException {
        if (!resourcesLoaded) {
            loadResources();
        }
    }

    private void ensureRoadsLoaded() throws SQLException {
        if (!roadsLoaded) {
            loadRoads();
        }
    }

    private static void validateRequestStatus(String status) {
        Objects.requireNonNull(status, "status cannot be null");
        try {
            RequestStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported request status: " + status, ex);
        }
    }

    private static void validateAvailabilityStatus(String status) {
        Objects.requireNonNull(status, "availabilityStatus cannot be null");
        try {
            ResourceAvailability.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported availabilityStatus: " + status, ex);
        }
    }

    private static void rollback(Connection connection, SQLException failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }

    /** Outcome of a greedy recommendation: all candidates plus the selected best. */
    public static final class AssignmentRecommendation {

        private final AssignmentCandidate[] candidates;
        private final AssignmentCandidate best;

        AssignmentRecommendation(AssignmentCandidate[] candidates, AssignmentCandidate best) {
            this.candidates = candidates;
            this.best = best;
        }

        public AssignmentCandidate[] getCandidates() {
            return candidates.clone();
        }

        public AssignmentCandidate getBest() {
            return best;
        }

        public boolean hasBest() {
            return best != null;
        }
    }
}
