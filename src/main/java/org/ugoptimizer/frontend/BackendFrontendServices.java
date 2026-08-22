package org.ugoptimizer.frontend;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.ugoptimizer.app.BackendContext;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.RequestStatusHistory;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.OptimizationComparison;
import org.ugoptimizer.result.RequestOptimizationCandidate;
import org.ugoptimizer.result.RequestOptimizationResult;
import org.ugoptimizer.result.TraversalResult;

/**
 * Adapts one {@link BackendContext} into the collection-oriented, unchecked contracts expected by
 * Selorm's Swing frontend.
 *
 * <p>ID methods reserve values atomically in SQLite. A reservation is intentionally not rolled
 * back if a user abandons the form, so IDs may contain gaps; callers must not assume contiguous
 * numbering.</p>
 */
public final class BackendFrontendServices {

  private static final String FRONTEND_ACTOR = "SELORM_SWING";

  private final LocationService locations;
  private final RequestService requests;
  private final ResourceService resources;
  private final RouteService routes;
  private final WorkflowService workflow;
  private final ReportService reports;
  private final PriorityService priority;
  private final OptimizationService optimization;

  private BackendFrontendServices(BackendContext backend) {
    locations = new LocationAdapter(backend);
    requests = new RequestAdapter(backend);
    resources = new ResourceAdapter(backend);
    routes = new RouteAdapter(backend);
    workflow = new WorkflowAdapter(backend);
    reports = new ReportAdapter(backend);
    priority = new PriorityAdapter(backend);
    optimization = new OptimizationAdapter(backend);
  }

  public static BackendFrontendServices from(BackendContext backend) {
    return new BackendFrontendServices(Objects.requireNonNull(backend, "backend cannot be null"));
  }

  public LocationService locations() {
    return locations;
  }

  public RequestService requests() {
    return requests;
  }

  public ResourceService resources() {
    return resources;
  }

  public RouteService routes() {
    return routes;
  }

  public WorkflowService workflow() {
    return workflow;
  }

  public ReportService reports() {
    return reports;
  }

  public PriorityService priority() {
    return priority;
  }

  public OptimizationService optimization() {
    return optimization;
  }


  private static final class LocationAdapter implements LocationService {
    private final BackendContext backend;

    private LocationAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public List<Location> findAllLocations() {
      return call("load locations", () -> list(backend.getLocationService().getAllLocations()));
    }

    @Override
    public int nextLocationId() {
      return call("reserve a location ID", backend.getIdService()::reserveLocationId);
    }

    @Override
    public Location addLocation(Location location) {
      return call("add location", () -> backend.getLocationService().createLocation(location));
    }

    @Override
    public List<Road> findAllRoads() {
      return call("load roads", () -> list(backend.getLocationService().getAllRoads()));
    }

    @Override
    public int nextRoadId() {
      return call("reserve a road ID", backend.getIdService()::reserveRoadId);
    }

    @Override
    public Road addRoad(Road road) {
      return call("add road", () -> backend.getLocationService().createRoad(road));
    }
  }

  private static final class RequestAdapter implements RequestService {
    private final BackendContext backend;

    private RequestAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public List<ServiceRequest> findAll() {
      return call("load service requests", () -> list(backend.getRequestService().getAllRequests()));
    }

    @Override
    public int nextRequestId() {
      return call("reserve a request ID", backend.getIdService()::reserveRequestId);
    }

    @Override
    public ServiceRequest add(ServiceRequest request) {
      return call("add service request", () -> backend.getRequestService().createRequest(request));
    }

    @Override
    public ServiceRequest updateStatus(int requestId, String newStatus) {
      return call("update request status", () -> updateStatusChecked(requestId, newStatus));
    }

    private ServiceRequest updateStatusChecked(int requestId, String targetStatus)
        throws SQLException {
      ServiceRequest current = backend.getRequestService().requireRequest(requestId);
      if (current.getStatus().equals(targetStatus)) {
        return current;
      }
      if (org.ugoptimizer.service.WorkflowService.PENDING.equals(current.getStatus())
          && org.ugoptimizer.service.WorkflowService.ASSIGNED.equals(targetStatus)) {
        backend.getAssignmentService().assignBestResource(requestId, FRONTEND_ACTOR);
        return backend.getRequestService().requireRequest(requestId);
      }
      if (backend.getWorkflowService().canTransition(current.getStatus(), targetStatus)) {
        return backend.getWorkflowService().transitionStatus(requestId, targetStatus, FRONTEND_ACTOR);
      }
      RequestStatusHistory reversible =
          backend
              .getUndoService()
              .findLatestReversible(requestId)
              .orElseThrow(
                  () -> new IllegalStateException("No reversible status change for request " + requestId));
      if (!targetStatus.equals(reversible.getPreviousStatus())) {
        throw new IllegalStateException(
            "Invalid request status transition: " + current.getStatus() + " -> " + targetStatus);
      }
      return backend.getUndoService().undoLatest(requestId, FRONTEND_ACTOR);
    }
  }

  private static final class ResourceAdapter implements ResourceService {
    private final BackendContext backend;

    private ResourceAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public List<Resource> findAll() {
      return call("load resources", () -> list(backend.getResourceService().getAllResources()));
    }

    @Override
    public int nextResourceId() {
      return call("reserve a resource ID", backend.getIdService()::reserveResourceId);
    }

    @Override
    public Resource add(Resource resource) {
      return call("add resource", () -> backend.getResourceService().createResource(resource));
    }
  }

  private static final class RouteAdapter implements RouteService {
    private final org.ugoptimizer.service.RouteService routes;

    private RouteAdapter(BackendContext backend) {
      routes = backend.getRouteService();
    }

    @Override
    public TraversalResult bfs(int startLocationId) {
      return call("run breadth-first traversal", () -> routes.bfs(startLocationId));
    }

    @Override
    public TraversalResult dfs(int startLocationId) {
      return call("run depth-first traversal", () -> routes.dfs(startLocationId));
    }

    @Override
    public PathResult shortestPath(int sourceLocationId, int destinationLocationId) {
      return call("find shortest path", () -> routes.shortestPath(sourceLocationId, destinationLocationId));
    }

    @Override
    public List<String> getScenarioNames() {
      return call("load road scenarios", () -> list(routes.getRoadScenarioNames()));
    }

    @Override
    public PathResult shortestPathUnderScenario(
        String scenarioName, int sourceLocationId, int destinationLocationId) {
      return call(
          "find a scenario route",
          () -> routes.findShortestRouteUnderScenario(
              scenarioName, sourceLocationId, destinationLocationId));
    }
  }

  private static final class WorkflowAdapter implements WorkflowService {
    private final BackendContext backend;

    private WorkflowAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public AuditEvent logEvent(String eventType, int entityId, String details) {
      return call(
          "record audit event",
          () ->
              backend
                  .getAuditService()
                  .record(eventType, "SERVICE_REQUEST", entityId, FRONTEND_ACTOR, details));
    }

    @Override
    public List<AuditEvent> findAuditLog() {
      return call("load audit log", () -> list(backend.getAuditService().getAuditLog()));
    }

    @Override
    public java.util.Optional<Assignment> findActiveAssignment(int requestId) {
      return call(
          "load active assignment",
          () -> backend.getAssignmentService().findActiveByRequestId(requestId));
    }
  }

  private static final class ReportAdapter implements ReportService {
    private final BackendContext backend;

    private ReportAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public AlgorithmRun record(AlgorithmRun run) {
      return call("record algorithm measurement", () -> backend.getPerformanceService().recordMeasuredRun(run));
    }

    @Override
    public org.ugoptimizer.result.SystemReport generateSystemReport() {
      return call("generate system report", () -> backend.getReportService().generateSystemReport());
    }

    @Override
    public List<AlgorithmRun> findAll() {
      return call("load algorithm runs", () -> list(backend.getPerformanceService().getAllRuns()));
    }
  }

  private static final class PriorityAdapter implements PriorityService {
    private final BackendContext backend;

    private PriorityAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public List<ServiceRequest> priorityOrder() {
      return call("load priority queue", () -> list(backend.getPriorityService().getDispatchQueue()));
    }
  }

  private static final class OptimizationAdapter implements OptimizationService {
    private static final int MAX_EXACT_CANDIDATES = 24;
    private final BackendContext backend;

    private OptimizationAdapter(BackendContext backend) {
      this.backend = backend;
    }

    @Override
    public int getBudget() {
      return backend.getOptimizationService().getApprovedBudget();
    }

    @Override
    public List<RequestOptimizationCandidate> pendingRequestCandidates() {
      List<RequestOptimizationCandidate> candidates = new java.util.ArrayList<>();
      for (ServiceRequest request : requests(backend)) {
        if (org.ugoptimizer.service.WorkflowService.PENDING.equals(request.getStatus())) {
          // ServiceRequest has no cost field: one unit of budget per request and
          // urgency as the real domain benefit keeps the mapping explicit.
          candidates.add(new RequestOptimizationCandidate(request, 1, request.getUrgency()));
          if (candidates.size() == MAX_EXACT_CANDIDATES) {
            break;
          }
        }
      }
      return List.copyOf(candidates);
    }

    @Override
    public RequestOptimizationResult runDynamicProgramming(
        List<RequestOptimizationCandidate> candidates) {
      return backend.getOptimizationService().optimizeRequestsWithDynamicProgramming(
          candidates.toArray(new RequestOptimizationCandidate[0]));
    }

    @Override
    public RequestOptimizationResult runBruteForce(List<RequestOptimizationCandidate> candidates) {
      return backend.getOptimizationService().optimizeRequestsWithBruteForce(
          candidates.toArray(new RequestOptimizationCandidate[0]));
    }

    @Override
    public OptimizationComparison compare(List<RequestOptimizationCandidate> candidates) {
      return backend.getOptimizationService().compareExact(
          candidates.toArray(new RequestOptimizationCandidate[0]));
    }

    @Override
    public AssignmentCandidate recommendResource(int requestId) {
      return call("recommend a resource", () -> backend.getAssignmentService().recommendResource(requestId));
    }

    private static ServiceRequest[] requests(BackendContext backend) throws FrontendServiceException {
      try {
        return backend.getRequestService().getAllRequests();
      } catch (SQLException exception) {
        throw new FrontendServiceException("Unable to load optimization requests", exception);
      }
    }
  }

  private static <T> List<T> list(T[] values) {
    return List.copyOf(Arrays.asList(values));
  }

  private static <T> T call(String action, SqlCall<T> operation) {
    try {
      return operation.execute();
    } catch (SQLException exception) {
      throw new FrontendServiceException("Unable to " + action, exception);
    }
  }

  @FunctionalInterface
  private interface SqlCall<T> {
    T execute() throws SQLException;
  }
}
