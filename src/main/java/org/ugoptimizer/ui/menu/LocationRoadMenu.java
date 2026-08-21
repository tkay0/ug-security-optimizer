package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.frontend.LocationService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.input.InputReader;
import org.ugoptimizer.ui.BackgroundAction;
import org.ugoptimizer.ui.UiErrors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;

/**
 * Menu for managing campus locations and roads.
 *
 * <p>Backed by an injected {@link LocationService}; button handlers call the
 * service instead of holding their own list, so a real DAO-backed
 * implementation can be swapped in from {@code Main.java} without touching
 * this file.
 */
public class LocationRoadMenu extends JPanel {

    private static final String[] ROAD_TYPES = {"ACCESS_ROAD", "CAMPUS_ROAD", "MAIN_ROAD", "RESIDENTIAL_ROAD"};
    private static final String[] TRAFFIC_LEVELS = {"LOW", "MODERATE", "HIGH"};

    private final LocationService locationService;
    private final BackgroundAction persistenceAction = new BackgroundAction();

    private DataTablePanel<Location> locationTable;
    private DataTablePanel<Road> roadTable;

    public LocationRoadMenu(LocationService locationService) {
        super(new BorderLayout());
        this.locationService = Objects.requireNonNull(locationService, "locationService cannot be null");

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.addTab("Locations", buildLocationsPanel());
        subTabs.addTab("Roads", buildRoadsPanel());
        add(subTabs, BorderLayout.CENTER);
    }

    private JPanel buildLocationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        locationTable = new DataTablePanel<>(List.of(
                new Column<>("ID", l -> String.valueOf(l.getLocationId())),
                new Column<>("Name", Location::getName),
                new Column<>("Area", Location::getArea),
                new Column<>("Type", Location::getLocationType)
        ), locationService.findAllLocations());
        panel.add(locationTable, BorderLayout.CENTER);

        InputReader form = new InputReader()
                .addTextField("Name")
                .addTextField("Area")
                .addTextField("Location Type")
                .addTextField("X Coordinate")
                .addTextField("Y Coordinate")
                .addTextField("Operating Hours")
                .addTextField("Source / Provenance");

        JButton addButton = new JButton("Add Location");
        addButton.addActionListener(e -> {
            try {
                LocationInput input = LocationInput.parse(
                        form.getValue("Name"),
                        form.getValue("Area"),
                        form.getValue("Location Type"),
                        form.getValue("X Coordinate"),
                        form.getValue("Y Coordinate"),
                        form.getValue("Operating Hours"),
                        form.getValue("Source / Provenance"));
                boolean started = persistenceAction.start(
                        addButton,
                        "Adding...",
                        () -> {
                            Location location = input.toLocation(locationService.nextLocationId());
                            locationService.addLocation(location);
                            return locationService.findAllLocations();
                        },
                        locations -> {
                            locationTable.setRows(locations);
                            form.clear();
                            MessagePrinter.showInfo(this, "Location added.");
                        },
                        failure -> UiErrors.show(this, "add the location", failure));
                if (!started) {
                    MessagePrinter.showInfo(this, "A location or road update is already in progress.");
                }
            } catch (IllegalArgumentException ex) {
                MessagePrinter.showError(this, ex.getMessage());
            }
        });

        panel.add(wrapForm(form, addButton, "New Location"), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRoadsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        roadTable = new DataTablePanel<>(List.of(
                new Column<>("ID", r -> String.valueOf(r.getRoadId())),
                new Column<>("From", r -> String.valueOf(r.getFromLocationId())),
                new Column<>("To", r -> String.valueOf(r.getToLocationId())),
                new Column<>("Distance (km)", r -> String.valueOf(r.getDistanceKm())),
                new Column<>("Travel Time (min)", r -> String.valueOf(r.getTravelTimeMin())),
                new Column<>("Type", Road::getRoadType),
                new Column<>("Traffic", Road::getTrafficLevel),
                new Column<>("Blocked", r -> String.valueOf(r.isBlocked()))
        ), locationService.findAllRoads());
        panel.add(roadTable, BorderLayout.CENTER);

        InputReader form = new InputReader()
                .addTextField("From Location ID")
                .addTextField("To Location ID")
                .addTextField("Distance (km)")
                .addTextField("Travel Time (min)")
                .addTextField("Condition Weight")
                .addTextField("Route Label")
                .addDropdownField("Road Type", ROAD_TYPES)
                .addDropdownField("Traffic Level", TRAFFIC_LEVELS)
                .addCheckboxField("Blocked");

        JButton addButton = new JButton("Add Road");
        addButton.addActionListener(e -> {
            try {
                int from = Integer.parseInt(form.getValue("From Location ID"));
                int to = Integer.parseInt(form.getValue("To Location ID"));
                double distance = Double.parseDouble(form.getValue("Distance (km)"));
                double travelTime = Double.parseDouble(form.getValue("Travel Time (min)"));
                double condition = Double.parseDouble(form.getValue("Condition Weight"));
                String routeLabel = form.getValue("Route Label");
                String roadType = form.getValue("Road Type");
                String traffic = form.getValue("Traffic Level");
                boolean blocked = form.getChecked("Blocked");
                boolean started = persistenceAction.start(
                        addButton,
                        "Adding...",
                        () -> {
                            Road road = new Road(
                                    locationService.nextRoadId(),
                                    from,
                                    to,
                                    distance,
                                    travelTime,
                                    condition,
                                    routeLabel,
                                    roadType,
                                    traffic,
                                    blocked);
                            locationService.addRoad(road);
                            return locationService.findAllRoads();
                        },
                        roads -> {
                            roadTable.setRows(roads);
                            form.clear();
                            MessagePrinter.showInfo(this, "Road added.");
                        },
                        failure -> UiErrors.show(this, "add the road", failure));
                if (!started) {
                    MessagePrinter.showInfo(this, "A location or road update is already in progress.");
                }
            } catch (NumberFormatException ex) {
                MessagePrinter.showError(this, "Location IDs, distance, travel time, and condition weight must be numbers.");
            } catch (IllegalArgumentException ex) {
                MessagePrinter.showError(this, ex.getMessage());
            }
        });

        panel.add(wrapForm(form, addButton, "New Road"), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel wrapForm(InputReader form, JButton addButton, String title) {
        JPanel formWrapper = new JPanel(new BorderLayout(8, 8));
        formWrapper.add(form.getComponent(), BorderLayout.CENTER);
        formWrapper.add(addButton, BorderLayout.SOUTH);
        formWrapper.setBorder(BorderFactory.createTitledBorder(title));
        return formWrapper;
    }
}

/** Validates the location fields collected by {@link LocationRoadMenu} before an ID is reserved. */
record LocationInput(
        String name,
        String area,
        String locationType,
        int xCoord,
        int yCoord,
        String operatingHours,
        String sourceUrl) {

    static LocationInput parse(
            String name,
            String area,
            String locationType,
            String xCoordinate,
            String yCoordinate,
            String operatingHours,
            String sourceUrl) {
        return new LocationInput(
                requiredText(name, "Name"),
                requiredText(area, "Area"),
                requiredText(locationType, "Location type"),
                parseCoordinate(xCoordinate, "X coordinate"),
                parseCoordinate(yCoordinate, "Y coordinate"),
                operatingHours,
                requiredText(sourceUrl, "Source / provenance"));
    }

    Location toLocation(int locationId) {
        return new Location(
                locationId, name, area, locationType, xCoord, yCoord, operatingHours, sourceUrl);
    }

    private static int parseCoordinate(String value, String label) {
        String text = requiredText(value, label);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private static String requiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }
}
