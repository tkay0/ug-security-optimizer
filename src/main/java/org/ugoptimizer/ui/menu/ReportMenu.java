package org.ugoptimizer.ui.menu;

import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.service.ReportService;
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
 */
public class ReportMenu extends JPanel {

    private final ReportService reportService;
    private final Map<String, Integer> runNumberByAlgorithm = new HashMap<>();
    private int nextRunId = 1;

    private DataTablePanel<AlgorithmRun> runTable;
    private JTextField sizeField;

    public ReportMenu(ReportService reportService) {
        super(new BorderLayout(8, 8));
        this.reportService = Objects.requireNonNull(reportService, "reportService cannot be null");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        sizeField = new JTextField("1000", 8);
        JButton mergeSortRun = new JButton("Run & Record MergeSort");
        JButton quickSortRun = new JButton("Run & Record QuickSort");

        mergeSortRun.addActionListener(e -> runAndRecord("MergeSort", MergeSort::sort));
        quickSortRun.addActionListener(e -> runAndRecord("QuickSort", QuickSort::sort));

        controls.add(new JLabel("Input size:"));
        controls.add(sizeField);
        controls.add(mergeSortRun);
        controls.add(quickSortRun);

        runTable = new DataTablePanel<>(List.of(
                new Column<>("Run ID", r -> String.valueOf(r.getRunId())),
                new Column<>("Algorithm", AlgorithmRun::getAlgorithmName),
                new Column<>("Input Size", r -> String.valueOf(r.getInputSize())),
                new Column<>("Time (ns)", r -> String.valueOf(r.getTimeNs())),
                new Column<>("Memory (KB, approx.)", r -> String.valueOf(r.getMemoryKb())),
                new Column<>("Run #", r -> String.valueOf(r.getRunNumber()))
        ), reportService.findAll());

        add(controls, BorderLayout.NORTH);
        add(runTable, BorderLayout.CENTER);
    }

    private interface SortFunction {
        void sort(Integer[] array);
    }

    private void runAndRecord(String algorithmName, SortFunction sortFunction) {
        int size;
        try {
            size = Integer.parseInt(sizeField.getText().trim());
            if (size <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Input size must be a positive whole number.");
            return;
        }

        Integer[] array = new Integer[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memBeforeBytes = runtime.totalMemory() - runtime.freeMemory();

        long startNs = System.nanoTime();
        sortFunction.sort(array);
        long elapsedNs = System.nanoTime() - startNs;

        long memAfterBytes = runtime.totalMemory() - runtime.freeMemory();
        double memoryKb = Math.max(0L, memAfterBytes - memBeforeBytes) / 1024.0;

        int runNumber = runNumberByAlgorithm.merge(algorithmName, 1, Integer::sum);
        AlgorithmRun run = new AlgorithmRun(
                nextRunId++,
                algorithmName,
                size,
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
