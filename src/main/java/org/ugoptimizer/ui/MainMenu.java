package org.ugoptimizer.ui;

import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.PriorityService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.frontend.OptimizationService;
import org.ugoptimizer.ui.menu.DispatchWorkflowMenu;
import org.ugoptimizer.ui.menu.DsaDemonstrationMenu;
import org.ugoptimizer.ui.menu.EfficiencyLabMenu;
import org.ugoptimizer.ui.menu.LocationRoadMenu;
import org.ugoptimizer.ui.menu.OptimizationMenu;
import org.ugoptimizer.ui.menu.PriorityQueueMenu;
import org.ugoptimizer.ui.menu.ReportMenu;
import org.ugoptimizer.ui.menu.RequestResourceMenu;
import org.ugoptimizer.ui.menu.RoutingMenu;
import org.ugoptimizer.ui.menu.SearchSortMenu;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/**
 * Top-level application window. Hosts every screen as a tab.
 *
 * <p>Per the project's per-layer restructuring, the frontend as a whole
 * (every screen below, not just requests/resources/search) is owned by one
 * team, so all tabs are wired up here rather than split across teams.
 *
 * <p>Every service is injected from {@code Main.java} rather than
 * constructed here, so swapping an in-memory service for a real DAO-backed
 * one only requires a change at that one call site.
 */
public class MainMenu extends JFrame {

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
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Locations & Roads", new LocationRoadMenu(locationService));
        tabs.addTab("Routing", new RoutingMenu(routeService, locationService));
        tabs.addTab("Requests & Resources", new RequestResourceMenu(requestService, resourceService));
        tabs.addTab("Search & Sort", new SearchSortMenu(requestService));
        tabs.addTab("Dispatch Workflow", new DispatchWorkflowMenu(requestService, workflowService));
        tabs.addTab("Priority Queue", new PriorityQueueMenu(priorityService));
        tabs.addTab("Optimization", new OptimizationMenu(requestService, optimizationService));
        tabs.addTab("DSA Demonstrations", new DsaDemonstrationMenu(requestService));
        tabs.addTab("Efficiency Lab", new EfficiencyLabMenu());
        tabs.addTab("Reports", new ReportMenu(reportService, routeService, locationService));

        setContentPane(tabs);
    }
}
