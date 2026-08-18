package org.ugoptimizer.service;

import java.sql.SQLException;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.IdSequenceDao;
import org.ugoptimizer.database.dao.IdSequenceDao.Entity;

/** Allocates frontend-compatible IDs through the database's atomic reservation sequence. */
public final class IdService {

  private final IdSequenceDao sequences;

  public IdService(DatabaseManager databaseManager) {
    this(new IdSequenceDao(Objects.requireNonNull(databaseManager, "databaseManager cannot be null")));
  }

  public IdService(IdSequenceDao sequences) {
    this.sequences = Objects.requireNonNull(sequences, "sequences cannot be null");
  }

  public int reserveLocationId() throws SQLException {
    return sequences.reserveNext(Entity.LOCATION);
  }

  public int reserveRoadId() throws SQLException {
    return sequences.reserveNext(Entity.ROAD);
  }

  public int reserveRequestId() throws SQLException {
    return sequences.reserveNext(Entity.SERVICE_REQUEST);
  }

  public int reserveResourceId() throws SQLException {
    return sequences.reserveNext(Entity.RESOURCE);
  }
}
