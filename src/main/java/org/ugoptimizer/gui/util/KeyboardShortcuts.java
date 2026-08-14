package org.ugoptimizer.gui.util;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import org.ugoptimizer.gui.i18n.Messages;

public final class KeyboardShortcuts {

    public static final String REFRESH = "refresh";
    public static final String CLOSE = "close";
    public static final String OPEN = "open";
    public static final String NAV_DASHBOARD = "navDashboard";
    public static final String NAV_INCIDENTS = "navIncidents";
    public static final String NAV_QUEUE = "navQueue";
    public static final String NAV_RESOURCES = "navResources";
    public static final String NAV_NETWORK = "navNetwork";
    public static final String NAV_ACTIVITY = "navActivity";

    private KeyboardShortcuts() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static KeyStroke refresh() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    }

    public static KeyStroke close() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    }

    public static KeyStroke open() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke nav(int index) {
        int keyCode = KeyEvent.VK_1 + index;
        return KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_DOWN_MASK);
    }

    public static String navMnemonic(int index) {
        return String.valueOf((char) ('1' + index));
    }
}
