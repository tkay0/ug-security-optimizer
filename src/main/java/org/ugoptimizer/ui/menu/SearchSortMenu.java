package org.ugoptimizer.ui.menu;

import org.ugoptimizer.algorithms.BinarySearch;
import org.ugoptimizer.algorithms.LinearSearch;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.frontend.RequestService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;
import org.ugoptimizer.ui.display.MessagePrinter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Menu for search and sort operations on incidents.
 *
 * <p>Backed by the same injected {@link RequestService} as
 * {@link RequestResourceMenu}, so both tabs show the same requests. Search
 * uses the project's own {@link BinarySearch} (exact request ID, over an
 * ID-sorted snapshot) and {@link LinearSearch} (exact category match) instead
 * of an inline scan &mdash; both algorithms only guarantee exact-equality
 * matches, so free-text substring search isn't offered here.
 */
public class SearchSortMenu extends JPanel {

    private static final String[] CATEGORIES = {
            "ACCESS_CONTROL", "CCTV_FAULT", "CROWD_CONTROL", "EMERGENCY_TRANSPORT",
            "FIRE_ALARM", "MEDICAL_EMERGENCY", "NIGHT_PATROL_REQUEST",
            "ROAD_OBSTRUCTION", "SECURITY_ESCORT", "SUSPICIOUS_ACTIVITY",
            "THEFT_REPORT", "WELFARE_CHECK"};
    private static final String[] SORT_OPTIONS = {"Request ID", "Urgency (highest first)", "Category"};

    private final RequestService requestService;
    private DataTablePanel<ServiceRequest> table;
    private JTextField idField;
    private JComboBox<String> categoryChoice;
    private JComboBox<String> sortChoice;

    public SearchSortMenu(RequestService requestService) {
        super(new BorderLayout(8, 8));
        this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        idField = new JTextField(6);
        JButton idSearchButton = new JButton("Find by ID (Binary Search)");
        categoryChoice = new JComboBox<>(CATEGORIES);
        JButton categorySearchButton = new JButton("Find by Category (Linear Search)");
        JButton resetButton = new JButton("Reset");
        sortChoice = new JComboBox<>(SORT_OPTIONS);
        JButton sortButton = new JButton("Sort");

        controls.add(new JLabel("Request ID:"));
        controls.add(idField);
        controls.add(idSearchButton);
        controls.add(categoryChoice);
        controls.add(categorySearchButton);
        controls.add(resetButton);
        controls.add(new JLabel("Sort by:"));
        controls.add(sortChoice);
        controls.add(sortButton);

        table = new DataTablePanel<>(List.of(
                new Column<>("ID", r -> String.valueOf(r.getRequestId())),
                new Column<>("Category", ServiceRequest::getCategory),
                new Column<>("Urgency", r -> String.valueOf(r.getUrgency())),
                new Column<>("Status", ServiceRequest::getStatus)
        ), requestService.findAll());

        idSearchButton.addActionListener(e -> searchById());
        categorySearchButton.addActionListener(e -> searchByCategory());
        resetButton.addActionListener(e -> table.setRows(requestService.findAll()));
        sortButton.addActionListener(e -> applySort());

        add(controls, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    private void searchById() {
        int targetId;
        try {
            targetId = Integer.parseInt(idField.getText().trim());
        } catch (NumberFormatException ex) {
            MessagePrinter.showError(this, "Request ID must be a number.");
            return;
        }

        ServiceRequest[] sortedById = requestService.findAll().toArray(new ServiceRequest[0]);
        Arrays.sort(sortedById, Comparator.comparingInt(ServiceRequest::getRequestId));

        Integer[] ids = new Integer[sortedById.length];
        for (int i = 0; i < sortedById.length; i++) {
            ids[i] = sortedById[i].getRequestId();
        }

        int index = BinarySearch.search(ids, targetId);
        if (index < 0) {
            MessagePrinter.showInfo(this, "No request with ID " + targetId + ".");
            table.setRows(List.of());
        } else {
            table.setRows(List.of(sortedById[index]));
        }
    }

    private void searchByCategory() {
        String targetCategory = (String) categoryChoice.getSelectedItem();
        ServiceRequest[] all = requestService.findAll().toArray(new ServiceRequest[0]);

        List<ServiceRequest> matches = new ArrayList<>();
        ServiceRequest[] remaining = all.clone();
        int index;
        while ((index = LinearSearch.search(categoriesOf(remaining), targetCategory)) >= 0) {
            matches.add(remaining[index]);
            remaining[index] = null;
        }
        table.setRows(matches);
    }

    private static String[] categoriesOf(ServiceRequest[] requests) {
        String[] categories = new String[requests.length];
        for (int i = 0; i < requests.length; i++) {
            categories[i] = requests[i] == null ? null : requests[i].getCategory();
        }
        return categories;
    }

    private void applySort() {
        List<ServiceRequest> sorted = new ArrayList<>(requestService.findAll());
        switch ((String) sortChoice.getSelectedItem()) {
            case "Urgency (highest first)" ->
                    sorted.sort(Comparator.comparingInt(ServiceRequest::getUrgency).reversed());
            case "Category" -> sorted.sort(Comparator.comparing(ServiceRequest::getCategory));
            default -> sorted.sort(Comparator.comparingInt(ServiceRequest::getRequestId));
        }
        table.setRows(sorted);
    }
}
