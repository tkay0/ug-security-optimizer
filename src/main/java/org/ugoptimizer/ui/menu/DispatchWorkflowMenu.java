package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.structures.stack.CustomStack;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

/**
 * Menu for dispatch workflow operations: advancing a request's status,
 * cancelling it, and undoing the most recent change.
 *
 * <p>Status changes go through the shared {@link RequestService} (the same
 * instance {@link RequestResourceMenu} and {@link SearchSortMenu} use), and
 * every change is logged as a real {@link AuditEvent} through
 * {@link WorkflowService}. Undo is still backed by the project's own
 * {@link CustomStack} kept locally here &mdash; that's UI workflow control,
 * not persisted state, so it doesn't belong in a service.
 */
public class DispatchWorkflowMenu extends JPanel {

    private final RequestService requestService;
    private final WorkflowService workflowService;
    private final CustomStack<UndoEntry> undoStack = new CustomStack<>();

    private DataTablePanel<ServiceRequest> requestTable;
    private DataTablePanel<AuditEvent> auditTable;

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

        advanceButton.addActionListener(e -> advanceSelected());
        cancelButton.addActionListener(e -> cancelSelected());
        undoButton.addActionListener(e -> undoLast());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(advanceButton);
        controls.add(cancelButton);
        controls.add(undoButton);

        JPanel requestsPanel = new JPanel(new BorderLayout());
        requestsPanel.setBorder(BorderFactory.createTitledBorder("Requests"));
        requestsPanel.add(controls, BorderLayout.NORTH);
        requestsPanel.add(requestTable, BorderLayout.CENTER);

        JPanel auditPanel = new JPanel(new BorderLayout());
        auditPanel.setBorder(BorderFactory.createTitledBorder("Audit Log"));
        auditPanel.add(auditTable, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestsPanel, auditPanel);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);
    }

    private void advanceSelected() {
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
        applyStatusChange(selected, next, "STATUS_CHANGE");
    }

    private void cancelSelected() {
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
        applyStatusChange(selected, "CANCELLED", "CANCELLATION");
    }

    private void applyStatusChange(ServiceRequest original, String newStatus, String eventType) {
        String oldStatus = original.getStatus();
        requestService.updateStatus(original.getRequestId(), newStatus);
        undoStack.push(new UndoEntry(original.getRequestId(), oldStatus));

        logEvent(eventType, original.getRequestId(),
                "Status changed from " + oldStatus + " to " + newStatus);
        requestTable.setRows(requestService.findAll());
    }

    private void undoLast() {
        if (undoStack.isEmpty()) {
            MessagePrinter.showError(this, "Nothing to undo.");
            return;
        }
        UndoEntry entry = undoStack.pop();
        String revertedFrom = findStatus(entry.requestId());
        if (revertedFrom == null) {
            MessagePrinter.showError(this, "Request " + entry.requestId() + " no longer exists.");
            return;
        }
        requestService.updateStatus(entry.requestId(), entry.previousStatus());

        logEvent("UNDO", entry.requestId(),
                "Reverted status from " + revertedFrom + " to " + entry.previousStatus());
        requestTable.setRows(requestService.findAll());
    }

    private void logEvent(String eventType, int entityId, String details) {
        workflowService.logEvent(eventType, entityId, details);
        auditTable.setRows(workflowService.findAuditLog());
    }

    private String nextStatus(String current) {
        return switch (current) {
            case "PENDING" -> "ASSIGNED";
            case "ASSIGNED" -> "IN_PROGRESS";
            case "IN_PROGRESS" -> "COMPLETED";
            default -> null;
        };
    }

    private String findStatus(int requestId) {
        for (ServiceRequest request : requestService.findAll()) {
            if (request.getRequestId() == requestId) {
                return request.getStatus();
            }
        }
        return null;
    }

    private record UndoEntry(int requestId, String previousStatus) {
    }
}
