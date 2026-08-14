package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.Resource;

/**
 * Provides dispatchable resources to the UI. A real implementation wraps
 * {@code ResourceDao}; {@code InMemoryResourceService} exists for development
 * before that lands.
 */
public interface ResourceService {

    List<Resource> findAll();

    /** Returns the ID the next added resource should use (mirrors DB auto-increment). */
    int nextResourceId();

    Resource add(Resource resource);
}
