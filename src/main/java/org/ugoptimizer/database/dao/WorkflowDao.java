package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.AuditEvent;

/** Performs atomic request-status and audit-event persistence for workflow services. */
public final class WorkflowDao {

    private final DatabaseManager databaseManager;

    public WorkflowDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
    }

    /**
     * Atomically changes a request from the expected status and records its
     * audit event. The expected-status predicate prevents stale UI actions from
     * overwriting a concurrent workflow transition.
     */
    public AuditEvent transitionStatus(
            int requestId,
            String expectedStatus,
            String newStatus,
            Instant timestamp,
            String actorType,
            String details) throws SQLException {
        requirePositiveId(requestId);
        requireText(expectedStatus, "expectedStatus");
        requireText(newStatus, "newStatus");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");

        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateExpectedStatus(connection, requestId, expectedStatus, newStatus);
                AuditEvent event = new AuditEvent(
                        nextAuditEventId(connection),
                        "REQUEST_STATUS_CHANGED",
                        timestamp,
                        "SERVICE_REQUEST",
                        requestId,
                        actorType,
                        details);
                AuditEventDao.insertEvent(connection, event);
                connection.commit();
                return event;
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to transition service request " + requestId
                            + " from " + expectedStatus + " to " + newStatus,
                    exception);
        }
    }

    private static void updateExpectedStatus(
            Connection connection, int requestId, String expectedStatus, String newStatus)
            throws SQLException {
        String sql = "UPDATE service_requests SET status = ?"
                + " WHERE request_id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setInt(2, requestId);
            statement.setString(3, expectedStatus);
            int affected = statement.executeUpdate();
            if (affected != 1) {
                throw new SQLException(
                        "Request is missing or no longer has expected status " + expectedStatus);
            }
        }
    }

    private static int nextAuditEventId(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(event_id), 0) + 1 FROM audit_events";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("Could not allocate an audit event ID");
            }
            return resultSet.getInt(1);
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }

    private static void requirePositiveId(int requestId) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
