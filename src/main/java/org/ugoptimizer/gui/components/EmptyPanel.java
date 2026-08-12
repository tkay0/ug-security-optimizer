package org.ugoptimizer.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.ugoptimizer.gui.theme.GuiTheme;

/**
 * Reusable centred state panel for loading, empty and error conditions.
 * Keeps major lists from appearing as blank, unexplained areas.
 */
public final class EmptyPanel extends JPanel {

    public EmptyPanel(String heading, String detail, StateKind kind) {
        super(new BorderLayout());
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(kind == StateKind.ERROR
                ? GuiTheme.STATUS_DANGER
                : GuiTheme.TEXT_PRIMARY);
        title.setHorizontalAlignment(JLabel.CENTER);

        JLabel message = new JLabel(detail);
        message.setFont(GuiTheme.FONT_BODY);
        message.setForeground(GuiTheme.TEXT_SECONDARY);
        message.setHorizontalAlignment(JLabel.CENTER);

        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.setOpaque(false);
        stack.add(title, BorderLayout.NORTH);
        stack.add(message, BorderLayout.CENTER);
        stack.setPreferredSize(new Dimension(420, 70));

        add(stack, BorderLayout.CENTER);
    }

    public static EmptyPanel loading(String message) {
        return new EmptyPanel("Loading", message, StateKind.INFO);
    }

    public static EmptyPanel empty(String message) {
        return new EmptyPanel("Nothing here yet", message, StateKind.INFO);
    }

    public static EmptyPanel error(String message) {
        return new EmptyPanel("Unable to complete the operation", message, StateKind.ERROR);
    }

    public enum StateKind {
        INFO,
        ERROR
    }
}
