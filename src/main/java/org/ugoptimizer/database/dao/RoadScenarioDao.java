package org.ugoptimizer.database.dao;

import java.sql.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.mapper.*;
import org.ugoptimizer.model.*;

public final class RoadScenarioDao {
  private static final String C =
      "scenario_id,scenario_name,road_id,route_label,scenario_start,scenario_end,is_blocked_override,condition_weight_multiplier,travel_time_multiplier,reason";
  private final DatabaseManager m;

  public RoadScenarioDao(DatabaseManager m) {
    this.m = Objects.requireNonNull(m);
  }

  public RoadScenario[] findByScenarioName(String name) throws SQLException {
    return query(
        "SELECT " + C + " FROM road_scenarios WHERE scenario_name=? ORDER BY road_id", name);
  }

  public RoadScenario[] findAll() throws SQLException {
    return query("SELECT " + C + " FROM road_scenarios ORDER BY scenario_name,road_id", null);
  }

  public String[] findScenarioNames() throws SQLException {
    try (Connection c = m.openConnection();
        Statement s = c.createStatement();
        ResultSet r =
            s.executeQuery(
                "SELECT DISTINCT scenario_name FROM road_scenarios ORDER BY scenario_name")) {
      String[] a = new String[8];
      int n = 0;
      while (r.next()) {
        if (n == a.length) a = Arrays.copyOf(a, a.length * 2);
        a[n++] = r.getString(1);
      }
      return Arrays.copyOf(a, n);
    }
  }

  private RoadScenario[] query(String sql, String name) throws SQLException {
    try (Connection c = m.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (name != null) s.setString(1, name);
      try (ResultSet r = s.executeQuery()) {
        RoadScenario[] a = new RoadScenario[8];
        int n = 0;
        while (r.next()) {
          if (n == a.length) a = Arrays.copyOf(a, a.length * 2);
          a[n++] = RoadScenarioMapper.map(r);
        }
        return Arrays.copyOf(a, n);
      }
    }
  }
}
