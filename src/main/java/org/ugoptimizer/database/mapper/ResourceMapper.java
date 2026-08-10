package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import org.ugoptimizer.model.Resource;

/** Maps one {@code resources} result row to the shared domain model. */
public final class ResourceMapper {

    private ResourceMapper() {
    }

    public static Resource map(ResultSet resultSet) throws SQLException {
        int currentLocationValue = resultSet.getInt("current_location_id");
        Integer currentLocationId = resultSet.wasNull() ? null : currentLocationValue;
        String shiftStartValue = resultSet.getString("shift_start");
        String shiftEndValue = resultSet.getString("shift_end");

        try {
            return new Resource(
                    resultSet.getInt("resource_id"),
                    resultSet.getString("resource_type"),
                    resultSet.getInt("home_location_id"),
                    resultSet.getInt("capacity"),
                    resultSet.getString("availability_status"),
                    currentLocationId,
                    shiftStartValue == null ? null : LocalTime.parse(shiftStartValue),
                    shiftEndValue == null ? null : LocalTime.parse(shiftEndValue));
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid resources row", exception);
        }
    }
}
