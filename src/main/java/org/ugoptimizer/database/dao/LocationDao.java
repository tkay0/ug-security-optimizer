package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.LocationMapper;
import org.ugoptimizer.model.Location;

/** Provides read-only persistent access to campus locations. */
public final class LocationDao {

    private static final String COLUMNS =
            "location_id, name, area, location_type, x_coord, y_coord, "
                    + "operating_hours, source_url";
    private static final int INITIAL_RESULT_CAPACITY = 16;

    private final DatabaseManager databaseManager;

    public LocationDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
    }

    public Optional<Location> findById(int locationId) throws SQLException {
        requirePositiveId(locationId);
        String sql = "SELECT " + COLUMNS + " FROM locations WHERE location_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, locationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(LocationMapper.map(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new SQLException("Failed to find location " + locationId, exception);
        }
    }

    public Location[] findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM locations ORDER BY location_id";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            Location[] buffer = new Location[INITIAL_RESULT_CAPACITY];
            int count = 0;
            while (resultSet.next()) {
                if (count == buffer.length) {
                    buffer = grow(buffer);
                }
                buffer[count++] = LocationMapper.map(resultSet);
            }
            Location[] result = new Location[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        } catch (SQLException exception) {
            throw new SQLException("Failed to read all locations", exception);
        }
    }

    private static Location[] grow(Location[] current) throws SQLException {
        if (current.length > Integer.MAX_VALUE / 2) {
            throw new SQLException("Location result exceeds supported array capacity");
        }
        Location[] expanded = new Location[current.length * 2];
        System.arraycopy(current, 0, expanded, 0, current.length);
        return expanded;
    }

    private static void requirePositiveId(int locationId) {
        if (locationId <= 0) {
            throw new IllegalArgumentException("locationId must be positive");
        }
    }
}
