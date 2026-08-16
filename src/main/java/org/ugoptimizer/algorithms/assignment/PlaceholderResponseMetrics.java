package org.ugoptimizer.algorithms.assignment;

import org.ugoptimizer.model.Resource;

/**
 * Stand-in response-time/workload numbers for a {@link Resource}, used only
 * until a real estimate exists (e.g. a routed distance from the resource's
 * current/home location to the request destination, the way
 * {@code AppContext.estimateResponseTimeMin} on the {@code feature/team2-gui}
 * branch derives it from schematic X/Y coordinates).
 *
 * <p>Deliberately isolated from {@code OptimizationMenu} so the placeholder
 * is easy to find and delete once real dispatch metrics are wired through a
 * service instead of synthesized in the UI.</p>
 */
public final class PlaceholderResponseMetrics {

    private PlaceholderResponseMetrics() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static double responseTime(Resource resource) {
        return 2.0d + resource.getResourceId();
    }

    public static int workload(Resource resource) {
        return resource.getCapacity() > 2 ? 1 : 0;
    }
}
