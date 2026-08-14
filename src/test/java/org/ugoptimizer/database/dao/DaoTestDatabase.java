package org.ugoptimizer.database.dao;

import java.nio.file.Path;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.importers.CsvDatasetImporter;

/** Creates isolated, canonically seeded databases for DAO tests. */
final class DaoTestDatabase {

    private static final Path CANONICAL_DATA_DIRECTORY = Path.of("data");

    private DaoTestDatabase() {
    }

    static DatabaseManager create(Path temporaryDirectory, String fileName) throws Exception {
        DatabaseManager manager = new DatabaseManager(temporaryDirectory.resolve(fileName));
        manager.initializeSchema();
        new CsvDatasetImporter(manager, CANONICAL_DATA_DIRECTORY).importAll();
        return manager;
    }
}
