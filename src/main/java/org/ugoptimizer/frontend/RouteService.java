package org.ugoptimizer.frontend;

import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;

/** Swing-facing routing contract delegated to the canonical graph service. */
public interface RouteService {
  TraversalResult bfs(int startLocationId);

  TraversalResult dfs(int startLocationId);

  PathResult shortestPath(int sourceLocationId, int destinationLocationId);
}
