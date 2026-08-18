package org.ugoptimizer.ui.menu;

import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.SelectionSort;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.algorithms.assignment.PlaceholderResponseMetrics;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Menu for running optimization algorithms: greedy resource assignment, and
 * a sort comparison across all four implemented sorts (MergeSort, QuickSort,
 * InsertionSort, SelectionSort). DP / brute-force are not wired up here
 * because no such algorithm exists on this branch yet &mdash; only real,
 * implemented algorithms are demonstrated.
 *
 * <p>Candidate resources come from an injected {@link ResourceService}
 * instead of a hardcoded array, refreshed on demand via the Refresh
 * Candidates button since resources can be added on another tab after this
 * screen is built. {@link Resource} has no persisted response-time or
 * workload fields (those are request-specific runtime inputs, not stored
 * resource attributes); {@link PlaceholderResponseMetrics} synthesizes
 * stand-in values until a real estimate is wired through a service.
 */
public class OptimizationMenu extends JPanel {

    private static final int SORT_SAMPLE_SIZE = 15;

    private final ResourceService resourceService;
    private final ServiceRequest sampleRequest;

    private DataTablePanel<AssignmentCandidate> candidateTable;
    private List<AssignmentCandidate> candidates;
    private Integer[] currentArray;
    private JLabel arrayLabel;

    public OptimizationMenu(ResourceService resourceService) {
        super(new BorderLayout(8, 8));
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");
        sampleRequest = buildSampleRequest();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildGreedyPanel(), buildSortPanel());
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildGreedyPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                "Greedy Resource Assignment — request: " + sampleRequest.getCategory()
                        + " (needs " + sampleRequest.getRequiredResourceType() + ")"));

        candidates = buildCandidates();
        candidateTable = new DataTablePanel<>(List.of(
                new Column<>("Resource ID", c -> String.valueOf(c.getResource().getResourceId())),
                new Column<>("Type", c -> c.getResource().getResourceType()),
                new Column<>("Availability", c -> c.getResource().getAvailabilityStatus()),
                new Column<>("Response Time", c -> String.valueOf(c.getResponseTime())),
                new Column<>("Workload", c -> String.valueOf(c.getCurrentWorkload()))
        ), candidates);
        panel.add(candidateTable, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Candidates");
        JButton runButton = new JButton("Run Greedy Assignment");

        refreshButton.addActionListener(e -> {
            candidates = buildCandidates();
            candidateTable.setRows(candidates);
        });

        runButton.addActionListener(e -> {
            AssignmentCandidate[] array = candidates.toArray(new AssignmentCandidate[0]);
            AssignmentCandidate best = GreedyAssignment.assignBestResource(sampleRequest, array);
            if (best == null) {
                MessagePrinter.showInfo(this, "No eligible resource: none are both AVAILABLE "
                        + "and of type " + sampleRequest.getRequiredResourceType() + ".");
            } else {
                MessagePrinter.showInfo(this, "Chosen: Resource " + best.getResource().getResourceId()
                        + " (" + best.getResource().getResourceType() + "), response time "
                        + best.getResponseTime() + ", workload " + best.getCurrentWorkload()
                        + ".\n\nWhy: lowest response time among AVAILABLE, matching-type candidates.");
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(refreshButton);
        controls.add(runButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSortPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Sort Comparison"));

        arrayLabel = new JLabel();
        generateArray();

        JButton regenerateButton = new JButton("Generate New Array");
        JButton mergeSortButton = new JButton("Sort with MergeSort");
        JButton quickSortButton = new JButton("Sort with QuickSort");
        JButton insertionSortButton = new JButton("Sort with InsertionSort");
        JButton selectionSortButton = new JButton("Sort with SelectionSort");

        regenerateButton.addActionListener(e -> generateArray());
        mergeSortButton.addActionListener(e -> runSort("MergeSort", MergeSort::sort));
        quickSortButton.addActionListener(e -> runSort("QuickSort", QuickSort::sort));
        insertionSortButton.addActionListener(e -> runSort("InsertionSort", InsertionSort::sort));
        selectionSortButton.addActionListener(e -> runSort("SelectionSort", SelectionSort::sort));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(regenerateButton);
        controls.add(mergeSortButton);
        controls.add(quickSortButton);
        controls.add(insertionSortButton);
        controls.add(selectionSortButton);

        panel.add(arrayLabel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private interface SortFunction {
        void sort(Integer[] array);
    }

    private void runSort(String algorithmName, SortFunction sortFunction) {
        Integer[] copy = currentArray.clone();
        long start = System.nanoTime();
        sortFunction.sort(copy);
        long elapsedNs = System.nanoTime() - start;

        arrayLabel.setText("<html>Unsorted: " + Arrays.toString(currentArray)
                + "<br>" + algorithmName + " result: " + Arrays.toString(copy)
                + "<br><i>" + elapsedNs + " ns for " + copy.length + " elements (measured just now)</i></html>");
    }

    private void generateArray() {
        Random random = new Random();
        currentArray = new Integer[SORT_SAMPLE_SIZE];
        for (int i = 0; i < currentArray.length; i++) {
            currentArray[i] = random.nextInt(100);
        }
        arrayLabel.setText("Unsorted: " + Arrays.toString(currentArray));
    }

    private ServiceRequest buildSampleRequest() {
        return new ServiceRequest(1, 1, 2, "MEDICAL_EMERGENCY", 5,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "MEDICAL_TEAM", "Sample request for greedy assignment demo");
    }

    /** Wraps each current resource with a {@link PlaceholderResponseMetrics} estimate. */
    private List<AssignmentCandidate> buildCandidates() {
        List<AssignmentCandidate> built = new ArrayList<>();
        for (Resource resource : resourceService.findAll()) {
            built.add(new AssignmentCandidate(
                    resource,
                    PlaceholderResponseMetrics.responseTime(resource),
                    PlaceholderResponseMetrics.workload(resource)));
        }
        return built;
    }
}
