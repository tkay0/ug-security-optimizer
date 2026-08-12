package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.components.EmptyPanel;
import org.ugoptimizer.gui.components.StatusPill;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Resource;

/**
 * Modal detail view for one dispatch resource with availability updates and the
 * resource's persisted audit history.
 */
public final class ResourceDetailDialog extends JDialog {

    private static final String[] AVAILABILITY_VALUES = {
        "AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY"
    };

    private final AppContext appContext;
    private final Runnable onChanged;
    private final JComboBox<String> availabilityBox = new JComboBox<>(AVAILABILITY_VALUES);
    private final JButton applyButton = new JButton("Update availability");
    private final JPanel historyList = new JPanel();

    public ResourceDetailDialog(
            JComponent owner, AppContext appContext, Resource resource, Runnable onChanged) {
        this.appContext = appContext;
        this.onChanged = onChanged;
        setTitle("Resource #" + resource.getResourceId());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(resource), BorderLayout.NORTH);
        root.add(buildDetails(resource), BorderLayout.CENTER);
        root.add(buildHistory(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
        loadHistory(resource);
    }

    private JPanel buildHeader(Resource resource) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Resource #" + resource.getResourceId());
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel type = new JLabel(UiFormatters.humanize(resource.getResourceType()));
        type.setFont(GuiTheme.FONT_SECTION);
        type.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel left = new JPanel(new BorderLayout(0, 2));
        left.setOpaque(false);
        left.add(title, BorderLayout.NORTH);
        left.add(type, BorderLayout.SOUTH);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(StatusPill.forAvailability(resource.getAvailabilityStatus()));

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildDetails(Resource resource) {
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

        field(details, c, "Resource ID", "#" + resource.getResourceId());
        field(details, c, "Type", UiFormatters.humanize(resource.getResourceType()));
        field(details, c, "Capacity", String.valueOf(resource.getCapacity()));
        field(details, c, "Home location",
                appContext.locationName(resource.getHomeLocationId()));
        field(details, c, "Current location",
                resource.getCurrentLocationId() == null
                        ? "Home base"
                        : appContext.locationName(resource.getCurrentLocationId()));
        field(details, c, "Shift",
                UiFormatters.shiftText(
                        resource.getShiftStart() == null ? null : resource.getShiftStart().toString(),
                        resource.getShiftEnd() == null ? null : resource.getShiftEnd().toString()));
        field(details, c, "Availability", resource.getAvailabilityStatus());

        c.gridx = 1;
        c.gridy = 0;
        field(details, c, "Dispatchable", dispatchableText(resource));
        field(details, c, "Assigned to requests", assignedWorkText(resource));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        actions.setOpaque(false);
        actions.add(new JLabel("Set availability:"));
        availabilityBox.setSelectedItem(resource.getAvailabilityStatus());
        actions.add(availabilityBox);
        actions.add(applyButton);
        applyButton.addActionListener(event -> updateAvailability(resource));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(details, BorderLayout.NORTH);
        wrapper.add(actions, BorderLayout.CENTER);
        return wrapper;
    }

    private String dispatchableText(Resource resource) {
        return "AVAILABLE".equals(resource.getAvailabilityStatus()) ? "Yes" : "No";
    }

    private String assignedWorkText(Resource resource) {
        int assigned = 0;
        try {
            for (org.ugoptimizer.model.ServiceRequest request : appContext.loadRequests()) {
                if ("ASSIGNED".equals(request.getStatus())
                        && request.getRequiredResourceType() != null
                        && request.getRequiredResourceType().equals(resource.getResourceType())) {
                    assigned++;
                }
            }
        } catch (Exception failure) {
            return "-";
        }
        return assigned + " active";
    }

    private void field(JPanel panel, GridBagConstraints c, String labelText, String valueText) {
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

    private void updateAvailability(Resource resource) {
        String target = (String) availabilityBox.getSelectedItem();
        if (target.equals(resource.getAvailabilityStatus())) {
            return;
        }
        applyButton.setEnabled(false);
        GuiWork.run(
                this,
                () -> {
                    appContext.updateResourceAvailability(resource.getResourceId(), target);
                    return null;
                },
                ignored -> {
                    applyButton.setEnabled(true);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    loadHistory(resource);
                },
                (error, anchor) -> {
                    applyButton.setEnabled(true);
                    GuiWork.showError(anchor, error);
                });
    }

    private JPanel buildHistory() {
        JPanel history = new JPanel(new BorderLayout());
        history.setOpaque(false);

        JLabel section = new JLabel("RESOURCE AUDIT HISTORY");
        section.setFont(GuiTheme.FONT_SMALL);
        section.setForeground(GuiTheme.TEXT_MUTED);
        history.add(section, BorderLayout.NORTH);

        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
        historyList.setOpaque(false);
        historyList.add(EmptyPanel.loading("Loading resource history..."));

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1));
        scroll.setPreferredSize(new Dimension(560, 110));
        history.add(scroll, BorderLayout.CENTER);
        return history;
    }

    private void loadHistory(Resource resource) {
        GuiWork.run(
                this,
                () -> appContext.loadAuditEventsFor("RESOURCE", resource.getResourceId()),
                events -> renderHistory(events),
                (error, anchor) -> {
                    historyList.removeAll();
                    historyList.add(EmptyPanel.error("Unable to load resource history. "
                            + error.getMessage()));
                    historyList.revalidate();
                    historyList.repaint();
                });
    }

    private void renderHistory(AuditEvent[] events) {
        historyList.removeAll();
        if (events.length == 0) {
            historyList.add(EmptyPanel.empty("No audit events recorded for this resource."));
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
}
