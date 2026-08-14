package org.ugoptimizer.app;

import org.ugoptimizer.service.LocationService;
import org.ugoptimizer.service.ReportService;
import org.ugoptimizer.service.RequestService;
import org.ugoptimizer.service.ResourceService;
import org.ugoptimizer.service.RouteService;
import org.ugoptimizer.service.WorkflowService;
import org.ugoptimizer.service.inmemory.InMemoryLocationService;
import org.ugoptimizer.service.inmemory.InMemoryReportService;
import org.ugoptimizer.service.inmemory.InMemoryRequestService;
import org.ugoptimizer.service.inmemory.InMemoryResourceService;
import org.ugoptimizer.service.inmemory.InMemoryRouteService;
import org.ugoptimizer.service.inmemory.InMemoryWorkflowService;
import org.ugoptimizer.ui.MainMenu;

import javax.swing.SwingUtilities;

/**
 * Application entry point for the UG Campus Security &amp; Emergency Response Optimizer.
 */
public class Main {

    public static void main(String[] args) {
        // Swap any InMemoryXService below for a real DAO-backed implementation of
        // the same interface once the database team's work lands -- MainMenu and
        // every ui/menu/* screen depend only on the interfaces, not on these
        // in-memory classes, so no UI code needs to change.
        LocationService locationService = new InMemoryLocationService();
        RequestService requestService = new InMemoryRequestService();
        ResourceService resourceService = new InMemoryResourceService();
        RouteService routeService = new InMemoryRouteService(locationService);
        WorkflowService workflowService = new InMemoryWorkflowService();
        ReportService reportService = new InMemoryReportService();

        SwingUtilities.invokeLater(() -> new MainMenu(
                locationService,
                requestService,
                resourceService,
                routeService,
                workflowService,
                reportService).setVisible(true));
    }
}
