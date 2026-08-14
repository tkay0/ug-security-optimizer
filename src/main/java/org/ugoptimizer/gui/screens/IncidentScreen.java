package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.Screen;
import org.ugoptimizer.gui.components.EmptyPanel;
import org.ugoptimizer.gui.i18n.Messages;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.theme.HoverEffects;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.SearchSortEngine;
import org.ugoptimizer.gui.util.SearchSortEngine.SortAlgorithm;
import org.ugoptimizer.gui.util.SearchSortEngine.SortKey;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Incident register: the full service-request dataset with linear/binary id
 * search and the project's selection, insertion, merge and quick sorts wired
 * to the table ordering. Opening a row shows the dispatch detail dialog.
 */
public final class IncidentScreen extends JPanel implements Screen {

    private static final String[] COLUMNS = {
        "ID", "Category", "Urgency", "Source", "Destination", "Submitted", "Deadline", "Status"
    };

    private final AppContext appContext;
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<SortKey> keyBox = new JComboBox<>(SortKey.values());
    private final JComboBox<SortAlgorithm> algorithmBox = new JComboBox<>(SortAlgorithm.values());
    private final JComboBox<String> directionBox = new JComboBox<>(
            new String[] {"Ascending", "Descending"});
    private final IncidentTableModel model = new IncidentTableModel();
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel();

    private ServiceRequest[] dataset = new ServiceRequest[0];
    private SortKey sortKey = SortKey.REQUEST_ID;
    private SortAlgorithm sortAlgorithm = SortAlgorithm.SELECTION_SORT;
    private boolean ascending = true;

    public IncidentScreen(AppContext appContext) {
        this.appContext = appContext;
        setLayout(new BorderLayout());
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        installKeyboardShortcuts();
    }

    private void installKeyboardShortcuts() {
        JComponent root = (JComponent) getTopLevelAncestor();
        if (root == null) {
            return;
        }
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        root.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
            }
        });

        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openSelected");
        table.getActionMap().put("openSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelected();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel title = new JLabel(Messages.get("incidents.title"));
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                Messages.get("incidents.subtitle"));
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);

        searchField.setToolTipText(Messages.get("incidents.searchById"));
        searchField.getAccessibleContext().setAccessibleName(Messages.get("incidents.find"));
        searchField.addActionListener(event -> searchById());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                applyFilters();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                applyFilters();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                applyFilters();
            }
        });

        JButton searchButton = new JButton(Messages.get("incidents.searchById"));
        searchButton.getAccessibleContext().setAccessibleName(Messages.get("incidents.searchById"));
        searchButton.addActionListener(event -> searchById());
        HoverEffects.applyButtonHover(searchButton, GuiTheme.SHELL_BACKGROUND_ALT, new Color(34, 42, 58), GuiTheme.TEXT_ON_DARK, GuiTheme.TEXT_ON_DARK);

        keyBox.addActionListener(event -> applySort());
        algorithmBox.addActionListener(event -> applySort());
        directionBox.addActionListener(event -> applySort());

        controls.add(new JLabel(Messages.get("incidents.find")));
        controls.add(searchField);
        controls.add(searchButton);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(new JLabel(Messages.get("incidents.sortBy")));
        controls.add(keyBox);
        controls.add(algorithmBox);
        controls.add(directionBox);

        header.add(titles, BorderLayout.NORTH);
        header.add(controls, BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);

        summary.setFont(GuiTheme.FONT_SMALL);
        summary.setForeground(GuiTheme.TEXT_SECONDARY);
        return header;
    }

    private JPanel buildTablePanel() {
        table.setFont(GuiTheme.FONT_BODY);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(GuiTheme.PANEL_BORDER);
        table.setBackground(GuiTheme.PANEL_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(GuiTheme.SHELL_BACKGROUND_ALT);
        table.getTableHeader().setForeground(GuiTheme.TEXT_ON_DARK);
        table.getTableHeader().setFont(GuiTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(GuiTheme.SHELL_BORDER));

        table.getAccessibleContext().setAccessibleName(Messages.get("incidents.title"));
        table.getAccessibleContext().setAccessibleDescription(Messages.get("incidents.subtitle"));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openSelected();
                }
            }
        });

        JButton openButton = new JButton(Messages.get("incidents.viewDispatch"));
        openButton.getAccessibleContext().setAccessibleName(Messages.get("incidents.viewDispatch"));
        openButton.addActionListener(event -> openSelected());
        HoverEffects.applyButtonHover(openButton, GuiTheme.ACCENT, GuiTheme.ACCENT_DARK, Color.WHITE, Color.WHITE);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        buttonBar.setOpaque(false);
        buttonBar.add(openButton);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonBar, BorderLayout.SOUTH);
        return panel;
    }

    private void searchById() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            applyFilters();
            return;
        }
        int requestId;
        try {
            requestId = Integer.parseInt(text);
        } catch (NumberFormatException failure) {
            applyFilters();
            return;
        }
        ServiceRequest[] ordered = dataset;
        Integer[] ids = new Integer[ordered.length];
        for (int index = 0; index < ordered.length; index++) {
            ids[index] = ordered[index].getRequestId();
        }
        int index;
        if (SearchSortEngine.idsSorted(ids)) {
            index = SearchSortEngine.findIndexBinary(ids, requestId);
        } else {
            index = SearchSortEngine.findIndexLinear(ids, requestId);
        }
        if (index < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get("incidents.noResults"),
                    Messages.get("incidents.searchById"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        applyFilters();
        int row = rowOf(ordered[index]);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        }
    }

    private void applySort() {
        sortKey = (SortKey) keyBox.getSelectedItem();
        sortAlgorithm = (SortAlgorithm) algorithmBox.getSelectedItem();
        ascending = "Ascending".equals(directionBox.getSelectedItem());
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        ServiceRequest[] sorted = SearchSortEngine.sort(
                dataset, sortKey, ascending, sortAlgorithm, appContext::locationName);
        ServiceRequest[] visible = sorted;
        if (!query.isEmpty()) {
            int matches = 0;
            for (ServiceRequest request : sorted) {
                if (matchesText(request, query)) {
                    matches++;
                }
            }
            visible = new ServiceRequest[matches];
            int index = 0;
            for (ServiceRequest request : sorted) {
                if (matchesText(request, query)) {
                    visible[index++] = request;
                }
            }
        }
        model.setRows(visible, appContext::locationName);
        summary.setText(
                visible.length + " incident(s) shown of " + dataset.length
                        + "  |  ordered by " + sortKey.getLabel() + " using "
                        + sortAlgorithm.getLabel());
    }

    private boolean matchesText(ServiceRequest request, String query) {
        return String.valueOf(request.getRequestId()).contains(query)
                || request.getCategory().toLowerCase().contains(query)
                || appContext.locationName(request.getSourceLocationId()).toLowerCase().contains(query)
                || appContext.locationName(request.getDestinationLocationId()).toLowerCase().contains(query)
                || request.getStatus().toLowerCase().contains(query);
    }

    private int rowOf(ServiceRequest request) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if ((Integer) model.getValueAt(row, 0) == request.getRequestId()) {
                return row;
            }
        }
        return -1;
    }

    private ServiceRequest selectedRequest() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this, "Select an incident row first.", "Incident Register",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int requestId = (Integer) model.getValueAt(row, 0);
        for (ServiceRequest request : dataset) {
            if (request.getRequestId() == requestId) {
                return request;
            }
        }
        return null;
    }

    private void openSelected() {
        ServiceRequest request = selectedRequest();
        if (request == null) {
            return;
        }
        IncidentDetailDialog dialog = new IncidentDetailDialog(this, appContext, request, this::refresh);
        dialog.setVisible(true);
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        model.setRows(new ServiceRequest[0], appContext::locationName);
        summary.setText("Loading incidents...");
        GuiWork.run(
                this,
                () -> appContext.loadRequests(),
                requests -> {
                    dataset = requests;
                    applyFilters();
                },
                (error, anchor) -> {
                    dataset = new ServiceRequest[0];
                    model.setRows(dataset, appContext::locationName);
                    summary.setText("Unable to load incidents: " + error.getMessage());
                });
    }

    /** Non-editable table model for service requests. */
    private static final class IncidentTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        void setRows(ServiceRequest[] requests, java.util.function.Function<Integer, String> names) {
            setRowCount(0);
            for (ServiceRequest request : requests) {
                addRow(new Object[] {
                    request.getRequestId(),
                    UiFormatters.humanize(request.getCategory()),
                    request.getUrgency(),
                    names.apply(request.getSourceLocationId()),
                    names.apply(request.getDestinationLocationId()),
                    UiFormatters.formatInstant(request.getTimeSubmitted()),
                    UiFormatters.formatInstant(request.getDeadline()),
                    UiFormatters.humanize(request.getStatus())
                });
            }
        }
    }
}
