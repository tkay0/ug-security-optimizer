package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.ugoptimizer.model.AuditEvent;

/** Maps one {@code audit_events} result row to the shared domain model. */
public final class AuditEventMapper {

    private AuditEventMapper() {
    }

    public static AuditEvent map(ResultSet resultSet) throws SQLException {
        try {
            return new AuditEvent(
                    resultSet.getInt("event_id"),
                    resultSet.getString("event_type"),
                    Instant.parse(resultSet.getString("event_timestamp")),
                    resultSet.getString("entity_type"),
                    resultSet.getInt("entity_id"),
                    resultSet.getString("actor_type"),
                    resultSet.getString("details"));
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid audit_events row", exception);
        }
    }
}
