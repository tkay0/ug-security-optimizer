package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.ServiceRequestMapper;
import org.ugoptimizer.model.ServiceRequest;

/** Provides persistent read and lifecycle-write access to service requests. */
public final class ServiceRequestDao {

    private static final String COLUMNS =
            "request_id, source_location_id, destination_location_id, category, urgency, "
                    + "time_submitted, deadline, status, required_resource_type, description";
    private static final int INITIAL_RESULT_CAPACITY = 32;

    private final DatabaseManager databaseManager;

    public ServiceRequestDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
    }

    public Optional<ServiceRequest> findById(int requestId) throws SQLException {
        requirePositiveId(requestId);
        String sql = "SELECT " + COLUMNS + " FROM service_requests WHERE request_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(ServiceRequestMapper.map(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new SQLException("Failed to find service request " + requestId, exception);
        }
    }

    public ServiceRequest[] findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM service_requests ORDER BY request_id";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            ServiceRequest[] buffer = new ServiceRequest[INITIAL_RESULT_CAPACITY];
            int count = 0;
            while (resultSet.next()) {
                if (count == buffer.length) {
                    buffer = grow(buffer);
                }
                buffer[count++] = ServiceRequestMapper.map(resultSet);
            }
            ServiceRequest[] result = new ServiceRequest[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        } catch (SQLException exception) {
            throw new SQLException("Failed to read all service requests", exception);
        }
    }

    public void insert(ServiceRequest request) throws SQLException {
        Objects.requireNonNull(request, "request cannot be null");
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                validateRequiredResourceType(connection, request.getRequiredResourceType());
                insertRequest(connection, request);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to insert service request "
                            + request.getRequestId()
                            + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    private static void validateRequiredResourceType(
            Connection connection, String requiredResourceType) throws SQLException {
        if (requiredResourceType == null) {
            return;
        }
        String sql = "SELECT 1 FROM resources WHERE resource_type = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requiredResourceType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Required resource type does not exist: " + requiredResourceType);
                }
            }
        }
    }

    private static void insertRequest(Connection connection, ServiceRequest request)
            throws SQLException {
        String sql = "INSERT INTO service_requests (" + COLUMNS
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, request.getRequestId());
            statement.setInt(2, request.getSourceLocationId());
            statement.setInt(3, request.getDestinationLocationId());
            statement.setString(4, request.getCategory());
            statement.setInt(5, request.getUrgency());
            statement.setString(6, request.getTimeSubmitted().toString());
            statement.setString(7, request.getDeadline().toString());
            statement.setString(8, request.getStatus());
            setNullableText(statement, 9, request.getRequiredResourceType());
            setNullableText(statement, 10, request.getDescription());
            int affectedRows = statement.executeUpdate();
            requireSingleInsertedRow(affectedRows);
        }
    }

    public boolean updateStatus(int requestId, String newStatus) throws SQLException {
        requirePositiveId(requestId);
        validateStatus(newStatus);
        String sql = "UPDATE service_requests SET status = ? WHERE request_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setInt(2, requestId);
            return changedOneOrNone(
                    statement.executeUpdate(), "service request status update");
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to update status for service request " + requestId, exception);
        }
    }

    private static ServiceRequest[] grow(ServiceRequest[] current) throws SQLException {
        if (current.length > Integer.MAX_VALUE / 2) {
            throw new SQLException("Service-request result exceeds supported array capacity");
        }
        ServiceRequest[] expanded = new ServiceRequest[current.length * 2];
        System.arraycopy(current, 0, expanded, 0, current.length);
        return expanded;
    }

    private static void setNullableText(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void requireSingleInsertedRow(int affectedRows) throws SQLException {
        if (affectedRows != 1) {
            throw new SQLException("Service-request insert affected " + affectedRows + " rows");
        }
    }

    private static void rollback(Connection connection, SQLException failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }

    private static boolean changedOneOrNone(int affectedRows, String operation) throws SQLException {
        if (affectedRows > 1) {
            throw new SQLException(operation + " affected more than one row");
        }
        return affectedRows == 1;
    }

    private static void requirePositiveId(int requestId) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
    }

    private static void validateStatus(String status) {
        Objects.requireNonNull(status, "newStatus cannot be null");
        switch (status) {
            case "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED" -> {
                return;
            }
            default -> throw new IllegalArgumentException("Unsupported request status: " + status);
        }
    }
}
