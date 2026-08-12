package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.AppContext.AssignmentRecommendation;
import org.ugoptimizer.gui.components.EmptyPanel;
import org.ugoptimizer.gui.components.StatusPill;
import org.ugoptimizer.gui.components.UrgencyBadge;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Modal detail view for one service request. Shows every canonical field,
 * the greedy resource recommendation for the request, workflow status
 * actions, and the request's persisted audit history.
 */
public final class IncidentDetailDialog extends JDialog {

    private final AppContext appContext;
    private final Runnable onChanged;
    private final JPanel recommendationArea = new JPanel();
    private final JPanel historyList = new JPanel();
    private final JPanel actionButtons = new JPanel();

    public IncidentDetailDialog(
            JComponent owner, AppContext appContext, ServiceRequest request) {
        this(owner, appContext, request, null);
    }

    public IncidentDetailDialog(
            JComponent owner,
            AppContext appContext,
            ServiceRequest request,
            Runnable onChanged) {
        super();
        this.appContext = appContext;
        this.onChanged = onChanged;
        setTitle("Incident #" + request.getRequestId());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(request), BorderLayout.NORTH);
        root.add(buildBody(request), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
        loadHistory(request);
    }

    private JPanel buildHeader(ServiceRequest request) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Incident #" + request.getRequestId());
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel category = new JLabel(UiFormatters.humanize(request.getCategory()));
        category.setFont(GuiTheme.FONT_SECTION);
        category.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel left = new JPanel(new BorderLayout(0, 2));
        left.setOpaque(false);
        left.add(title, BorderLayout.NORTH);
        left.add(category, BorderLayout.SOUTH);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badges.setOpaque(false);
        badges.add(new UrgencyBadge(request.getUrgency()));
        badges.add(StatusPill.forRequestStatus(request.getStatus()));

        header.add(left, BorderLayout.WEST);
        header.add(badges, BorderLayout.EAST);
        return header;
    }

    private JComponent buildBody(ServiceRequest request) {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(GuiTheme.PANEL_BACKGROUND);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(3, 0, 3, 18);
        c.gridx = 0;
        c.gridy = 0;

        field(details, c, "Source", appContext.locationName(request.getSourceLocationId()));
        field(details, c, "Destination", appContext.locationName(request.getDestinationLocationId()));
        field(details, c, "Submitted", UiFormatters.formatInstant(request.getTimeSubmitted()));
        field(details, c, "Deadline", UiFormatters.formatInstant(request.getDeadline()));
        field(details, c, "Required resource",
                UiFormatters.humanize(request.getRequiredResourceType()));
        field(details, c, "Status", UiFormatters.humanize(request.getStatus()));

        c.gridx = 1;
        c.gridy = 0;
        field(details, c, "Priority queue position",
                requestQueuePosition(request));
        field(details, c, "Urgency",
                request.getUrgency() + " - " + UiFormatters.urgencyLabel(request.getUrgency()));
        field(details, c, "Category", UiFormatters.humanize(request.getCategory()));
        field(details, c, "Request ID", "#" + request.getRequestId());
        field(details, c, "Actor", "DISPATCH_OPERATOR");
        field(details, c, "Description", descriptionText(request));

        body.add(details, BorderLayout.NORTH);
        body.add(buildActions(request), BorderLayout.CENTER);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(null);
        return bodyScroll;
    }

    private String requestQueuePosition(ServiceRequest request) {
        ServiceRequest[] ordered;
        try {
            ordered = org.ugoptimizer.gui.util.ResponseQueueBuilder.orderedOpenRequests(
                    appContext.loadRequests());
        } catch (Exception failure) {
            return "-";
        }
        for (int index = 0; index < ordered.length; index++) {
            if (ordered[index].getRequestId() == request.getRequestId()) {
                return "#" + (index + 1) + " of " + ordered.length;
            }
        }
        return "Not queued";
    }

    private void field(
            JPanel panel, GridBagConstraints c, String labelText, String valueText) {
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(GuiTheme.FONT_SMALL);
        label.setForeground(GuiTheme.TEXT_MUTED);

        JLabel value = new JLabel(valueText == null ? "-" : valueText);
        value.setFont(GuiTheme.FONT_BODY_BOLD);
        value.setForeground(GuiTheme.TEXT_PRIMARY);
        value.setPreferredSize(new Dimension(230, 20));

        panel.add(label, c);
        c.gridy++;
        panel.add(value, c);
        c.gridy++;
    }

    private String descriptionText(ServiceRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return "No description recorded.";
        }
        return request.getDescription();
    }

    private JPanel buildActions(ServiceRequest request) {
        JPanel actions = new JPanel(new BorderLayout(0, 8));
        actions.setOpaque(false);

        JLabel section = new JLabel("DISPATCH ACTIONS");
        section.setFont(GuiTheme.FONT_SMALL);
        section.setForeground(GuiTheme.TEXT_MUTED);
        actions.add(section, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        JButton recommend = new JButton("Recommend resource");
        recommend.addActionListener(event -> recommend(request));
        row.add(recommend);

        boolean open = isOpen(request);
        boolean assigned = "ASSIGNED".equals(request.getStatus());
        boolean inProgress = "IN_PROGRESS".equals(request.getStatus());

        JButton startWork = new JButton("Start work");
        startWork.setEnabled(assigned);
        startWork.addActionListener(event -> changeStatus(request, "IN_PROGRESS"));
        row.add(startWork);

        JButton complete = new JButton("Mark completed");
        complete.setEnabled(assigned || inProgress);
        complete.addActionListener(event -> changeStatus(request, "COMPLETED"));
        row.add(complete);

        JButton cancel = new JButton("Cancel incident");
        cancel.setEnabled(open);
        cancel.addActionListener(event -> changeStatus(request, "CANCELLED"));
        row.add(cancel);

        actionButtons.removeAll();
        actionButtons.setLayout(new BorderLayout());
        actionButtons.setOpaque(false);
        actionButtons.add(row, BorderLayout.WEST);

        actions.add(actionButtons, BorderLayout.CENTER);
        actions.add(recommendationArea, BorderLayout.SOUTH);
        return actions;
    }

    private void recommend(ServiceRequest request) {
        GuiWork.run(
                this,
                () -> appContext.recommendAssignment(request),
                recommendation -> renderRecommendation(request, recommendation),
                (error, anchor) -> {
                    recommendationArea.removeAll();
                    recommendationArea.add(EmptyPanel.error(
                            "Recommendation failed. " + error.getMessage()));
                    recommendationArea.revalidate();
                    recommendationArea.repaint();
                });
    }

    private void renderRecommendation(ServiceRequest request, AssignmentRecommendation recommendation) {
        recommendationArea.removeAll();
        if (!recommendation.hasBest()) {
            recommendationArea.add(EmptyPanel.empty(
                    "No AVAILABLE resource can be assigned to this incident."));
        } else {
            AssignmentCandidate best = recommendation.getBest();
            Resource resource = best.getResource();
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(GuiTheme.ACCENT_SOFT);
            card.setBorder(BorderFactory.createLineBorder(GuiTheme.ACCENT, 1));

            JPanel copy = new JPanel(new BorderLayout(8, 0));
            copy.setOpaque(false);
            copy.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JLabel resourceLabel = new JLabel("Recommended: " + resource.getResourceType()
                    + " #" + resource.getResourceId());
            resourceLabel.setFont(GuiTheme.FONT_BODY_BOLD);
            resourceLabel.setForeground(GuiTheme.TEXT_PRIMARY);

            JLabel estimate = new JLabel("Estimated response "
                    + String.format("%.1f min", best.getResponseTime()));
            estimate.setFont(GuiTheme.FONT_SMALL);
            estimate.setForeground(GuiTheme.TEXT_SECONDARY);

            JPanel text = new JPanel(new BorderLayout(0, 2));
            text.setOpaque(false);
            text.add(resourceLabel, BorderLayout.NORTH);
            text.add(estimate, BorderLayout.SOUTH);
            copy.add(text, BorderLayout.CENTER);

            JButton assign = new JButton("Confirm assignment");
            assign.addActionListener(event -> confirmAssign(request, resource));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
            right.setOpaque(false);
            right.add(assign);
            copy.add(right, BorderLayout.EAST);

            card.add(copy, BorderLayout.CENTER);
            recommendationArea.add(card);
        }
        recommendationArea.revalidate();
        recommendationArea.repaint();
    }

    private void confirmAssign(ServiceRequest request, Resource resource) {
        setActionsEnabled(false);
        GuiWork.run(
                this,
                () -> {
                    appContext.confirmAssignment(request, resource);
                    return null;
                },
                ignored -> afterMutation(request),
                (error, anchor) -> {
                    setActionsEnabled(true);
                    GuiWork.showError(anchor, error);
                });
    }

    private void changeStatus(ServiceRequest request, String status) {
        setActionsEnabled(false);
        GuiWork.run(
                this,
                () -> {
                    appContext.updateRequestStatus(request.getRequestId(), status);
                    return null;
                },
                ignored -> afterMutation(request),
                (error, anchor) -> {
                    setActionsEnabled(true);
                    GuiWork.showError(anchor, error);
                });
    }

    private void afterMutation(ServiceRequest request) {
        setActionsEnabled(true);
        refreshRequest(request);
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void refreshRequest(ServiceRequest request) {
        GuiWork.run(
                this,
                () -> {
                    ServiceRequest[] requests = appContext.loadRequests();
                    for (ServiceRequest candidate : requests) {
                        if (candidate.getRequestId() == request.getRequestId()) {
                            return candidate;
                        }
                    }
                    return null;
                },
                refreshed -> {
                    if (refreshed != null) {
                        updateContent(refreshed);
                    }
                },
                (error, anchor) -> GuiWork.showError(anchor, error));
    }

    private void updateContent(ServiceRequest request) {
        setTitle("Incident #" + request.getRequestId());
        JPanel root = (JPanel) getContentPane();
        root.removeAll();
        root.add(buildHeader(request), BorderLayout.NORTH);
        root.add(buildBody(request), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        root.revalidate();
        root.repaint();
        loadHistory(request);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel section = new JLabel("AUDIT HISTORY");
        section.setFont(GuiTheme.FONT_SMALL);
        section.setForeground(GuiTheme.TEXT_MUTED);
        footer.add(section, BorderLayout.NORTH);

        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
        historyList.setOpaque(false);
        historyList.add(EmptyPanel.loading("Loading audit history..."));

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1));
        scroll.setPreferredSize(new Dimension(560, 130));
        footer.add(scroll, BorderLayout.CENTER);
        return footer;
    }

    private void loadHistory(ServiceRequest request) {
        GuiWork.run(
                this,
                () -> appContext.loadAuditEventsFor("SERVICE_REQUEST", request.getRequestId()),
                events -> renderHistory(events),
                (error, anchor) -> {
                    historyList.removeAll();
                    historyList.add(EmptyPanel.error("Unable to load history. "
                            + error.getMessage()));
                    historyList.revalidate();
                    historyList.repaint();
                });
    }

    private void renderHistory(AuditEvent[] events) {
        historyList.removeAll();
        if (events.length == 0) {
            historyList.add(EmptyPanel.empty("No audit events recorded for this incident."));
        } else {
            for (AuditEvent event : events) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(GuiTheme.PANEL_BACKGROUND);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, GuiTheme.PANEL_BORDER),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));

                JLabel eventType = new JLabel(UiFormatters.humanize(event.getEventType()));
                eventType.setFont(GuiTheme.FONT_BODY_BOLD);
                eventType.setForeground(GuiTheme.TEXT_PRIMARY);

                JLabel meta = new JLabel(UiFormatters.formatInstant(event.getTimestamp())
                        + (event.getActorType() == null ? "" : "  |  " + event.getActorType()));
                meta.setFont(GuiTheme.FONT_SMALL);
                meta.setForeground(GuiTheme.TEXT_SECONDARY);

                JPanel top = new JPanel(new BorderLayout());
                top.setOpaque(false);
                top.add(eventType, BorderLayout.WEST);
                top.add(meta, BorderLayout.EAST);
                row.add(top, BorderLayout.NORTH);

                if (event.getDetails() != null) {
                    JTextArea details = new JTextArea(event.getDetails());
                    details.setEditable(false);
                    details.setLineWrap(true);
                    details.setWrapStyleWord(true);
                    details.setFont(GuiTheme.FONT_SMALL);
                    details.setForeground(GuiTheme.TEXT_SECONDARY);
                    details.setBorder(null);
                    details.setOpaque(false);
                    row.add(details, BorderLayout.CENTER);
                }

                historyList.add(row);
            }
        }
        historyList.revalidate();
        historyList.repaint();
    }

    private void setActionsEnabled(boolean enabled) {
        for (Component component : actionButtons.getComponents()) {
            if (component instanceof JPanel panel) {
                for (Component nested : panel.getComponents()) {
                    if (nested instanceof JButton button) {
                        button.setEnabled(enabled);
                    }
                }
            }
        }
    }

    private static boolean isOpen(ServiceRequest request) {
        return org.ugoptimizer.gui.util.ResponseQueueBuilder.isOpen(request);
    }
}
