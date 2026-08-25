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
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.ui.menu.DashboardMenu;
import org.ugoptimizer.ui.menu.DispatchWorkflowMenu;
import org.ugoptimizer.ui.menu.IncidentMenu;
import org.ugoptimizer.ui.menu.LocationRoadMenu;
import org.ugoptimizer.ui.menu.OperationalReportMenu;
import org.ugoptimizer.ui.menu.ResourceMenu;
import org.ugoptimizer.ui.menu.RoutingMenu;

/** Operator-facing navigation over the shared application services and runtime state. */
public final class OperationalModePanel extends JPanel {

    private static final String DASHBOARD = "Dashboard";
    private static final String INCIDENTS = "Incidents";
    private static final String DISPATCH = "Dispatch";
    private static final String ROUTES = "Routes";
    private static final String RESOURCES = "Resources";
    private static final String REPORTS = "Reports";

    private final JTabbedPane tabs = new JTabbedPane();

    public OperationalModePanel(
            LocationService locationService,
            RequestService requestService,
            ResourceService resourceService,
            RouteService routeService,
            WorkflowService workflowService,
            Runnable openDsaLab) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(openDsaLab, "openDsaLab cannot be null");

        add(header(openDsaLab), BorderLayout.NORTH);

        DashboardMenu dashboard = new DashboardMenu(
                requestService,
                resourceService,
                () -> select(INCIDENTS),
                () -> select(DISPATCH),
                () -> select(ROUTES),
                () -> select(RESOURCES));
        tabs.addTab(DASHBOARD, dashboard);
        tabs.addTab(INCIDENTS, new IncidentMenu(
                requestService, resourceService, locationService));
        tabs.addTab(DISPATCH, new DispatchWorkflowMenu(
                requestService, workflowService, locationService));
        tabs.addTab(ROUTES, routesPanel(routeService, locationService));
        tabs.addTab(RESOURCES, new ResourceMenu(resourceService, locationService));
        tabs.addTab(REPORTS, new OperationalReportMenu(
                requestService, resourceService, workflowService));
        tabs.addChangeListener(event -> {
            if (DASHBOARD.equals(tabs.getTitleAt(tabs.getSelectedIndex()))) {
                dashboard.refresh();
            }
        });
        add(tabs, BorderLayout.CENTER);
    }

    private static JPanel header(Runnable openDsaLab) {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        JLabel title = new JLabel("Operational Mode");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20.0f));
        JLabel subtitle = new JLabel("UG campus security and emergency-response operations");
        JPanel titles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        titles.add(title);
        titles.add(subtitle);

        JButton dsaLabButton = new JButton("DSA Lab");
        dsaLabButton.addActionListener(event -> openDsaLab.run());
        header.add(titles, BorderLayout.WEST);
        header.add(dsaLabButton, BorderLayout.EAST);
        return header;
    }

    private static JPanel routesPanel(RouteService routeService, LocationService locationService) {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane routeTabs = new JTabbedPane();
        routeTabs.addTab("Find Route", RoutingMenu.operational(routeService, locationService));
        routeTabs.addTab("Campus Network", new LocationRoadMenu(locationService));
        panel.add(routeTabs, BorderLayout.CENTER);
        return panel;
    }

    private void select(String title) {
        for (int index = 0; index < tabs.getTabCount(); index++) {
            if (title.equals(tabs.getTitleAt(index))) {
                tabs.setSelectedIndex(index);
                return;
            }
        }
        throw new IllegalStateException("Unknown operational section: " + title);
    }

    JTabbedPane tabs() {
        return tabs;
    }
}
