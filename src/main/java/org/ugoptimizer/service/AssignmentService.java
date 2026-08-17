package org.ugoptimizer.service;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.AssignmentDao;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.PathResult;

/** Validates, selects, and transactionally persists request-resource assignments. */
public final class AssignmentService {
  private final RequestService requests;
  private final ResourceService resources;
  private final RouteService routes;
  private final GreedyAssignmentService greedy;
  private final AssignmentDao dao;
  private final Clock clock;

  public AssignmentService(DatabaseManager m) {
    this(
        new RequestService(m),
        new ResourceService(m),
        new RouteService(m),
        new GreedyAssignmentService(),
        new AssignmentDao(m),
        Clock.systemUTC());
  }

  public AssignmentService(
      RequestService q,
      ResourceService r,
      RouteService route,
      GreedyAssignmentService g,
      AssignmentDao d,
      Clock c) {
    requests = Objects.requireNonNull(q);
    resources = Objects.requireNonNull(r);
    routes = Objects.requireNonNull(route);
    greedy = Objects.requireNonNull(g);
    dao = Objects.requireNonNull(d);
    clock = Objects.requireNonNull(c);
  }

  public AssignmentCandidate recommendResource(int requestId) throws SQLException {
    ServiceRequest request = requirePending(requestId);
    Resource[] available = resources.getAvailableResources();
    AssignmentCandidate[] candidates = new AssignmentCandidate[available.length];
    int count = 0;
    for (Resource resource : available) {
      PathResult path =
          routes.findShortestRoute(currentLocation(resource), request.getSourceLocationId());
      if (path.isReachable())
        candidates[count++] =
            new AssignmentCandidate(resource, path.getTotalWeight().orElseThrow(), 0);
    }
    AssignmentCandidate[] trimmed = new AssignmentCandidate[count];
    System.arraycopy(candidates, 0, trimmed, 0, count);
    AssignmentCandidate selected = greedy.assign(request, trimmed);
    if (selected == null)
      throw new IllegalStateException("No compatible reachable available resource");
    return selected;
  }

  public Assignment assignBestResource(int requestId, String actor) throws SQLException {
    AssignmentCandidate selected = recommendResource(requestId);
    return persist(requestId, selected, actor);
  }

  public Assignment assignResource(int requestId, int resourceId, String actor)
      throws SQLException {
    ServiceRequest request = requirePending(requestId);
    Resource resource = resources.requireResource(resourceId);
    if (!"AVAILABLE".equals(resource.getAvailabilityStatus()))
      throw new IllegalStateException("Resource is not available");
    PathResult path =
        routes.findShortestRoute(currentLocation(resource), request.getSourceLocationId());
    if (!path.isReachable())
      throw new IllegalStateException("Resource cannot reach request location");
    AssignmentCandidate candidate =
        new AssignmentCandidate(resource, path.getTotalWeight().orElseThrow(), 0);
    if (greedy.assign(request, new AssignmentCandidate[] {candidate}) == null)
      throw new IllegalArgumentException("Resource capability does not match request");
    return persist(requestId, candidate, actor);
  }

  public Optional<Assignment> findActiveByRequestId(int id) throws SQLException {
    return dao.findActiveByRequestId(id);
  }

  public Optional<Assignment> findActiveByResourceId(int id) throws SQLException {
    return dao.findActiveByResourceId(id);
  }

  public Assignment[] getActiveAssignments() throws SQLException {
    return dao.findAllActive();
  }

  public Assignment[] getRequestAssignments(int id) throws SQLException {
    return dao.findByRequestId(id);
  }

  private Assignment persist(int id, AssignmentCandidate c, String actor) throws SQLException {
    if (actor == null || actor.isBlank())
      throw new IllegalArgumentException("actor cannot be blank");
    return dao.createAssignment(
        id, c.getResource().getResourceId(), c.getResponseTime(), clock.instant(), actor);
  }

  private ServiceRequest requirePending(int id) throws SQLException {
    ServiceRequest request = requests.requireRequest(id);
    if (!"PENDING".equals(request.getStatus()))
      throw new IllegalStateException("Only PENDING requests can be assigned");
    if (dao.findActiveByRequestId(id).isPresent())
      throw new IllegalStateException("Request already has an active assignment");
    return request;
  }

  private static int currentLocation(Resource r) {
    return r.getCurrentLocationId() == null ? r.getHomeLocationId() : r.getCurrentLocationId();
  }
}
