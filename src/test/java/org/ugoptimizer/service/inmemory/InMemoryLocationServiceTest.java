package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;

class InMemoryLocationServiceTest {

    @Test
    void seedsThreeLocationsAndThreeRoads() {
        InMemoryLocationService service = new InMemoryLocationService();

        assertEquals(3, service.findAllLocations().size());
        assertEquals(3, service.findAllRoads().size());
    }

    @Test
    void nextLocationIdAdvancesAfterAdd() {
        InMemoryLocationService service = new InMemoryLocationService();
        int nextId = service.nextLocationId();
        assertEquals(4, nextId);

        service.addLocation(new Location(nextId, "Test Gate", "Test Area", "GATE",
                0, 0, null, "https://example.com"));

        assertEquals(5, service.nextLocationId());
        assertEquals(4, service.findAllLocations().size());
        assertTrue(service.findAllLocations().stream().anyMatch(l -> l.getName().equals("Test Gate")));
    }

    @Test
    void nextRoadIdAdvancesAfterAdd() {
        InMemoryLocationService service = new InMemoryLocationService();
        int nextId = service.nextRoadId();
        assertEquals(4, nextId);

        service.addRoad(new Road(nextId, 1, 2, 1.0d, 2.0d, 1.0d,
                "Test Road", "CAMPUS_ROAD", "LOW", false));

        assertEquals(5, service.nextRoadId());
        assertEquals(4, service.findAllRoads().size());
    }
}
