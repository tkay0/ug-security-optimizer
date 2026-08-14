package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.Screen;
import org.ugoptimizer.gui.components.EmptyPanel;
import org.ugoptimizer.gui.components.IncidentRowCard;
import org.ugoptimizer.gui.components.StatCard;
import org.ugoptimizer.gui.i18n.Messages;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.theme.HoverEffects;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.ResponseQueueBuilder;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.RequestStatus;
import org.ugoptimizer.model.ResourceAvailability;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Security operations dashboard. Every statistic is computed from the loaded
 * canonical dataset at refresh time; no value is hardcoded.
 */
public final class DashboardScreen extends JPanel implements Screen {

    private final AppContext appContext;
    private final StatCard totalCard = new StatCard(Messages.get("dashboard.total"), GuiTheme.TEXT_PRIMARY);
    private final StatCard openCard = new StatCard(Messages.get("dashboard.open"), GuiTheme.STATUS_WARN);
    private final StatCard criticalCard = new StatCard(Messages.get("dashboard.critical"), GuiTheme.STATUS_DANGER);
    private final StatCard highCard = new StatCard(Messages.get("dashboard.high"), new Color(0xE8, 0x5D, 0x1F));
    private final StatCard completedCard = new StatCard(Messages.get("dashboard.completed"), GuiTheme.STATUS_OK);
    private final StatCard cancelledCard = new StatCard(Messages.get("dashboard.cancelled"), GuiTheme.STATUS_NEUTRAL);
    private final StatCard availableCard = new StatCard(Messages.get("dashboard.available"), GuiTheme.STATUS_OK);
    private final StatCard busyCard = new StatCard(Messages.get("dashboard.busy"), GuiTheme.STATUS_INFO);

    private final JPanel queueList = new JPanel();
    private final JPanel recentList = new JPanel();
    private final JLabel queueSummary = new JLabel();
    private final JLabel recentSummary = new JLabel();

    public DashboardScreen(AppContext appContext) {
        this.appContext = appContext;
        setLayout(new BorderLayout(0, 12));
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel(Messages.get("dashboard.title"));
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                Messages.get("dashboard.subtitle"));
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        header.add(titles, BorderLayout.WEST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        JPanel kpis = new JPanel(new GridLayout(2, 4, 10, 10));
        kpis.setOpaque(false);
        kpis.add(totalCard);
        kpis.add(openCard);
        kpis.add(criticalCard);
        kpis.add(highCard);
        kpis.add(completedCard);
        kpis.add(cancelledCard);
        kpis.add(availableCard);
        kpis.add(busyCard);
        body.add(kpis, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.setOpaque(false);
        lower.add(buildQueuePanel());
        lower.add(buildRecentPanel());
        body.add(lower, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GuiTheme.PANEL_BACKGROUND);
        panel.setBorder(GuiTheme.shadowBorder());

        JLabel title = new JLabel(Messages.get("dashboard.queue"));
        title.setFont(GuiTheme.FONT_SECTION);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        queueSummary.setFont(GuiTheme.FONT_SMALL);
        queueSummary.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(queueSummary, BorderLayout.EAST);

        queueList.setLayout(new BoxLayout(queueList, BoxLayout.Y_AXIS));
        queueList.setOpaque(false);
        queueList.add(Box.createVerticalStrut(2));

        JScrollPane scroll = new JScrollPane(queueList);
        scroll.setBorder(null);
        scroll.setBackground(GuiTheme.PANEL_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRecentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GuiTheme.PANEL_BACKGROUND);
        panel.setBorder(GuiTheme.shadowBorder());

        JLabel title = new JLabel(Messages.get("dashboard.recent"));
        title.setFont(GuiTheme.FONT_SECTION);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        recentSummary.setFont(GuiTheme.FONT_SMALL);
        recentSummary.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(recentSummary, BorderLayout.EAST);

        recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));
        recentList.setOpaque(false);
        recentList.add(Box.createVerticalStrut(2));

        JScrollPane scroll = new JScrollPane(recentList);
        scroll.setBorder(null);
        scroll.setBackground(GuiTheme.PANEL_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        queueList.removeAll();
        queueList.add(EmptyPanel.loading(Messages.get("dashboard.loadingQueue")));
        recentList.removeAll();
        recentList.add(EmptyPanel.loading(Messages.get("dashboard.loadingRecent")));
        queueSummary.setText("");
        recentSummary.setText("");
        revalidate();
        repaint();

        GuiWork.run(
                this,
                () -> {
                    Location[] locations = appContext.loadLocations();
                    Resource[] resources = appContext.loadResources();
                    ServiceRequest[] requests = appContext.loadRequests();
                    return new DashboardData(locations, resources, requests);
                },
                this::render,
                (error, anchor) -> {
                    queueList.removeAll();
                    queueList.add(EmptyPanel.error(Messages.format("dashboard.errorLoading", error.getMessage())));
                    recentList.removeAll();
                    revalidate();
                    repaint();
                });
    }

    private void render(DashboardData data) {
        ServiceRequest[] requests = data.requests;
        int open = 0;
        int criticalOpen = 0;
        int highOpen = 0;
        int completed = 0;
        int cancelled = 0;
        for (ServiceRequest request : requests) {
            if (ResponseQueueBuilder.isOpen(request)) {
                open++;
            }
            if (RequestStatus.COMPLETED.name().equals(request.getStatus())) {
                completed++;
            }
            if (RequestStatus.CANCELLED.name().equals(request.getStatus())) {
                cancelled++;
            }
            if (request.getUrgency() == 5 && ResponseQueueBuilder.isOpen(request)) {
                criticalOpen++;
            }
            if (request.getUrgency() == 4 && ResponseQueueBuilder.isOpen(request)) {
                highOpen++;
            }
        }

        int available = 0;
        int busy = 0;
        for (Resource resource : data.resources) {
            if (ResourceAvailability.AVAILABLE.name().equals(resource.getAvailabilityStatus())) {
                available++;
            } else if (ResourceAvailability.BUSY.name().equals(resource.getAvailabilityStatus())) {
                busy++;
            }
        }

        totalCard.setValue(String.valueOf(requests.length));
        openCard.setValue(String.valueOf(open));
        criticalCard.setValue(String.valueOf(criticalOpen));
        highCard.setValue(String.valueOf(highOpen));
        completedCard.setValue(String.valueOf(completed));
        cancelledCard.setValue(String.valueOf(cancelled));
        availableCard.setValue(String.valueOf(available));
        busyCard.setValue(String.valueOf(busy));

        renderQueue(data.requests);
        renderRecent(data.requests);
    }

    private void renderQueue(ServiceRequest[] requests) {
        ServiceRequest[] ordered = ResponseQueueBuilder.orderedOpenRequests(requests);
        queueSummary.setText(Messages.format("dashboard.waiting", ordered.length));
        queueList.removeAll();
        if (ordered.length == 0) {
            queueList.add(EmptyPanel.empty(Messages.get("dashboard.noOpen")));
        } else {
            int shown = Math.min(ordered.length, 6);
            for (int index = 0; index < shown; index++) {
                queueList.add(new IncidentRowCard(
                        appContext, ordered[index], this::openIncident));
                queueList.add(Box.createVerticalStrut(8));
            }
        }
        queueList.revalidate();
        queueList.repaint();
    }

    private void renderRecent(ServiceRequest[] requests) {
        ServiceRequest[] sorted = requests.clone();
        Arrays.sort(sorted, (a, b) -> b.getTimeSubmitted().compareTo(a.getTimeSubmitted()));
        recentSummary.setText(Messages.format("dashboard.recorded", sorted.length));
        recentList.removeAll();
        int shown = Math.min(sorted.length, 6);
        for (int index = 0; index < shown; index++) {
            recentList.add(new IncidentRowCard(
                    appContext, sorted[index], this::openIncident));
            recentList.add(Box.createVerticalStrut(8));
        }
        recentList.revalidate();
        recentList.repaint();
    }

    private void openIncident(org.ugoptimizer.model.ServiceRequest request) {
        IncidentDetailDialog dialog = new IncidentDetailDialog(this, appContext, request);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Immutable snapshot of the dashboard's loaded datasets. */
    private static final class DashboardData {

        private final Location[] locations;
        private final Resource[] resources;
        private final ServiceRequest[] requests;

        DashboardData(Location[] locations, Resource[] resources, ServiceRequest[] requests) {
            this.locations = locations;
            this.resources = resources;
            this.requests = requests;
        }
    }
}
