package org.ugoptimizer.database.mapper;

import java.sql.*;
import java.time.*;
import org.ugoptimizer.model.*;

public final class RequestStatusHistoryMapper {
  private RequestStatusHistoryMapper() {}

  public static RequestStatusHistory map(ResultSet r) throws SQLException {
    int a = r.getInt("assignment_id");
    Integer ai = r.wasNull() ? null : a;
    int v = r.getInt("reversed_history_id");
    Integer ri = r.wasNull() ? null : v;
    return new RequestStatusHistory(
        r.getInt("history_id"),
        r.getInt("request_id"),
        r.getString("previous_status"),
        r.getString("new_status"),
        r.getString("actor_type"),
        Instant.parse(r.getString("event_timestamp")),
        r.getString("change_type"),
        ai,
        ri,
        r.getString("details"));
  }
}
