package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable append-only request workflow change. */
public final class RequestStatusHistory {
  public static final String STATUS_CHANGE = "STATUS_CHANGE",
      ASSIGNMENT = "ASSIGNMENT",
      UNDO = "UNDO";
  private final int historyId, requestId;
  private final String previousStatus, newStatus, actorType, changeType, details;
  private final Instant timestamp;
  private final Integer assignmentId, reversedHistoryId;

  public RequestStatusHistory(
      int h,
      int q,
      String p,
      String n,
      String actor,
      Instant at,
      String type,
      Integer assignment,
      Integer reversed,
      String details) {
    if (h <= 0 || q <= 0)
      throw new IllegalArgumentException("History identifiers must be positive");
    historyId = h;
    requestId = q;
    previousStatus = status(p);
    newStatus = status(n);
    if (p.equals(n)) throw new IllegalArgumentException("History must change status");
    actorType = text(actor);
    timestamp = Objects.requireNonNull(at);
    if (!STATUS_CHANGE.equals(type) && !ASSIGNMENT.equals(type) && !UNDO.equals(type))
      throw new IllegalArgumentException("Unsupported change type");
    if (UNDO.equals(type) != (reversed != null))
      throw new IllegalArgumentException("Only undo rows reference reversed history");
    changeType = type;
    assignmentId = assignment;
    reversedHistoryId = reversed;
    this.details = details;
  }

  public int getHistoryId() {
    return historyId;
  }

  public int getRequestId() {
    return requestId;
  }

  public String getPreviousStatus() {
    return previousStatus;
  }

  public String getNewStatus() {
    return newStatus;
  }

  public String getActorType() {
    return actorType;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getChangeType() {
    return changeType;
  }

  public Integer getAssignmentId() {
    return assignmentId;
  }

  public Integer getReversedHistoryId() {
    return reversedHistoryId;
  }

  public String getDetails() {
    return details;
  }

  private static String status(String s) {
    return switch (text(s)) {
      case "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED" -> s;
      default -> throw new IllegalArgumentException("Unsupported status");
    };
  }

  private static String text(String s) {
    Objects.requireNonNull(s);
    if (s.isBlank()) throw new IllegalArgumentException("text cannot be blank");
    return s;
  }
}
