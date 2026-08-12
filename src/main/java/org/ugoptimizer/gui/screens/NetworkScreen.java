package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.ugoptimizer.algorithms.traversal.BreadthFirstSearch;
import org.ugoptimizer.algorithms.traversal.DepthFirstSearch;
import org.ugoptimizer.gui.AppContext;
import org.ugoptimizer.gui.Screen;
import org.ugoptimizer.gui.components.EmptyPanel;
import org.ugoptimizer.gui.components.StatCard;
import org.ugoptimizer.gui.theme.GuiTheme;
import org.ugoptimizer.gui.util.GuiWork;
import org.ugoptimizer.gui.util.UiFormatters;
import org.ugoptimizer.model.Location;
import org.ugoptimizer.model.Road;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.WeightedGraph;

/**
 * Campus network view: the location and road tables from the canonical dataset
 * plus the graph built by the persistence loader. A connectivity check runs the
 * project's BFS and DFS from the security operations hub and reports reachability,
 * so graph traversal algorithms are exercised against real data.
 */
public final class NetworkScreen extends JPanel implements Screen {

    private final AppContext appContext;
    private final LocationTableModel locationModel = new LocationTableModel();
    private final RoadTableModel roadModel = new RoadTableModel();
    private final JTable locationTable = new JTable(locationModel);
    private final JTable roadTable = new JTable(roadModel);
    private final JLabel summary = new JLabel();
    private final StatCard nodeCard = new StatCard("Locations", GuiTheme.STATUS_INFO);
    private final StatCard edgeCard = new StatCard("Roads", GuiTheme.TEXT_PRIMARY);
    private final StatCard blockedCard = new StatCard("Blocked", GuiTheme.STATUS_DANGER);
    private final StatCard reachCard = new StatCard("Reachable from hub", GuiTheme.STATUS_OK);

    private Location[] locations = new Location[0];
    private Road[] roads = new Road[0];

    public NetworkScreen(AppContext appContext) {
        this.appContext = appContext;
        setLayout(new BorderLayout(0, 12));
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTables(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel title = new JLabel("Campus Network");
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                "The campus road graph loaded from the canonical dataset; "
                        + "reachability uses BFS and DFS from the hub");
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 10, 0));
        kpis.setOpaque(false);
        kpis.add(nodeCard);
        kpis.add(edgeCard);
        kpis.add(blockedCard);
        kpis.add(reachCard);

        header.add(titles, BorderLayout.NORTH);
        header.add(kpis, BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);

        summary.setFont(GuiTheme.FONT_SMALL);
        summary.setForeground(GuiTheme.TEXT_SECONDARY);
        return header;
    }

    private JPanel buildTables() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setOpaque(false);

        panel.add(buildTablePanel(locationTable, "Campus Locations", new String[] {
            "ID", "Name", "Area", "Type", "X", "Y", "Hours"
        }));
        panel.add(buildTablePanel(roadTable, "Campus Roads", new String[] {
            "ID", "Route", "From", "To", "Distance (km)", "Travel (min)", "Condition", "Blocked"
        }));
        return panel;
    }

    private JPanel buildTablePanel(JTable table, String heading, String[] columns) {
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
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel(heading);
        title.setFont(GuiTheme.FONT_SECTION);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.setOpaque(false);
        headerRow.add(title);

        panel.add(headerRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void renderLocations(Location[] allLocations) {
        locations = allLocations;
        locationModel.setRows(allLocations);
        nodeCard.setValue(String.valueOf(allLocations.length));
    }

    private void renderRoads(Road[] allRoads, WeightedGraph graph, Location[] allLocations) {
        roads = allRoads;
        roadModel.setRows(allRoads, appContext::locationName);
        edgeCard.setValue(String.valueOf(graph.getEdgeCount()));

        int blocked = 0;
        for (Road road : allRoads) {
            if (road.isBlocked()) {
                blocked++;
            }
        }
        blockedCard.setValue(String.valueOf(blocked));

        int hubId = hubLocationId(allLocations);
        TraversalResult breadthFirst = new BreadthFirstSearch().traverse(graph, hubId);
        TraversalResult depthFirst = new DepthFirstSearch().traverse(graph, hubId);
        reachCard.setValue(breadthFirst.getVisitedCount() + "/" + graph.getVertexCount());

        summary.setText(
                "Hub: " + appContext.locationName(hubId)
                        + "  |  BFS reachable " + breadthFirst.getVisitedCount()
                        + " (" + breadthFirst.getStatus().name().toLowerCase() + ")"
                        + "  |  DFS reachable " + depthFirst.getVisitedCount()
                        + "  |  roads recorded " + allRoads.length);
    }

    private int hubLocationId(Location[] allLocations) {
        for (Location location : allLocations) {
            if ("HEADQUARTERS".equalsIgnoreCase(location.getLocationType())
                    || location.getName().toUpperCase().contains("SECURITY")) {
                return location.getLocationId();
            }
        }
        return allLocations.length == 0 ? -1 : allLocations[0].getLocationId();
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        summary.setText("Loading campus network...");
        locationModel.setRows(new Location[0]);
        roadModel.setRows(new Road[0], appContext::locationName);

        GuiWork.run(
                this,
                () -> {
                    Location[] allLocations = appContext.loadLocations();
                    Road[] allRoads = appContext.loadRoads();
                    WeightedGraph graph = appContext.loadCampusGraph();
                    return new NetworkData(allLocations, allRoads, graph);
                },
                data -> {
                    renderLocations(data.locations);
                    renderRoads(data.roads, data.graph, data.locations);
                },
                (error, anchor) -> {
                    locationModel.setRows(new Location[0]);
                    roadModel.setRows(new Road[0], appContext::locationName);
                    summary.setText("Unable to load campus network: " + error.getMessage());
                });
    }

    private static final class NetworkData {

        private final Location[] locations;
        private final Road[] roads;
        private final WeightedGraph graph;

        NetworkData(Location[] locations, Road[] roads, WeightedGraph graph) {
            this.locations = locations;
            this.roads = roads;
            this.graph = graph;
        }
    }

    private static final class LocationTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        void setRows(Location[] locations) {
            setRowCount(0);
            for (Location location : locations) {
                addRow(new Object[] {
                    location.getLocationId(),
                    location.getName(),
                    location.getArea(),
                    UiFormatters.humanize(location.getLocationType()),
                    location.getXCoord(),
                    location.getYCoord(),
                    location.getOperatingHours() == null ? "-" : location.getOperatingHours()
                });
            }
        }
    }

    private static final class RoadTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        void setRows(Road[] roads, java.util.function.Function<Integer, String> names) {
            setRowCount(0);
            for (Road road : roads) {
                addRow(new Object[] {
                    road.getRoadId(),
                    road.getRouteLabel(),
                    names.apply(road.getFromLocationId()),
                    names.apply(road.getToLocationId()),
                    String.format("%.2f", road.getDistanceKm()),
                    String.format("%.1f", road.getTravelTimeMin()),
                    String.format("%.1f", road.getConditionWeight()),
                    road.isBlocked() ? "Blocked" : "Open"
                });
            }
        }
    }
}
