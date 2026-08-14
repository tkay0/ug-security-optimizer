package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.service.LocationService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.input.InputReader;

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
                .addTextField("Operating Hours");

        JButton addButton = new JButton("Add Location");
        addButton.addActionListener(e -> {
            try {
                // xCoord/yCoord and sourceUrl are required by the Location model and
                // the database schema (schematic map coordinates and a citation link),
                // but nothing in this UI or any algorithm currently reads them, so
                // this screen no longer asks for them and uses placeholder defaults
                // instead of showing unused fields.
                Location location = new Location(
                        locationService.nextLocationId(),
                        form.getValue("Name"),
                        form.getValue("Area"),
                        form.getValue("Location Type"),
                        0,
                        0,
                        form.getValue("Operating Hours"),
                        "N/A");
                locationService.addLocation(location);
                locationTable.setRows(locationService.findAllLocations());
                form.clear();
                MessagePrinter.showInfo(this, "Location added.");
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
                Road road = new Road(
                        locationService.nextRoadId(),
                        Integer.parseInt(form.getValue("From Location ID")),
                        Integer.parseInt(form.getValue("To Location ID")),
                        Double.parseDouble(form.getValue("Distance (km)")),
                        Double.parseDouble(form.getValue("Travel Time (min)")),
                        Double.parseDouble(form.getValue("Condition Weight")),
                        form.getValue("Route Label"),
                        form.getValue("Road Type"),
                        form.getValue("Traffic Level"),
                        form.getChecked("Blocked"));
                locationService.addRoad(road);
                roadTable.setRows(locationService.findAllRoads());
                form.clear();
                MessagePrinter.showInfo(this, "Road added.");
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
