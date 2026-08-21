package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.frontend.OptimizationService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.OptimizationComparison;
import org.ugoptimizer.result.RequestOptimizationCandidate;
import org.ugoptimizer.result.RequestOptimizationResult;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

/** Swing view over the canonical request optimization and assignment services. */
public class OptimizationMenu extends JPanel {
  private final RequestService requestService;
  private final OptimizationService optimizationService;
  private final DataTablePanel<ServiceRequest> requestTable;
  private final JTextArea resultArea;
  private final BackgroundAction operation = new BackgroundAction();

  public OptimizationMenu(RequestService requestService, OptimizationService optimizationService) {
    super(new BorderLayout(8, 8));
    this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
    this.optimizationService = Objects.requireNonNull(
        optimizationService, "optimizationService cannot be null");
    requestTable = new DataTablePanel<>(List.of(
        new Column<>("ID", r -> String.valueOf(r.getRequestId())),
        new Column<>("Category", ServiceRequest::getCategory),
        new Column<>("Urgency", r -> String.valueOf(r.getUrgency())),
        new Column<>("Status", ServiceRequest::getStatus),
        new Column<>("Required Resource", r -> String.valueOf(r.getRequiredResourceType()))
    ), List.of());

    resultArea = new JTextArea(8, 60);
    resultArea.setEditable(false);
    resultArea.setLineWrap(true);
    resultArea.setWrapStyleWord(true);

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton refresh = new JButton("Refresh Pending Requests");
    JButton dp = new JButton("Run Dynamic Programming");
    JButton brute = new JButton("Run Brute Force");
    JButton compare = new JButton("Compare DP / Brute Force");
    JButton recommend = new JButton("Recommend Resource");
    refresh.addActionListener(e -> refreshPending(refresh));
    dp.addActionListener(e -> runSingle(dp, true));
    brute.addActionListener(e -> runSingle(brute, false));
    compare.addActionListener(e -> runComparison(compare));
    recommend.addActionListener(e -> recommendSelected(recommend));
    controls.add(refresh);
    controls.add(dp);
    controls.add(brute);
    controls.add(compare);
    controls.add(recommend);

    JPanel requestPanel = new JPanel(new BorderLayout(8, 8));
    requestPanel.setBorder(BorderFactory.createTitledBorder(
        "Backend request optimization (budget " + optimizationService.getBudget()
            + "; urgency is the domain benefit)"));
    requestPanel.add(requestTable, BorderLayout.CENTER);
    requestPanel.add(controls, BorderLayout.SOUTH);

    JPanel resultPanel = new JPanel(new BorderLayout());
    resultPanel.setBorder(BorderFactory.createTitledBorder("Optimization result"));
    resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
    add(requestPanel, BorderLayout.CENTER);
    add(resultPanel, BorderLayout.SOUTH);
    SwingUtilities.invokeLater(() -> refreshPending(refresh));
  }

  private List<ServiceRequest> pendingRequests() {
    return requestService.findAll().stream()
        .filter(request -> "PENDING".equals(request.getStatus()))
        .toList();
  }

  private List<RequestOptimizationCandidate> candidates() {
    return optimizationService.pendingRequestCandidates();
  }

  private void refreshPending(JButton control) {
    start(
        control,
        "Refreshing...",
        this::pendingRequests,
        requestTable::setRows,
        "refresh pending requests");
  }

  private void runSingle(JButton control, boolean dynamicProgramming) {
    start(
        control,
        "Optimizing...",
        () -> {
          List<RequestOptimizationCandidate> candidates = candidates();
          return dynamicProgramming
              ? optimizationService.runDynamicProgramming(candidates)
              : optimizationService.runBruteForce(candidates);
        },
        result -> resultArea.setText(format(result)),
        "run request optimization");
  }

  private void runComparison(JButton control) {
    start(
        control,
        "Comparing...",
        () -> optimizationService.compare(candidates()),
        comparison -> resultArea.setText(format(comparison.getDynamicProgramming())
            + "\n\n" + format(comparison.getBruteForce())
            + "\n\nSame optimum: " + comparison.hasSameOptimum()),
        "compare optimization methods");
  }

  private void recommendSelected(JButton control) {
    ServiceRequest selected = requestTable.getSelectedRow();
    if (selected == null) {
      MessagePrinter.showError(this, "Select a pending request first.");
      return;
    }
    start(
        control,
        "Recommending...",
        () -> optimizationService.recommendResource(selected.getRequestId()),
        candidate -> resultArea.setText(formatRecommendation(selected, candidate)),
        "recommend a compatible resource");
  }

  private <T> void start(
      JButton control,
      String busyText,
      java.util.concurrent.Callable<T> task,
      java.util.function.Consumer<T> success,
      String action) {
    boolean started = operation.start(
        control,
        busyText,
        task,
        success,
        failure -> UiErrors.show(this, action, failure));
    if (!started) {
      MessagePrinter.showInfo(this, "An optimization operation is already in progress.");
    }
  }

  static String formatRecommendation(ServiceRequest request, AssignmentCandidate candidate) {
    var resource = candidate.getResource();
    Integer currentLocation = resource.getCurrentLocationId();
    return "Recommendation only — no assignment has been persisted\n"
        + "Request: " + request.getRequestId() + " (urgency " + request.getUrgency() + ")\n"
        + "Request locations: " + request.getSourceLocationId() + " -> "
        + request.getDestinationLocationId() + "\n"
        + "Selected resource: " + resource.getResourceId() + " ("
        + resource.getResourceType() + ")\n"
        + "Resource location: "
        + (currentLocation == null ? resource.getHomeLocationId() : currentLocation) + "\n"
        + "Current resource state: " + resource.getAvailabilityStatus() + "\n"
        + "Estimated response cost: " + String.format("%.2f minutes", candidate.getResponseTime());
  }

  private static String format(RequestOptimizationResult result) {
    var objective = result.getObjective();
    StringBuilder text = new StringBuilder(result.getAlgorithm()).append('\n')
        .append("Selected requests: ").append(objective.getSelectedCount()).append('\n')
        .append("Total cost: ").append(objective.getTotalCost()).append(" / ")
        .append(objective.getCapacity()).append('\n')
        .append("Total urgency benefit: ").append(objective.getTotalBenefit()).append('\n')
        .append("Request IDs: ");
    int[] ids = objective.getSelectedItemIds();
    for (int i = 0; i < ids.length; i++) {
      if (i > 0) text.append(", ");
      text.append(ids[i]);
    }
    return text.toString();
  }
}
