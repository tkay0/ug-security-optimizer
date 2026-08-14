package org.ugoptimizer.gui.components;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatCardTest {

    @Test
    void createsCardWithCaptionAndZeroValue() {
        StatCard card = new StatCard("Test", Color.BLUE);
        assertEquals("0", card.getValue());
        assertEquals("Test", card.getCaption());
    }

    @Test
    void updatesValue() {
        StatCard card = new StatCard("Test", Color.BLUE);
        card.setValue("42");
        assertEquals("42", card.getValue());
    }

    @Test
    void updatesCaption() {
        StatCard card = new StatCard("Test", Color.BLUE);
        card.setCaption("New caption");
        assertEquals("New caption", card.getCaption());
    }
}
