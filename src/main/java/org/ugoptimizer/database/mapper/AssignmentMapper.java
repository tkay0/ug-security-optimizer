package org.ugoptimizer.database.mapper;

import java.sql.*;
import java.time.*;
import org.ugoptimizer.model.*;

public final class AssignmentMapper {
  private AssignmentMapper() {}

  public static Assignment map(ResultSet r) throws SQLException {
    String x = r.getString("released_at");
    try {
      return new Assignment(
          r.getInt("assignment_id"),
          r.getInt("request_id"),
          r.getInt("resource_id"),
          Instant.parse(r.getString("assigned_at")),
          x == null ? null : Instant.parse(x),
          r.getString("status"),
          r.getDouble("estimated_response_time_min"));
    } catch (RuntimeException e) {
      throw new SQLException("Invalid assignment row", e);
    }
  }
}
