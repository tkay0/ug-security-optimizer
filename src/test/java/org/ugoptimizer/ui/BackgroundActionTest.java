package org.ugoptimizer.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class BackgroundActionTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void runsWorkOffEdtCompletesOnEdtAndPreventsDuplicates() throws Exception {
        BackgroundAction action = new BackgroundAction();
        JButton button = new JButton("Run");
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean taskOnEdt = new AtomicBoolean(true);
        AtomicBoolean completionOnEdt = new AtomicBoolean(false);
        AtomicBoolean duplicateStarted = new AtomicBoolean(true);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(action.start(
                    button,
                    "Running...",
                    () -> {
                        taskOnEdt.set(SwingUtilities.isEventDispatchThread());
                        taskStarted.countDown();
                        assertTrue(releaseTask.await(5, TimeUnit.SECONDS));
                        return 42;
                    },
                    result -> {
                        assertEquals(42, result);
                        completionOnEdt.set(SwingUtilities.isEventDispatchThread());
                        completed.countDown();
                    },
                    failure -> completed.countDown()));
            duplicateStarted.set(action.start(
                    button, "Duplicate", () -> 0, ignored -> { }, ignored -> { }));
            assertFalse(button.isEnabled());
            assertEquals("Running...", button.getText());
        });

        assertTrue(taskStarted.await(5, TimeUnit.SECONDS));
        releaseTask.countDown();
        assertTrue(completed.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(action.isRunning());
            assertTrue(button.isEnabled());
            assertEquals("Run", button.getText());
        });
        assertFalse(taskOnEdt.get());
        assertTrue(completionOnEdt.get());
        assertFalse(duplicateStarted.get());
    }

    @Test
    void restoresControlAndDeliversFailureOnEdt() throws Exception {
        BackgroundAction action = new BackgroundAction();
        JButton button = new JButton("Retry");
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> received = new AtomicReference<>();
        AtomicBoolean failureOnEdt = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> action.start(
                button,
                "Working...",
                () -> {
                    throw new IllegalStateException("failed operation");
                },
                ignored -> completed.countDown(),
                failure -> {
                    received.set(failure);
                    failureOnEdt.set(SwingUtilities.isEventDispatchThread());
                    completed.countDown();
                }));

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(button.isEnabled());
            assertEquals("Retry", button.getText());
        });
        assertEquals("failed operation", received.get().getMessage());
        assertTrue(failureOnEdt.get());
    }

    @Test
    void rejectsStartingOutsideTheEdt() {
        BackgroundAction action = new BackgroundAction();
        assertThrows(IllegalStateException.class, () -> action.start(
                new JButton("Run"), "Busy", () -> 1, ignored -> { }, ignored -> { }));
    }
}
