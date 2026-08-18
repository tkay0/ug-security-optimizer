package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.RoadMapper;
import org.ugoptimizer.model.Road;

/** Provides persistent access to baseline roads. */
public final class RoadDao {

    private static final String COLUMNS =
            "road_id, from_location_id, to_location_id, distance_km, travel_time_min, "
                    + "condition_weight, route_label, road_type, traffic_level, is_blocked";
    private static final int INITIAL_RESULT_CAPACITY = 16;

    private final DatabaseManager databaseManager;

    public RoadDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
    }

    public Optional<Road> findById(int roadId) throws SQLException {
        requirePositiveId(roadId);
        String sql = "SELECT " + COLUMNS + " FROM roads WHERE road_id = ?";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roadId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(RoadMapper.map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new SQLException("Failed to find road " + roadId, exception);
        }
    }

    public Road[] findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM roads ORDER BY road_id";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            Road[] buffer = new Road[INITIAL_RESULT_CAPACITY];
            int count = 0;
            while (resultSet.next()) {
                if (count == buffer.length) {
                    buffer = grow(buffer);
                }
                buffer[count++] = RoadMapper.map(resultSet);
            }
            Road[] result = new Road[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        } catch (SQLException exception) {
            throw new SQLException("Failed to read all roads", exception);
        }
    }

    public void insert(Road road) throws SQLException {
        Objects.requireNonNull(road, "road cannot be null");
        String sql = "INSERT INTO roads (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, road.getRoadId());
            statement.setInt(2, road.getFromLocationId());
            statement.setInt(3, road.getToLocationId());
            statement.setDouble(4, road.getDistanceKm());
            statement.setDouble(5, road.getTravelTimeMin());
            statement.setDouble(6, road.getConditionWeight());
            statement.setString(7, road.getRouteLabel());
            setNullableText(statement, 8, road.getRoadType());
            setNullableText(statement, 9, road.getTrafficLevel());
            statement.setInt(10, road.isBlocked() ? 1 : 0);
            requireSingleInsert(statement.executeUpdate());
        } catch (SQLException exception) {
            throw new SQLException("Failed to insert road " + road.getRoadId(), exception);
        }
    }

    private static Road[] grow(Road[] current) throws SQLException {
        if (current.length > Integer.MAX_VALUE / 2) {
            throw new SQLException("Road result exceeds supported array capacity");
        }
        Road[] expanded = new Road[current.length * 2];
        System.arraycopy(current, 0, expanded, 0, current.length);
        return expanded;
    }

    private static void requirePositiveId(int roadId) {
        if (roadId <= 0) {
            throw new IllegalArgumentException("roadId must be positive");
        }
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
            throw new SQLException("Road insert affected " + affectedRows + " rows");
        }
    }
}
