package org.ugoptimizer.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.frontend.FrontendServiceException;

class UiErrorsTest {

    @Test
    void givesDatabaseFailuresAUsefulRetryMessage() {
        String message = UiErrors.message(
                "save the request",
                new FrontendServiceException("SQL details", new SQLException("locked")));

        assertEquals(
                "Unable to save the request because the application data could not be read or saved. Please retry.",
                message);
    }

    @Test
    void preservesUsefulDomainFailureMessagesAndHandlesMissingMessages() {
        assertEquals(
                "Unknown road scenario: TEST.",
                UiErrors.message(
                        "calculate a route",
                        new IllegalArgumentException("Unknown road scenario: TEST")));
        assertFalse(UiErrors.message("calculate a route", new RuntimeException()).isBlank());
    }
}
