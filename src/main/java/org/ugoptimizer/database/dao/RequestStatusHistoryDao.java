package org.ugoptimizer.database.dao;

import java.sql.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.mapper.*;
import org.ugoptimizer.model.*;

public final class RequestStatusHistoryDao {
  static final String COLUMNS =
      "history_id,request_id,previous_status,new_status,actor_type,event_timestamp,change_type,assignment_id,reversed_history_id,details";
  private final DatabaseManager manager;

  public RequestStatusHistoryDao(DatabaseManager m) {
    manager = Objects.requireNonNull(m);
  }

  public RequestStatusHistory[] findByRequestId(int id) throws SQLException {
    try (Connection c = manager.openConnection();
        PreparedStatement s =
            c.prepareStatement(
                "SELECT "
                    + COLUMNS
                    + " FROM request_status_history WHERE request_id=? ORDER BY"
                    + " event_timestamp,history_id")) {
      s.setInt(1, id);
      try (ResultSet r = s.executeQuery()) {
        RequestStatusHistory[] a = new RequestStatusHistory[8];
        int n = 0;
        while (r.next()) {
          if (n == a.length) {
            RequestStatusHistory[] x = new RequestStatusHistory[a.length * 2];
            System.arraycopy(a, 0, x, 0, n);
            a = x;
          }
          a[n++] = RequestStatusHistoryMapper.map(r);
        }
        RequestStatusHistory[] out = new RequestStatusHistory[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
      }
    }
  }

  public Optional<RequestStatusHistory> findLatestReversible(int id) throws SQLException {
    try (Connection c = manager.openConnection()) {
      return findLatestReversible(c, id);
    }
  }

  static Optional<RequestStatusHistory> findLatestReversible(Connection c, int id)
      throws SQLException {
    String sql =
        "SELECT h."
            + COLUMNS.replace(",", ",h.")
            + " FROM request_status_history h WHERE h.request_id=? AND h.change_type<>'UNDO' AND"
            + " NOT EXISTS(SELECT 1 FROM request_status_history u WHERE"
            + " u.reversed_history_id=h.history_id) ORDER BY h.event_timestamp DESC,h.history_id"
            + " DESC LIMIT 1";
    try (PreparedStatement s = c.prepareStatement(sql)) {
      s.setInt(1, id);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? Optional.of(RequestStatusHistoryMapper.map(r)) : Optional.empty();
      }
    }
  }

  static int nextHistoryId(Connection c) throws SQLException {
    return AssignmentDao.nextId(c, "request_status_history", "history_id");
  }

  static void insertHistory(Connection c, RequestStatusHistory h) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "INSERT INTO request_status_history(" + COLUMNS + ") VALUES(?,?,?,?,?,?,?,?,?,?)")) {
      s.setInt(1, h.getHistoryId());
      s.setInt(2, h.getRequestId());
      s.setString(3, h.getPreviousStatus());
      s.setString(4, h.getNewStatus());
      s.setString(5, h.getActorType());
      s.setString(6, h.getTimestamp().toString());
      s.setString(7, h.getChangeType());
      if (h.getAssignmentId() == null) s.setNull(8, Types.INTEGER);
      else s.setInt(8, h.getAssignmentId());
      if (h.getReversedHistoryId() == null) s.setNull(9, Types.INTEGER);
      else s.setInt(9, h.getReversedHistoryId());
      s.setString(10, h.getDetails());
      if (s.executeUpdate() != 1) throw new SQLException("History insert failed");
    }
  }
}
