package org.ugoptimizer.gui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrgencyBadgeTest {

    @Test
    void createsBadgeForCriticalUrgency() {
        UrgencyBadge badge = new UrgencyBadge(5);
        assertNotNull(badge);
    }

    @Test
    void createsBadgeForHighUrgency() {
        UrgencyBadge badge = new UrgencyBadge(4);
        assertNotNull(badge);
    }

    @Test
    void createsBadgeForMediumUrgency() {
        UrgencyBadge badge = new UrgencyBadge(3);
        assertNotNull(badge);
    }

    @Test
    void createsBadgeForLowUrgency() {
        UrgencyBadge badge = new UrgencyBadge(2);
        assertNotNull(badge);
    }

    @Test
    void createsBadgeForInformationalUrgency() {
        UrgencyBadge badge = new UrgencyBadge(1);
        assertNotNull(badge);
    }
}
