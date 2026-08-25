package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;

/** Compact operational dashboard calculated from current request and resource data. */
public final class DashboardMenu extends JPanel {

    private static final int RECENT_LIMIT = 8;

    private final RequestService requestService;
    private final ResourceService resourceService;
    private final JLabel totalIncidents = metricValue("dashboard.totalIncidents");
    private final JLabel activeIncidents = metricValue("dashboard.activeIncidents");
    private final JLabel criticalIncidents = metricValue("dashboard.criticalIncidents");
    private final JLabel awaitingAssignment = metricValue("dashboard.awaitingAssignment");
    private final JLabel availableResources = metricValue("dashboard.availableResources");
    private final DataTablePanel<ServiceRequest> recentIncidents;
    private final JLabel recentState = new JLabel();

    public DashboardMenu(
            RequestService requestService,
            ResourceService resourceService,
            Runnable viewIncidents,
            Runnable dispatchResource,
            Runnable findRoute,
            Runnable viewResources) {
        super(new BorderLayout(12, 12));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");

        JPanel metrics = new JPanel(new GridLayout(1, 5, 10, 10));
        metrics.setPreferredSize(new Dimension(0, 140));
        metrics.add(metricCard("Total Incidents", totalIncidents));
        metrics.add(metricCard("Active Incidents", activeIncidents));
        metrics.add(metricCard("Critical Incidents", criticalIncidents));
        metrics.add(metricCard("Awaiting Assignment", awaitingAssignment));
        metrics.add(metricCard("Available Resources", availableResources));

        recentIncidents = new DataTablePanel<>(List.of(
                new Column<>("Incident", request -> "#" + request.getRequestId()),
                new Column<>("Type", request -> UiFormat.humanize(request.getCategory())),
                new Column<>("Urgency", request -> UiFormat.urgencyOption(request.getUrgency()).label()),
                new Column<>("Status", request -> UiFormat.humanize(request.getStatus())),
                new Column<>("Submitted", request -> UiFormat.dateTime(request.getTimeSubmitted()))
        ), List.of());
        JPanel recentPanel = new JPanel(new BorderLayout(6, 6));
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent / Priority Incidents"));
        recentPanel.add(recentState, BorderLayout.NORTH);
        recentPanel.add(recentIncidents, BorderLayout.CENTER);

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        quickActions.setBorder(BorderFactory.createTitledBorder("Quick Actions"));
        quickActions.add(actionButton("View Incidents", viewIncidents));
        quickActions.add(actionButton("Dispatch Resource", dispatchResource));
        quickActions.add(actionButton("Find Route", findRoute));
        quickActions.add(actionButton("View Resources", viewResources));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(new JLabel("Live operational overview from the current application database."),
                BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(recentPanel, BorderLayout.CENTER);
        add(quickActions, BorderLayout.SOUTH);
        refresh();
    }

    /** Refreshes every metric and recent row from the injected services. */
    public void refresh() {
        List<ServiceRequest> requests = requestService.findAll();
        List<Resource> resources = resourceService.findAll();
        int active = 0;
        int critical = 0;
        int pending = 0;
        for (ServiceRequest request : requests) {
            boolean isActive = !"COMPLETED".equals(request.getStatus())
                    && !"CANCELLED".equals(request.getStatus());
            if (isActive) {
                active++;
                if (request.getUrgency() == 5) {
                    critical++;
                }
            }
            if ("PENDING".equals(request.getStatus())) {
                pending++;
            }
        }
        int available = 0;
        for (Resource resource : resources) {
            if ("AVAILABLE".equals(resource.getAvailabilityStatus())) {
                available++;
            }
        }

        totalIncidents.setText(String.valueOf(requests.size()));
        activeIncidents.setText(String.valueOf(active));
        criticalIncidents.setText(String.valueOf(critical));
        awaitingAssignment.setText(String.valueOf(pending));
        availableResources.setText(String.valueOf(available));

        List<ServiceRequest> recent = new ArrayList<>(requests);
        recent.sort(Comparator
                .comparingInt(ServiceRequest::getUrgency).reversed()
                .thenComparing(ServiceRequest::getTimeSubmitted, Comparator.reverseOrder()));
        if (recent.size() > RECENT_LIMIT) {
            recent = new ArrayList<>(recent.subList(0, RECENT_LIMIT));
        }
        recentIncidents.setRows(recent);
        recentState.setText(recent.isEmpty()
                ? "No recent incidents to display."
                : "Highest urgency first; ties show the most recently submitted incident first.");
    }

    private static JPanel metricCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(12, 8, 12, 8)));
        card.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricValue(String name) {
        JLabel value = new JLabel("0", SwingConstants.CENTER);
        value.setName(name);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 28.0f));
        return value;
    }

    private static JButton actionButton(String text, Runnable action) {
        Objects.requireNonNull(action, text + " action cannot be null");
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }
}
