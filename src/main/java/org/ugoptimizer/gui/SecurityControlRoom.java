package org.ugoptimizer.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import org.ugoptimizer.gui.screens.ActivityScreen;
import org.ugoptimizer.gui.screens.DashboardScreen;
import org.ugoptimizer.gui.screens.IncidentScreen;
import org.ugoptimizer.gui.screens.NetworkScreen;
import org.ugoptimizer.gui.screens.PriorityQueueScreen;
import org.ugoptimizer.gui.screens.ResourceScreen;
import org.ugoptimizer.gui.theme.GuiTheme;

/**
 * Application shell: header bar, navigation sidebar, screen content area and
 * a status bar. Every major screen is hosted inside this single reusable
 * layout so the control-room structure stays consistent.
 */
public final class SecurityControlRoom extends JFrame {

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss");

    private final AppContext appContext;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final Map<String, Screen> screens;
    private final Map<String, JButton> navButtons;
    private final JLabel clockLabel;
    private final JLabel statusLabel;
    private String currentKey = "dashboard";

    public SecurityControlRoom(AppContext appContext) {
        super("UG Campus Security & Emergency Response Management System");
        this.appContext = appContext;
        this.contentLayout = new CardLayout();
        this.contentPanel = new JPanel(contentLayout);
        this.screens = new LinkedHashMap<>();
        this.navButtons = new LinkedHashMap<>();
        this.clockLabel = new JLabel();
        this.statusLabel = new JLabel("Connecting to database...");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        registerScreens();
        navigate("dashboard");

        Timer clockTimer = new Timer(1000, event -> updateClock());
        clockTimer.start();

        refreshDatabaseStatus();
        installKeyboardShortcuts();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiTheme.SHELL_BACKGROUND);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GuiTheme.SHELL_BORDER));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        brand.setOpaque(false);

        JLabel mark = new JLabel("UG");
        mark.setOpaque(true);
        mark.setBackground(GuiTheme.ACCENT);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 16));
        mark.setHorizontalAlignment(JLabel.CENTER);
        mark.setPreferredSize(new Dimension(40, 40));
        mark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("UG SECURITY OPERATIONS");
        appName.setForeground(GuiTheme.TEXT_ON_DARK);
        appName.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel subtitle = new JLabel("Campus Security & Emergency Response Management System");
        subtitle.setForeground(GuiTheme.TEXT_ON_DARK_MUTED);
        subtitle.setFont(GuiTheme.FONT_SMALL);

        titles.add(appName);
        titles.add(subtitle);

        brand.add(mark);
        brand.add(titles);

        JPanel session = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        session.setOpaque(false);

        JLabel operator = new JLabel("OPERATOR SESSION");
        operator.setForeground(GuiTheme.TEXT_ON_DARK_MUTED);
        operator.setFont(GuiTheme.FONT_SMALL);

        JLabel operatorValue = new JLabel("Dispatch Operator");
        operatorValue.setForeground(GuiTheme.TEXT_ON_DARK);
        operatorValue.setFont(GuiTheme.FONT_BODY_BOLD);

        clockLabel.setForeground(GuiTheme.TEXT_ON_DARK);
        clockLabel.setFont(GuiTheme.FONT_BODY);
        updateClock();

        session.add(operator);
        session.add(operatorValue);
        session.add(new JSeparator(JSeparator.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 22));
                setForeground(GuiTheme.SHELL_BORDER);
            }
        });
        session.add(clockLabel);

        header.add(brand, BorderLayout.WEST);
        header.add(session, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(GuiTheme.SHELL_BACKGROUND_ALT);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GuiTheme.SHELL_BORDER));
        sidebar.setPreferredSize(new Dimension(210, 0));

        JLabel navTitle = new JLabel("CONTROL PANEL");
        navTitle.setForeground(GuiTheme.TEXT_ON_DARK_MUTED);
        navTitle.setFont(GuiTheme.FONT_SMALL);
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        navTitle.setBorder(new EmptyBorder(14, 16, 8, 16));
        sidebar.add(navTitle);

        addNavButton(sidebar, "dashboard", "Dashboard");
        addNavButton(sidebar, "incidents", "Incidents");
        addNavButton(sidebar, "queue", "Response Queue");
        addNavButton(sidebar, "resources", "Resources");
        addNavButton(sidebar, "network", "Campus Network");
        addNavButton(sidebar, "activity", "Activity Log");

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String key, String label) {
        JButton button = new JButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setHorizontalAlignment(JButton.LEADING);
        button.setBorder(new EmptyBorder(0, 16, 0, 16));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> navigate(key));
        button.getAccessibleContext().setAccessibleName("Navigate to " + label);
        button.getAccessibleContext().setAccessibleDescription("Switch to the " + label + " screen");

        Color defaultBg = GuiTheme.SHELL_BACKGROUND_ALT;
        Color hoverBg = new Color(34, 42, 58);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!key.equals(currentKey)) {
                    button.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!key.equals(currentKey)) {
                    button.setBackground(defaultBg);
                }
            }
        });

        navButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private JPanel buildContent() {
        contentPanel.setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        return contentPanel;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(GuiTheme.SHELL_BACKGROUND);
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiTheme.SHELL_BORDER));

        statusLabel.setBorder(new EmptyBorder(4, 12, 4, 12));
        statusLabel.setForeground(GuiTheme.TEXT_ON_DARK_MUTED);
        statusLabel.setFont(GuiTheme.FONT_SMALL);

        JLabel source = new JLabel("Canonical dataset: SQLite  |  Actor: DISPATCH_OPERATOR");
        source.setBorder(new EmptyBorder(4, 12, 4, 12));
        source.setForeground(GuiTheme.TEXT_ON_DARK_MUTED);
        source.setFont(GuiTheme.FONT_SMALL);

        status.add(statusLabel, BorderLayout.WEST);
        status.add(source, BorderLayout.EAST);
        return status;
    }

    private void registerScreens() {
        addScreen("dashboard", new DashboardScreen(appContext));
        addScreen("incidents", new IncidentScreen(appContext));
        addScreen("queue", new PriorityQueueScreen(appContext));
        addScreen("resources", new ResourceScreen(appContext));
        addScreen("network", new NetworkScreen(appContext));
        addScreen("activity", new ActivityScreen(appContext));
    }

    private void addScreen(String key, Screen screen) {
        screens.put(key, screen);
        contentPanel.add(screen.asComponent(), key);
    }

    private void navigate(String key) {
        Screen screen = screens.get(key);
        if (screen == null) {
            return;
        }
        currentKey = key;
        contentLayout.show(contentPanel, key);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(key);
            entry.getValue().setBackground(active
                    ? GuiTheme.ACCENT
                    : GuiTheme.SHELL_BACKGROUND_ALT);
            entry.getValue().setForeground(active
                    ? Color.WHITE
                    : GuiTheme.TEXT_ON_DARK);
        }
        screen.refresh();
    }

    private void installKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();

        String[] navKeys = {"dashboard", "incidents", "queue", "resources", "network", "activity"};
        for (int i = 0; i < navKeys.length; i++) {
            final String key = navKeys[i];
            final int index = i;
            KeyStroke keyStroke = KeyStroke.getKeyStroke(
                    KeyEvent.VK_1 + index, KeyEvent.ALT_DOWN_MASK);
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "nav" + index);
            rootPane.getActionMap().put("nav" + index, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    navigate(key);
                }
            });
        }

        KeyStroke refreshKey = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(refreshKey, "refresh");
        rootPane.getActionMap().put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshCurrentScreen();
            }
        });

        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKey, "close");
        rootPane.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new java.awt.event.WindowEvent(
                        SecurityControlRoom.this, java.awt.event.WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private void refreshCurrentScreen() {
        Screen screen = screens.get(currentKey);
        if (screen != null) {
            screen.refresh();
        }
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    private void refreshDatabaseStatus() {
        org.ugoptimizer.gui.util.GuiWork.run(
                this,
                () -> {
                    if (appContext.getDatabaseManager().getDatabasePath().toFile().exists()) {
                        return "Database online: " + appContext.getDatabaseManager()
                                .getDatabasePath().getFileName();
                    }
                    return "Database not yet initialized";
                },
                message -> statusLabel.setText(message),
                (error, anchor) -> statusLabel.setText("Database unavailable"));
    }
}
