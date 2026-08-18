package org.ugoptimizer.ui.menu;

import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.SelectionSort;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.algorithms.assignment.PlaceholderResponseMetrics;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Menu for recording and viewing algorithm performance runs.
 *
 * <p>Every row in the table comes from an actual timed execution started by
 * clicking a button here &mdash; nothing is pre-seeded or typed in, per the
 * project rule that algorithm-run timings must come from real executions.
 * The finished {@link AlgorithmRun} is persisted through an injected
 * {@link ReportService} instead of a private list.
 *
 * <p>Covers every algorithm category with a real, wired implementation:
 * sorting (against a random array of the given input size), graph traversal
 * and shortest-path (against the current location/road graph via
 * {@link RouteService}/{@link LocationService}, input size = current
 * location count), and greedy assignment (against current resources via
 * {@link ResourceService}, input size = current resource count).
 */
public class ReportMenu extends JPanel {

    private final ReportService reportService;
    private final RouteService routeService;
    private final LocationService locationService;
    private final ResourceService resourceService;
    private final Map<String, Integer> runNumberByAlgorithm = new HashMap<>();
    private int nextRunId = 1;

    private DataTablePanel<AlgorithmRun> runTable;
    private JTextField sizeField;
    private JTextField routeStartField;
    private JTextField routeDestinationField;

    public ReportMenu(
            ReportService reportService,
            RouteService routeService,
            LocationService locationService,
            ResourceService resourceService) {
        super(new BorderLayout(8, 8));
        this.reportService = Objects.requireNonNull(reportService, "reportService cannot be null");
        this.routeService = Objects.requireNonNull(routeService, "routeService cannot be null");
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");

        JPanel sortControls = buildSortControls();
        JPanel routeControls = buildRouteControls();
        JPanel greedyControls = buildGreedyControls();

        JPanel allControls = new JPanel();
        allControls.setLayout(new javax.swing.BoxLayout(allControls, javax.swing.BoxLayout.Y_AXIS));
        allControls.add(sortControls);
        allControls.add(routeControls);
        allControls.add(greedyControls);

        runTable = new DataTablePanel<>(List.of(
                new Column<>("Run ID", r -> String.valueOf(r.getRunId())),
                new Column<>("Algorithm", AlgorithmRun::getAlgorithmName),
                new Column<>("Input Size", r -> String.valueOf(r.getInputSize())),
                new Column<>("Time (ns)", r -> String.valueOf(r.getTimeNs())),
                new Column<>("Memory (KB, approx.)", r -> String.valueOf(r.getMemoryKb())),
                new Column<>("Run #", r -> String.valueOf(r.getRunNumber()))
        ), reportService.findAll());

        add(allControls, BorderLayout.NORTH);
        add(runTable, BorderLayout.CENTER);
    }

    private JPanel buildSortControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        sizeField = new JTextField("1000", 8);
        JButton mergeSortRun = new JButton("Run & Record MergeSort");
        JButton quickSortRun = new JButton("Run & Record QuickSort");
        JButton insertionSortRun = new JButton("Run & Record InsertionSort");
        JButton selectionSortRun = new JButton("Run & Record SelectionSort");

        mergeSortRun.addActionListener(e -> runAndRecordSort("MergeSort", MergeSort::sort));
        quickSortRun.addActionListener(e -> runAndRecordSort("QuickSort", QuickSort::sort));
        insertionSortRun.addActionListener(e -> runAndRecordSort("InsertionSort", InsertionSort::sort));
        selectionSortRun.addActionListener(e -> runAndRecordSort("SelectionSort", SelectionSort::sort));

        controls.add(new JLabel("Sort input size:"));
        controls.add(sizeField);
        controls.add(mergeSortRun);
        controls.add(quickSortRun);
        controls.add(insertionSortRun);
        controls.add(selectionSortRun);
        return controls;
    }

    private JPanel buildRouteControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        routeStartField = new JTextField(4);
        routeDestinationField = new JTextField(4);
        JButton bfsRun = new JButton("Run & Record BFS");
        JButton dfsRun = new JButton("Run & Record DFS");
        JButton dijkstraRun = new JButton("Run & Record Dijkstra");

        bfsRun.addActionListener(e -> runAndRecordTraversal("BFS", routeService::bfs));
        dfsRun.addActionListener(e -> runAndRecordTraversal("DFS", routeService::dfs));
        dijkstraRun.addActionListener(e -> runAndRecordDijkstra());

        controls.add(new JLabel("Start Location ID:"));
        controls.add(routeStartField);
        controls.add(bfsRun);
        controls.add(dfsRun);
        controls.add(new JLabel("Destination Location ID:"));
        controls.add(routeDestinationField);
        controls.add(dijkstraRun);
        return controls;
    }

    private JPanel buildGreedyControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton greedyRun = new JButton("Run & Record Greedy Assignment");
        greedyRun.addActionListener(e -> runAndRecordGreedy());
        controls.add(greedyRun);
        return controls;
    }

    private interface SortFunction {
        void sort(Integer[] array);
    }

    private interface TraversalFunction {
        Object traverse(int startVertexId);
    }

    private void runAndRecordSort(String algorithmName, SortFunction sortFunction) {
        int size;
        try {
            size = Integer.parseInt(sizeField.getText().trim());
            if (size <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Sort input size must be a positive whole number.");
            return;
        }

        Integer[] array = new Integer[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }

        recordRun(algorithmName, size, () -> sortFunction.sort(array));
    }

    private void runAndRecordTraversal(String algorithmName, TraversalFunction algorithm) {
        int start;
        try {
            start = Integer.parseInt(routeStartField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start Location ID must be a number.");
            return;
        }

        int inputSize = locationService.findAllLocations().size();
        if (inputSize <= 0) {
            MessagePrinter.showError(this, "No locations exist to traverse.");
            return;
        }

        recordRun(algorithmName, inputSize, () -> algorithm.traverse(start));
    }

    private void runAndRecordDijkstra() {
        int source;
        int destination;
        try {
            source = Integer.parseInt(routeStartField.getText().trim());
            destination = Integer.parseInt(routeDestinationField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start and Destination Location ID must both be numbers.");
            return;
        }

        int inputSize = locationService.findAllLocations().size();
        if (inputSize <= 0) {
            MessagePrinter.showError(this, "No locations exist to route between.");
            return;
        }

        recordRun("Dijkstra", inputSize, () -> routeService.shortestPath(source, destination));
    }

    private void runAndRecordGreedy() {
        List<Resource> resources = resourceService.findAll();
        int inputSize = resources.size();
        if (inputSize <= 0) {
            MessagePrinter.showError(this, "No resources exist to assign.");
            return;
        }

        AssignmentCandidate[] candidates = new AssignmentCandidate[inputSize];
        for (int i = 0; i < inputSize; i++) {
            Resource resource = resources.get(i);
            candidates[i] = new AssignmentCandidate(
                    resource,
                    PlaceholderResponseMetrics.responseTime(resource),
                    PlaceholderResponseMetrics.workload(resource));
        }
        ServiceRequest sampleRequest = buildSampleRequest();

        recordRun("GreedyAssignment", inputSize,
                () -> GreedyAssignment.assignBestResource(sampleRequest, candidates));
    }

    private ServiceRequest buildSampleRequest() {
        return new ServiceRequest(1, 1, 2, "MEDICAL_EMERGENCY", 5,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "MEDICAL_TEAM", "Sample request for greedy assignment benchmark");
    }

    /** Times {@code action}, records the measured {@link AlgorithmRun}, and refreshes the table. */
    private void recordRun(String algorithmName, int inputSize, Runnable action) {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memBeforeBytes = runtime.totalMemory() - runtime.freeMemory();

        long startNs = System.nanoTime();
        action.run();
        long elapsedNs = System.nanoTime() - startNs;

        long memAfterBytes = runtime.totalMemory() - runtime.freeMemory();
        double memoryKb = Math.max(0L, memAfterBytes - memBeforeBytes) / 1024.0;

        int runNumber = runNumberByAlgorithm.merge(algorithmName, 1, Integer::sum);
        AlgorithmRun run = new AlgorithmRun(
                nextRunId++,
                algorithmName,
                inputSize,
                elapsedNs,
                memoryKb,
                Instant.now(),
                "MEASURED",
                "GUI_MANUAL_RUN",
                runNumber);
        reportService.record(run);
        runTable.setRows(reportService.findAll());
    }
}
