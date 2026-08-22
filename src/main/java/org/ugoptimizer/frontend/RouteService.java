package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;

/** Swing-facing routing contract delegated to the canonical graph service. */
public interface RouteService {
  TraversalResult bfs(int startLocationId);

  TraversalResult dfs(int startLocationId);

  PathResult shortestPath(int sourceLocationId, int destinationLocationId);

  /** Returns persisted road-scenario names available for shortest-path routing. */
  default List<String> getScenarioNames() {
    return List.of();
  }

  /** Runs shortest-path routing with one persisted road scenario applied. */
  default PathResult shortestPathUnderScenario(
      String scenarioName, int sourceLocationId, int destinationLocationId) {
    throw new IllegalStateException("Scenario routing is not available");
  }
}
