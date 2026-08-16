package org.ugoptimizer.ui.menu;

import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.service.PriorityService;
import org.ugoptimizer.ui.display.Column;
import org.ugoptimizer.ui.display.DataTablePanel;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Emergency dispatch priority queue: PENDING requests ordered by the
 * project's own {@link org.ugoptimizer.structures.heap.BinaryHeap}, highest
 * urgency first and earliest submission time as the tiebreaker.
 *
 * <p>This is a derived view over the shared {@link PriorityService} /
 * {@code RequestService} data, not its own data source &mdash; advancing or
 * cancelling a request on the Dispatch Workflow tab, or adding a new one on
 * Requests &amp; Resources, changes what belongs here. This screen does not
 * poll for changes automatically; click Refresh after making changes on
 * another tab.
 */
public class PriorityQueueMenu extends JPanel {

    private final PriorityService priorityService;
    private final DataTablePanel<RankedRequest> table;

    public PriorityQueueMenu(PriorityService priorityService) {
        super(new BorderLayout(8, 8));
        this.priorityService = Objects.requireNonNull(priorityService, "priorityService cannot be null");

        table = new DataTablePanel<>(List.of(
                new Column<>("Rank", r -> String.valueOf(r.rank())),
                new Column<>("Request ID", r -> String.valueOf(r.request().getRequestId())),
                new Column<>("Category", r -> r.request().getCategory()),
                new Column<>("Urgency", r -> String.valueOf(r.request().getUrgency())),
                new Column<>("Submitted", r -> r.request().getTimeSubmitted().toString())
        ), List.of());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.add(refreshButton);

        add(controls, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);

        refresh();
    }

    private void refresh() {
        List<ServiceRequest> ordered = priorityService.priorityOrder();
        List<RankedRequest> ranked = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            ranked.add(new RankedRequest(i + 1, ordered.get(i)));
        }
        table.setRows(ranked);
    }

    private record RankedRequest(int rank, ServiceRequest request) {
    }
}
