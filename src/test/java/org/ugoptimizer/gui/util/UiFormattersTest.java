package org.ugoptimizer.gui.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiFormattersTest {

    @Test
    void humanizesSnakeCase() {
        assertEquals("Theft Report", UiFormatters.humanize("THEFT_REPORT"));
    }

    @Test
    void humanizesNullToDash() {
        assertEquals("-", UiFormatters.humanize(null));
    }

    @Test
    void humanizesBlankToDash() {
        assertEquals("-", UiFormatters.humanize(""));
    }

    @Test
    void returnsCriticalForUrgencyFive() {
        assertEquals("Critical", UiFormatters.urgencyLabel(5));
    }

    @Test
    void returnsHighForUrgencyFour() {
        assertEquals("High", UiFormatters.urgencyLabel(4));
    }

    @Test
    void returnsMediumForUrgencyThree() {
        assertEquals("Medium", UiFormatters.urgencyLabel(3));
    }

    @Test
    void returnsLowForUrgencyTwo() {
        assertEquals("Low", UiFormatters.urgencyLabel(2));
    }

    @Test
    void returnsInformationalForUrgencyOne() {
        assertEquals("Informational", UiFormatters.urgencyLabel(1));
    }

    @Test
    void returnsNoShiftForNulls() {
        assertEquals("No shift", UiFormatters.shiftText(null, null));
    }

    @Test
    void returnsPartialShiftForOneNull() {
        assertEquals("Partial shift", UiFormatters.shiftText("08:00", null));
    }

    @Test
    void formatsShiftForBothValues() {
        assertEquals("08:00 - 16:00", UiFormatters.shiftText("08:00", "16:00"));
    }
}
