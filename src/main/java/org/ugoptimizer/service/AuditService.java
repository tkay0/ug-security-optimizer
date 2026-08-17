package org.ugoptimizer.service;

import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.dao.*;
import org.ugoptimizer.model.*;

/** Service boundary for querying and recording audit activity. */
public final class AuditService {
  private final AuditEventDao dao;
  private final Clock clock;

  public AuditService(DatabaseManager m) {
    this(new AuditEventDao(m), Clock.systemUTC());
  }

  public AuditService(AuditEventDao d, Clock c) {
    dao = Objects.requireNonNull(d);
    clock = Objects.requireNonNull(c);
  }

  public AuditEvent[] getAuditLog() throws SQLException {
    return dao.findAll();
  }

  public AuditEvent[] getEntityHistory(String type, int id) throws SQLException {
    return dao.findByEntity(type, id);
  }

  public Optional<AuditEvent> findById(int id) throws SQLException {
    return dao.findById(id);
  }

  public AuditEvent record(
      String eventType, String entityType, int entityId, String actor, String details)
      throws SQLException {
    return dao.insertGenerated(eventType, clock.instant(), entityType, entityId, actor, details);
  }
}
