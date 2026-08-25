package org.ugoptimizer.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.OptimizationService;
import org.ugoptimizer.frontend.ReportService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.frontend.RouteService;
import org.ugoptimizer.frontend.WorkflowService;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.OptimizationComparison;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.result.RequestOptimizationCandidate;
import org.ugoptimizer.result.RequestOptimizationResult;
import org.ugoptimizer.result.TraversalResult;

class UserModeNavigationTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void resetConstructionCounter() {
        CountingRequestService.instances = 0;
    }

    @Test
    void defaultsToOperationalModeAndExposesOperatorSectionsWithLiveMetrics() throws Exception {
        MainMenu.ModeContent content = createContent();

        assertEquals(MainMenu.OPERATIONAL_MODE, content.currentMode());
        assertTabTitles(content.operationalMode().tabs(),
                "Dashboard", "Incidents", "Dispatch", "Routes", "Resources", "Reports");
        assertEquals("2", namedLabel(content, "dashboard.totalIncidents").getText());
        assertEquals("1", namedLabel(content, "dashboard.activeIncidents").getText());
        assertEquals("1", namedLabel(content, "dashboard.criticalIncidents").getText());
        assertEquals("1", namedLabel(content, "dashboard.awaitingAssignment").getText());
        assertEquals("1", namedLabel(content, "dashboard.availableResources").getText());
    }

    @Test
    void switchesToDsaLabAndBackWithoutConstructingAnotherRequestService() throws Exception {
        MainMenu.ModeContent content = createContent();
        assertEquals(1, CountingRequestService.instances);

        onEdt(() -> findButton(content, "DSA Lab").doClick());
        assertEquals(MainMenu.DSA_LAB_MODE, content.currentMode());
        assertTabTitles(content.dsaLab().tabs(),
                "Structures", "Search & Sort", "Graph Algorithms",
                "Optimization", "Correctness", "Efficiency Lab");

        onEdt(() -> findButton(content, "\u2190 Back to Operations").doClick());
        assertEquals(MainMenu.OPERATIONAL_MODE, content.currentMode());
        assertEquals(1, CountingRequestService.instances);
    }

    @Test
    void dashboardQuickActionsNavigateToTheirOperationalSections() throws Exception {
        MainMenu.ModeContent content = createContent();
        String[] buttons = {"View Incidents", "Dispatch Resource", "Find Route", "View Resources"};
        String[] destinations = {"Incidents", "Dispatch", "Routes", "Resources"};

        for (int index = 0; index < buttons.length; index++) {
            String button = buttons[index];
            onEdt(() -> findButton(content, button).doClick());
            JTabbedPane tabs = content.operationalMode().tabs();
            assertEquals(destinations[index], tabs.getTitleAt(tabs.getSelectedIndex()));
            onEdt(() -> tabs.setSelectedIndex(0));
        }
    }

    @Test
    void operationalRoutesUseCampusLocationNamesWhileAlgorithmsRemainInDsaLab()
            throws Exception {
        MainMenu.ModeContent content = createContent();
        JComboBox<?> from = namedComboBox(content, "operationalRoute.from");
        JComboBox<?> to = namedComboBox(content, "operationalRoute.to");

        assertEquals("Sports Stadium", from.getSelectedItem().toString());
        assertEquals("Commonwealth Hall", to.getSelectedItem().toString());
        assertEquals(1, ((UiOption<?>) from.getSelectedItem()).value());
        assertEquals(2, ((UiOption<?>) to.getSelectedItem()).value());
        assertNull(findButtonOrNull(content.operationalMode(), "Run BFS"));
        findButton(content.dsaLab(), "Run BFS");
    }

    @Test
    void operationalReportsHideTechnicalControlsWhileDsaLabRetainsThem() throws Exception {
        MainMenu.ModeContent content = createContent();

        assertNull(findButtonOrNull(content.operationalMode(), "Run & Record MergeSort"));
        assertNull(findButtonOrNull(content.operationalMode(), "Run & Record BFS"));
        assertNull(findButtonOrNull(content.operationalMode(), "Run & Record Dijkstra"));
        findButton(content.operationalMode(), "Refresh Report");
        findButton(content.dsaLab(), "Run & Record MergeSort");
        findButton(content.dsaLab(), "Run & Record BFS");
        findButton(content.dsaLab(), "Run & Record Dijkstra");
        findButton(content.dsaLab(), "Refresh Recorded Runs");
    }

    private static MainMenu.ModeContent createContent() throws Exception {
        AtomicReference<MainMenu.ModeContent> content = new AtomicReference<>();
        onEdt(() -> content.set(new MainMenu.ModeContent(
                new TestLocationService(),
                new CountingRequestService(),
                new TestResourceService(),
                new TestRouteService(),
                new TestWorkflowService(),
                new TestReportService(),
                () -> List.of(),
                new TestOptimizationService())));
        return content.get();
    }

    private static void assertTabTitles(JTabbedPane tabs, String... expected) {
        assertEquals(expected.length, tabs.getTabCount());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], tabs.getTitleAt(index));
        }
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton match = findButtonOrNull(child, text);
                if (match != null) {
                    return match;
                }
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    private static JButton findButtonOrNull(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton match = findButtonOrNull(child, text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static JLabel namedLabel(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && name.equals(label.getName())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel match = namedLabelOrNull(child, name);
                if (match != null) {
                    return match;
                }
            }
        }
        throw new AssertionError("Label not found: " + name);
    }

    private static JLabel namedLabelOrNull(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && name.equals(label.getName())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel match = namedLabelOrNull(child, name);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static JComboBox<?> namedComboBox(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (component instanceof JComboBox<?> comboBox && name.equals(comboBox.getName())) {
                return comboBox;
            }
            if (component instanceof Container child) {
                JComboBox<?> match = namedComboBoxOrNull(child, name);
                if (match != null) {
                    return match;
                }
            }
        }
        throw new AssertionError("Combo box not found: " + name);
    }

    private static JComboBox<?> namedComboBoxOrNull(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (component instanceof JComboBox<?> comboBox && name.equals(comboBox.getName())) {
                return comboBox;
            }
            if (component instanceof Container child) {
                JComboBox<?> match = namedComboBoxOrNull(child, name);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        AtomicReference<Exception> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                failure.set(exception);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CountingRequestService implements RequestService {
        private static int instances;
        private final List<ServiceRequest> requests;

        private CountingRequestService() {
            instances++;
            Instant submitted = Instant.parse("2026-08-21T10:00:00Z");
            requests = List.of(
                    new ServiceRequest(1, 1, 2, "MEDICAL_EMERGENCY", 5,
                            submitted, submitted.plusSeconds(3600), "PENDING",
                            "MEDICAL_TEAM", "Medical response requested"),
                    new ServiceRequest(2, 2, 3, "SECURITY_ESCORT", 3,
                            submitted, submitted.plusSeconds(7200), "COMPLETED",
                            "PATROL_OFFICER", "Escort completed"));
        }

        @Override
        public List<ServiceRequest> findAll() {
            return requests;
        }

        @Override
        public int nextRequestId() {
            return 3;
        }

        @Override
        public ServiceRequest add(ServiceRequest request) {
            return request;
        }

        @Override
        public ServiceRequest updateStatus(int requestId, String newStatus) {
            return requests.get(0);
        }
    }

    private static final class TestResourceService implements ResourceService {
        private final List<Resource> resources = List.of(
                new Resource(1, "MEDICAL_TEAM", 1, 2, "AVAILABLE", 1, null, null),
                new Resource(2, "PATROL_OFFICER", 2, 1, "BUSY", 2, null, null));

        @Override
        public List<Resource> findAll() {
            return resources;
        }

        @Override
        public int nextResourceId() {
            return 3;
        }

        @Override
        public Resource add(Resource resource) {
            return resource;
        }
    }

    private static final class TestLocationService implements LocationService {
        @Override
        public List<Location> findAllLocations() {
            return List.of(
                    new Location(1, "Sports Stadium", "Legon", "SPORTS", 1, 1,
                            "06:00-22:00", "academic test"),
                    new Location(2, "Commonwealth Hall", "Legon", "HALL", 2, 2,
                            "24 hours", "academic test"));
        }

        @Override
        public int nextLocationId() {
            return 3;
        }

        @Override
        public Location addLocation(Location location) {
            return location;
        }

        @Override
        public List<Road> findAllRoads() {
            return List.of();
        }

        @Override
        public int nextRoadId() {
            return 1;
        }

        @Override
        public Road addRoad(Road road) {
            return road;
        }
    }

    private static final class TestRouteService implements RouteService {
        @Override
        public TraversalResult bfs(int startLocationId) {
            return TraversalResult.missingStart(startLocationId, 0);
        }

        @Override
        public TraversalResult dfs(int startLocationId) {
            return TraversalResult.missingStart(startLocationId, 0);
        }

        @Override
        public PathResult shortestPath(int sourceLocationId, int destinationLocationId) {
            return PathResult.missingBoth(sourceLocationId, destinationLocationId);
        }

        @Override
        public List<String> getScenarioNames() {
            return List.of("RAINY_EVENING");
        }

        @Override
        public PathResult shortestPathUnderScenario(
                String scenarioName, int sourceLocationId, int destinationLocationId) {
            return PathResult.missingBoth(sourceLocationId, destinationLocationId);
        }
    }

    private static final class TestWorkflowService implements WorkflowService {
        @Override
        public AuditEvent logEvent(String eventType, int entityId, String details) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AuditEvent> findAuditLog() {
            return List.of();
        }
    }

    private static final class TestReportService implements ReportService {
        @Override
        public AlgorithmRun record(AlgorithmRun run) {
            return run;
        }

        @Override
        public List<AlgorithmRun> findAll() {
            return List.of();
        }
    }

    private static final class TestOptimizationService implements OptimizationService {
        @Override
        public int getBudget() {
            return 80;
        }

        @Override
        public List<RequestOptimizationCandidate> pendingRequestCandidates() {
            return List.of();
        }

        @Override
        public RequestOptimizationResult runDynamicProgramming(
                List<RequestOptimizationCandidate> candidates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestOptimizationResult runBruteForce(
                List<RequestOptimizationCandidate> candidates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OptimizationComparison compare(List<RequestOptimizationCandidate> candidates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssignmentCandidate recommendResource(int requestId) {
            throw new UnsupportedOperationException();
        }
    }
}
