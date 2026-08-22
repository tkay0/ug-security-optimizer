package org.ugoptimizer.ui;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ugoptimizer.frontend.FrontendServiceException;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Converts operational failures into concise dialogs while retaining technical diagnostics. */
public final class UiErrors {

    private static final Logger LOGGER = Logger.getLogger(UiErrors.class.getName());

    private UiErrors() {
    }

    public static void show(Component parent, String action, Throwable failure) {
        LOGGER.log(Level.SEVERE, "Unable to " + action, failure);
        MessagePrinter.showError(parent, message(action, failure));
    }

    /** Returns the non-technical message shown to an operator. */
    public static String message(String action, Throwable failure) {
        if (failure instanceof FrontendServiceException) {
            return "Unable to " + action
                    + " because the application data could not be read or saved. Please retry.";
        }
        if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) {
            String detail = usefulMessage(failure);
            return detail.endsWith(".") ? detail : detail + ".";
        }
        return "Unable to " + action + " because of an unexpected error. Please retry.";
    }

    private static String usefulMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank() ? "The operation could not be completed" : message;
    }
}
