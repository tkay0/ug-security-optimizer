package org.ugoptimizer.gui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import org.ugoptimizer.model.RequestStatus;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Emergency response queue. Open incidents are ordered with the project's
 * {@link org.ugoptimizer.structures.heap.BinaryHeap} using urgency first and
 * then submission time, and the operator sees the full dispatch order with the
 * current head of queue highlighted.
 */
public final class PriorityQueueScreen extends JPanel implements Screen {

    private final AppContext appContext;
    private final JPanel queueList = new JPanel();
    private final JLabel summary = new JLabel();
    private final StatCard waitingCard = new StatCard(Messages.get("queue.waiting"), GuiTheme.STATUS_WARN);
    private final StatCard criticalCard = new StatCard(Messages.get("queue.critical"), GuiTheme.STATUS_DANGER);
    private final StatCard highCard = new StatCard(Messages.get("queue.high"), new Color(0xE8, 0x5D, 0x1F));
    private final StatCard inProgressCard = new StatCard(Messages.get("queue.inProgress"), GuiTheme.STATUS_INFO);

    public PriorityQueueScreen(AppContext appContext) {
        this.appContext = appContext;
        setLayout(new BorderLayout(0, 12));
        setBackground(GuiTheme.WORKSPACE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildQueuePanel(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel title = new JLabel(Messages.get("queue.title"));
        title.setFont(GuiTheme.FONT_TITLE);
        title.setForeground(GuiTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(
                Messages.get("queue.subtitle"));
        subtitle.setFont(GuiTheme.FONT_BODY);
        subtitle.setForeground(GuiTheme.TEXT_SECONDARY);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 10, 0));
        kpis.setOpaque(false);
        kpis.add(waitingCard);
        kpis.add(criticalCard);
        kpis.add(highCard);
        kpis.add(inProgressCard);

        header.add(titles, BorderLayout.NORTH);
        header.add(kpis, BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);

        summary.setFont(GuiTheme.FONT_SMALL);
        summary.setForeground(GuiTheme.TEXT_SECONDARY);
        return header;
    }

    private JPanel buildQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GuiTheme.PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        queueList.setLayout(new BoxLayout(queueList, BoxLayout.Y_AXIS));
        queueList.setOpaque(false);
        queueList.add(Box.createVerticalStrut(2));

        JScrollPane scroll = new JScrollPane(queueList);
        scroll.setBorder(null);
        scroll.setBackground(GuiTheme.PANEL_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void openIncident(ServiceRequest request) {
        IncidentDetailDialog dialog = new IncidentDetailDialog(this, appContext, request, this::refresh);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void refresh() {
        queueList.removeAll();
        queueList.add(EmptyPanel.loading(Messages.get("queue.loading")));
        summary.setText("");
        revalidate();
        repaint();

        GuiWork.run(
                this,
                () -> ResponseQueueBuilder.orderedOpenRequests(appContext.loadRequests()),
                ordered -> renderQueue(ordered),
                (error, anchor) -> {
                    queueList.removeAll();
                    queueList.add(EmptyPanel.error(
                            Messages.format("queue.errorLoading", error.getMessage())));
                    queueList.revalidate();
                    queueList.repaint();
                });
    }

    private void renderQueue(ServiceRequest[] ordered) {
        int critical = 0;
        int high = 0;
        int inProgress = 0;
        for (ServiceRequest request : ordered) {
            if (request.getUrgency() == 5) {
                critical++;
            }
            if (request.getUrgency() == 4) {
                high++;
            }
            if (RequestStatus.IN_PROGRESS.name().equals(request.getStatus())) {
                inProgress++;
            }
        }

        waitingCard.setValue(String.valueOf(ordered.length));
        criticalCard.setValue(String.valueOf(critical));
        highCard.setValue(String.valueOf(high));
        inProgressCard.setValue(String.valueOf(inProgress));
        summary.setText(Messages.get("queue.dispatchOrder"));

        queueList.removeAll();
        if (ordered.length == 0) {
            queueList.add(EmptyPanel.empty(Messages.get("queue.noOpen")));
        } else {
            for (int index = 0; index < ordered.length; index++) {
                ServiceRequest request = ordered[index];
                IncidentRowCard card = new IncidentRowCard(
                        appContext, request, this::openIncident);
                if (index == 0) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(GuiTheme.STATUS_DANGER, 2),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                }
                JPanel entry = new JPanel(new BorderLayout(10, 0));
                entry.setOpaque(false);
                entry.add(card, BorderLayout.CENTER);

                JLabel position = new JLabel("#" + (index + 1));
                position.setFont(GuiTheme.FONT_BODY_BOLD);
                position.setForeground(index == 0
                        ? GuiTheme.STATUS_DANGER
                        : GuiTheme.TEXT_MUTED);
                position.setPreferredSize(new Dimension(34, 20));
                position.setHorizontalAlignment(JLabel.CENTER);

                JPanel positionBox = new JPanel(new BorderLayout());
                positionBox.setOpaque(false);
                positionBox.add(position, BorderLayout.NORTH);
                if (index == 0) {
                    JLabel next = new JLabel(Messages.get("queue.next"));
                    next.setFont(GuiTheme.FONT_SMALL);
                    next.setForeground(GuiTheme.STATUS_DANGER);
                    next.setHorizontalAlignment(JLabel.CENTER);
                    positionBox.add(next, BorderLayout.SOUTH);
                }
                entry.add(positionBox, BorderLayout.WEST);

                queueList.add(entry);
                queueList.add(Box.createVerticalStrut(8));
            }
        }
        queueList.revalidate();
        queueList.repaint();
    }
}
