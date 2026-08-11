package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.ugoptimizer.model.Location;

/** Maps one {@code locations} result row to the shared domain model. */
public final class LocationMapper {

    private LocationMapper() {
    }

    public static Location map(ResultSet resultSet) throws SQLException {
        try {
            return new Location(
                    resultSet.getInt("location_id"),
                    resultSet.getString("name"),
                    resultSet.getString("area"),
                    resultSet.getString("location_type"),
                    resultSet.getInt("x_coord"),
                    resultSet.getInt("y_coord"),
                    resultSet.getString("operating_hours"),
                    resultSet.getString("source_url"));
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid locations row", exception);
        }
    }
}
