package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable persisted request-resource assignment. */
public final class Assignment {
  public static final String ACTIVE = "ACTIVE", RELEASED = "RELEASED";
  private final int assignmentId, requestId, resourceId;
  private final Instant assignedAt, releasedAt;
  private final String status;
  private final double response;

  public Assignment(
      int a, int q, int r, Instant at, Instant released, String status, double response) {
    if (a <= 0 || q <= 0 || r <= 0)
      throw new IllegalArgumentException("Assignment identifiers must be positive");
    if (!Double.isFinite(response) || response < 0)
      throw new IllegalArgumentException("response time must be finite and non-negative");
    assignmentId = a;
    requestId = q;
    resourceId = r;
    assignedAt = Objects.requireNonNull(at);
    releasedAt = released;
    if (ACTIVE.equals(status) && released == null || RELEASED.equals(status) && released != null)
      this.status = status;
    else throw new IllegalArgumentException("Assignment status and releasedAt disagree");
    this.response = response;
  }

  public int getAssignmentId() {
    return assignmentId;
  }

  public int getRequestId() {
    return requestId;
  }

  public int getResourceId() {
    return resourceId;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getStatus() {
    return status;
  }

  public double getEstimatedResponseTimeMinutes() {
    return response;
  }

  public boolean isActive() {
    return ACTIVE.equals(status);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Assignment a)) return false;
    return assignmentId == a.assignmentId
        && requestId == a.requestId
        && resourceId == a.resourceId
        && Double.compare(response, a.response) == 0
        && assignedAt.equals(a.assignedAt)
        && Objects.equals(releasedAt, a.releasedAt)
        && status.equals(a.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        assignmentId, requestId, resourceId, assignedAt, releasedAt, status, response);
  }
}
