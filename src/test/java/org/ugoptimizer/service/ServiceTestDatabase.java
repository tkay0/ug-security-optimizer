package org.ugoptimizer.service;

import java.nio.file.Path;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.importers.CsvDatasetImporter;

/** Creates isolated SQLite databases for backend-service integration tests. */
final class ServiceTestDatabase {

    private ServiceTestDatabase() {
    }

    static DatabaseManager createSeeded(Path directory, String fileName) throws Exception {
        DatabaseManager manager = new DatabaseManager(directory.resolve(fileName));
        manager.initializeSchema();
        new CsvDatasetImporter(manager, Path.of("data")).importAll();
        return manager;
    }

    static DatabaseManager createEmpty(Path directory, String fileName) throws Exception {
        DatabaseManager manager = new DatabaseManager(directory.resolve(fileName));
        manager.initializeSchema();
        return manager;
    }
}
