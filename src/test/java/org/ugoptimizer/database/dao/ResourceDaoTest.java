package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.Resource;

class ResourceDaoTest {

    @TempDir
    Path temporaryDirectory;

    private ResourceDao dao;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager manager = DaoTestDatabase.create(temporaryDirectory, "resources.db");
        dao = new ResourceDao(manager);
    }

    @Test
    void readsKnownResourceAndAllResourcesInIdOrder() throws Exception {
        Resource resource = dao.findById(1).orElseThrow();
        assertEquals("PATROL_OFFICER", resource.getResourceType());
        assertEquals(45, resource.getHomeLocationId());
        assertEquals("AVAILABLE", resource.getAvailabilityStatus());
        assertEquals(45, resource.getCurrentLocationId());
        assertEquals(LocalTime.of(6, 0), resource.getShiftStart());
        assertEquals(LocalTime.of(14, 0), resource.getShiftEnd());

        Resource[] resources = dao.findAll();
        assertEquals(30, resources.length);
        for (int index = 0; index < resources.length; index++) {
            assertEquals(index + 1, resources[index].getResourceId());
        }
    }

    @Test
    void missingIdIsEmptyAndInvalidIdIsRejected() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(0));
    }

    @Test
    void updatesAvailabilityAndPersistsIt() throws Exception {
        assertTrue(dao.updateAvailability(1, "BUSY"));
        assertEquals("BUSY", dao.findById(1).orElseThrow().getAvailabilityStatus());
    }

    @Test
    void updatesAndClearsCurrentLocation() throws Exception {
        assertTrue(dao.updateCurrentLocation(1, 2));
        assertEquals(2, dao.findById(1).orElseThrow().getCurrentLocationId());

        assertTrue(dao.updateCurrentLocation(1, null));
        assertNull(dao.findById(1).orElseThrow().getCurrentLocationId());
    }

    @Test
    void invalidCurrentLocationForeignKeyIsRejected() {
        assertThrows(SQLException.class, () -> dao.updateCurrentLocation(1, 999));
    }

    @Test
    void missingUpdatesReturnFalse() throws Exception {
        assertFalse(dao.updateAvailability(999, "AVAILABLE"));
        assertFalse(dao.updateCurrentLocation(999, null));
    }

    @Test
    void invalidUpdateArgumentsAreRejectedBeforeSql() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateAvailability(1, "UNKNOWN"));
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateCurrentLocation(1, 0));
    }
}
