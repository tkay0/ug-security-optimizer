package org.ugoptimizer.result;

import java.time.*;
import java.util.*;

/** Immutable aggregate backend report suitable for presentation by any frontend. */
public final class SystemReport {
  private final Instant at;
  private final LabelCount[] statuses, categories, availability, algorithms;
  private final int requests, resources, active, audits, locations, roads, blocked, runs, measured;

  public SystemReport(
      Instant at,
      int requests,
      LabelCount[] statuses,
      LabelCount[] categories,
      int resources,
      LabelCount[] availability,
      int active,
      int audits,
      int locations,
      int roads,
      int blocked,
      int runs,
      int measured,
      LabelCount[] algorithms) {
    this.at = Objects.requireNonNull(at);
    this.requests = requests;
    this.statuses = statuses.clone();
    this.categories = categories.clone();
    this.resources = resources;
    this.availability = availability.clone();
    this.active = active;
    this.audits = audits;
    this.locations = locations;
    this.roads = roads;
    this.blocked = blocked;
    this.runs = runs;
    this.measured = measured;
    this.algorithms = algorithms.clone();
  }

  public Instant getGeneratedAt() {
    return at;
  }

  public int getTotalRequests() {
    return requests;
  }

  public LabelCount[] getRequestsByStatus() {
    return statuses.clone();
  }

  public LabelCount[] getRequestsByCategory() {
    return categories.clone();
  }

  public int getTotalResources() {
    return resources;
  }

  public LabelCount[] getResourcesByAvailability() {
    return availability.clone();
  }

  public int getActiveAssignmentCount() {
    return active;
  }

  public int getAuditEventCount() {
    return audits;
  }

  public int getLocationCount() {
    return locations;
  }

  public int getRoadCount() {
    return roads;
  }

  public int getBlockedRoadCount() {
    return blocked;
  }

  public int getAlgorithmRunCount() {
    return runs;
  }

  public int getMeasuredAlgorithmRunCount() {
    return measured;
  }

  public LabelCount[] getRunsByAlgorithm() {
    return algorithms.clone();
  }
}
