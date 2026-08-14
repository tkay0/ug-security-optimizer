package org.ugoptimizer.ui.display;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Central place for every dialog message in the app, so error, info, and
 * confirmation wording stays consistent no matter which team's screen
 * triggers it.
 */
public final class MessagePrinter {

    private MessagePrinter() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /** @return true if the user confirmed, false if they cancelled or closed the dialog */
    public static boolean confirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(
                parent, message, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
