package org.ugoptimizer.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Compact, clickable summary card for one service request, used by the
 * dashboard, the response queue and recent-incident lists.
 */
public final class IncidentRowCard extends JPanel {

    private final ServiceRequest request;

    public IncidentRowCard(
            AppContext appContext, ServiceRequest request, Consumer<ServiceRequest> onClick) {
        this.request = request;
        setLayout(new BorderLayout(12, 0));
        setBackground(GuiTheme.PANEL_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        add(buildIdentity(appContext), BorderLayout.WEST);
        add(buildMeta(appContext), BorderLayout.CENTER);
        add(buildStatus(), BorderLayout.EAST);

        if (onClick != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter open = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    onClick.accept(IncidentRowCard.this.request);
                }
            };
            addMouseListener(open);
        }
    }

    private JPanel buildIdentity(AppContext appContext) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 2, 0);

        JLabel id = new JLabel("#" + request.getRequestId());
        id.setFont(GuiTheme.FONT_BODY_BOLD);
        id.setForeground(GuiTheme.ACCENT);
        panel.add(id, constraints);

        constraints.gridy = 1;
        JLabel category = new JLabel(UiFormatters.humanize(request.getCategory()));
        category.setFont(GuiTheme.FONT_BODY_BOLD);
        category.setForeground(GuiTheme.TEXT_PRIMARY);
        panel.add(category, constraints);

        constraints.gridy = 2;
        JLabel location = new JLabel(
                appContext.locationName(request.getSourceLocationId())
                        + " → " + appContext.locationName(request.getDestinationLocationId()));
        location.setFont(GuiTheme.FONT_SMALL);
        location.setForeground(GuiTheme.TEXT_SECONDARY);
        panel.add(location, constraints);

        panel.setPreferredSize(new Dimension(260, 0));
        return panel;
    }

    private JPanel buildMeta(AppContext appContext) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panel.setOpaque(false);

        JLabel submitted = new JLabel("Reported " + UiFormatters.formatInstant(
                request.getTimeSubmitted()));
        submitted.setFont(GuiTheme.FONT_SMALL);
        submitted.setForeground(GuiTheme.TEXT_SECONDARY);

        JLabel required = new JLabel("Requires " + UiFormatters.humanize(
                request.getRequiredResourceType() != null
                        ? request.getRequiredResourceType()
                        : "any resource"));
        required.setFont(GuiTheme.FONT_SMALL);
        required.setForeground(GuiTheme.TEXT_SECONDARY);

        panel.add(submitted);
        panel.add(required);
        return panel;
    }

    private JPanel buildStatus() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        panel.add(new UrgencyBadge(request.getUrgency()));
        panel.add(StatusPill.forRequestStatus(request.getStatus()));
        return panel;
    }
}
