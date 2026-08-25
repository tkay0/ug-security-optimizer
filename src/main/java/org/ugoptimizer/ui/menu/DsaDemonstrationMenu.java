package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.ugoptimizer.demo.IndexingDemonstration;
import org.ugoptimizer.demo.SchedulingDemonstration;
import org.ugoptimizer.evidence.CorrectnessEvidenceGenerator;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Examiner-facing demonstrations of scheduling and indexing custom structures. */
public final class DsaDemonstrationMenu extends JPanel {

    private final RequestService requestService;
    private final SchedulingDemonstration scheduling = new SchedulingDemonstration();
    private final IndexingDemonstration indexing = new IndexingDemonstration();
    private final CorrectnessEvidenceGenerator evidence = new CorrectnessEvidenceGenerator();
    private final BackgroundAction action = new BackgroundAction();
    private final JTextField requestId = new JTextField("1", 6);
    private final JTextArea output = new JTextArea();

    public DsaDemonstrationMenu(RequestService requestService) {
        super(new BorderLayout(8, 8));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        output.setEditable(false);
        output.setLineWrap(false);
        output.setText("Select a demonstration above to inspect the custom data structures.");

        JButton schedulingButton = new JButton("Demonstrate Queues & Heap");
        schedulingButton.addActionListener(event -> runScheduling(schedulingButton));
        JButton indexingButton = new JButton("Demonstrate Tree Indexes");
        indexingButton.addActionListener(event -> runIndexing(indexingButton));
        JButton evidenceButton = new JButton("Generate Full Trace Evidence");
        evidenceButton.addActionListener(event -> start(
                evidenceButton, "Generating...", evidence::generate, "generate correctness evidence"));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(schedulingButton);
        controls.add(new JLabel("Request ID:"));
        controls.add(requestId);
        controls.add(indexingButton);
        controls.add(evidenceButton);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    private void runScheduling(JButton control) {
        start(control, "Running...", () -> scheduling.demonstrate(requestSnapshot()),
                "run the scheduling demonstration");
    }

    private void runIndexing(JButton control) {
        int target;
        try {
            target = Integer.parseInt(requestId.getText().trim());
        } catch (NumberFormatException exception) {
            MessagePrinter.showError(this, "Request ID must be a whole number.");
            return;
        }
        int selectedId = target;
        start(control, "Searching...", () -> indexing.demonstrate(requestSnapshot(), selectedId),
                "run the indexing demonstration");
    }

    private ServiceRequest[] requestSnapshot() {
        return requestService.findAll().toArray(new ServiceRequest[0]);
    }

    private void start(
            JButton control, String busyText, java.util.concurrent.Callable<String> task,
            String operation) {
        action.start(control, busyText, task, output::setText,
                failure -> UiErrors.show(this, operation, failure));
    }
}
