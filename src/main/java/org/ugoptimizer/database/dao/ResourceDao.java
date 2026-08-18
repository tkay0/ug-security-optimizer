package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.ResourceMapper;
import org.ugoptimizer.model.Resource;

/** Provides persistent read and state-update access to resources. */
public final class ResourceDao {

    private static final String COLUMNS =
            "resource_id, resource_type, home_location_id, capacity, availability_status, "
                    + "current_location_id, shift_start, shift_end";
    private static final int INITIAL_RESULT_CAPACITY = 16;

    private final DatabaseManager databaseManager;

    public ResourceDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
    }

    public Optional<Resource> findById(int resourceId) throws SQLException {
        requirePositiveId(resourceId);
        String sql = "SELECT " + COLUMNS + " FROM resources WHERE resource_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resourceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(ResourceMapper.map(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new SQLException("Failed to find resource " + resourceId, exception);
        }
    }

    public Resource[] findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM resources ORDER BY resource_id";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            Resource[] buffer = new Resource[INITIAL_RESULT_CAPACITY];
            int count = 0;
            while (resultSet.next()) {
                if (count == buffer.length) {
                    buffer = grow(buffer);
                }
                buffer[count++] = ResourceMapper.map(resultSet);
            }
            Resource[] result = new Resource[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        } catch (SQLException exception) {
            throw new SQLException("Failed to read all resources", exception);
        }
    }

    public void insert(Resource resource) throws SQLException {
        Objects.requireNonNull(resource, "resource cannot be null");
        String sql = "INSERT INTO resources (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resource.getResourceId());
            statement.setString(2, resource.getResourceType());
            statement.setInt(3, resource.getHomeLocationId());
            statement.setInt(4, resource.getCapacity());
            statement.setString(5, resource.getAvailabilityStatus());
            if (resource.getCurrentLocationId() == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, resource.getCurrentLocationId());
            }
            setNullableText(statement, 7,
                    resource.getShiftStart() == null ? null : resource.getShiftStart().toString());
            setNullableText(statement, 8,
                    resource.getShiftEnd() == null ? null : resource.getShiftEnd().toString());
            requireSingleInsert(statement.executeUpdate());
        } catch (SQLException exception) {
            throw new SQLException("Failed to insert resource " + resource.getResourceId(), exception);
        }
    }

    public boolean updateAvailability(int resourceId, String availabilityStatus)
            throws SQLException {
        requirePositiveId(resourceId);
        validateAvailability(availabilityStatus);
        String sql = "UPDATE resources SET availability_status = ? WHERE resource_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, availabilityStatus);
            statement.setInt(2, resourceId);
            return changedOneOrNone(statement.executeUpdate(), "resource availability update");
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to update availability for resource " + resourceId, exception);
        }
    }

    public boolean updateCurrentLocation(int resourceId, Integer currentLocationId)
            throws SQLException {
        requirePositiveId(resourceId);
        if (currentLocationId != null && currentLocationId <= 0) {
            throw new IllegalArgumentException("currentLocationId must be positive when present");
        }
        String sql = "UPDATE resources SET current_location_id = ? WHERE resource_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (currentLocationId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, currentLocationId);
            }
            statement.setInt(2, resourceId);
            return changedOneOrNone(statement.executeUpdate(), "resource location update");
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to update current location for resource " + resourceId, exception);
        }
    }

    private static Resource[] grow(Resource[] current) throws SQLException {
        if (current.length > Integer.MAX_VALUE / 2) {
            throw new SQLException("Resource result exceeds supported array capacity");
        }
        Resource[] expanded = new Resource[current.length * 2];
        System.arraycopy(current, 0, expanded, 0, current.length);
        return expanded;
    }

    private static boolean changedOneOrNone(int affectedRows, String operation) throws SQLException {
        if (affectedRows > 1) {
            throw new SQLException(operation + " affected more than one row");
        }
        return affectedRows == 1;
    }

    private static void setNullableText(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void requireSingleInsert(int affectedRows) throws SQLException {
        if (affectedRows != 1) {
            throw new SQLException("Resource insert affected " + affectedRows + " rows");
        }
    }

    private static void requirePositiveId(int resourceId) {
        if (resourceId <= 0) {
            throw new IllegalArgumentException("resourceId must be positive");
        }
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
}
