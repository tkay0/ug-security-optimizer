package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.Location;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.service.LocationService;
import org.ugoptimizer.service.RouteService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
 * visited vertex's display name.
 */
public class RoutingMenu extends JPanel {

    private final LocationService locationService;
    private final DataTablePanel<VisitStep> resultTable;
    private final JTextField startField;
    private final JTextField destinationField;

    public RoutingMenu(RouteService routeService, LocationService locationService) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(routeService, "routeService cannot be null");
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        startField = new JTextField(6);
        destinationField = new JTextField(6);
        JButton bfsButton = new JButton("Run BFS");
        JButton dfsButton = new JButton("Run DFS");
        JButton shortestPathButton = new JButton("Run Shortest Path (Dijkstra)");

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
                new Column<>("Name", VisitStep::name)
        ), List.of());

        bfsButton.addActionListener(e -> runTraversal(routeService::bfs));
        dfsButton.addActionListener(e -> runTraversal(routeService::dfs));
        shortestPathButton.addActionListener(e -> runShortestPath(routeService));

        add(controls, BorderLayout.NORTH);
        add(resultTable, BorderLayout.CENTER);
    }

    private interface TraversalFunction {
        TraversalResult traverse(int startVertexId);
    }

    private void runTraversal(TraversalFunction algorithm) {
        int start;
        try {
            start = Integer.parseInt(startField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start Location ID must be a number.");
            return;
        }

        TraversalResult result = algorithm.traverse(start);

        if (result.getStatus() == TraversalResult.Status.MISSING_START) {
            MessagePrinter.showError(this, "Location " + start + " is not in the current location list.");
            return;
        }

        List<VisitStep> steps = new ArrayList<>();
        int[] order = result.getVisitOrder();
        for (int i = 0; i < order.length; i++) {
            steps.add(new VisitStep(i + 1, order[i], nameOf(order[i])));
        }
        resultTable.setRows(steps);

        if (result.getStatus() == TraversalResult.Status.PARTIAL) {
            MessagePrinter.showInfo(this, "Reached " + result.getVisitedCount() + " of "
                    + result.getTotalVertexCount()
                    + " locations. The rest are unreachable from location " + start
                    + " (no unblocked road connects them).");
        }
    }

    private void runShortestPath(RouteService routeService) {
        int source;
        int destination;
        try {
            source = Integer.parseInt(startField.getText().trim());
            destination = Integer.parseInt(destinationField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start and Destination Location ID must both be numbers.");
            return;
        }

        PathResult result = routeService.shortestPath(source, destination);

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
                for (int i = 0; i < path.length; i++) {
                    steps.add(new VisitStep(i + 1, path[i], nameOf(path[i])));
                }
                resultTable.setRows(steps);
                MessagePrinter.showInfo(this, "Shortest path found: " + path.length + " location(s), "
                        + result.getEdgeCount() + " road(s), total travel time "
                        + result.getTotalWeight().orElse(0.0d) + " minutes.");
            }
        }
    }

    private String nameOf(int locationId) {
        for (Location location : locationService.findAllLocations()) {
            if (location.getLocationId() == locationId) {
                return location.getName();
            }
        }
        return "(unknown)";
    }

    private record VisitStep(int order, int locationId, String name) {
    }
}
