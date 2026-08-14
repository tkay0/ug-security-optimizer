package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.Location;

class LocationDaoTest {

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager manager;
    private LocationDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = DaoTestDatabase.create(temporaryDirectory, "locations.db");
        dao = new LocationDao(manager);
    }

    @Test
    void readsKnownSeededLocationWithEveryField() throws Exception {
        Location location = dao.findById(1).orElseThrow();

        assertEquals(1, location.getLocationId());
        assertEquals("Main University Gate", location.getName());
        assertEquals("Main Entrance", location.getArea());
        assertEquals("GATE", location.getLocationType());
        assertEquals(0, location.getXCoord());
        assertEquals(0, location.getYCoord());
        assertEquals("24/7", location.getOperatingHours());
        assertEquals("https://old1.ug.edu.gh/about/overview", location.getSourceUrl());
    }

    @Test
    void missingIdIsEmptyAndInvalidIdIsRejected() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(0));
    }

    @Test
    void findAllReturnsFiftyLocationsInIdOrder() throws Exception {
        Location[] locations = dao.findAll();

        assertEquals(50, locations.length);
        for (int index = 0; index < locations.length; index++) {
            assertEquals(index + 1, locations[index].getLocationId());
        }
    }

    @Test
    void daoUsesTemporaryDatabaseWithForeignKeysEnabled() throws Exception {
        assertTrue(manager.getDatabasePath().startsWith(temporaryDirectory));
        try (Connection connection = manager.openConnection()) {
            assertTrue(manager.isForeignKeyEnforcementEnabled(connection));
            assertFalse(connection.isClosed());
        }
    }
}
