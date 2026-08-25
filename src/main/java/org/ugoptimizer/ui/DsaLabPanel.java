package org.ugoptimizer.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.OptimizationService;
import org.ugoptimizer.frontend.PriorityService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.ui.menu.CorrectnessMenu;
import org.ugoptimizer.ui.menu.DsaDemonstrationMenu;
import org.ugoptimizer.ui.menu.EfficiencyLabMenu;
import org.ugoptimizer.ui.menu.OptimizationMenu;
import org.ugoptimizer.ui.menu.PriorityQueueMenu;
import org.ugoptimizer.ui.menu.ReportMenu;
import org.ugoptimizer.ui.menu.RoutingMenu;
import org.ugoptimizer.ui.menu.SearchSortMenu;

/** Examiner-facing workspace for the assessed structures, algorithms, and evidence. */
public final class DsaLabPanel extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane();

    public DsaLabPanel(
            LocationService locationService,
            RequestService requestService,
            RouteService routeService,
            ReportService reportService,
            PriorityService priorityService,
            OptimizationService optimizationService,
            Runnable backToOperations) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(backToOperations, "backToOperations cannot be null");

        add(header(backToOperations), BorderLayout.NORTH);
        tabs.addTab("Structures", structuresPanel(requestService, priorityService));
        tabs.addTab("Search & Sort", searchSortPanel(
                requestService, reportService, routeService, locationService));
        tabs.addTab("Graph Algorithms", graphPanel(
                reportService, routeService, locationService));
        tabs.addTab("Optimization", new OptimizationMenu(requestService, optimizationService));
        tabs.addTab("Correctness", new CorrectnessMenu());
        tabs.addTab("Efficiency Lab", efficiencyPanel(
                reportService, routeService, locationService));
        add(tabs, BorderLayout.CENTER);
    }

    private static JPanel header(Runnable backToOperations) {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        JLabel title = new JLabel("Academic / DSA Lab");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20.0f));
        JLabel subtitle = new JLabel("Structures, algorithms, correctness, and efficiency evidence");
        JPanel titles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        titles.add(title);
        titles.add(subtitle);

        JButton backButton = new JButton("\u2190 Back to Operations");
        backButton.addActionListener(event -> backToOperations.run());
        header.add(titles, BorderLayout.WEST);
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private static JPanel structuresPanel(
            RequestService requestService, PriorityService priorityService) {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane structureTabs = new JTabbedPane();
        structureTabs.addTab("Queues & Tree Indexes", new DsaDemonstrationMenu(requestService));
        structureTabs.addTab("Priority Queue", new PriorityQueueMenu(priorityService));
        panel.add(structureTabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel searchSortPanel(
            RequestService requestService,
            ReportService reportService,
            RouteService routeService,
            LocationService locationService) {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane searchTabs = new JTabbedPane();
        searchTabs.addTab("Explore Search & Sort", new SearchSortMenu(requestService));
        searchTabs.addTab("Run & Record Sorts", ReportMenu.sortingRuns(
                reportService, routeService, locationService));
        panel.add(searchTabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel graphPanel(
            ReportService reportService,
            RouteService routeService,
            LocationService locationService) {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane graphTabs = new JTabbedPane();
        graphTabs.addTab("Explore Traversals & Routes", new RoutingMenu(routeService, locationService));
        graphTabs.addTab("Run & Record Graph Algorithms", ReportMenu.graphRuns(
                reportService, routeService, locationService));
        panel.add(graphTabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel efficiencyPanel(
            ReportService reportService,
            RouteService routeService,
            LocationService locationService) {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane efficiencyTabs = new JTabbedPane();
        efficiencyTabs.addTab("Benchmark Lab", new EfficiencyLabMenu());
        efficiencyTabs.addTab("Recorded Runs", ReportMenu.recordedRuns(
                reportService, routeService, locationService));
        panel.add(efficiencyTabs, BorderLayout.CENTER);
        return panel;
    }

    JTabbedPane tabs() {
        return tabs;
    }
}
