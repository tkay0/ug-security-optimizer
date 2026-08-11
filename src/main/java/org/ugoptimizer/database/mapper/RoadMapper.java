package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.ugoptimizer.model.Road;

/** Maps one {@code roads} result row to the shared domain model. */
public final class RoadMapper {

    private RoadMapper() {
    }

    public static Road map(ResultSet resultSet) throws SQLException {
        int blockedValue = resultSet.getInt("is_blocked");
        if (resultSet.wasNull() || (blockedValue != 0 && blockedValue != 1)) {
            throw new SQLException("Invalid roads.is_blocked value: " + blockedValue);
        }

        try {
            return new Road(
                    resultSet.getInt("road_id"),
                    resultSet.getInt("from_location_id"),
                    resultSet.getInt("to_location_id"),
                    resultSet.getDouble("distance_km"),
                    resultSet.getDouble("travel_time_min"),
                    resultSet.getDouble("condition_weight"),
                    resultSet.getString("route_label"),
                    resultSet.getString("road_type"),
                    resultSet.getString("traffic_level"),
                    blockedValue == 1);
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid roads row", exception);
        }
    }
}
