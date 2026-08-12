package org.ugoptimizer.gui.components;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.UiFormatters;

/**
 * Urgency badge combining the canonical 1..5 urgency value with a readable
 * severity label and a severity colour.
 */
public final class UrgencyBadge extends JPanel {

    public UrgencyBadge(int urgency) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);

        Color accent;
        Color soft;
        switch (urgency) {
            case 5 -> {
                accent = GuiTheme.STATUS_DANGER;
                soft = GuiTheme.STATUS_DANGER_SOFT;
            }
            case 4 -> {
                accent = new Color(0xE8, 0x5D, 0x1F);
                soft = new Color(0xFD, 0xEE, 0xE6);
            }
            case 3 -> {
                accent = GuiTheme.STATUS_WARN;
                soft = GuiTheme.STATUS_WARN_SOFT;
            }
            case 2 -> {
                accent = GuiTheme.STATUS_INFO;
                soft = GuiTheme.STATUS_INFO_SOFT;
            }
            default -> {
                accent = GuiTheme.STATUS_NEUTRAL;
                soft = GuiTheme.STATUS_NEUTRAL_SOFT;
            }
        }

        JLabel number = new JLabel(String.valueOf(urgency));
        number.setOpaque(true);
        number.setBackground(accent);
        number.setForeground(Color.WHITE);
        number.setFont(new Font("Segoe UI", Font.BOLD, 11));
        number.setHorizontalAlignment(JLabel.CENTER);
        number.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel label = new JLabel(UiFormatters.urgencyLabel(urgency));
        label.setOpaque(true);
        label.setBackground(soft);
        label.setForeground(accent);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        add(number);
        add(label);
    }
}
