package org.ugoptimizer.app;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.importers.CsvDatasetImporter;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.SecurityControlRoom;

/**
 * Application entry point. Bootstraps the SQLite database from the canonical
 * datasets and launches the Swing security operations control room on the
 * event-dispatch thread.
 */
public final class Main {

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
            DatabaseManager manager = new DatabaseManager(AppContext.defaultDatabasePath());
            manager.initializeSchema();
            importSeedDataIfEmpty(manager);

            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // keep the cross-platform look and feel when the system one is unavailable
                }
                SecurityControlRoom room = new SecurityControlRoom(new AppContext(manager));
                room.setVisible(true);
            });
        } catch (Exception failure) {
            System.err.println("The application could not start: " + failure.getMessage());
            failure.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "The application could not start.\n\n" + failure.getMessage(),
                    "UG Security Operations",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Imports the canonical CSVs exactly once. The importer is atomic and
     * requires empty seed tables, so a database that already contains data is
     * left untouched.
     */
    private static void importSeedDataIfEmpty(DatabaseManager manager)
            throws java.io.IOException, SQLException {
        boolean empty = true;
        try (Connection connection = manager.openConnection();
                Statement statement = connection.createStatement()) {
            for (String table : SEED_TABLES) {
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + table)) {
                    resultSet.next();
                    if (resultSet.getInt(1) != 0) {
                        empty = false;
                        break;
                    }
                }
            }
        }
        if (empty) {
            new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();
        }
    }
}
