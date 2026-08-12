package org.ugoptimizer.gui;

import java.awt.Component;

/**
 * Contract for every control-room screen hosted by {@link SecurityControlRoom}.
 * Screens are shown inside the shell's card layout and refresh their data
 * whenever they become visible.
 */
public interface Screen {

    /** Returns the top-level component to place inside the content area. */
    Component asComponent();

    /** Reloads the screen's data from the real system state. */
    void refresh();
}
