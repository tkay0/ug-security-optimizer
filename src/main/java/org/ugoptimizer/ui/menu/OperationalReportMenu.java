package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Operational incident/resource summary and persisted activity history. */
public final class OperationalReportMenu extends JPanel {

    private final RequestService requestService;
    private final ResourceService resourceService;
    private final WorkflowService workflowService;
    private final BackgroundAction reportAction = new BackgroundAction();
    private final JTextArea summary = new JTextArea(12, 60);
    private final DataTablePanel<AuditEvent> activity;

    public OperationalReportMenu(
            RequestService requestService,
            ResourceService resourceService,
            WorkflowService workflowService) {
        super(new BorderLayout(10, 10));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");
        this.workflowService = Objects.requireNonNull(workflowService, "workflowService cannot be null");

        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setText("Choose Refresh Report to load the current operational summary.");
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Operational Summary"));
        summaryPanel.add(new JScrollPane(summary), BorderLayout.CENTER);

        activity = new DataTablePanel<>(List.of(
                new Column<>("Time", event -> UiFormat.dateTime(event.getTimestamp())),
                new Column<>("Activity", event -> UiFormat.humanize(event.getEventType())),
                new Column<>("Subject", event -> UiFormat.humanize(event.getEntityType())
                        + " #" + event.getEntityId()),
                new Column<>("Actor", event -> UiFormat.humanize(event.getActorType())),
                new Column<>("Details", event -> Objects.toString(event.getDetails(), ""))
        ), List.of());
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity / Audit History"));
        activityPanel.add(activity, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summaryPanel, activityPanel);
        split.setResizeWeight(0.42d);
        JButton refresh = new JButton("Refresh Report");
        refresh.addActionListener(event -> refresh(refresh));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.add(refresh);
        add(controls, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private void refresh(JButton control) {
        boolean started = reportAction.start(
                control,
                "Loading Report...",
                () -> new Snapshot(
                        requestService.findAll(),
                        resourceService.findAll(),
                        workflowService.findAuditLog()),
                snapshot -> {
                    summary.setText(formatSummary(snapshot.requests(), snapshot.resources()));
                    List<AuditEvent> events = new ArrayList<>(snapshot.events());
                    Collections.reverse(events);
                    activity.setRows(events);
                    MessagePrinter.showInfo(this, "Operational report refreshed.");
                },
                failure -> UiErrors.show(this, "load the operational report", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "The operational report is already loading.");
        }
    }

    static String formatSummary(List<ServiceRequest> requests, List<Resource> resources) {
        int pending = 0;
        int assigned = 0;
        int inProgress = 0;
        int completed = 0;
        int cancelled = 0;
        int critical = 0;
        for (ServiceRequest request : requests) {
            switch (request.getStatus()) {
                case "PENDING" -> pending++;
                case "ASSIGNED" -> assigned++;
                case "IN_PROGRESS" -> inProgress++;
                case "COMPLETED" -> completed++;
                case "CANCELLED" -> cancelled++;
                default -> throw new IllegalStateException(
                        "Unsupported incident status: " + request.getStatus());
            }
            if (request.getUrgency() == 5
                    && !"COMPLETED".equals(request.getStatus())
                    && !"CANCELLED".equals(request.getStatus())) {
                critical++;
            }
        }

        int available = 0;
        int busy = 0;
        int offDuty = 0;
        int maintenance = 0;
        for (Resource resource : resources) {
            switch (resource.getAvailabilityStatus()) {
                case "AVAILABLE" -> available++;
                case "BUSY" -> busy++;
                case "OFF_DUTY" -> offDuty++;
                case "MAINTENANCE" -> maintenance++;
                default -> throw new IllegalStateException(
                        "Unsupported resource availability: " + resource.getAvailabilityStatus());
            }
        }

        int active = pending + assigned + inProgress;
        return "INCIDENT SUMMARY\n"
                + "Total: " + requests.size() + "\n"
                + "Active: " + active + "\n"
                + "Pending: " + pending + "\n"
                + "Assigned: " + assigned + "\n"
                + "In Progress: " + inProgress + "\n"
                + "Completed: " + completed + "\n"
                + "Cancelled: " + cancelled + "\n"
                + "Critical (active urgency 5): " + critical + "\n\n"
                + "RESOURCE SUMMARY\n"
                + "Total: " + resources.size() + "\n"
                + "Available: " + available + "\n"
                + "Busy: " + busy + "\n"
                + "Off Duty: " + offDuty + "\n"
                + "Maintenance: " + maintenance;
    }

    private record Snapshot(
            List<ServiceRequest> requests,
            List<Resource> resources,
            List<AuditEvent> events) {
    }
}
