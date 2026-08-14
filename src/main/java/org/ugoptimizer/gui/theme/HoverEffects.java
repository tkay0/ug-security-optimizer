package org.ugoptimizer.gui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public final class HoverEffects {

    private HoverEffects() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static void applyButtonHover(JButton button, Color defaultBg, Color hoverBg, Color defaultFg, Color hoverFg) {
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverBg);
                    if (hoverFg != null) {
                        button.setForeground(hoverFg);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(defaultBg);
                if (hoverFg != null) {
                    button.setForeground(defaultFg);
                }
            }
        });
    }
}
