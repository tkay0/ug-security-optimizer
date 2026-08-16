package org.ugoptimizer.app;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.importers.CsvDatasetImporter;
import org.ugoptimizer.service.LocationService;
import org.ugoptimizer.service.PriorityService;
import org.ugoptimizer.service.ReportService;
import org.ugoptimizer.service.RequestService;
import org.ugoptimizer.service.ResourceService;
import org.ugoptimizer.service.RouteService;
import org.ugoptimizer.service.WorkflowService;
import org.ugoptimizer.service.inmemory.InMemoryLocationService;
import org.ugoptimizer.service.inmemory.InMemoryPriorityService;
import org.ugoptimizer.service.inmemory.InMemoryReportService;
import org.ugoptimizer.service.inmemory.InMemoryRequestService;
import org.ugoptimizer.service.inmemory.InMemoryResourceService;
import org.ugoptimizer.service.inmemory.InMemoryRouteService;
import org.ugoptimizer.service.inmemory.InMemoryWorkflowService;
import org.ugoptimizer.ui.MainMenu;

/**
 * Application entry point for the UG Campus Security &amp; Emergency Response Optimizer.
 */
public final class Main {

    private static final Path DATABASE_PATH = Path.of("database", "campus.db");
    private static final Path CANONICAL_DATA_DIRECTORY = Path.of("data");
    private static final String[] SEED_TABLES = {
        "locations", "roads", "resources", "service_requests", "road_scenarios",
        "audit_events", "algorithm_runs"
    };

    private Main() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static void main(String[] args) {
        try {
            bootstrapDatabase();
        } catch (IOException | SQLException failure) {
            System.err.println("Database bootstrap failed: " + failure.getMessage());
            failure.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "The database could not be initialized:\n\n" + failure.getMessage()
                            + "\n\nContinuing with in-memory sample data.",
                    "UG Campus Security Optimizer",
                    JOptionPane.WARNING_MESSAGE);
            // Non-fatal: nothing below reads the database yet (see the comment
            // on the in-memory services), so a failed bootstrap shouldn't block
            // the app from launching against its existing in-memory data.
        }

        // Swap any InMemoryXService below for a real DAO-backed implementation of
        // the same interface once the database team's work lands -- MainMenu and
        // every ui/menu/* screen depend only on the interfaces, not on these
        // in-memory classes, so no UI code needs to change. The database
        // bootstrapped above isn't read by any of these yet.
        LocationService locationService = new InMemoryLocationService();
        RequestService requestService = new InMemoryRequestService();
        ResourceService resourceService = new InMemoryResourceService();
        RouteService routeService = new InMemoryRouteService(locationService);
        WorkflowService workflowService = new InMemoryWorkflowService();
        ReportService reportService = new InMemoryReportService();
        PriorityService priorityService = new InMemoryPriorityService(requestService);

        SwingUtilities.invokeLater(() -> new MainMenu(
                locationService,
                requestService,
                resourceService,
                routeService,
                workflowService,
                reportService,
                priorityService).setVisible(true));
    }

    /**
     * Initializes the SQLite schema and imports the canonical CSVs exactly
     * once (only when every seed table is empty) -- the same bootstrap
     * sequence used on the {@code feature/team2-gui} branch. Safe to call on
     * every launch: a database that already has data is left untouched.
     */
    private static void bootstrapDatabase() throws IOException, SQLException {
        DatabaseManager manager = new DatabaseManager(DATABASE_PATH);
        manager.initializeSchema();

        if (seedTablesAreEmpty(manager)) {
            new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();
        }
    }

    private static boolean seedTablesAreEmpty(DatabaseManager manager) throws SQLException {
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            for (String table : SEED_TABLES) {
                try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    resultSet.next();
                    if (resultSet.getInt(1) != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
