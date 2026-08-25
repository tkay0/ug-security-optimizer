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
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.UiOption;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
    private final boolean academicMode;
    private final DataTablePanel<VisitStep> resultTable;
    private final JTextField startField;
    private final JTextField destinationField;
    private final JComboBox<UiOption<Integer>> operationalStartField;
    private final JComboBox<UiOption<Integer>> operationalDestinationField;
    private final JComboBox<UiOption<String>> scenarioField;
    private final JLabel statusLabel;
    private final JTextArea recommendedRouteArea;
    private final JLabel routeMetricsLabel;
    private final BackgroundAction routeAction = new BackgroundAction();
    private final BackgroundAction scenarioAction = new BackgroundAction();

    public RoutingMenu(RouteService routeService, LocationService locationService) {
        this(routeService, locationService, true);
    }

    /** Creates the operator-facing route finder without technical traversal controls. */
    public static RoutingMenu operational(
            RouteService routeService, LocationService locationService) {
        return new RoutingMenu(routeService, locationService, false);
    }

    private RoutingMenu(
            RouteService routeService,
            LocationService locationService,
            boolean academicMode) {
        super(new BorderLayout(8, 8));
        this.routeService = Objects.requireNonNull(routeService, "routeService cannot be null");
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");
        this.academicMode = academicMode;

        if (academicMode) {
            startField = new JTextField(6);
            destinationField = new JTextField(6);
            operationalStartField = null;
            operationalDestinationField = null;
        } else {
            startField = null;
            destinationField = null;
            UiOption<Integer>[] locations = UiFormat.locationOptions(
                    locationService.findAllLocations());
            operationalStartField = new JComboBox<>(locations);
            operationalStartField.setName("operationalRoute.from");
            operationalDestinationField = new JComboBox<>(locations);
            operationalDestinationField.setName("operationalRoute.to");
            selectLocation(operationalStartField, "Sports Stadium");
            selectLocation(operationalDestinationField, "Commonwealth Hall");
        }
        JButton bfsButton = new JButton("Run BFS");
        JButton dfsButton = new JButton("Run DFS");
        JButton shortestPathButton = new JButton(
                academicMode ? "Run Shortest Path (Dijkstra)" : "Find Best Route");
        JButton refreshScenariosButton = new JButton(
                academicMode ? "Refresh Networks" : "Reload Conditions");
        scenarioField = new JComboBox<>(scenarioOptions(List.of()));
        scenarioField.setName(academicMode ? "academicRoute.scenario" : "operationalRoute.scenario");
        statusLabel = new JLabel(academicMode
                ? "Choose a graph algorithm action to inspect its result or recorded evidence."
                : "Select two campus locations and road conditions, then choose Find Best Route.");
        if (academicMode) {
            recommendedRouteArea = null;
            routeMetricsLabel = null;
        } else {
            recommendedRouteArea = new JTextArea(3, 60);
            recommendedRouteArea.setName("operationalRoute.recommendedRoute");
            recommendedRouteArea.setEditable(false);
            recommendedRouteArea.setLineWrap(true);
            recommendedRouteArea.setWrapStyleWord(true);
            recommendedRouteArea.setText(
                    "Select two campus locations and road conditions, then choose Find Best Route.");
            routeMetricsLabel = new JLabel("Modeled Travel Cost: —    Road Conditions: —    Stops: —");
            routeMetricsLabel.setName("operationalRoute.metrics");
            routeMetricsLabel.setFont(routeMetricsLabel.getFont().deriveFont(Font.BOLD));
        }

        JPanel controls = academicMode
                ? academicControls(bfsButton, dfsButton, shortestPathButton, refreshScenariosButton)
                : operationalControls(shortestPathButton, refreshScenariosButton);

        resultTable = new DataTablePanel<>(routeColumns(academicMode), List.of());
        configureResultColumns();

        if (academicMode) {
            bfsButton.addActionListener(e -> runTraversal(bfsButton, routeService::bfs));
            dfsButton.addActionListener(e -> runTraversal(dfsButton, routeService::dfs));
        }
        shortestPathButton.addActionListener(e -> runShortestPath(shortestPathButton));
        refreshScenariosButton.addActionListener(e -> refreshScenarios(refreshScenariosButton));

        add(controls, BorderLayout.NORTH);
        add(resultTable, BorderLayout.CENTER);
        add(academicMode ? academicStatusPanel() : operationalResultPanel(), BorderLayout.SOUTH);
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
                    UiOption<String> selected = selectedScenario();
                    scenarioField.removeAllItems();
                    for (UiOption<String> option : scenarioOptions(names)) {
                        scenarioField.addItem(option);
                    }
                    selectScenario(names.contains(selected.value())
                            || BASE_NETWORK.equals(selected.value())
                            ? selected.value() : BASE_NETWORK);
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
        if (academicMode) {
            try {
                source = Integer.parseInt(startField.getText().trim());
                destination = Integer.parseInt(destinationField.getText().trim());
            } catch (NumberFormatException ex) {
                MessagePrinter.showError(this,
                        "Start and Destination Location ID must both be numbers.");
                return;
            }
        } else {
            UiOption<Integer> from = selectedLocation(operationalStartField);
            UiOption<Integer> to = selectedLocation(operationalDestinationField);
            if (from == null || to == null) {
                MessagePrinter.showError(this,
                        "At least two campus locations are required to find a route.");
                return;
            }
            source = from.value();
            destination = to.value();
        }

        UiOption<String> scenario = selectedScenario();
        String network = scenario.value();
        boolean started = routeAction.start(
                control,
                academicMode ? "Running..." : "Calculating route...",
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
        if (academicMode) {
            statusLabel.setText("Applied network: " + scenarioLabel(network));
        }
        if (result.getStatus() != PathResult.Status.FOUND) {
            resultTable.setRows(List.of());
        }

        switch (result.getStatus()) {
            case MISSING_SOURCE -> {
                showUnavailableRoute("The selected starting location is unavailable.", network);
                MessagePrinter.showError(this,
                        "Location " + source + " is not in the current location list.");
            }
            case MISSING_DESTINATION -> {
                showUnavailableRoute("The selected destination is unavailable.", network);
                MessagePrinter.showError(this,
                        "Location " + destination + " is not in the current location list.");
            }
            case MISSING_BOTH -> {
                showUnavailableRoute("The selected locations are unavailable.", network);
                MessagePrinter.showError(this,
                        "Neither location " + source + " nor " + destination
                                + " is in the current location list.");
            }
            case UNREACHABLE -> {
                showUnavailableRoute(
                        "No route is available between the selected campus locations.", network);
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
                double totalWeight = result.getTotalWeight().orElse(0.0d);
                if (academicMode) {
                    statusLabel.setText("Applied network: " + scenarioLabel(network)
                            + " | Total travel time: "
                            + String.format("%.2f minutes", totalWeight));
                } else {
                    recommendedRouteArea.setText(
                            "Recommended Route:\n" + routeNames(view.locations(), path));
                    recommendedRouteArea.setCaretPosition(0);
                    routeMetricsLabel.setText("Modeled Travel Cost: "
                            + String.format("%.2f min", totalWeight)
                            + "    Road Conditions: " + scenarioLabel(network)
                            + "    Stops: " + path.length);
                }
                MessagePrinter.showInfo(this,
                        (academicMode ? "Shortest path" : "Recommended route")
                        + " found using " + scenarioLabel(network) + ": "
                        + path.length + " location(s), "
                        + result.getEdgeCount() + " road(s), total travel time "
                        + String.format("%.2f", totalWeight) + " minutes.");
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

    private static String routeNames(List<Location> locations, int[] path) {
        StringBuilder route = new StringBuilder();
        for (int index = 0; index < path.length; index++) {
            if (index > 0) {
                route.append(" → ");
            }
            route.append(nameOf(locations, path[index]));
        }
        return route.toString();
    }

    private JPanel academicStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(statusLabel);
        return panel;
    }

    private JPanel operationalResultPanel() {
        JPanel result = new JPanel(new BorderLayout(6, 6));
        result.setBorder(BorderFactory.createTitledBorder("Route Result"));
        JScrollPane routeScroll = new JScrollPane(
                recommendedRouteArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        result.add(routeScroll, BorderLayout.CENTER);
        result.add(routeMetricsLabel, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(result, BorderLayout.CENTER);
        panel.add(new JLabel(
                "Route values are based on the selected project road scenario and are not live navigation data."),
                BorderLayout.SOUTH);
        return panel;
    }

    private void showUnavailableRoute(String message, String network) {
        if (academicMode) {
            return;
        }
        recommendedRouteArea.setText(message);
        recommendedRouteArea.setCaretPosition(0);
        routeMetricsLabel.setText("Modeled Travel Cost: Unavailable"
                + "    Road Conditions: " + scenarioLabel(network)
                + "    Stops: 0");
    }

    private void configureResultColumns() {
        if (academicMode) {
            return;
        }
        int[] widths = {70, 280, 180};
        for (int index = 0; index < widths.length; index++) {
            resultTable.getTable().getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        resultTable.getTable().setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    }

    private static void selectLocation(JComboBox<UiOption<Integer>> field, String name) {
        for (int index = 0; index < field.getItemCount(); index++) {
            if (name.equals(field.getItemAt(index).label())) {
                field.setSelectedIndex(index);
                return;
            }
        }
    }

    private JPanel academicControls(
            JButton bfs,
            JButton dfs,
            JButton shortest,
            JButton refresh) {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(new JLabel("Shortest-path network:"));
        controls.add(scenarioField);
        controls.add(refresh);
        controls.add(new JLabel("Start Location ID:"));
        controls.add(startField);
        controls.add(bfs);
        controls.add(dfs);
        controls.add(new JLabel("Destination Location ID:"));
        controls.add(destinationField);
        controls.add(shortest);
        return controls;
    }

    private JPanel operationalControls(JButton find, JButton refresh) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createTitledBorder("Find Campus Route"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 7, 5, 7);
        constraints.anchor = GridBagConstraints.WEST;
        addRouteField(form, constraints, 0, "Road Conditions", scenarioField);
        constraints.gridy = 0;
        constraints.gridx = 2;
        form.add(refresh, constraints);
        addRouteField(form, constraints, 1, "From", operationalStartField);
        addRouteField(form, constraints, 2, "To", operationalDestinationField);
        constraints.gridy = 3;
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0d;
        form.add(find, constraints);
        return form;
    }

    private static void addRouteField(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0.0d;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, constraints);
    }

    private static List<Column<VisitStep>> routeColumns(boolean academic) {
        if (academic) {
            return List.of(
                    new Column<>("Order", step -> String.valueOf(step.order())),
                    new Column<>("Location ID", step -> String.valueOf(step.locationId())),
                    new Column<>("Name", VisitStep::name),
                    new Column<>("Via Road (travel time)", VisitStep::viaRoad));
        }
        return List.of(
                new Column<>("Stop", step -> String.valueOf(step.order())),
                new Column<>("Location", VisitStep::name),
                new Column<>("Segment Travel Cost", VisitStep::viaRoad));
    }

    private static UiOption<String>[] scenarioOptions(List<String> names) {
        @SuppressWarnings("unchecked")
        UiOption<String>[] options = (UiOption<String>[]) new UiOption<?>[names.size() + 1];
        options[0] = new UiOption<>(BASE_NETWORK, scenarioLabel(BASE_NETWORK));
        for (int index = 0; index < names.size(); index++) {
            options[index + 1] = new UiOption<>(names.get(index), scenarioLabel(names.get(index)));
        }
        return options;
    }

    private static String scenarioLabel(String scenario) {
        return BASE_NETWORK.equals(scenario)
                ? "Normal / Base Conditions"
                : UiFormat.humanize(scenario);
    }

    private UiOption<String> selectedScenario() {
        @SuppressWarnings("unchecked")
        UiOption<String> selected = (UiOption<String>) scenarioField.getSelectedItem();
        return selected == null
                ? new UiOption<>(BASE_NETWORK, scenarioLabel(BASE_NETWORK))
                : selected;
    }

    private static UiOption<Integer> selectedLocation(
            JComboBox<UiOption<Integer>> field) {
        @SuppressWarnings("unchecked")
        UiOption<Integer> selected = (UiOption<Integer>) field.getSelectedItem();
        return selected;
    }

    private void selectScenario(String value) {
        for (int index = 0; index < scenarioField.getItemCount(); index++) {
            if (value.equals(scenarioField.getItemAt(index).value())) {
                scenarioField.setSelectedIndex(index);
                return;
            }
        }
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
