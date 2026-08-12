package org.ugoptimizer.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.ugoptimizer.gui.theme.GuiTheme;

/**
 * Key-performance card for the dashboard. Displays a computed system value and
 * a label; values always originate from live loaded data, never hardcoded.
 */
public final class StatCard extends JPanel {

    private final JLabel valueLabel;
    private final JLabel captionLabel;

    public StatCard(String caption, Color accent) {
        super(new BorderLayout(0, 4));
        setBackground(GuiTheme.PANEL_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        valueLabel = new JLabel("0");
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(accent);

        captionLabel = new JLabel(caption);
        captionLabel.setFont(GuiTheme.FONT_SMALL);
        captionLabel.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        valuePanel.setOpaque(false);
        valuePanel.add(valueLabel);

        add(valuePanel, BorderLayout.CENTER);
        add(captionLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setCaption(String caption) {
        captionLabel.setText(caption);
    }
}
