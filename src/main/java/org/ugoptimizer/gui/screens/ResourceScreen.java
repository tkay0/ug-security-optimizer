package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.Screen;
import org.ugoptimizer.gui.components.StatCard;
import org.ugoptimizer.gui.i18n.Messages;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.theme.HoverEffects;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ResourceAvailability;

/**
 * Dispatchable resource register with live availability counts. Opening a row
 * shows the resource detail dialog where availability can be updated (each
 * change is persisted atomically with an audit event).
 */
public final class ResourceScreen extends JPanel implements Screen {

    private static final String[] COLUMNS = {
        "ID", "Type", "Capacity", "Home", "Current", "Availability", "Shift"
    };

    private final AppContext appContext;
    private final ResourceTableModel model = new ResourceTableModel();
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel();
    private final StatCard availableCard = new StatCard(Messages.get("resources.available"), GuiTheme.STATUS_OK);
    private final StatCard busyCard = new StatCard(Messages.get("resources.busy"), GuiTheme.STATUS_INFO);
    private final StatCard maintenanceCard = new StatCard(Messages.get("resources.maintenance"), GuiTheme.STATUS_WARN);
    private final StatCard offDutyCard = new StatCard(Messages.get("resources.offDuty"), GuiTheme.STATUS_NEUTRAL);

    private Resource[] dataset = new Resource[0];

    public ResourceScreen(AppContext appContext) {
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

        JLabel title = new JLabel(Messages.get("resources.title"));
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                Messages.get("resources.subtitle"));
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 10, 0));
        kpis.setOpaque(false);
        kpis.add(availableCard);
        kpis.add(busyCard);
        kpis.add(maintenanceCard);
        kpis.add(offDutyCard);

        header.add(titles, BorderLayout.NORTH);
        header.add(kpis, BorderLayout.CENTER);
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
        table.setBackground(GuiTheme.PANEL_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(GuiTheme.SHELL_BACKGROUND_ALT);
        table.getTableHeader().setForeground(GuiTheme.TEXT_ON_DARK);
        table.getTableHeader().setFont(GuiTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(GuiTheme.SHELL_BORDER));

        table.getAccessibleContext().setAccessibleName(Messages.get("resources.title"));
        table.getAccessibleContext().setAccessibleDescription(
                Messages.get("resources.title") + " - " + Messages.get("resources.selectFirst"));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openSelected();
                }
            }
        });

        table.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "openSelected");
        table.getActionMap().put("openSelected", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openSelected();
            }
        });

        JButton openButton = new JButton(Messages.get("resources.view"));
        openButton.addActionListener(event -> openSelected());
        openButton.getAccessibleContext().setAccessibleName(Messages.get("resources.view"));
        openButton.getAccessibleContext().setAccessibleDescription(
                Messages.get("resources.view") + " - " + Messages.get("resources.selectFirst"));
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

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            javax.swing.JOptionPane.showMessageDialog(
                    this, Messages.get("resources.selectFirst"), Messages.get("resources.title"),
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int resourceId = (Integer) model.getValueAt(row, 0);
        for (Resource resource : dataset) {
            if (resource.getResourceId() == resourceId) {
                ResourceDetailDialog dialog =
                        new ResourceDetailDialog(this, appContext, resource, this::refresh);
                dialog.setVisible(true);
                return;
            }
        }
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        model.setRows(new Resource[0], appContext::locationName);
        summary.setText(Messages.get("resources.loading"));
        GuiWork.run(
                this,
                () -> appContext.loadResources(),
                resources -> {
                    dataset = resources;
                    render(resources);
                },
                (error, anchor) -> {
                    dataset = new Resource[0];
                    model.setRows(dataset, appContext::locationName);
                    summary.setText(Messages.format("resources.errorLoading", error.getMessage()));
                });
    }

    private void render(Resource[] resources) {
        int available = 0;
        int busy = 0;
        int maintenance = 0;
        int offDuty = 0;
        for (Resource resource : resources) {
            switch (resource.getAvailabilityStatus()) {
                case "AVAILABLE" -> available++;
                case "BUSY" -> busy++;
                case "MAINTENANCE" -> maintenance++;
                case "OFF_DUTY" -> offDuty++;
                default -> {
                }
            }
        }
        availableCard.setValue(String.valueOf(available));
        busyCard.setValue(String.valueOf(busy));
        maintenanceCard.setValue(String.valueOf(maintenance));
        offDutyCard.setValue(String.valueOf(offDuty));
        summary.setText(Messages.format("resources.summary", resources.length, available));

        model.setRows(resources, appContext::locationName);
    }

    /** Non-editable table model for resources. */
    private static final class ResourceTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        void setRows(Resource[] resources, java.util.function.Function<Integer, String> names) {
            setRowCount(0);
            for (Resource resource : resources) {
                addRow(new Object[] {
                    resource.getResourceId(),
                    UiFormatters.humanize(resource.getResourceType()),
                    resource.getCapacity(),
                    names.apply(resource.getHomeLocationId()),
                    resource.getCurrentLocationId() == null
                            ? Messages.get("resources.homeBase")
                            : names.apply(resource.getCurrentLocationId()),
                    UiFormatters.humanize(resource.getAvailabilityStatus()),
                    UiFormatters.shiftText(
                            resource.getShiftStart() == null
                                    ? null
                                    : resource.getShiftStart().toString(),
                            resource.getShiftEnd() == null
                                    ? null
                                    : resource.getShiftEnd().toString())
                });
            }
        }
    }
}
