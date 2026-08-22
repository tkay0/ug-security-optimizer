package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.ugoptimizer.performance.BenchmarkCsvWriter;
import org.ugoptimizer.performance.BenchmarkReport;
import org.ugoptimizer.performance.EfficiencyLab;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

/** Examiner-facing access to genuine representative and full benchmark exports. */
public final class EfficiencyLabMenu extends JPanel {

    private final EfficiencyLab lab = new EfficiencyLab();
    private final BenchmarkCsvWriter writer = new BenchmarkCsvWriter();
    private final BackgroundAction action = new BackgroundAction();
    private final JTextArea output = new JTextArea();

    public EfficiencyLabMenu() {
        super(new BorderLayout(8, 8));
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setText("Representative runs are small. The full official-scale lab may take several minutes.\n"
                + "CSV rows contain three raw trials and their group average; memory is approximate.");

        JButton representative = new JButton("Run Representative Lab");
        representative.addActionListener(event -> run(representative, false));
        JButton full = new JButton("Run Full Official Lab");
        full.addActionListener(event -> confirmFull(full));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(representative);
        controls.add(full);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    private void confirmFull(JButton control) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Run every official input size with three trials? This may take several minutes.",
                "Full Efficiency Lab",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            run(control, true);
        }
    }

    private void run(JButton control, boolean full) {
        Path directory = Path.of("results", full ? "full-efficiency-lab" : "representative-efficiency-lab");
        action.start(
                control,
                "Running...",
                () -> {
                    BenchmarkReport report = full ? lab.runFull() : lab.runRepresentative();
                    writer.write(directory, report);
                    return "Completed " + report.getRecords().length + " genuine raw trial rows.\n"
                            + "Results: " + directory.resolve("benchmark-results.csv") + "\n"
                            + "Environment: " + directory.resolve("environment.txt") + "\n\n"
                            + report.getEnvironment();
                },
                output::setText,
                failure -> UiErrors.show(this, "run the efficiency lab", failure));
    }
}
