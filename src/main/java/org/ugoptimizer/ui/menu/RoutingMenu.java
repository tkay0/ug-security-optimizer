package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.Edge;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Menu for campus routing operations.
 *
 * <p>Delegates traversal to an injected {@link RouteService}, which owns
 * building the graph from wherever locations/roads actually live &mdash;
 * this menu no longer depends on the concrete {@link LocationRoadMenu} class.
 * BFS, DFS, and shortest-path (Dijkstra, minimizing total road travel time)
 * are all wired up. {@link LocationService} is only used here to look up a
 * visited vertex's display name. For a found shortest path, the result table
 * also shows the specific road (by travel time) used to reach each stop, not
 * just the destination sequence -- BFS/DFS only carry visit order, not
 * per-edge weights, so that column is a placeholder for those two.
 */
public class RoutingMenu extends JPanel {

    static final String BASE_NETWORK = "Base network";

    private final RouteService routeService;
    private final LocationService locationService;
    private final DataTablePanel<VisitStep> resultTable;
    private final JTextField startField;
    private final JTextField destinationField;
    private final JComboBox<String> scenarioField;
    private final JLabel statusLabel;
    private final BackgroundAction routeAction = new BackgroundAction();
    private final BackgroundAction scenarioAction = new BackgroundAction();

    public RoutingMenu(RouteService routeService, LocationService locationService) {
        super(new BorderLayout(8, 8));
        this.routeService = Objects.requireNonNull(routeService, "routeService cannot be null");
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        startField = new JTextField(6);
        destinationField = new JTextField(6);
        JButton bfsButton = new JButton("Run BFS");
        JButton dfsButton = new JButton("Run DFS");
        JButton shortestPathButton = new JButton("Run Shortest Path (Dijkstra)");
        JButton refreshScenariosButton = new JButton("Refresh Networks");
        scenarioField = new JComboBox<>(new String[] {BASE_NETWORK});
        statusLabel = new JLabel("BFS/DFS use the base network.");

        controls.add(new JLabel("Shortest-path network:"));
        controls.add(scenarioField);
        controls.add(refreshScenariosButton);
        controls.add(new JLabel("Start Location ID:"));
        controls.add(startField);
        controls.add(bfsButton);
        controls.add(dfsButton);
        controls.add(new JLabel("Destination Location ID:"));
        controls.add(destinationField);
        controls.add(shortestPathButton);

        resultTable = new DataTablePanel<>(List.of(
                new Column<>("Order", s -> String.valueOf(s.order())),
                new Column<>("Location ID", s -> String.valueOf(s.locationId())),
                new Column<>("Name", VisitStep::name),
                new Column<>("Via Road (travel time)", VisitStep::viaRoad)
        ), List.of());

        bfsButton.addActionListener(e -> runTraversal(bfsButton, routeService::bfs));
        dfsButton.addActionListener(e -> runTraversal(dfsButton, routeService::dfs));
        shortestPathButton.addActionListener(e -> runShortestPath(shortestPathButton));
        refreshScenariosButton.addActionListener(e -> refreshScenarios(refreshScenariosButton));

        add(controls, BorderLayout.NORTH);
        add(resultTable, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        SwingUtilities.invokeLater(() -> refreshScenarios(refreshScenariosButton));
    }

    private interface TraversalFunction {
        TraversalResult traverse(int startVertexId);
    }

    private void refreshScenarios(JButton control) {
        boolean started = scenarioAction.start(
                control,
                "Loading...",
                routeService::getScenarioNames,
                names -> {
                    Object selected = scenarioField.getSelectedItem();
                    scenarioField.removeAllItems();
                    scenarioField.addItem(BASE_NETWORK);
                    for (String name : names) {
                        scenarioField.addItem(name);
                    }
                    if (BASE_NETWORK.equals(selected) || names.contains(selected)) {
                        scenarioField.setSelectedItem(selected);
                    } else {
                        scenarioField.setSelectedItem(BASE_NETWORK);
                    }
                },
                failure -> UiErrors.show(this, "load routing scenarios", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "Scenario loading is already in progress.");
        }
    }

    private void runTraversal(JButton control, TraversalFunction algorithm) {
        int start;
        try {
            start = Integer.parseInt(startField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start Location ID must be a number.");
            return;
        }

        boolean started = routeAction.start(
                control,
                "Running...",
                () -> new TraversalView(
                        algorithm.traverse(start), locationService.findAllLocations()),
                view -> showTraversal(start, view),
                failure -> UiErrors.show(this, "run graph traversal", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A routing operation is already in progress.");
        }
    }

    private void showTraversal(int start, TraversalView view) {
        TraversalResult result = view.result();

        if (result.getStatus() == TraversalResult.Status.MISSING_START) {
            resultTable.setRows(List.of());
            MessagePrinter.showError(this, "Location " + start + " is not in the current location list.");
            return;
        }

        List<VisitStep> steps = new ArrayList<>();
        int[] order = result.getVisitOrder();
        for (int i = 0; i < order.length; i++) {
            steps.add(new VisitStep(i + 1, order[i], nameOf(view.locations(), order[i]), "—"));
        }
        resultTable.setRows(steps);
        statusLabel.setText("Traversal completed on the base network.");

        if (result.getStatus() == TraversalResult.Status.PARTIAL) {
            MessagePrinter.showInfo(this, "Reached " + result.getVisitedCount() + " of "
                    + result.getTotalVertexCount()
                    + " locations. The rest are unreachable from location " + start
                    + " (no unblocked road connects them).");
        }
    }

    private void runShortestPath(JButton control) {
        int source;
        int destination;
        try {
            source = Integer.parseInt(startField.getText().trim());
            destination = Integer.parseInt(destinationField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start and Destination Location ID must both be numbers.");
            return;
        }

        String network = Objects.toString(scenarioField.getSelectedItem(), BASE_NETWORK);
        boolean started = routeAction.start(
                control,
                "Routing...",
                () -> {
                    PathResult result = BASE_NETWORK.equals(network)
                            ? routeService.shortestPath(source, destination)
                            : routeService.shortestPathUnderScenario(network, source, destination);
                    return new PathView(result, locationService.findAllLocations(), network);
                },
                view -> showShortestPath(source, destination, view),
                failure -> UiErrors.show(this, "calculate the selected route", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A routing operation is already in progress.");
        }
    }

    private void showShortestPath(int source, int destination, PathView view) {
        PathResult result = view.result();
        String network = view.network();
        statusLabel.setText("Applied network: " + network);

        switch (result.getStatus()) {
            case MISSING_SOURCE -> MessagePrinter.showError(this,
                    "Location " + source + " is not in the current location list.");
            case MISSING_DESTINATION -> MessagePrinter.showError(this,
                    "Location " + destination + " is not in the current location list.");
            case MISSING_BOTH -> MessagePrinter.showError(this,
                    "Neither location " + source + " nor " + destination + " is in the current location list.");
            case UNREACHABLE -> {
                resultTable.setRows(List.of());
                MessagePrinter.showInfo(this, "No path exists from location " + source
                        + " to location " + destination + " (no unblocked road connects them).");
            }
            case FOUND -> {
                List<VisitStep> steps = new ArrayList<>();
                int[] path = result.getVertexIds();
                Edge[] edges = result.getEdges();
                for (int i = 0; i < path.length; i++) {
                    String viaRoad = i == 0
                            ? "(start)"
                            : String.format("%.2f min", edges[i - 1].getWeight());
                    steps.add(new VisitStep(
                            i + 1, path[i], nameOf(view.locations(), path[i]), viaRoad));
                }
                resultTable.setRows(steps);
                MessagePrinter.showInfo(this, "Shortest path found using " + network + ": "
                        + path.length + " location(s), "
                        + result.getEdgeCount() + " road(s), total travel time "
                        + String.format("%.2f", result.getTotalWeight().orElse(0.0d)) + " minutes.");
            }
        }
    }

    private static String nameOf(List<Location> locations, int locationId) {
        for (Location location : locations) {
            if (location.getLocationId() == locationId) {
                return location.getName();
            }
        }
        return "(unknown)";
    }

    /** {@code viaRoad} is the incoming road's travel time for a shortest-path step, or a
     *  placeholder for BFS/DFS steps (which only carry visit order, not per-edge weights). */
    private record VisitStep(int order, int locationId, String name, String viaRoad) {
    }

    private record TraversalView(TraversalResult result, List<Location> locations) {
    }

    private record PathView(PathResult result, List<Location> locations, String network) {
    }
}
