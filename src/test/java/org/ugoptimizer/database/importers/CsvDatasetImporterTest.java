package org.ugoptimizer.database.importers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;

class CsvDatasetImporterTest {

    private static final Path CANONICAL_DATA_DIRECTORY = Path.of("data");
    private static final String[] DATA_FILES = {
        "locations.csv",
        "roads.csv",
        "service_requests.csv",
        "resources.csv",
        "road_scenarios.csv",
        "audit_events.csv",
        "algorithm_runs.csv"
    };
    private static final String[] TABLES = {
        "locations",
        "roads",
        "service_requests",
        "resources",
        "road_scenarios",
        "audit_events",
        "algorithm_runs"
    };
    private static final int[] EXPECTED_COUNTS = {50, 100, 300, 30, 12, 60, 30};

    @TempDir
    Path temporaryDirectory;

    @Test
    void importsAllCanonicalDatasetsWithExactCountsAndNoForeignKeyViolations()
            throws Exception {
        DatabaseManager manager = initializedManager("complete.db");

        new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();

        try (Connection connection = manager.openConnection()) {
            assertExpectedCounts(connection);
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(resultSet.next());
            }
        }
        assertTrue(manager.getDatabasePath().startsWith(temporaryDirectory));
    }

    @Test
    void mapsNullsBooleansAndCamelCaseExtensionColumns() throws Exception {
        DatabaseManager manager = initializedManager("mappings.db");
        new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();

        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT time_ns, memory_kb, date_run, status, experiment_group, run_number "
                            + "FROM algorithm_runs WHERE run_id = 1")) {
                assertTrue(resultSet.next());
                assertNull(resultSet.getObject("time_ns"));
                assertNull(resultSet.getObject("memory_kb"));
                assertNull(resultSet.getObject("date_run"));
                assertEquals("PLANNED", resultSet.getString("status"));
                assertEquals("BFS_50", resultSet.getString("experiment_group"));
                assertEquals(1, resultSet.getInt("run_number"));
            }

            assertEquals(0, scalar(statement, "SELECT MAX(is_blocked) FROM roads"));
            assertEquals(0, scalar(statement,
                    "SELECT MIN(is_blocked_override) FROM road_scenarios"));
            assertEquals(1, scalar(statement,
                    "SELECT MAX(is_blocked_override) FROM road_scenarios"));

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT scenario_name, road_id, scenario_start, is_blocked_override "
                            + "FROM road_scenarios WHERE scenario_id = 1")) {
                assertTrue(resultSet.next());
                assertEquals("ACCESS_BLOCKAGE_DRILL", resultSet.getString("scenario_name"));
                assertEquals(6, resultSet.getInt("road_id"));
                assertEquals("2026-08-07T16:00:00Z", resultSet.getString("scenario_start"));
                assertEquals(1, resultSet.getInt("is_blocked_override"));
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT event_timestamp, entity_type, entity_id "
                            + "FROM audit_events WHERE event_id = 1")) {
                assertTrue(resultSet.next());
                assertEquals("2026-08-01T07:37:00Z", resultSet.getString("event_timestamp"));
                assertEquals("SERVICE_REQUEST", resultSet.getString("entity_type"));
                assertEquals(18, resultSet.getInt("entity_id"));
            }
        }
    }

    @Test
    void unexpectedHeaderIsRejectedWithoutAnyImportedRows() throws Exception {
        Path fixture = copiedDataset("bad-header");
        replaceLine(fixture.resolve("locations.csv"), 1, "unexpected,header");
        DatabaseManager manager = initializedManager("bad-header.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("locations.csv"));
        assertTrue(exception.getMessage().contains("unexpected header"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void malformedRowWidthRollsBackEarlierDatasets() throws Exception {
        Path fixture = copiedDataset("bad-width");
        replaceLine(fixture.resolve("road_scenarios.csv"), 2, "1,too,few,columns");
        DatabaseManager manager = initializedManager("bad-width.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("road_scenarios.csv"));
        assertTrue(exception.getMessage().contains("row 2"));
        assertTrue(exception.getMessage().contains("expected 10 columns"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void invalidNumberLateInImportRollsBackCompleteTransaction() throws Exception {
        Path fixture = copiedDataset("bad-number");
        replaceLine(
                fixture.resolve("algorithm_runs.csv"),
                2,
                "1,BFS,not-a-number,,,,PLANNED,BFS_50,1");
        DatabaseManager manager = initializedManager("bad-number.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("algorithm_runs.csv"));
        assertTrue(exception.getMessage().contains("inputSize"));
        assertTrue(exception.getMessage().contains("not-a-number"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void invalidBooleanIsRejectedRatherThanSilentlyMappedToFalse() throws Exception {
        Path fixture = copiedDataset("bad-boolean");
        replaceLine(
                fixture.resolve("roads.csv"),
                2,
                "1,1,2,0.11,1.18,1.16,Main Gate - Police,MAIN_ROAD,HIGH,perhaps");
        DatabaseManager manager = initializedManager("bad-boolean.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("is_blocked"));
        assertTrue(exception.getMessage().contains("perhaps"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void invalidForeignKeyRecordIsRejectedAndRolledBack() throws Exception {
        Path fixture = copiedDataset("bad-foreign-key");
        replaceLine(
                fixture.resolve("roads.csv"),
                2,
                "1,999,2,0.11,1.18,1.16,Missing endpoint,MAIN_ROAD,HIGH,False");
        DatabaseManager manager = initializedManager("bad-foreign-key.db");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("roads.csv"));
        assertTrue(exception.getMessage().contains("row 2"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void unavailableRequiredResourceTypeIsRejectedAndRolledBack() throws Exception {
        Path fixture = copiedDataset("bad-resource-type");
        replaceLine(
                fixture.resolve("service_requests.csv"),
                2,
                "1,1,13,THEFT_REPORT,2,2026-08-04T16:17:00Z,"
                        + "2026-08-04T19:46:00Z,IN_PROGRESS,UNKNOWN_TEAM,Theft report request.");
        DatabaseManager manager = initializedManager("bad-resource-type.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("required_resource_type"));
        assertTrue(exception.getMessage().contains("UNKNOWN_TEAM"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void invalidPolymorphicAuditTargetIsRejectedAndRolledBack() throws Exception {
        Path fixture = copiedDataset("bad-audit-target");
        replaceLine(
                fixture.resolve("audit_events.csv"),
                2,
                "1,REQUEST_CREATED,2026-08-01T07:37:00Z,SERVICE_REQUEST,9999,"
                        + "DISPATCH_OPERATOR,Missing request target.");
        DatabaseManager manager = initializedManager("bad-audit.db");

        IOException exception = assertThrows(
                IOException.class,
                () -> new CsvDatasetImporter(manager, fixture).importAll());

        assertTrue(exception.getMessage().contains("audit_events.csv"));
        assertTrue(exception.getMessage().contains("entityId"));
        assertTrue(exception.getMessage().contains("9999"));
        assertAllTablesEmpty(manager);
    }

    @Test
    void repeatedImportFailsClearlyAndPreservesFirstImport() throws Exception {
        DatabaseManager manager = initializedManager("repeat.db");
        CsvDatasetImporter importer = new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY);
        importer.importAll();

        SQLException exception = assertThrows(SQLException.class, importer::importAll);

        assertTrue(exception.getMessage().contains("requires an empty database"));
        try (Connection connection = manager.openConnection()) {
            assertExpectedCounts(connection);
        }
    }

    private DatabaseManager initializedManager(String databaseFile) throws Exception {
        Path databasePath = temporaryDirectory.resolve(databaseFile);
        DatabaseManager manager = new DatabaseManager(databasePath);
        manager.initializeSchema();
        assertTrue(databasePath.startsWith(temporaryDirectory));
        return manager;
    }

    private Path copiedDataset(String directoryName) throws IOException {
        Path fixture = temporaryDirectory.resolve(directoryName);
        Files.createDirectories(fixture);
        for (String file : DATA_FILES) {
            Files.copy(
                    CANONICAL_DATA_DIRECTORY.resolve(file),
                    fixture.resolve(file),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return fixture;
    }

    private static void replaceLine(Path file, int physicalRowNumber, String replacement)
            throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        lines.set(physicalRowNumber - 1, replacement);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void assertExpectedCounts(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (int index = 0; index < TABLES.length; index++) {
                assertEquals(
                        EXPECTED_COUNTS[index],
                        scalar(statement, "SELECT COUNT(*) FROM " + TABLES[index]),
                        TABLES[index]);
            }
        }
    }

    private static void assertAllTablesEmpty(DatabaseManager manager) throws SQLException {
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                assertEquals(0, scalar(statement, "SELECT COUNT(*) FROM " + table), table);
            }
        }
    }

    private static int scalar(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
