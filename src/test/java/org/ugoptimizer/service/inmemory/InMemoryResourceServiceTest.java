package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Resource;

class InMemoryResourceServiceTest {

    @Test
    void seedsTwoResources() {
        InMemoryResourceService service = new InMemoryResourceService();
        assertEquals(2, service.findAll().size());
    }

    @Test
    void addAppearsInFindAllAndAdvancesNextId() {
        InMemoryResourceService service = new InMemoryResourceService();
        int nextId = service.nextResourceId();
        assertEquals(3, nextId);

        service.add(new Resource(nextId, "FIRE_TEAM", 1, 3, "AVAILABLE", null, null, null));

        assertEquals(3, service.findAll().size());
        assertEquals(4, service.nextResourceId());
        assertTrue(service.findAll().stream().anyMatch(r -> r.getResourceId() == nextId));
    }
}
