package org.ugoptimizer.ui;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Runs one material backend operation at a time without blocking Swing's event thread. */
public final class BackgroundAction {

    private boolean running;

    /**
     * Starts an operation from the event-dispatch thread.
     *
     * <p>The task runs on a Swing worker thread. Completion callbacks, button restoration, and
     * failure callbacks run on the event-dispatch thread. A second call while work is active is
     * rejected without starting another task.
     *
     * @return {@code true} when work was started, or {@code false} when already running
     */
    public <T> boolean start(
            AbstractButton control,
            String busyText,
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("BackgroundAction must be started on the Swing EDT");
        }
        Objects.requireNonNull(control, "control cannot be null");
        Objects.requireNonNull(busyText, "busyText cannot be null");
        Objects.requireNonNull(task, "task cannot be null");
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
        Objects.requireNonNull(onFailure, "onFailure cannot be null");
        if (running) {
            return false;
        }

        running = true;
        String normalText = control.getText();
        control.setEnabled(false);
        control.setText(busyText);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(exception);
                } catch (ExecutionException exception) {
                    onFailure.accept(exception.getCause());
                } finally {
                    running = false;
                    control.setText(normalText);
                    control.setEnabled(true);
                }
            }
        }.execute();
        return true;
    }

    /** Returns whether this action currently owns a worker. Must be called on the EDT. */
    public boolean isRunning() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("BackgroundAction state must be read on the Swing EDT");
        }
        return running;
    }
}
