package org.ugoptimizer.database.loader;

import java.sql.SQLException;
import java.util.*;
import org.ugoptimizer.database.dao.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.structures.graph.*;

/** Loads the baseline graph with all overrides from one named persisted scenario. */
public final class ScenarioGraphLoader {
  private final LocationDao locations;
  private final RoadDao roads;
  private final RoadScenarioDao scenarios;

  public ScenarioGraphLoader(LocationDao l, RoadDao r, RoadScenarioDao s) {
    locations = Objects.requireNonNull(l);
    roads = Objects.requireNonNull(r);
    scenarios = Objects.requireNonNull(s);
  }

  public void loadInto(WeightedGraph graph, String name) throws SQLException {
    Objects.requireNonNull(graph);
    if (!graph.isEmpty()) throw new IllegalArgumentException("graph must be empty");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("scenarioName cannot be blank");
    Location[] ls = locations.findAll();
    Road[] rs = roads.findAll();
    RoadScenario[] os = scenarios.findByScenarioName(name);
    if (os.length == 0) throw new IllegalArgumentException("Unknown road scenario: " + name);
    for (Location l : ls) graph.addVertex(l.getLocationId());
    for (Road r : rs) {
      RoadScenario o = find(os, r.getRoadId());
      boolean blocked = o == null ? r.isBlocked() : o.isBlockedOverride();
      if (blocked) continue;
      double weight = r.getTravelTimeMin() * r.getConditionWeight();
      if (o != null) weight *= o.getTravelTimeMultiplier() * o.getConditionWeightMultiplier();
      WeightedGraph.EdgeUpdate update =
          graph.addEdge(r.getFromLocationId(), r.getToLocationId(), weight);
      if (update != WeightedGraph.EdgeUpdate.ADDED)
        throw new IllegalStateException("Could not load road " + r.getRoadId() + ": " + update);
    }
  }

  private static RoadScenario find(RoadScenario[] a, int road) {
    for (RoadScenario s : a) if (s.getRoadId() == road) return s;
    return null;
  }
}
