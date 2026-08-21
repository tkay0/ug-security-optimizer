package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.structures.stack.CustomStack;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Menu for dispatch workflow operations: advancing a request's status,
 * cancelling it, and undoing the most recent change.
 *
 * <p>Status changes go through the shared {@link RequestService} (the same
 * instance {@link RequestResourceMenu} and {@link SearchSortMenu} use).
 * {@code requestService.updateStatus(...)} already records its own
 * {@link AuditEvent}(s) as part of that call on a real backend (e.g. a
 * PENDING-to-ASSIGNED transition logs both a REQUEST_ASSIGNED and a
 * RESOURCE_ASSIGNED event) -- this menu only re-reads
 * {@link WorkflowService#findAuditLog()} afterward to refresh what it
 * displays, it does not also write its own duplicate entry. Undo is still
 * backed by the project's own {@link CustomStack} kept locally here
 * &mdash; that's UI workflow control, not persisted state, so it doesn't
 * belong in a service.
 */
public class DispatchWorkflowMenu extends JPanel {

    private final RequestService requestService;
    private final WorkflowService workflowService;
    private final CustomStack<UndoEntry> undoStack = new CustomStack<>();
    private final BackgroundAction statusAction = new BackgroundAction();

    private DataTablePanel<ServiceRequest> requestTable;
    private DataTablePanel<AuditEvent> auditTable;
    private final JTextArea outcomeArea;

    public DispatchWorkflowMenu(RequestService requestService, WorkflowService workflowService) {
        super(new BorderLayout(8, 8));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        this.workflowService = Objects.requireNonNull(workflowService, "workflowService cannot be null");

        requestTable = new DataTablePanel<>(List.of(
                new Column<>("ID", r -> String.valueOf(r.getRequestId())),
                new Column<>("Category", ServiceRequest::getCategory),
                new Column<>("Status", ServiceRequest::getStatus)
        ), requestService.findAll());

        auditTable = new DataTablePanel<>(List.of(
                new Column<>("Event ID", a -> String.valueOf(a.getEventId())),
                new Column<>("Type", AuditEvent::getEventType),
                new Column<>("Entity ID", a -> String.valueOf(a.getEntityId())),
                new Column<>("Details", AuditEvent::getDetails)
        ), workflowService.findAuditLog());

        JButton advanceButton = new JButton("Advance Status");
        JButton cancelButton = new JButton("Cancel Request");
        JButton undoButton = new JButton("Undo Last Action");

        advanceButton.addActionListener(e -> advanceSelected(advanceButton));
        cancelButton.addActionListener(e -> cancelSelected(cancelButton));
        undoButton.addActionListener(e -> undoLast(undoButton));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(advanceButton);
        controls.add(cancelButton);
        controls.add(undoButton);

        JPanel requestsPanel = new JPanel(new BorderLayout());
        requestsPanel.setBorder(BorderFactory.createTitledBorder("Requests"));
        requestsPanel.add(controls, BorderLayout.NORTH);
        requestsPanel.add(requestTable, BorderLayout.CENTER);

        outcomeArea = new JTextArea(7, 60);
        outcomeArea.setEditable(false);
        outcomeArea.setLineWrap(true);
        outcomeArea.setWrapStyleWord(true);
        JScrollPane outcomeScroll = new JScrollPane(outcomeArea);
        outcomeScroll.setBorder(BorderFactory.createTitledBorder("Latest dispatch outcome"));
        requestsPanel.add(outcomeScroll, BorderLayout.SOUTH);

        JPanel auditPanel = new JPanel(new BorderLayout());
        auditPanel.setBorder(BorderFactory.createTitledBorder("Audit Log"));
        auditPanel.add(auditTable, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestsPanel, auditPanel);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);
    }

    private void advanceSelected(JButton control) {
        ServiceRequest selected = requestTable.getSelectedRow();
        if (selected == null) {
            MessagePrinter.showError(this, "Select a request first.");
            return;
        }
        String next = nextStatus(selected.getStatus());
        if (next == null) {
            MessagePrinter.showError(this, "Request " + selected.getRequestId()
                    + " is already " + selected.getStatus() + " and cannot advance further.");
            return;
        }
        applyStatusChange(selected, next, control);
    }

    private void cancelSelected(JButton control) {
        ServiceRequest selected = requestTable.getSelectedRow();
        if (selected == null) {
            MessagePrinter.showError(this, "Select a request first.");
            return;
        }
        if ("COMPLETED".equals(selected.getStatus()) || "CANCELLED".equals(selected.getStatus())) {
            MessagePrinter.showError(this, "Request " + selected.getRequestId()
                    + " is already " + selected.getStatus() + ".");
            return;
        }
        applyStatusChange(selected, "CANCELLED", control);
    }

    private void applyStatusChange(
            ServiceRequest original, String newStatus, JButton control) {
        String oldStatus = original.getStatus();
        boolean started = statusAction.start(
                control,
                "Working...",
                () -> {
                    ServiceRequest updated =
                            requestService.updateStatus(original.getRequestId(), newStatus);
                    Optional<Assignment> assignment = "ASSIGNED".equals(newStatus)
                            ? workflowService.findActiveAssignment(original.getRequestId())
                            : Optional.empty();
                    return new DispatchUpdate(
                            updated,
                            assignment,
                            requestService.findAll(),
                            workflowService.findAuditLog());
                },
                update -> {
                    undoStack.push(new UndoEntry(original.getRequestId(), oldStatus));
                    requestTable.setRows(update.requests());
                    auditTable.setRows(update.auditEvents());
                    outcomeArea.setText(formatOutcome(original, update.updated(), update.assignment()));
                },
                failure -> UiErrors.show(this, "update the dispatch status", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A dispatch operation is already in progress.");
        }
    }

    private void undoLast(JButton control) {
        if (undoStack.isEmpty()) {
            MessagePrinter.showError(this, "Nothing to undo.");
            return;
        }
        UndoEntry entry = undoStack.peek();
        boolean started = statusAction.start(
                control,
                "Undoing...",
                () -> {
                    ServiceRequest current = findRequest(entry.requestId(), requestService.findAll());
                    if (current == null) {
                        throw new IllegalStateException(
                                "Request " + entry.requestId() + " no longer exists");
                    }
                    ServiceRequest updated =
                            requestService.updateStatus(entry.requestId(), entry.previousStatus());
                    return new DispatchUpdate(
                            updated,
                            Optional.empty(),
                            requestService.findAll(),
                            workflowService.findAuditLog());
                },
                update -> {
                    undoStack.pop();
                    requestTable.setRows(update.requests());
                    auditTable.setRows(update.auditEvents());
                    outcomeArea.setText("Request " + update.updated().getRequestId()
                            + " restored to " + update.updated().getStatus() + ".");
                },
                failure -> UiErrors.show(this, "undo the last dispatch change", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A dispatch operation is already in progress.");
        }
    }

    private String nextStatus(String current) {
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
            Optional<Assignment> assignment) {
        if (assignment.isEmpty()) {
            return "Request " + updated.getRequestId() + " changed from "
                    + original.getStatus() + " to " + updated.getStatus() + ".";
        }
        Assignment value = assignment.orElseThrow();
        return "Assignment " + value.getAssignmentId() + " persisted\n"
                + "Request: " + updated.getRequestId() + " (urgency "
                + updated.getUrgency() + ", status " + updated.getStatus() + ")\n"
                + "Required resource type: " + updated.getRequiredResourceType() + "\n"
                + "Selected resource: " + value.getResourceId() + "\n"
                + "Request locations: " + updated.getSourceLocationId()
                + " -> " + updated.getDestinationLocationId() + "\n"
                + "Estimated response time: "
                + String.format("%.2f minutes", value.getEstimatedResponseTimeMinutes()) + "\n"
                + "Assignment status: " + value.getStatus();
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
            List<ServiceRequest> requests,
            List<AuditEvent> auditEvents) {
    }
}
