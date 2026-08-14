package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.ugoptimizer.model.ServiceRequest;

/** Maps one {@code service_requests} result row to the shared domain model. */
public final class ServiceRequestMapper {

    private ServiceRequestMapper() {
    }

    public static ServiceRequest map(ResultSet resultSet) throws SQLException {
        try {
            return new ServiceRequest(
                    resultSet.getInt("request_id"),
                    resultSet.getInt("source_location_id"),
                    resultSet.getInt("destination_location_id"),
                    resultSet.getString("category"),
                    resultSet.getInt("urgency"),
                    Instant.parse(resultSet.getString("time_submitted")),
                    Instant.parse(resultSet.getString("deadline")),
                    resultSet.getString("status"),
                    resultSet.getString("required_resource_type"),
                    resultSet.getString("description"));
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid service_requests row", exception);
        }
    }
}
