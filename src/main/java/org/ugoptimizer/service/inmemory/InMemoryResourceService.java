package org.ugoptimizer.service.inmemory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.frontend.ResourceService;

/**
 * In-memory {@link ResourceService} seeded with the same sample resources
 * {@code RequestResourceMenu} used to hold itself. Replace with a real
 * DAO-backed implementation once the database team's work lands.
 */
public final class InMemoryResourceService implements ResourceService {

    private final List<Resource> resources = new ArrayList<>();
    private int nextResourceId = 1;

    public InMemoryResourceService() {
        seedSampleData();
    }

    @Override
    public List<Resource> findAll() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public int nextResourceId() {
        return nextResourceId;
    }

    @Override
    public Resource add(Resource resource) {
        resources.add(resource);
        nextResourceId++;
        return resource;
    }

    private void seedSampleData() {
        resources.add(new Resource(nextResourceId++, "MEDICAL_TEAM", 1, 4,
                "AVAILABLE", null, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        resources.add(new Resource(nextResourceId++, "PATROL_TEAM", 2, 2,
                "BUSY", 3, LocalTime.of(0, 0), LocalTime.of(23, 59)));
    }
}
