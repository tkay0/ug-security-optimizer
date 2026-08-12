package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.Screen;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.AuditEvent;

/**
 * Activity log: the persisted audit trail with a filter by event type. Every
 * workflow write performed from this control room appears here as an event.
 */
public final class ActivityScreen extends JPanel implements Screen {

    private static final String[] COLUMNS = {
        "Time", "Event", "Entity", "Entity ID", "Actor", "Details"
    };

    private final AppContext appContext;
    private final ActivityTableModel model = new ActivityTableModel();
    private final JTable table = new JTable(model);
    private final JComboBox<String> filterBox = new JComboBox<>(
            new String[] {"ALL", "REQUEST_ASSIGNED", "REQUEST_STATUS_CHANGED",
                    "RESOURCE_STATUS_CHANGED"});
    private final JLabel summary = new JLabel();

    private AuditEvent[] dataset = new AuditEvent[0];

    public ActivityScreen(AppContext appContext) {
        this.appContext = appContext;
        setLayout(new BorderLayout(0, 12));
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel title = new JLabel("Activity Log");
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                "Persisted audit trail of assignment, request-status and "
                        + "resource-availability events");
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Event type:"));
        filters.add(filterBox);
        filterBox.addActionListener(event -> applyFilter());

        header.add(titles, BorderLayout.NORTH);
        header.add(filters, BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);

        summary.setFont(GuiTheme.FONT_SMALL);
        summary.setForeground(GuiTheme.TEXT_SECONDARY);
        return header;
    }

    private JPanel buildTablePanel() {
        table.setFont(GuiTheme.FONT_BODY);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(GuiTheme.PANEL_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(GuiTheme.SHELL_BACKGROUND_ALT);
        table.getTableHeader().setForeground(GuiTheme.TEXT_ON_DARK);
        table.getTableHeader().setFont(GuiTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(GuiTheme.SHELL_BORDER));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GuiTheme.PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void applyFilter() {
        String selected = (String) filterBox.getSelectedItem();
        String filter = "ALL".equals(selected) ? null : selected;

        AuditEvent[] visible = dataset;
        if (filter != null) {
            int matches = 0;
            for (AuditEvent event : dataset) {
                if (filter.equals(event.getEventType())) {
                    matches++;
                }
            }
            visible = new AuditEvent[matches];
            int index = 0;
            for (AuditEvent event : dataset) {
                if (filter.equals(event.getEventType())) {
                    visible[index++] = event;
                }
            }
        }

        model.setRows(visible);
        summary.setText(
                visible.length + " event(s) shown of " + dataset.length
                        + "  |  newest first");
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        model.setRows(new AuditEvent[0]);
        summary.setText("Loading activity log...");
        GuiWork.run(
                this,
                () -> appContext.loadAuditEvents(),
                events -> {
                    dataset = events;
                    applyFilter();
                },
                (error, anchor) -> {
                    dataset = new AuditEvent[0];
                    model.setRows(dataset);
                    summary.setText("Unable to load activity log: " + error.getMessage());
                });
    }

    /** Non-editable table model for audit events, newest first. */
    private static final class ActivityTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        void setRows(AuditEvent[] events) {
            setRowCount(0);
            for (AuditEvent event : events) {
                addRow(new Object[] {
                    UiFormatters.formatInstant(event.getTimestamp()),
                    UiFormatters.humanize(event.getEventType()),
                    UiFormatters.humanize(event.getEntityType()),
                    event.getEntityId(),
                    event.getActorType() == null ? "-" : event.getActorType(),
                    event.getDetails() == null ? "-" : event.getDetails()
                });
            }
        }
    }
}
