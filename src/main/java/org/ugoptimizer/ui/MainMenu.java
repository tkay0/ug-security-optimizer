package org.ugoptimizer.ui;

import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.PriorityService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.frontend.OptimizationService;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Top-level application window for the operational interface and Academic / DSA Lab.
 *
 * <p>Per the project's per-layer restructuring, the frontend as a whole
 * <p>Every service is injected from {@code Main.java} rather than
 * constructed here. Both presentation modes therefore share one backend,
 * database, and runtime state while exposing terminology suited to their
 * respective operator and examiner audiences.
 */
public class MainMenu extends JFrame {

    static final String OPERATIONAL_MODE = "Operational Mode";
    static final String DSA_LAB_MODE = "Academic / DSA Lab";

    public MainMenu(
            LocationService locationService,
            RequestService requestService,
            ResourceService resourceService,
            RouteService routeService,
            WorkflowService workflowService,
            ReportService reportService,
            PriorityService priorityService,
            OptimizationService optimizationService) {
        super("UG Campus Security & Emergency Response Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setContentPane(new ModeContent(
                locationService,
                requestService,
                resourceService,
                routeService,
                workflowService,
                reportService,
                priorityService,
                optimizationService));
    }

    /** Headless-testable CardLayout shell used as the frame's single content pane. */
    static final class ModeContent extends JPanel {

        private final CardLayout cards = new CardLayout();
        private final OperationalModePanel operationalMode;
        private final DsaLabPanel dsaLab;
        private String currentMode;

        ModeContent(
                LocationService locationService,
                RequestService requestService,
                ResourceService resourceService,
                RouteService routeService,
                WorkflowService workflowService,
                ReportService reportService,
                PriorityService priorityService,
                OptimizationService optimizationService) {
            setLayout(cards);
            operationalMode = new OperationalModePanel(
                    locationService,
                    requestService,
                    resourceService,
                    routeService,
                    workflowService,
                    () -> showMode(DSA_LAB_MODE));
            dsaLab = new DsaLabPanel(
                    locationService,
                    requestService,
                    routeService,
                    reportService,
                    priorityService,
                    optimizationService,
                    () -> showMode(OPERATIONAL_MODE));
            add(operationalMode, OPERATIONAL_MODE);
            add(dsaLab, DSA_LAB_MODE);
            showMode(OPERATIONAL_MODE);
        }

        private void showMode(String mode) {
            cards.show(this, mode);
            currentMode = mode;
        }

        String currentMode() {
            return currentMode;
        }

        OperationalModePanel operationalMode() {
            return operationalMode;
        }

        DsaLabPanel dsaLab() {
            return dsaLab;
        }
    }
}
