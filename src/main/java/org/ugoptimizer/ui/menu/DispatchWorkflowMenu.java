package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.structures.stack.CustomStack;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Operator-facing status workflow using the canonical request lifecycle and assignment service. */
public class DispatchWorkflowMenu extends JPanel {

    private final RequestService requestService;
    private final WorkflowService workflowService;
    private final List<Location> locations;
    private final CustomStack<UndoEntry> undoStack = new CustomStack<>();
    private final BackgroundAction statusAction = new BackgroundAction();
    private final DataTablePanel<ServiceRequest> requestTable;
    private final JTextArea outcomeArea = new JTextArea(8, 60);
    private final JLabel selectedState = new JLabel(
            "Select an incident to view its current status and next workflow action.");
    private final JButton advanceButton = new JButton("Select an incident");
    private final JButton cancelButton = new JButton("Cancel Incident");
    private final JButton undoButton = new JButton("Undo Last Action");

    public DispatchWorkflowMenu(
            RequestService requestService,
            WorkflowService workflowService,
            LocationService locationService) {
        super(new BorderLayout(10, 10));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        this.workflowService = Objects.requireNonNull(workflowService, "workflowService cannot be null");
        Objects.requireNonNull(locationService, "locationService cannot be null");
        locations = List.copyOf(locationService.findAllLocations());

        requestTable = new DataTablePanel<>(List.of(
                new Column<>("Incident ID", request -> "#" + request.getRequestId()),
                new Column<>("Type", request -> UiFormat.humanize(request.getCategory())),
                new Column<>("Incident Location", request ->
                        UiFormat.locationName(locations, request.getSourceLocationId())),
                new Column<>("Urgency", request -> UiFormat.urgencyOption(request.getUrgency()).label()),
                new Column<>("Current Status", request -> UiFormat.humanize(request.getStatus())),
                new Column<>("Required Response", request ->
                        UiFormat.humanize(request.getRequiredResourceType()))
        ), requestService.findAll());

        advanceButton.addActionListener(event -> advanceSelected(advanceButton));
        cancelButton.addActionListener(event -> cancelSelected(cancelButton));
        undoButton.addActionListener(event -> undoLast(undoButton));
        JButton refresh = new JButton("Refresh Incidents");
        refresh.addActionListener(event -> refresh(refresh));
        requestTable.getTable().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionPresentation();
            }
        });

        advanceButton.setEnabled(false);
        cancelButton.setEnabled(false);
        undoButton.setEnabled(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(refresh);
        controls.add(advanceButton);
        controls.add(cancelButton);
        controls.add(undoButton);

        JPanel selection = new JPanel(new BorderLayout(6, 6));
        selection.setBorder(BorderFactory.createTitledBorder("Selected Incident Workflow"));
        selection.add(selectedState, BorderLayout.NORTH);
        selection.add(controls, BorderLayout.CENTER);

        JPanel incidents = new JPanel(new BorderLayout(8, 8));
        incidents.setBorder(BorderFactory.createTitledBorder("Incidents Ready for Workflow Actions"));
        incidents.add(selection, BorderLayout.NORTH);
        incidents.add(requestTable, BorderLayout.CENTER);

        outcomeArea.setEditable(false);
        outcomeArea.setLineWrap(true);
        outcomeArea.setWrapStyleWord(true);
        outcomeArea.setText("Select an incident and perform a dispatch action to view the result.");
        JScrollPane outcome = new JScrollPane(outcomeArea);
        outcome.setBorder(BorderFactory.createTitledBorder("Latest Dispatch Outcome"));

        add(incidents, BorderLayout.CENTER);
        add(outcome, BorderLayout.SOUTH);
    }

    private void updateActionPresentation() {
        ServiceRequest selected = requestTable.getSelectedRow();
        if (selected == null) {
            selectedState.setText(
                    "Select an incident to view its current status and next workflow action.");
            advanceButton.setText("Select an incident");
            advanceButton.setEnabled(false);
            cancelButton.setEnabled(false);
            return;
        }
        String next = nextStatus(selected.getStatus());
        selectedState.setText("Incident #" + selected.getRequestId()
                + " | Current Status: " + UiFormat.humanize(selected.getStatus())
                + " | Next Step: " + (next == null ? "No further workflow step"
                        : UiFormat.humanize(next)));
        advanceButton.setText(next == null
                ? "Workflow Complete"
                : "Move to " + UiFormat.humanize(next));
        advanceButton.setEnabled(next != null && !statusAction.isRunning());
        cancelButton.setEnabled(next != null && !statusAction.isRunning());
    }

    private void advanceSelected(JButton control) {
        ServiceRequest selected = requestTable.getSelectedRow();
        if (selected == null) {
            return;
        }
        String next = nextStatus(selected.getStatus());
        if (next == null) {
            MessagePrinter.showError(this, "This incident has no further workflow action.");
            return;
        }
        applyStatusChange(selected, next, control);
    }

    private void cancelSelected(JButton control) {
        ServiceRequest selected = requestTable.getSelectedRow();
        if (selected == null || nextStatus(selected.getStatus()) == null) {
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Cancel incident #" + selected.getRequestId() + "?",
                "Confirm Incident Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirmation == JOptionPane.YES_OPTION) {
            applyStatusChange(selected, "CANCELLED", control);
        }
    }

    private void applyStatusChange(ServiceRequest original, String newStatus, JButton control) {
        String oldStatus = original.getStatus();
        boolean started = statusAction.start(
                control,
                "Updating...",
                () -> {
                    ServiceRequest updated =
                            requestService.updateStatus(original.getRequestId(), newStatus);
                    Optional<Assignment> assignment = "ASSIGNED".equals(updated.getStatus())
                            ? workflowService.findActiveAssignment(original.getRequestId())
                            : Optional.empty();
                    return new DispatchUpdate(updated, assignment, requestService.findAll());
                },
                update -> {
                    undoStack.push(new UndoEntry(original.getRequestId(), oldStatus));
                    requestTable.setRows(update.requests());
                    requestTable.getTable().clearSelection();
                    undoButton.setEnabled(true);
                    outcomeArea.setText(formatOutcome(
                            original, update.updated(), update.assignment(), locations));
                    updateActionPresentation();
                },
                failure -> {
                    updateActionPresentation();
                    UiErrors.show(this, "update the dispatch status", failure);
                });
        if (!started) {
            MessagePrinter.showInfo(this, "A dispatch operation is already in progress.");
        }
    }

    private void undoLast(JButton control) {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoEntry entry = undoStack.peek();
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Undo the last dispatch action for incident #" + entry.requestId() + "?",
                "Confirm Undo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        boolean started = statusAction.start(
                control,
                "Undoing...",
                () -> {
                    ServiceRequest current = findRequest(entry.requestId(), requestService.findAll());
                    if (current == null) {
                        throw new IllegalStateException(
                                "Incident #" + entry.requestId() + " no longer exists");
                    }
                    ServiceRequest updated =
                            requestService.updateStatus(entry.requestId(), entry.previousStatus());
                    return new DispatchUpdate(updated, Optional.empty(), requestService.findAll());
                },
                update -> {
                    undoStack.pop();
                    requestTable.setRows(update.requests());
                    requestTable.getTable().clearSelection();
                    undoButton.setEnabled(!undoStack.isEmpty());
                    outcomeArea.setText("Incident #" + update.updated().getRequestId()
                            + " restored to " + UiFormat.humanize(update.updated().getStatus()) + ".");
                    updateActionPresentation();
                },
                failure -> UiErrors.show(this, "undo the last dispatch action", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A dispatch operation is already in progress.");
        }
    }

    private void refresh(JButton control) {
        boolean started = statusAction.start(
                control,
                "Refreshing...",
                requestService::findAll,
                requests -> {
                    requestTable.setRows(requests);
                    requestTable.getTable().clearSelection();
                    updateActionPresentation();
                },
                failure -> UiErrors.show(this, "refresh dispatch incidents", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A dispatch operation is already in progress.");
        }
    }

    static String nextStatus(String current) {
        return switch (current) {
            case "PENDING" -> "ASSIGNED";
            case "ASSIGNED" -> "IN_PROGRESS";
            case "IN_PROGRESS" -> "COMPLETED";
            default -> null;
        };
    }

    static String formatOutcome(
            ServiceRequest original,
            ServiceRequest updated,
            Optional<Assignment> assignment,
            List<Location> locations) {
        String transition = "Incident #" + updated.getRequestId() + " moved from "
                + UiFormat.humanize(original.getStatus()) + " to "
                + UiFormat.humanize(updated.getStatus()) + ".";
        if (assignment.isEmpty()) {
            return transition;
        }
        Assignment value = assignment.orElseThrow();
        return transition + "\n"
                + "Assignment #" + value.getAssignmentId() + " was created.\n"
                + "Selected resource: #" + value.getResourceId() + "\n"
                + "Required response: " + UiFormat.humanize(updated.getRequiredResourceType()) + "\n"
                + "Incident location: "
                + UiFormat.locationName(locations, updated.getSourceLocationId()) + "\n"
                + "Response destination: "
                + UiFormat.locationName(locations, updated.getDestinationLocationId()) + "\n"
                + "Estimated response time: "
                + String.format("%.2f minutes", value.getEstimatedResponseTimeMinutes()) + "\n"
                + "Assignment status: " + UiFormat.humanize(value.getStatus());
    }

    private static ServiceRequest findRequest(int requestId, List<ServiceRequest> requests) {
        for (ServiceRequest request : requests) {
            if (request.getRequestId() == requestId) {
                return request;
            }
        }
        return null;
    }

    private record UndoEntry(int requestId, String previousStatus) {
    }

    private record DispatchUpdate(
            ServiceRequest updated,
            Optional<Assignment> assignment,
            List<ServiceRequest> requests) {
    }
}
