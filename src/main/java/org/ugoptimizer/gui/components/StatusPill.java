package org.ugoptimizer.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.CompoundBorder;
import org.ugoptimizer.gui.theme.GuiTheme;

/**
 * Compact text badge whose colour reinforces a state while the text remains
 * the primary signal (status is never communicated by colour alone).
 */
public final class StatusPill extends JLabel {

    public StatusPill(String text, Color background, Color foreground) {
        super(text.toUpperCase());
        setOpaque(true);
        setBackground(background);
        setForeground(foreground);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(foreground, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        setPreferredSize(new Dimension(getPreferredSize().width, 20));
    }

    /** Builds the pill that corresponds to a canonical request status. */
    public static StatusPill forRequestStatus(String status) {
        return switch (status) {
            case "PENDING" ->
                    new StatusPill("Pending", GuiTheme.STATUS_WARN_SOFT, GuiTheme.STATUS_WARN);
            case "ASSIGNED" ->
                    new StatusPill("Assigned", GuiTheme.STATUS_INFO_SOFT, GuiTheme.STATUS_INFO);
            case "IN_PROGRESS" ->
                    new StatusPill("In Progress", GuiTheme.STATUS_OK_SOFT, GuiTheme.STATUS_OK);
            case "COMPLETED" ->
                    new StatusPill("Completed", GuiTheme.STATUS_NEUTRAL_SOFT, GuiTheme.STATUS_NEUTRAL);
            case "CANCELLED" ->
                    new StatusPill("Cancelled", GuiTheme.STATUS_DANGER_SOFT, GuiTheme.STATUS_DANGER);
            default -> new StatusPill(status, GuiTheme.STATUS_NEUTRAL_SOFT, GuiTheme.STATUS_NEUTRAL);
        };
    }

    /** Builds the pill that corresponds to a canonical resource availability value. */
    public static StatusPill forAvailability(String availabilityStatus) {
        return switch (availabilityStatus) {
            case "AVAILABLE" ->
                    new StatusPill("Available", GuiTheme.STATUS_OK_SOFT, GuiTheme.STATUS_OK);
            case "BUSY" ->
                    new StatusPill("Busy", GuiTheme.STATUS_INFO_SOFT, GuiTheme.STATUS_INFO);
            case "MAINTENANCE" ->
                    new StatusPill("Maintenance", GuiTheme.STATUS_WARN_SOFT, GuiTheme.STATUS_WARN);
            case "OFF_DUTY" ->
                    new StatusPill("Off Duty", GuiTheme.STATUS_NEUTRAL_SOFT, GuiTheme.STATUS_NEUTRAL);
            default -> new StatusPill(
                    availabilityStatus, GuiTheme.STATUS_NEUTRAL_SOFT, GuiTheme.STATUS_NEUTRAL);
        };
    }
}
