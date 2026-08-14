package org.ugoptimizer.database.mapper;

import java.sql.*;
import java.time.*;
import org.ugoptimizer.model.*;

public final class RoadScenarioMapper {
  private RoadScenarioMapper() {}

  public static RoadScenario map(ResultSet r) throws SQLException {
    return new RoadScenario(
        r.getInt("scenario_id"),
        r.getString("scenario_name"),
        r.getInt("road_id"),
        r.getString("route_label"),
        Instant.parse(r.getString("scenario_start")),
        Instant.parse(r.getString("scenario_end")),
        r.getInt("is_blocked_override") == 1,
        r.getDouble("condition_weight_multiplier"),
        r.getDouble("travel_time_multiplier"),
        r.getString("reason"));
  }
}
