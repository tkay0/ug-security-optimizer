package org.ugoptimizer.app;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.importers.CsvDatasetImporter;
import org.ugoptimizer.service.*;

/** Single backend composition root for dependency injection into a future frontend. */
public final class BackendContext {
  private final LocationService locations;
  private final RequestService requests;
  private final ResourceService resources;
  private final PriorityService priority;
  private final RouteService routes;
  private final WorkflowService workflow;
  private final AssignmentService assignments;
  private final AuditService audit;
  private final UndoService undo;
  private final OptimizationService optimization;
  private final PerformanceService performance;
  private final ReportService reports;

  private BackendContext(DatabaseManager d) {
    locations = new LocationService(d);
    requests = new RequestService(d);
    resources = new ResourceService(d);
    priority = new PriorityService(requests);
    routes = new RouteService(d);
    workflow = new WorkflowService(d);
    assignments = new AssignmentService(d);
    audit = new AuditService(d);
    undo = new UndoService(d);
    optimization = new OptimizationService();
    performance = new PerformanceService(d);
    reports = new ReportService(d);
  }

  public static BackendContext initialize(Path path) throws IOException, SQLException {
    DatabaseManager d = new DatabaseManager(Objects.requireNonNull(path));
    d.initializeSchema();
    return new BackendContext(d);
  }

  public static BackendContext compose(DatabaseManager d) {
    return new BackendContext(Objects.requireNonNull(d));
  }

  /** Initializes and imports the canonical dataset into a new, empty database. */
  public static BackendContext initializeWithDataset(Path databasePath, Path datasetDirectory)
      throws IOException, SQLException {
    DatabaseManager manager = new DatabaseManager(Objects.requireNonNull(databasePath));
    manager.initializeSchema();
    new CsvDatasetImporter(manager, Objects.requireNonNull(datasetDirectory)).importAll();
    return new BackendContext(manager);
  }

  public LocationService getLocationService() {
    return locations;
  }

  public RequestService getRequestService() {
    return requests;
  }

  public ResourceService getResourceService() {
    return resources;
  }

  public PriorityService getPriorityService() {
    return priority;
  }

  public RouteService getRouteService() {
    return routes;
  }

  public WorkflowService getWorkflowService() {
    return workflow;
  }

  public AssignmentService getAssignmentService() {
    return assignments;
  }

  public AuditService getAuditService() {
    return audit;
  }

  public UndoService getUndoService() {
    return undo;
  }

  public OptimizationService getOptimizationService() {
    return optimization;
  }

  public PerformanceService getPerformanceService() {
    return performance;
  }

  public ReportService getReportService() {
    return reports;
  }
}
