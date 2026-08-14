package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.service.RequestService;
import org.ugoptimizer.service.ResourceService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;
import org.ugoptimizer.ui.input.InputReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Menu for submitting and managing service requests (incidents) and the
 * resources that respond to them.
 *
 * <p>Backed by injected {@link RequestService} and {@link ResourceService}
 * instances. The {@code RequestService} instance is shared with
 * {@link SearchSortMenu}, so a request added here is immediately searchable
 * there.
 */
public class RequestResourceMenu extends JPanel {

    private static final String[] CATEGORIES = {
            "ACCESS_CONTROL", "CCTV_FAULT", "CROWD_CONTROL", "EMERGENCY_TRANSPORT",
            "FIRE_ALARM", "MEDICAL_EMERGENCY", "NIGHT_PATROL_REQUEST",
            "ROAD_OBSTRUCTION", "SECURITY_ESCORT", "SUSPICIOUS_ACTIVITY",
            "THEFT_REPORT", "WELFARE_CHECK"};
    private static final String[] STATUSES = {"PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"};
    private static final String[] RESOURCE_TYPES = {"PATROL_TEAM", "MEDICAL_TEAM", "FIRE_TEAM", "ESCORT_VEHICLE"};
    private static final String[] AVAILABILITY = {"AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY"};

    private final RequestService requestService;
    private final ResourceService resourceService;

    private DataTablePanel<ServiceRequest> requestTable;
    private DataTablePanel<Resource> resourceTable;

    public RequestResourceMenu(RequestService requestService, ResourceService resourceService) {
        super(new BorderLayout());
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService cannot be null");

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.addTab("Requests", buildRequestsPanel());
        subTabs.addTab("Resources", buildResourcesPanel());
        add(subTabs, BorderLayout.CENTER);
    }

    private JPanel buildRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        requestTable = new DataTablePanel<>(List.of(
                new Column<>("ID", r -> String.valueOf(r.getRequestId())),
                new Column<>("Category", ServiceRequest::getCategory),
                new Column<>("Urgency", r -> String.valueOf(r.getUrgency())),
                new Column<>("Status", ServiceRequest::getStatus),
                new Column<>("Resource Type", r -> String.valueOf(r.getRequiredResourceType()))
        ), requestService.findAll());
        panel.add(requestTable, BorderLayout.CENTER);

        InputReader form = new InputReader()
                .addTextField("Source Location ID")
                .addTextField("Destination Location ID")
                .addDropdownField("Category", CATEGORIES)
                .addTextField("Urgency (1-5)")
                .addDropdownField("Status", STATUSES)
                .addDropdownField("Required Resource Type", RESOURCE_TYPES)
                .addTextField("Description");

        JButton addButton = new JButton("Add Request");
        addButton.addActionListener(e -> {
            try {
                ServiceRequest request = new ServiceRequest(
                        requestService.nextRequestId(),
                        Integer.parseInt(form.getValue("Source Location ID")),
                        Integer.parseInt(form.getValue("Destination Location ID")),
                        form.getValue("Category"),
                        Integer.parseInt(form.getValue("Urgency (1-5)")),
                        Instant.now(),
                        Instant.now().plus(2, ChronoUnit.HOURS),
                        form.getValue("Status"),
                        form.getValue("Required Resource Type"),
                        form.getValue("Description"));
                requestService.add(request);
                requestTable.setRows(requestService.findAll());
                form.clear();
                MessagePrinter.showInfo(this, "Request added.");
            } catch (NumberFormatException ex) {
                MessagePrinter.showError(this, "Location ID and urgency must be numbers.");
            } catch (IllegalArgumentException ex) {
                MessagePrinter.showError(this, ex.getMessage());
            }
        });

        panel.add(wrapForm(form, addButton, "New Request"), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildResourcesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        resourceTable = new DataTablePanel<>(List.of(
                new Column<>("ID", r -> String.valueOf(r.getResourceId())),
                new Column<>("Type", Resource::getResourceType),
                new Column<>("Home Location", r -> String.valueOf(r.getHomeLocationId())),
                new Column<>("Capacity", r -> String.valueOf(r.getCapacity())),
                new Column<>("Availability", Resource::getAvailabilityStatus)
        ), resourceService.findAll());
        panel.add(resourceTable, BorderLayout.CENTER);

        InputReader form = new InputReader()
                .addDropdownField("Type", RESOURCE_TYPES)
                .addTextField("Home Location ID")
                .addTextField("Capacity")
                .addDropdownField("Availability", AVAILABILITY);

        JButton addButton = new JButton("Add Resource");
        addButton.addActionListener(e -> {
            try {
                Resource resource = new Resource(
                        resourceService.nextResourceId(),
                        form.getValue("Type"),
                        Integer.parseInt(form.getValue("Home Location ID")),
                        Integer.parseInt(form.getValue("Capacity")),
                        form.getValue("Availability"),
                        null,
                        null,
                        null);
                resourceService.add(resource);
                resourceTable.setRows(resourceService.findAll());
                form.clear();
                MessagePrinter.showInfo(this, "Resource added.");
            } catch (NumberFormatException ex) {
                MessagePrinter.showError(this, "Home location and capacity must be numbers.");
            } catch (IllegalArgumentException ex) {
                MessagePrinter.showError(this, ex.getMessage());
            }
        });

        panel.add(wrapForm(form, addButton, "New Resource"), BorderLayout.SOUTH);
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
