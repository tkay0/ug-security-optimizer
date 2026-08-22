package org.ugoptimizer.ui.menu;

import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.SelectionSort;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.result.LabelCount;
import org.ugoptimizer.result.SystemReport;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
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
 * location count). The deterministic multi-trial scheduling, greedy, and
 * comparison experiments are exposed separately by {@link EfficiencyLabMenu}.
 */
public class ReportMenu extends JPanel {

    private final ReportService reportService;
    private final RouteService routeService;
    private final LocationService locationService;
    private final Map<String, Integer> runNumberByAlgorithm = new HashMap<>();
    private final BackgroundAction reportAction = new BackgroundAction();
    private int nextRunId = 1;

    private DataTablePanel<AlgorithmRun> runTable;
    private JTextField sizeField;
    private JTextField routeStartField;
    private JTextField routeDestinationField;
    private JTextArea systemReportArea;

    public ReportMenu(
            ReportService reportService,
            RouteService routeService,
            LocationService locationService) {
        super(new BorderLayout(8, 8));
        this.reportService = Objects.requireNonNull(reportService, "reportService cannot be null");
        this.routeService = Objects.requireNonNull(routeService, "routeService cannot be null");
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");

        JPanel sortControls = buildSortControls();
        JPanel routeControls = buildRouteControls();
        JPanel systemReportControls = buildSystemReportControls();

        JPanel allControls = new JPanel();
        allControls.setLayout(new javax.swing.BoxLayout(allControls, javax.swing.BoxLayout.Y_AXIS));
        allControls.add(sortControls);
        allControls.add(routeControls);
        allControls.add(systemReportControls);

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
        systemReportArea = new JTextArea(8, 60);
        systemReportArea.setEditable(false);
        systemReportArea.setLineWrap(true);
        systemReportArea.setWrapStyleWord(true);
        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Backend System Report"));
        reportPanel.add(new JScrollPane(systemReportArea), BorderLayout.CENTER);
        add(reportPanel, BorderLayout.SOUTH);
        systemReportArea.setText("Select Refresh System Report to load current backend totals.");
    }

    private JPanel buildSystemReportControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton refresh = new JButton("Refresh System Report");
        refresh.addActionListener(e -> refreshSystemReport(refresh));
        controls.add(refresh);
        return controls;
    }

    private void refreshSystemReport(JButton control) {
        start(
                control,
                "Generating...",
                reportService::generateSystemReport,
                report -> systemReportArea.setText(formatSystemReport(report)),
                "generate the system report");
    }

    private static String formatSystemReport(SystemReport report) {
        StringBuilder text = new StringBuilder()
                .append("Generated: ").append(report.getGeneratedAt()).append('\n')
                .append("Requests: ").append(report.getTotalRequests()).append('\n')
                .append("Requests by status: ").append(formatCounts(report.getRequestsByStatus())).append('\n')
                .append("Resources: ").append(report.getTotalResources()).append('\n')
                .append("Resources by availability: ")
                .append(formatCounts(report.getResourcesByAvailability())).append('\n')
                .append("Active assignments: ").append(report.getActiveAssignmentCount()).append('\n')
                .append("Audit events: ").append(report.getAuditEventCount()).append('\n')
                .append("Locations / roads: ").append(report.getLocationCount()).append(" / ")
                .append(report.getRoadCount()).append(" (blocked: ")
                .append(report.getBlockedRoadCount()).append(")\n")
                .append("Algorithm runs: ").append(report.getAlgorithmRunCount())
                .append(" (measured: ").append(report.getMeasuredAlgorithmRunCount()).append(")\n")
                .append("Runs by algorithm: ").append(formatCounts(report.getRunsByAlgorithm()));
        return text.toString();
    }

    private static String formatCounts(LabelCount[] counts) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) text.append(", ");
            text.append(counts[i].getLabel()).append('=').append(counts[i].getCount());
        }
        return text.toString();
    }

    private JPanel buildSortControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        sizeField = new JTextField("1000", 8);
        JButton mergeSortRun = new JButton("Run & Record MergeSort");
        JButton quickSortRun = new JButton("Run & Record QuickSort");
        JButton insertionSortRun = new JButton("Run & Record InsertionSort");
        JButton selectionSortRun = new JButton("Run & Record SelectionSort");

        mergeSortRun.addActionListener(
                e -> runAndRecordSort(mergeSortRun, "MergeSort", MergeSort::sort));
        quickSortRun.addActionListener(
                e -> runAndRecordSort(quickSortRun, "QuickSort", QuickSort::sort));
        insertionSortRun.addActionListener(
                e -> runAndRecordSort(insertionSortRun, "InsertionSort", InsertionSort::sort));
        selectionSortRun.addActionListener(
                e -> runAndRecordSort(selectionSortRun, "SelectionSort", SelectionSort::sort));

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

        bfsRun.addActionListener(e -> runAndRecordTraversal(bfsRun, "BFS", routeService::bfs));
        dfsRun.addActionListener(e -> runAndRecordTraversal(dfsRun, "DFS", routeService::dfs));
        dijkstraRun.addActionListener(e -> runAndRecordDijkstra(dijkstraRun));

        controls.add(new JLabel("Start Location ID:"));
        controls.add(routeStartField);
        controls.add(bfsRun);
        controls.add(dfsRun);
        controls.add(new JLabel("Destination Location ID:"));
        controls.add(routeDestinationField);
        controls.add(dijkstraRun);
        return controls;
    }

    private interface SortFunction {
        void sort(Integer[] array);
    }

    private interface TraversalFunction {
        Object traverse(int startVertexId);
    }

    private void runAndRecordSort(
            JButton control, String algorithmName, SortFunction sortFunction) {
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

        start(
                control,
                "Running...",
                () -> {
                    Integer[] array = new Integer[size];
                    Random random = new Random();
                    for (int i = 0; i < size; i++) {
                        array[i] = random.nextInt();
                    }
                    recordRun(algorithmName, size, () -> sortFunction.sort(array));
                    return reportService.findAll();
                },
                runTable::setRows,
                "run and record " + algorithmName);
    }

    private void runAndRecordTraversal(
            JButton control, String algorithmName, TraversalFunction algorithm) {
        int start;
        try {
            start = Integer.parseInt(routeStartField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start Location ID must be a number.");
            return;
        }

        start(
                control,
                "Running...",
                () -> {
                    int inputSize = locationService.findAllLocations().size();
                    if (inputSize <= 0) {
                        throw new IllegalStateException("No locations exist to traverse");
                    }
                    recordRun(algorithmName, inputSize, () -> algorithm.traverse(start));
                    return reportService.findAll();
                },
                runTable::setRows,
                "run and record " + algorithmName);
    }

    private void runAndRecordDijkstra(JButton control) {
        int source;
        int destination;
        try {
            source = Integer.parseInt(routeStartField.getText().trim());
            destination = Integer.parseInt(routeDestinationField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Start and Destination Location ID must both be numbers.");
            return;
        }

        start(
                control,
                "Running...",
                () -> {
                    int inputSize = locationService.findAllLocations().size();
                    if (inputSize <= 0) {
                        throw new IllegalStateException("No locations exist to route between");
                    }
                    recordRun(
                            "Dijkstra",
                            inputSize,
                            () -> routeService.shortestPath(source, destination));
                    return reportService.findAll();
                },
                runTable::setRows,
                "run and record Dijkstra");
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
    }

    private <T> void start(
            JButton control,
            String busyText,
            java.util.concurrent.Callable<T> task,
            java.util.function.Consumer<T> success,
            String action) {
        boolean started = reportAction.start(
                control,
                busyText,
                task,
                success,
                failure -> UiErrors.show(this, action, failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A report or benchmark operation is already in progress.");
        }
    }
}
