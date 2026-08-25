package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.ugoptimizer.evidence.CorrectnessEvidenceGenerator;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

/** Examiner-facing access to generated traces and retained correctness evidence. */
public final class CorrectnessMenu extends JPanel {

    private static final String RETAINED_EVIDENCE = """
            Generate trace evidence to inspect algorithm correctness.

            Retained correctness material:
            - docs/evidence/CORRECTNESS_ARGUMENTS.md (proof sketches and counterexamples)
            - results/correctness-evidence.txt (retained generated trace evidence)
            - src/test/java (executable JUnit correctness tests)

            Select Generate Full Trace Evidence to produce a fresh in-memory trace from the
            current implementations. This screen does not claim or fabricate a live test result.
            """;

    private final CorrectnessEvidenceGenerator generator = new CorrectnessEvidenceGenerator();
    private final BackgroundAction action = new BackgroundAction();
    private final JTextArea output = new JTextArea(RETAINED_EVIDENCE);

    public CorrectnessMenu() {
        super(new BorderLayout(8, 8));
        output.setEditable(false);
        output.setLineWrap(false);

        JButton generate = new JButton("Generate Full Trace Evidence");
        generate.addActionListener(event -> generate(generate));
        JButton locations = new JButton("Show Retained Evidence Locations");
        locations.addActionListener(event -> output.setText(RETAINED_EVIDENCE));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(new JLabel("Correctness traces, arguments, and counterexamples"));
        controls.add(generate);
        controls.add(locations);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    private void generate(JButton control) {
        action.start(
                control,
                "Generating...",
                generator::generate,
                output::setText,
                failure -> UiErrors.show(this, "generate correctness evidence", failure));
    }
}
