package org.ugoptimizer.ui.menu;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.frontend.ResourceService;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;
import org.ugoptimizer.ui.UiFormat;
import org.ugoptimizer.ui.UiOption;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

/** Operator-facing resource inventory backed by shared resource and location services. */
public final class ResourceMenu extends JPanel {

    private static final String[] AVAILABILITY = {
            "AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY"};

    private final ResourceService resourceService;
    private final List<Location> locations;
    private final BackgroundAction action = new BackgroundAction();
    private final DataTablePanel<Resource> table;
    private final JComboBox<UiOption<String>> resourceType;
    private final JComboBox<UiOption<Integer>> homeLocation;
    private final JTextField capacity = new JTextField();
    private final JComboBox<UiOption<String>> availability;

    public ResourceMenu(ResourceService resourceService, LocationService locationService) {
        super(new BorderLayout(8, 8));
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");
        Objects.requireNonNull(locationService, "locationService cannot be null");
        locations = List.copyOf(locationService.findAllLocations());
        List<Resource> resources = resourceService.findAll();
        resourceType = new JComboBox<>(codeOptions(distinctResourceTypes(resources)));
        homeLocation = new JComboBox<>(UiFormat.locationOptions(locations));
        availability = new JComboBox<>(codeOptions(AVAILABILITY));
        resourceType.setName("resource.type");
        homeLocation.setName("resource.homeLocation");
        capacity.setName("resource.capacity");
        availability.setName("resource.availability");

        table = new DataTablePanel<>(List.of(
                new Column<>("Resource ID", resource -> "#" + resource.getResourceId()),
                new Column<>("Type", resource -> UiFormat.humanize(resource.getResourceType())),
                new Column<>("Home Location", resource ->
                        UiFormat.locationName(locations, resource.getHomeLocationId())),
                new Column<>("Current Location", resource -> resource.getCurrentLocationId() == null
                        ? "Not recorded"
                        : UiFormat.locationName(locations, resource.getCurrentLocationId())),
                new Column<>("Capacity", resource -> String.valueOf(resource.getCapacity())),
                new Column<>("Availability", resource ->
                        UiFormat.humanize(resource.getAvailabilityStatus())),
                new Column<>("Shift Start", resource -> optional(resource.getShiftStart())),
                new Column<>("Shift End", resource -> optional(resource.getShiftEnd()))
        ), resources);
        configureColumns();

        JPanel inventory = new JPanel(new BorderLayout(8, 8));
        JButton refresh = new JButton("Refresh Resources");
        refresh.addActionListener(event -> refresh(refresh));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(refresh);
        inventory.add(controls, BorderLayout.NORTH);
        inventory.add(table, BorderLayout.CENTER);
        inventory.setBorder(BorderFactory.createTitledBorder("Response Resources"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inventory, creationPanel());
        split.setResizeWeight(0.75d);
        split.setDividerLocation(820);
        add(split, BorderLayout.CENTER);
    }

    private JPanel creationPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        addField(fields, constraints, 0, "Resource Type", resourceType);
        addField(fields, constraints, 1, "Home Location", homeLocation);
        addField(fields, constraints, 2, "Capacity", capacity);
        addField(fields, constraints, 3, "Availability", availability);

        JButton register = new JButton("Register Resource");
        register.addActionListener(event -> addResource(register));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(register);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Register Resource"));
        panel.add(fields, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void addResource(JButton control) {
        try {
            int capacityValue = Integer.parseInt(capacity.getText().trim());
            UiOption<String> selectedType = selected(resourceType, "Resource Type");
            UiOption<Integer> selectedHome = selected(homeLocation, "Home Location");
            UiOption<String> selectedAvailability = selected(availability, "Availability");
            boolean started = action.start(
                    control,
                    "Registering...",
                    () -> {
                        Resource resource = buildResource(
                                resourceService.nextResourceId(),
                                selectedType,
                                selectedHome,
                                capacityValue,
                                selectedAvailability);
                        Resource added = resourceService.add(resource);
                        return new ResourceUpdate(added, resourceService.findAll());
                    },
                    update -> {
                        table.setRows(update.resources());
                        capacity.setText("");
                        MessagePrinter.showInfo(this, "Resource #"
                                + update.added().getResourceId() + " registered successfully.");
                    },
                    failure -> UiErrors.show(this, "register the resource", failure));
            if (!started) {
                MessagePrinter.showInfo(this, "A resource operation is already in progress.");
            }
        } catch (NumberFormatException exception) {
            MessagePrinter.showError(this, "Capacity must be a positive whole number.");
        } catch (IllegalArgumentException exception) {
            MessagePrinter.showError(this, exception.getMessage());
        }
    }

    static Resource buildResource(
            int resourceId,
            UiOption<String> type,
            UiOption<Integer> home,
            int capacity,
            UiOption<String> availability) {
        return new Resource(
                resourceId,
                type.value(),
                home.value(),
                capacity,
                availability.value(),
                null,
                null,
                null);
    }

    private void refresh(JButton control) {
        boolean started = action.start(
                control,
                "Refreshing...",
                resourceService::findAll,
                table::setRows,
                failure -> UiErrors.show(this, "refresh resources", failure));
        if (!started) {
            MessagePrinter.showInfo(this, "A resource operation is already in progress.");
        }
    }

    private void configureColumns() {
        int[] widths = {80, 175, 185, 185, 75, 110, 90, 90};
        for (int index = 0; index < widths.length; index++) {
            table.getTable().getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        table.getTable().setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
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

    private static String optional(Object value) {
        return value == null ? "Not recorded" : value.toString();
    }

    private record ResourceUpdate(Resource added, List<Resource> resources) {
    }
}
