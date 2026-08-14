package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable persisted road-scenario override. */
public final class RoadScenario {
  private final int id, roadId;
  private final String name, label, reason;
  private final Instant start, end;
  private final boolean blocked;
  private final double condition, travel;

  public RoadScenario(
      int id,
      String name,
      int road,
      String label,
      Instant start,
      Instant end,
      boolean blocked,
      double condition,
      double travel,
      String reason) {
    if (id <= 0
        || road <= 0
        || condition <= 0
        || travel <= 0
        || !Double.isFinite(condition)
        || !Double.isFinite(travel)) throw new IllegalArgumentException("Invalid scenario values");
    this.id = id;
    this.roadId = road;
    this.name = text(name);
    this.label = text(label);
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
    if (!end.isAfter(start)) throw new IllegalArgumentException("scenarioEnd must follow start");
    this.blocked = blocked;
    this.condition = condition;
    this.travel = travel;
    this.reason = text(reason);
  }

  public int getScenarioId() {
    return id;
  }

  public String getScenarioName() {
    return name;
  }

  public int getRoadId() {
    return roadId;
  }

  public String getRouteLabel() {
    return label;
  }

  public Instant getScenarioStart() {
    return start;
  }

  public Instant getScenarioEnd() {
    return end;
  }

  public boolean isBlockedOverride() {
    return blocked;
  }

  public double getConditionWeightMultiplier() {
    return condition;
  }

  public double getTravelTimeMultiplier() {
    return travel;
  }

  public String getReason() {
    return reason;
  }

  private static String text(String s) {
    Objects.requireNonNull(s);
    if (s.isBlank()) throw new IllegalArgumentException("text cannot be blank");
    return s;
  }
}
