package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceServiceTest {

    @TempDir
    Path temporaryDirectory;

    private ResourceService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ResourceService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "resources.db"));
    }

    @Test
    void returnsAllAndAvailableResourcesWithTypeFiltering() throws Exception {
        assertEquals(30, service.getAllResources().length);
        assertEquals(21, service.getAvailableResources().length);
        assertEquals(6, service.getAvailableResourcesByType("PATROL_OFFICER").length);
        assertEquals(0, service.getAvailableResourcesByType("UNKNOWN_TEAM").length);
    }

    @Test
    void persistsAvailabilityAndCurrentLocationUpdates() throws Exception {
        assertEquals("BUSY", service.updateAvailability(1, "BUSY").getAvailabilityStatus());
        assertEquals(2, service.updateCurrentLocation(1, 2).getCurrentLocationId());
        assertNull(service.updateCurrentLocation(1, null).getCurrentLocationId());
    }

    @Test
    void rejectsInvalidStateLocationAndMissingResources() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateAvailability(1, "UNKNOWN"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCurrentLocation(1, 999));
        assertThrows(NoSuchElementException.class,
                () -> service.updateAvailability(999, "AVAILABLE"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableResourcesByType(" "));
    }
}
