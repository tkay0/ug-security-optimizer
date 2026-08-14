package org.ugoptimizer.gui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmptyPanelTest {

    @Test
    void createsLoadingPanel() {
        EmptyPanel panel = EmptyPanel.loading("Loading data...");
        assertNotNull(panel);
    }

    @Test
    void createsEmptyPanel() {
        EmptyPanel panel = EmptyPanel.empty("Nothing here");
        assertNotNull(panel);
    }

    @Test
    void createsErrorPanel() {
        EmptyPanel panel = EmptyPanel.error("Something failed");
        assertNotNull(panel);
    }
}
