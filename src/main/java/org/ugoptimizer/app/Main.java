package org.ugoptimizer.app;

import java.nio.file.Path;
import java.util.Objects;
import javax.swing.SwingUtilities;
import org.ugoptimizer.frontend.BackendFrontendServices;
import org.ugoptimizer.ui.MainMenu;

/**
 * Application entry point for the UG Campus Security &amp; Emergency Response Optimizer.
 */
public final class Main {

    private static final Path DEFAULT_DATABASE = Path.of("database", "ug-security-optimizer.db");
    private static final Path DEFAULT_DATASET = Path.of("data");

    private Main() {
    }

    /** Frontend handoff that keeps presentation code dependent only on public backend services. */
    @FunctionalInterface
    public interface FrontendLauncher {
        void launch(BackendContext backend) throws Exception;
    }

    public static void main(String[] args) {
        try {
            start(args, Main::launchSwingFrontend);
        } catch (Exception exception) {
            System.err.println("Application startup failed: " + usefulMessage(exception));
            System.exit(1);
        }
    }

    /** Starts the backend and hands its service composition root to the selected frontend. */
    public static void start(String[] args, FrontendLauncher frontend) throws Exception {
        Objects.requireNonNull(args, "args cannot be null");
        if (args.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: Main [database-file] [canonical-dataset-directory]");
        }
        Path database = args.length >= 1 ? Path.of(args[0]) : DEFAULT_DATABASE;
        Path dataset = args.length == 2 ? Path.of(args[1]) : DEFAULT_DATASET;
        start(database, dataset, frontend);
    }

    /** Testable/configurable startup overload used by a frontend integration point. */
    public static void start(Path database, Path dataset, FrontendLauncher frontend)
            throws Exception {
        Objects.requireNonNull(frontend, "frontend cannot be null");
        BackendContext backend = BackendContext.initializeApplication(database, dataset);
        frontend.launch(backend);
    }

    /**
     * Adapts the backend composition root into the Swing-facing
     * {@code org.ugoptimizer.frontend} contracts and launches {@link MainMenu}
     * on the event-dispatch thread.
     */
    private static void launchSwingFrontend(BackendContext backend) {
        Objects.requireNonNull(backend);
        BackendFrontendServices services = BackendFrontendServices.from(backend);

        SwingUtilities.invokeLater(() -> new MainMenu(
                services.locations(),
                services.requests(),
                services.resources(),
                services.routes(),
                services.workflow(),
                services.reports(),
                services.priority(),
                services.optimization()).setVisible(true));
    }

    private static String usefulMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
