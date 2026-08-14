package org.ugoptimizer.database.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.Road;

class RoadDaoTest {

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager manager;
    private RoadDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = DaoTestDatabase.create(temporaryDirectory, "roads.db");
        dao = new RoadDao(manager);
    }

    @Test
    void readsKnownSeededRoadWithEveryField() throws Exception {
        Road road = dao.findById(1).orElseThrow();

        assertEquals(1, road.getRoadId());
        assertEquals(1, road.getFromLocationId());
        assertEquals(2, road.getToLocationId());
        assertEquals(0.11d, road.getDistanceKm());
        assertEquals(1.18d, road.getTravelTimeMin());
        assertEquals(1.16d, road.getConditionWeight());
        assertEquals("Main University Gate - Legon Police Station", road.getRouteLabel());
        assertEquals("MAIN_ROAD", road.getRoadType());
        assertEquals("HIGH", road.getTrafficLevel());
        assertFalse(road.isBlocked());
    }

    @Test
    void missingIdIsEmptyAndInvalidIdIsRejected() throws Exception {
        assertTrue(dao.findById(999).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.findById(-1));
    }

    @Test
    void findAllReturnsOneHundredRoadsInIdOrder() throws Exception {
        Road[] roads = dao.findAll();

        assertEquals(100, roads.length);
        for (int index = 0; index < roads.length; index++) {
            assertEquals(index + 1, roads[index].getRoadId());
        }
    }

    @Test
    void mapsTrueBooleanAndNullableRoadMetadata() throws Exception {
        try (Connection connection = manager.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE roads SET road_type = NULL, traffic_level = NULL, "
                                + "is_blocked = 1 WHERE road_id = 1")) {
            assertEquals(1, statement.executeUpdate());
        }

        Road road = dao.findById(1).orElseThrow();
        assertNull(road.getRoadType());
        assertNull(road.getTrafficLevel());
        assertTrue(road.isBlocked());
    }
}
