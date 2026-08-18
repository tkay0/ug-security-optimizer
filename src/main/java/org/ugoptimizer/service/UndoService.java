package org.ugoptimizer.service;

import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.dao.*;
import org.ugoptimizer.model.*;

/** Business service for append-only workflow undo. */
public final class UndoService {
  private final RequestService requests;
  private final UndoDao undoDao;
  private final RequestStatusHistoryDao history;
  private final Clock clock;

  public UndoService(DatabaseManager m) {
    this(new RequestService(m), new UndoDao(m), new RequestStatusHistoryDao(m), Clock.systemUTC());
  }

  public UndoService(RequestService r, UndoDao u, RequestStatusHistoryDao h, Clock c) {
    requests = Objects.requireNonNull(r);
    undoDao = Objects.requireNonNull(u);
    history = Objects.requireNonNull(h);
    clock = Objects.requireNonNull(c);
  }

  public ServiceRequest undoLatest(int requestId, String actor) throws SQLException {
    requests.requireRequest(requestId);
    if (actor == null || actor.isBlank())
      throw new IllegalArgumentException("actor cannot be blank");
    undoDao.undoLatest(requestId, clock.instant(), actor);
    return requests.requireRequest(requestId);
  }

  public RequestStatusHistory[] getHistory(int requestId) throws SQLException {
    requests.requireRequest(requestId);
    return history.findByRequestId(requestId);
  }

  public Optional<RequestStatusHistory> findLatestReversible(int requestId) throws SQLException {
    requests.requireRequest(requestId);
    return history.findLatestReversible(requestId);
  }
}
