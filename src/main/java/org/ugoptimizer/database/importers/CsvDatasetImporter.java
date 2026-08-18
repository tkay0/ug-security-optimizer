package org.ugoptimizer.database.importers;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.importers.CsvRecordParser.CsvRecord;

/**
 * Imports the seven canonical CSV datasets into an initialized empty database.
 *
 * <p>The complete import uses one connection and one transaction. Any parsing,
 * validation, or SQL failure rolls back every inserted row. Re-importing into a
 * non-empty seed table fails clearly; the importer never clears, replaces,
 * merges, or silently overwrites existing records.</p>
 */
public final class CsvDatasetImporter {

    private static final String LOCATIONS_FILE = "locations.csv";
    private static final String ROADS_FILE = "roads.csv";
    private static final String REQUESTS_FILE = "service_requests.csv";
    private static final String RESOURCES_FILE = "resources.csv";
    private static final String SCENARIOS_FILE = "road_scenarios.csv";
    private static final String AUDIT_FILE = "audit_events.csv";
    private static final String RUNS_FILE = "algorithm_runs.csv";

    private static final String[] LOCATIONS_HEADER = {
        "location_id", "name", "area", "location_type", "x_coord", "y_coord",
        "operating_hours", "source_url"
    };
    private static final String[] ROADS_HEADER = {
        "road_id", "from_location_id", "to_location_id", "distance_km",
        "travel_time_min", "condition_weight", "route_label", "road_type",
        "traffic_level", "is_blocked"
    };
    private static final String[] REQUESTS_HEADER = {
        "request_id", "source_location_id", "destination_location_id", "category",
        "urgency", "time_submitted", "deadline", "status", "required_resource_type",
        "description"
    };
    private static final String[] RESOURCES_HEADER = {
        "resource_id", "resource_type", "home_location_id", "capacity",
        "availability_status", "current_location_id", "shift_start", "shift_end"
    };
    private static final String[] SCENARIOS_HEADER = {
        "scenarioId", "scenarioName", "roadId", "routeLabel", "scenarioStart",
        "scenarioEnd", "isBlockedOverride", "conditionWeightMultiplier",
        "travelTimeMultiplier", "reason"
    };
    private static final String[] AUDIT_HEADER = {
        "eventId", "eventType", "timestamp", "entityType", "entityId", "actorType",
        "details"
    };
    private static final String[] RUNS_HEADER = {
        "runId", "algorithmName", "inputSize", "timeNs", "memoryKb", "dateRun",
        "status", "experimentGroup", "runNumber"
    };

    private static final String[] SEED_TABLES = {
        "locations", "roads", "resources", "service_requests", "road_scenarios",
        "audit_events", "algorithm_runs"
    };

    private final DatabaseManager databaseManager;
    private final Path datasetDirectory;

    /** Creates an importer for a configured database and dataset directory. */
    public CsvDatasetImporter(DatabaseManager databaseManager, Path datasetDirectory) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "databaseManager cannot be null");
        this.datasetDirectory = Objects.requireNonNull(
                datasetDirectory, "datasetDirectory cannot be null").toAbsolutePath().normalize();
    }

    /**
     * Imports all seven datasets atomically into an initialized empty database.
     *
     * @throws IOException for missing/malformed CSV data or failed validation
     * @throws SQLException for schema, constraint, or transaction failures
     */
    public void importAll() throws IOException, SQLException {
        importAll(false);
    }

    /**
     * Imports the canonical dataset only when every seed table is empty.
     *
     * @return {@code true} when data was imported, or {@code false} when an existing dataset was
     *         left untouched
     */
    public boolean importAllIfEmpty() throws IOException, SQLException {
        return importAll(true);
    }

    private boolean importAll(boolean skipExistingDataset) throws IOException, SQLException {
        try (Connection connection = databaseManager.openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable failure = null;
            connection.setAutoCommit(false);
            try {
                if (skipExistingDataset && containsSeedData(connection)) {
                    connection.rollback();
                    return false;
                }
                requireEmptySeedTables(connection);
                importLocations(connection);
                importRoads(connection);
                importResources(connection);
                importServiceRequests(connection);
                importRoadScenarios(connection);
                importAuditEvents(connection);
                importAlgorithmRuns(connection);
                connection.commit();
                return true;
            } catch (IOException | SQLException | RuntimeException exception) {
                failure = exception;
                rollback(connection, exception);
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException restoreException) {
                    if (failure != null) {
                        failure.addSuppressed(restoreException);
                    } else {
                        throw restoreException;
                    }
                }
            }
        }
    }

    private static boolean containsSeedData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String table : SEED_TABLES) {
                try (ResultSet resultSet =
                        statement.executeQuery("SELECT 1 FROM " + table + " LIMIT 1")) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void importLocations(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                LOCATIONS_FILE,
                LOCATIONS_HEADER,
                50,
                "INSERT INTO locations (location_id, name, area, location_type, x_coord, "
                        + "y_coord, operating_hours, source_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    statement.setInt(1, integer(file, record, 0, "location_id"));
                    statement.setString(2, requiredText(file, record, 1, "name"));
                    statement.setString(3, requiredText(file, record, 2, "area"));
                    statement.setString(4, requiredText(file, record, 3, "location_type"));
                    statement.setInt(5, integer(file, record, 4, "x_coord"));
                    statement.setInt(6, integer(file, record, 5, "y_coord"));
                    setNullableText(statement, 7, nullableText(file, record, 6, "operating_hours"));
                    statement.setString(8, requiredText(file, record, 7, "source_url"));
                });
    }

    private void importRoads(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                ROADS_FILE,
                ROADS_HEADER,
                100,
                "INSERT INTO roads (road_id, from_location_id, to_location_id, distance_km, "
                        + "travel_time_min, condition_weight, route_label, road_type, "
                        + "traffic_level, is_blocked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    statement.setInt(1, integer(file, record, 0, "road_id"));
                    statement.setInt(2, integer(file, record, 1, "from_location_id"));
                    statement.setInt(3, integer(file, record, 2, "to_location_id"));
                    statement.setDouble(4, real(file, record, 3, "distance_km"));
                    statement.setDouble(5, real(file, record, 4, "travel_time_min"));
                    statement.setDouble(6, real(file, record, 5, "condition_weight"));
                    statement.setString(7, requiredText(file, record, 6, "route_label"));
                    setNullableText(statement, 8, nullableText(file, record, 7, "road_type"));
                    setNullableText(statement, 9, nullableText(file, record, 8, "traffic_level"));
                    statement.setInt(10, bool(file, record, 9, "is_blocked"));
                });
    }

    private void importResources(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                RESOURCES_FILE,
                RESOURCES_HEADER,
                30,
                "INSERT INTO resources (resource_id, resource_type, home_location_id, capacity, "
                        + "availability_status, current_location_id, shift_start, shift_end) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    statement.setInt(1, integer(file, record, 0, "resource_id"));
                    statement.setString(2, requiredText(file, record, 1, "resource_type"));
                    statement.setInt(3, integer(file, record, 2, "home_location_id"));
                    statement.setInt(4, integer(file, record, 3, "capacity"));
                    statement.setString(5, requiredText(file, record, 4, "availability_status"));
                    setNullableInteger(
                            statement,
                            6,
                            nullableInteger(file, record, 5, "current_location_id"));
                    setNullableText(statement, 7, nullableText(file, record, 6, "shift_start"));
                    setNullableText(statement, 8, nullableText(file, record, 7, "shift_end"));
                });
    }

    private void importServiceRequests(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                REQUESTS_FILE,
                REQUESTS_HEADER,
                300,
                "INSERT INTO service_requests (request_id, source_location_id, "
                        + "destination_location_id, category, urgency, time_submitted, deadline, "
                        + "status, required_resource_type, description) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    String requiredResourceType =
                            nullableText(file, record, 8, "required_resource_type");
                    validateRequiredResourceType(
                            connection, file, record, requiredResourceType);

                    statement.setInt(1, integer(file, record, 0, "request_id"));
                    statement.setInt(2, integer(file, record, 1, "source_location_id"));
                    statement.setInt(3, integer(file, record, 2, "destination_location_id"));
                    statement.setString(4, requiredText(file, record, 3, "category"));
                    statement.setInt(5, integer(file, record, 4, "urgency"));
                    statement.setString(6, requiredText(file, record, 5, "time_submitted"));
                    statement.setString(7, requiredText(file, record, 6, "deadline"));
                    statement.setString(8, requiredText(file, record, 7, "status"));
                    setNullableText(statement, 9, requiredResourceType);
                    setNullableText(statement, 10, nullableText(file, record, 9, "description"));
                });
    }

    private void importRoadScenarios(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                SCENARIOS_FILE,
                SCENARIOS_HEADER,
                12,
                "INSERT INTO road_scenarios (scenario_id, scenario_name, road_id, route_label, "
                        + "scenario_start, scenario_end, is_blocked_override, "
                        + "condition_weight_multiplier, travel_time_multiplier, reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    statement.setInt(1, integer(file, record, 0, "scenarioId"));
                    statement.setString(2, requiredText(file, record, 1, "scenarioName"));
                    statement.setInt(3, integer(file, record, 2, "roadId"));
                    statement.setString(4, requiredText(file, record, 3, "routeLabel"));
                    statement.setString(5, requiredText(file, record, 4, "scenarioStart"));
                    statement.setString(6, requiredText(file, record, 5, "scenarioEnd"));
                    statement.setInt(7, bool(file, record, 6, "isBlockedOverride"));
                    statement.setDouble(
                            8, real(file, record, 7, "conditionWeightMultiplier"));
                    statement.setDouble(9, real(file, record, 8, "travelTimeMultiplier"));
                    statement.setString(10, requiredText(file, record, 9, "reason"));
                });
    }

    private void importAuditEvents(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                AUDIT_FILE,
                AUDIT_HEADER,
                60,
                "INSERT INTO audit_events (event_id, event_type, event_timestamp, entity_type, "
                        + "entity_id, actor_type, details) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    int entityId = integer(file, record, 4, "entityId");
                    String entityType = requiredText(file, record, 3, "entityType");
                    validateAuditTarget(connection, file, record, entityType, entityId);

                    statement.setInt(1, integer(file, record, 0, "eventId"));
                    statement.setString(2, requiredText(file, record, 1, "eventType"));
                    statement.setString(3, requiredText(file, record, 2, "timestamp"));
                    statement.setString(4, entityType);
                    statement.setInt(5, entityId);
                    setNullableText(statement, 6, nullableText(file, record, 5, "actorType"));
                    setNullableText(statement, 7, nullableText(file, record, 6, "details"));
                });
    }

    private void importAlgorithmRuns(Connection connection) throws IOException, SQLException {
        importFile(
                connection,
                RUNS_FILE,
                RUNS_HEADER,
                30,
                "INSERT INTO algorithm_runs (run_id, algorithm_name, input_size, time_ns, "
                        + "memory_kb, date_run, status, experiment_group, run_number) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, record, file) -> {
                    statement.setInt(1, integer(file, record, 0, "runId"));
                    statement.setString(2, requiredText(file, record, 1, "algorithmName"));
                    statement.setInt(3, integer(file, record, 2, "inputSize"));
                    setNullableLong(statement, 4, nullableLong(file, record, 3, "timeNs"));
                    setNullableReal(statement, 5, nullableReal(file, record, 4, "memoryKb"));
                    setNullableText(statement, 6, nullableText(file, record, 5, "dateRun"));
                    statement.setString(7, requiredText(file, record, 6, "status"));
                    statement.setString(8, requiredText(file, record, 7, "experimentGroup"));
                    statement.setInt(9, integer(file, record, 8, "runNumber"));
                });
    }

    private int importFile(
            Connection connection,
            String fileName,
            String[] expectedHeader,
            int expectedRows,
            String insertSql,
            RowBinder binder) throws IOException, SQLException {
        Path file = datasetDirectory.resolve(fileName);
        int importedRows = 0;

        try (CsvRecordParser parser = new CsvRecordParser(file)) {
            CsvRecord header = parser.nextRecord();
            if (header == null || !Arrays.equals(expectedHeader, header.values())) {
                String actual = header == null ? "<missing>" : Arrays.toString(header.values());
                throw new IOException(
                        file + " row 1: unexpected header; expected "
                                + Arrays.toString(expectedHeader) + " but found " + actual);
            }

            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                CsvRecord record;
                while ((record = parser.nextRecord()) != null) {
                    requireWidth(file, record, expectedHeader.length);
                    try {
                        binder.bind(statement, record, file);
                        statement.executeUpdate();
                    } catch (SQLException exception) {
                        throw new SQLException(
                                file + " row " + record.physicalRowNumber()
                                        + ": database rejected record: " + exception.getMessage(),
                                exception);
                    }
                    importedRows++;
                }
            }
        }

        if (importedRows != expectedRows) {
            throw new IOException(
                    file + ": expected " + expectedRows
                            + " data rows but found " + importedRows);
        }
        return importedRows;
    }

    private static void requireEmptySeedTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String table : SEED_TABLES) {
                try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (!resultSet.next()) {
                        throw new SQLException("Could not count seed table " + table);
                    }
                    int count = resultSet.getInt(1);
                    if (count != 0) {
                        throw new SQLException(
                                "Seed import requires an empty database; table "
                                        + table + " contains " + count + " record(s)");
                    }
                }
            }
        }
    }

    private static void validateAuditTarget(
            Connection connection,
            Path file,
            CsvRecord record,
            String entityType,
            int entityId) throws IOException, SQLException {
        String query = switch (entityType) {
            case "SERVICE_REQUEST" ->
                    "SELECT 1 FROM service_requests WHERE request_id = ?";
            case "RESOURCE" -> "SELECT 1 FROM resources WHERE resource_id = ?";
            case "ROAD" -> "SELECT 1 FROM roads WHERE road_id = ?";
            default -> throw invalid(
                    file, record, "entityType", entityType, "unsupported audit entity type", null);
        };

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, entityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw invalid(
                            file,
                            record,
                            "entityId",
                            Integer.toString(entityId),
                            "no " + entityType + " target exists",
                            null);
                }
            }
        }
    }

    private static void validateRequiredResourceType(
            Connection connection,
            Path file,
            CsvRecord record,
            String resourceType) throws IOException, SQLException {
        if (resourceType == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM resources WHERE resource_type = ? LIMIT 1")) {
            statement.setString(1, resourceType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw invalid(
                            file,
                            record,
                            "required_resource_type",
                            resourceType,
                            "no matching resource type exists",
                            null);
                }
            }
        }
    }

    private static void requireWidth(Path file, CsvRecord record, int expectedWidth)
            throws IOException {
        if (record.values().length != expectedWidth) {
            throw new IOException(
                    file + " row " + record.physicalRowNumber()
                            + ": expected " + expectedWidth + " columns but found "
                            + record.values().length);
        }
    }

    private static int integer(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = requiredText(file, record, index, column);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalid(file, record, column, value, "expected an integer", exception);
        }
    }

    private static Integer nullableInteger(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = nullableText(file, record, index, column);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw invalid(file, record, column, value, "expected an integer or blank", exception);
        }
    }

    private static long nullableLongValue(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = requiredText(file, record, index, column);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalid(file, record, column, value, "expected a long integer", exception);
        }
    }

    private static Long nullableLong(Path file, CsvRecord record, int index, String column)
            throws IOException {
        if (record.values()[index].isEmpty()) {
            return null;
        }
        return nullableLongValue(file, record, index, column);
    }

    private static double real(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = requiredText(file, record, index, column);
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                throw new NumberFormatException("value is not finite");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(file, record, column, value, "expected a finite number", exception);
        }
    }

    private static Double nullableReal(Path file, CsvRecord record, int index, String column)
            throws IOException {
        if (record.values()[index].isEmpty()) {
            return null;
        }
        return real(file, record, index, column);
    }

    private static int bool(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = requiredText(file, record, index, column);
        if ("true".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(value)) {
            return 0;
        }
        throw invalid(file, record, column, value, "expected True or False", null);
    }

    private static String requiredText(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = record.values()[index];
        if (value.isBlank()) {
            throw invalid(file, record, column, value, "value is required", null);
        }
        return value;
    }

    private static String nullableText(Path file, CsvRecord record, int index, String column)
            throws IOException {
        String value = record.values()[index];
        if (value.isEmpty()) {
            return null;
        }
        if (value.isBlank()) {
            throw invalid(file, record, column, value, "blank whitespace is not valid", null);
        }
        return value;
    }

    private static IOException invalid(
            Path file,
            CsvRecord record,
            String column,
            String value,
            String reason,
            Throwable cause) {
        String message = file + " row " + record.physicalRowNumber()
                + " column " + column + " value '" + value + "': " + reason;
        return cause == null ? new IOException(message) : new IOException(message, cause);
    }

    private static void setNullableText(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableReal(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.REAL);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(PreparedStatement statement, CsvRecord record, Path file)
                throws IOException, SQLException;
    }
}
