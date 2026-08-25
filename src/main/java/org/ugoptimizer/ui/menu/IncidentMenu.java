package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.UiOption;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Operator-facing incident list, filtering, and reporting form. */
public final class IncidentMenu extends JPanel {

    private static final String[] CATEGORIES = {
            "ACCESS_CONTROL", "CCTV_FAULT", "CROWD_CONTROL", "EMERGENCY_TRANSPORT",
            "FIRE_ALARM", "MEDICAL_EMERGENCY", "NIGHT_PATROL_REQUEST",
            "ROAD_OBSTRUCTION", "SECURITY_ESCORT", "SUSPICIOUS_ACTIVITY",
            "THEFT_REPORT", "WELFARE_CHECK"};
    private static final String[] STATUSES = {
            "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"};

    private final RequestService requestService;
    private final List<Location> locations;
    private final BackgroundAction action = new BackgroundAction();
    private final DataTablePanel<ServiceRequest> table;
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<UiOption<String>> statusFilter;
    private final JComboBox<UiOption<Integer>> urgencyFilter;
    private final JLabel filterState = new JLabel();
    private final JComboBox<UiOption<Integer>> incidentLocation;
    private final JComboBox<UiOption<Integer>> responseDestination;
    private final JComboBox<UiOption<String>> incidentType;
    private final JComboBox<UiOption<Integer>> urgency;
    private final JComboBox<UiOption<String>> requiredResponse;
    private final JTextArea description = new JTextArea(4, 24);
    private List<ServiceRequest> allRequests;

    public IncidentMenu(
            RequestService requestService,
            ResourceService resourceService,
            LocationService locationService) {
        super(new BorderLayout(8, 8));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        Objects.requireNonNull(resourceService, "resourceService cannot be null");
        Objects.requireNonNull(locationService, "locationService cannot be null");
        locations = List.copyOf(locationService.findAllLocations());
        allRequests = requestService.findAll();

        statusFilter = new JComboBox<>(filterStatusOptions());
        urgencyFilter = new JComboBox<>(filterUrgencyOptions());
        incidentLocation = new JComboBox<>(UiFormat.locationOptions(locations));
        responseDestination = new JComboBox<>(UiFormat.locationOptions(locations));
        incidentType = new JComboBox<>(codeOptions(CATEGORIES));
        urgency = new JComboBox<>(urgencyOptions());
        requiredResponse = new JComboBox<>(codeOptions(distinctResourceTypes(resourceService.findAll())));
        nameInputs();

        table = new DataTablePanel<>(List.of(
                new Column<>("Incident ID", request -> "#" + request.getRequestId()),
                new Column<>("Type", request -> UiFormat.humanize(request.getCategory())),
                new Column<>("Incident Location", request ->
                        UiFormat.locationName(locations, request.getSourceLocationId())),
                new Column<>("Response Destination", request ->
                        UiFormat.locationName(locations, request.getDestinationLocationId())),
                new Column<>("Urgency", request -> UiFormat.urgencyOption(request.getUrgency()).label()),
                new Column<>("Status", request -> UiFormat.humanize(request.getStatus())),
                new Column<>("Submitted", request -> UiFormat.dateTime(request.getTimeSubmitted())),
                new Column<>("Required Response", request ->
                        UiFormat.humanize(request.getRequiredResourceType()))
        ), allRequests);
        configureColumns();

        JPanel listPanel = new JPanel(new BorderLayout(8, 8));
        listPanel.add(filterPanel(), BorderLayout.NORTH);
        listPanel.add(table, BorderLayout.CENTER);
        listPanel.add(filterState, BorderLayout.SOUTH);
        listPanel.setBorder(BorderFactory.createTitledBorder("Current Incidents"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, creationPanel());
        split.setResizeWeight(0.62d);
        split.setDividerLocation(690);
        split.setOneTouchExpandable(true);
        add(split, BorderLayout.CENTER);
        applyFilters();
    }

    private JPanel filterPanel() {
        JPanel rows = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();
        addField(rows, constraints, 0, "Search", searchField);
        JButton apply = new JButton("Apply Filters");
        apply.addActionListener(event -> applyFilters());
        JButton clear = new JButton("Clear Filters");
        clear.addActionListener(event -> clearFilters());
        JButton refresh = new JButton("Refresh Incidents");
        refresh.addActionListener(event -> refresh(refresh));

        constraints.gridy = 1;
        constraints.gridx = 0;
        rows.add(new JLabel("Status"), constraints);
        constraints.gridx = 1;
        rows.add(statusFilter, constraints);
        constraints.gridx = 2;
        rows.add(new JLabel("Urgency"), constraints);
        constraints.gridx = 3;
        rows.add(urgencyFilter, constraints);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(apply);
        actions.add(clear);
        actions.add(refresh);
        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        rows.add(actions, constraints);
        return rows;
    }

    private JPanel creationPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();
        addField(fields, constraints, 0, "Incident Location", incidentLocation);
        addField(fields, constraints, 1, "Response Destination", responseDestination);
        addField(fields, constraints, 2, "Incident Type", incidentType);
        addField(fields, constraints, 3, "Urgency", urgency);
        addField(fields, constraints, 4, "Required Response", requiredResponse);
        constraints.gridy = 5;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        fields.add(new JLabel("Description"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0d;
        constraints.weighty = 1.0d;
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        fields.add(new JScrollPane(description), constraints);

        JButton report = new JButton("Report Incident");
        report.addActionListener(event -> reportIncident(report));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(report);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Report New Incident"));
        panel.setMinimumSize(new Dimension(400, 0));
        panel.setPreferredSize(new Dimension(430, 500));
        panel.add(fields, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void reportIncident(JButton control) {
        try {
            Instant submitted = Instant.now();
            UiOption<Integer> selectedIncidentLocation = selected(
                    incidentLocation, "Incident Location");
            UiOption<Integer> selectedResponseDestination = selected(
                    responseDestination, "Response Destination");
            UiOption<String> selectedIncidentType = selected(incidentType, "Incident Type");
            UiOption<Integer> selectedUrgency = selected(urgency, "Urgency");
            UiOption<String> selectedResponse = selected(requiredResponse, "Required Response");
            String enteredDescription = description.getText().trim();
            boolean started = action.start(
                    control,
                    "Reporting...",
                    () -> {
                        ServiceRequest request = buildRequest(
                                requestService.nextRequestId(),
                                selectedIncidentLocation,
                                selectedResponseDestination,
                                selectedIncidentType,
                                selectedUrgency,
                                selectedResponse,
                                enteredDescription,
                                submitted);
                        ServiceRequest added = requestService.add(request);
                        return new IncidentUpdate(added, requestService.findAll());
                    },
                    update -> {
                        allRequests = update.requests();
                        applyFilters();
                        description.setText("");
                        MessagePrinter.showInfo(this, "Incident #"
                                + update.added().getRequestId() + " reported successfully.");
                    },
                    failure -> UiErrors.show(this, "report the incident", failure));
            if (!started) {
                MessagePrinter.showInfo(this, "An incident operation is already in progress.");
            }
        } catch (IllegalArgumentException exception) {
            MessagePrinter.showError(this, exception.getMessage());
        }
    }

    private void refresh(JButton control) {
        boolean started = action.start(
                control,
                "Refreshing...",
                requestService::findAll,
                requests -> {
                    allRequests = requests;
                    applyFilters();
                },
                failure -> UiErrors.show(this, "refresh incidents", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "An incident operation is already in progress.");
        }
    }

    private void applyFilters() {
        UiOption<String> status = selected(statusFilter, "Status");
        UiOption<Integer> urgencyValue = selected(urgencyFilter, "Urgency");
        List<ServiceRequest> matches = filterIncidents(
                allRequests,
                searchField.getText(),
                "ALL".equals(status.value()) ? null : status.value(),
                urgencyValue.value() == 0 ? null : urgencyValue.value(),
                locations);
        table.setRows(matches);
        filterState.setText(matches.isEmpty()
                ? "No incidents match the current filters."
                : "Showing " + matches.size() + " of " + allRequests.size() + " incidents.");
    }

    private void clearFilters() {
        searchField.setText("");
        statusFilter.setSelectedIndex(0);
        urgencyFilter.setSelectedIndex(0);
        applyFilters();
    }

    static ServiceRequest buildRequest(
            int requestId,
            UiOption<Integer> incidentLocation,
            UiOption<Integer> responseDestination,
            UiOption<String> incidentType,
            UiOption<Integer> urgency,
            UiOption<String> requiredResponse,
            String description,
            Instant submitted) {
        return new ServiceRequest(
                requestId,
                incidentLocation.value(),
                responseDestination.value(),
                incidentType.value(),
                urgency.value(),
                submitted,
                submitted.plus(2, ChronoUnit.HOURS),
                "PENDING",
                requiredResponse.value(),
                description);
    }

    static List<ServiceRequest> filterIncidents(
            List<ServiceRequest> requests,
            String query,
            String status,
            Integer urgency,
            List<Location> locations) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<ServiceRequest> matches = new ArrayList<>();
        for (ServiceRequest request : requests) {
            if (status != null && !status.equals(request.getStatus())) {
                continue;
            }
            if (urgency != null && urgency != request.getUrgency()) {
                continue;
            }
            String searchable = (request.getRequestId() + " "
                    + Objects.toString(request.getDescription(), "") + " "
                    + UiFormat.locationName(locations, request.getSourceLocationId()) + " "
                    + UiFormat.locationName(locations, request.getDestinationLocationId()) + " "
                    + request.getCategory()).toLowerCase(Locale.ENGLISH);
            if (needle.isEmpty() || searchable.contains(needle)) {
                matches.add(request);
            }
        }
        return matches;
    }

    private void configureColumns() {
        int[] widths = {70, 130, 180, 195, 110, 95, 175, 180};
        for (int index = 0; index < widths.length; index++) {
            table.getTable().getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        table.getTable().setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    }

    private void nameInputs() {
        incidentLocation.setName("incident.location");
        responseDestination.setName("incident.destination");
        incidentType.setName("incident.type");
        urgency.setName("incident.urgency");
        requiredResponse.setName("incident.requiredResponse");
        searchField.setName("incident.search");
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private static void addField(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0.0d;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, constraints);
    }

    private static UiOption<String>[] codeOptions(String[] values) {
        @SuppressWarnings("unchecked")
        UiOption<String>[] options = (UiOption<String>[]) new UiOption<?>[values.length];
        for (int index = 0; index < values.length; index++) {
            options[index] = UiFormat.codeOption(values[index]);
        }
        return options;
    }

    private static UiOption<Integer>[] urgencyOptions() {
        @SuppressWarnings("unchecked")
        UiOption<Integer>[] options = (UiOption<Integer>[]) new UiOption<?>[5];
        for (int urgency = 1; urgency <= 5; urgency++) {
            options[urgency - 1] = UiFormat.urgencyOption(urgency);
        }
        return options;
    }

    private static UiOption<String>[] filterStatusOptions() {
        String[] values = new String[STATUSES.length + 1];
        values[0] = "ALL";
        System.arraycopy(STATUSES, 0, values, 1, STATUSES.length);
        UiOption<String>[] options = codeOptions(values);
        options[0] = new UiOption<>("ALL", "All Statuses");
        return options;
    }

    private static UiOption<Integer>[] filterUrgencyOptions() {
        @SuppressWarnings("unchecked")
        UiOption<Integer>[] options = (UiOption<Integer>[]) new UiOption<?>[6];
        options[0] = new UiOption<>(0, "All Urgency Levels");
        for (int urgency = 1; urgency <= 5; urgency++) {
            options[urgency] = UiFormat.urgencyOption(urgency);
        }
        return options;
    }

    private static String[] distinctResourceTypes(List<Resource> resources) {
        TreeSet<String> types = new TreeSet<>();
        for (Resource resource : resources) {
            types.add(resource.getResourceType());
        }
        return types.toArray(new String[0]);
    }

    private static <T> UiOption<T> selected(JComboBox<UiOption<T>> field, String label) {
        @SuppressWarnings("unchecked")
        UiOption<T> selected = (UiOption<T>) field.getSelectedItem();
        if (selected == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return selected;
    }

    private record IncidentUpdate(ServiceRequest added, List<ServiceRequest> requests) {
    }
}
