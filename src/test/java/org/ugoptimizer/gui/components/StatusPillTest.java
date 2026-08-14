package org.ugoptimizer.gui.components;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusPillTest {

    @Test
    void createsPillForPendingStatus() {
        StatusPill pill = StatusPill.forRequestStatus("PENDING");
        assertNotNull(pill);
        assertEquals("PENDING", pill.getText());
    }

    @Test
    void createsPillForAssignedStatus() {
        StatusPill pill = StatusPill.forRequestStatus("ASSIGNED");
        assertNotNull(pill);
        assertEquals("ASSIGNED", pill.getText());
    }

    @Test
    void createsPillForInProgressStatus() {
        StatusPill pill = StatusPill.forRequestStatus("IN_PROGRESS");
        assertNotNull(pill);
        assertEquals("IN PROGRESS", pill.getText());
    }

    @Test
    void createsPillForCompletedStatus() {
        StatusPill pill = StatusPill.forRequestStatus("COMPLETED");
        assertNotNull(pill);
        assertEquals("COMPLETED", pill.getText());
    }

    @Test
    void createsPillForCancelledStatus() {
        StatusPill pill = StatusPill.forRequestStatus("CANCELLED");
        assertNotNull(pill);
        assertEquals("CANCELLED", pill.getText());
    }

    @Test
    void createsPillForUnknownStatus() {
        StatusPill pill = StatusPill.forRequestStatus("UNKNOWN");
        assertNotNull(pill);
        assertEquals("UNKNOWN", pill.getText());
    }

    @Test
    void createsPillForAvailableResource() {
        StatusPill pill = StatusPill.forAvailability("AVAILABLE");
        assertNotNull(pill);
        assertEquals("AVAILABLE", pill.getText());
    }

    @Test
    void createsPillForBusyResource() {
        StatusPill pill = StatusPill.forAvailability("BUSY");
        assertNotNull(pill);
        assertEquals("BUSY", pill.getText());
    }
}
