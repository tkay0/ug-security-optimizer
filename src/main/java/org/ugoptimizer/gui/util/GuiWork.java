package org.ugoptimizer.gui.util;

import java.awt.Component;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.ugoptimizer.gui.theme.GuiTheme;

/**
 * Minimal background-task helper used by every screen so that database and
 * algorithm work never blocks the event-dispatch thread and every failure
 * surfaces as a readable error state instead of a raw stack trace.
 */
public final class GuiWork {

    private GuiWork() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    @FunctionalInterface
    public interface Task<T> {
        T run() throws Exception;
    }

    public static <T> void run(
            Component anchor,
            Task<T> task,
            Consumer<T> onSuccess,
            BiConsumer<Exception, Component> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (CancellationException cancelled) {
                    // task cancelled; nothing to show
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    Exception exception = cause instanceof Exception
                            ? (Exception) cause
                            : new RuntimeException(cause);
                    if (onError != null) {
                        onError.accept(exception, anchor);
                    } else {
                        showError(anchor, exception);
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showError(anchor, interrupted);
                }
            }
        }.execute();
    }

    public static void showError(Component anchor, Throwable error) {
        JOptionPane.showMessageDialog(
                anchor,
                "The operation could not be completed.\n\n"
                        + friendlyMessage(error),
                "System error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static String friendlyMessage(Throwable error) {
        String detail = error.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = error.getClass().getSimpleName();
        }
        return "Please try again. (" + detail + ")";
    }
}
