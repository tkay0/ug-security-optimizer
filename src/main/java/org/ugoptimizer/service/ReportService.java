package org.ugoptimizer.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.result.*;

/** Aggregates persisted backend state into immutable GUI-ready report objects. */
public final class ReportService {
  private static final String[] STATUSES = {
    "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
  };
  private static final String[] CATEGORIES = {
    "ACCESS_CONTROL",
    "CCTV_FAULT",
    "CROWD_CONTROL",
    "EMERGENCY_TRANSPORT",
    "FIRE_ALARM",
    "MEDICAL_EMERGENCY",
    "NIGHT_PATROL_REQUEST",
    "ROAD_OBSTRUCTION",
    "SECURITY_ESCORT",
    "SUSPICIOUS_ACTIVITY",
    "THEFT_REPORT",
    "WELFARE_CHECK"
  };
  private static final String[] AVAILABILITY = {"AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY"};
  private final RequestService requests;
  private final ResourceService resources;
  private final AssignmentService assignments;
  private final AuditService audits;
  private final LocationService locations;
  private final PerformanceService performance;
  private final Clock clock;

  public ReportService(DatabaseManager m) {
    this(
        new RequestService(m),
        new ResourceService(m),
        new AssignmentService(m),
        new AuditService(m),
        new LocationService(m),
        new PerformanceService(m),
        Clock.systemUTC());
  }

  public ReportService(
      RequestService q,
      ResourceService r,
      AssignmentService a,
      AuditService u,
      LocationService l,
      PerformanceService p,
      Clock c) {
    requests = Objects.requireNonNull(q);
    resources = Objects.requireNonNull(r);
    assignments = Objects.requireNonNull(a);
    audits = Objects.requireNonNull(u);
    locations = Objects.requireNonNull(l);
    performance = Objects.requireNonNull(p);
    clock = Objects.requireNonNull(c);
  }

  public SystemReport generateSystemReport() throws SQLException {
    ServiceRequest[] qs = requests.getAllRequests();
    Resource[] rs = resources.getAllResources();
    Road[] roads = locations.getAllRoads();
    AlgorithmRun[] runs = performance.getAllRuns();
    int blocked = 0, measured = 0;
    for (Road r : roads) if (r.isBlocked()) blocked++;
    for (AlgorithmRun r : runs) if ("MEASURED".equals(r.getStatus())) measured++;
    return new SystemReport(
        clock.instant(),
        qs.length,
        countRequests(qs, STATUSES, false),
        countRequests(qs, CATEGORIES, true),
        rs.length,
        countResources(rs),
        assignments.getActiveAssignments().length,
        audits.getAuditLog().length,
        locations.getAllLocations().length,
        roads.length,
        blocked,
        runs.length,
        measured,
        countAlgorithms(runs));
  }

  private static LabelCount[] countRequests(ServiceRequest[] q, String[] labels, boolean category) {
    LabelCount[] out = new LabelCount[labels.length];
    for (int i = 0; i < labels.length; i++) {
      int n = 0;
      for (ServiceRequest r : q)
        if (labels[i].equals(category ? r.getCategory() : r.getStatus())) n++;
      out[i] = new LabelCount(labels[i], n);
    }
    return out;
  }

  private static LabelCount[] countResources(Resource[] r) {
    LabelCount[] out = new LabelCount[AVAILABILITY.length];
    for (int i = 0; i < out.length; i++) {
      int n = 0;
      for (Resource x : r) if (AVAILABILITY[i].equals(x.getAvailabilityStatus())) n++;
      out[i] = new LabelCount(AVAILABILITY[i], n);
    }
    return out;
  }

  private static LabelCount[] countAlgorithms(AlgorithmRun[] runs) {
    String[] names = new String[runs.length];
    int size = 0;
    for (AlgorithmRun r : runs) {
      boolean seen = false;
      for (int i = 0; i < size; i++) if (names[i].equals(r.getAlgorithmName())) seen = true;
      if (!seen) names[size++] = r.getAlgorithmName();
    }
    LabelCount[] out = new LabelCount[size];
    for (int i = 0; i < size; i++) {
      int n = 0;
      for (AlgorithmRun r : runs) if (names[i].equals(r.getAlgorithmName())) n++;
      out[i] = new LabelCount(names[i], n);
    }
    return out;
  }
}
