package org.ugoptimizer.demo;

import java.util.Comparator;
import java.util.Objects;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.structures.heap.BinaryHeap;
import org.ugoptimizer.structures.queue.CircularQueue;
import org.ugoptimizer.structures.queue.Deque;
import org.ugoptimizer.structures.queue.FIFOQueue;

/** Builds an examiner-readable scheduling trace with the project's custom queues and heap. */
public final class SchedulingDemonstration {

    private static final int REQUIRED_REQUESTS = 4;

    /**
     * Demonstrates FIFO dispatch, circular-buffer wrap-around, urgent deque insertion, and
     * urgency-ordered heap dispatch using the first four supplied requests.
     *
     * @param requests current canonical requests in their repository order
     * @return deterministic text trace; the supplied array and requests are not mutated
     */
    public String demonstrate(ServiceRequest[] requests) {
        Objects.requireNonNull(requests, "requests cannot be null");
        if (requests.length < REQUIRED_REQUESTS) {
            throw new IllegalArgumentException("At least four requests are required for the scheduling demo");
        }
        for (int index = 0; index < REQUIRED_REQUESTS; index++) {
            Objects.requireNonNull(requests[index], "requests[" + index + "] cannot be null");
        }

        StringBuilder trace = new StringBuilder("SCHEDULING STRUCTURE DEMONSTRATION\n");
        appendFifo(trace, requests);
        appendCircular(trace, requests);
        appendDeque(trace, requests);
        appendHeap(trace, requests);
        return trace.toString();
    }

    private static void appendFifo(StringBuilder trace, ServiceRequest[] requests) {
        FIFOQueue<ServiceRequest> queue = new FIFOQueue<>();
        for (int index = 0; index < REQUIRED_REQUESTS; index++) {
            queue.enqueue(requests[index]);
        }
        trace.append("\nFIFO arrival -> dispatch: ");
        appendDrain(trace, queue);
    }

    private static void appendCircular(StringBuilder trace, ServiceRequest[] requests) {
        CircularQueue<ServiceRequest> queue = new CircularQueue<>(3);
        queue.enqueue(requests[0]);
        queue.enqueue(requests[1]);
        queue.enqueue(requests[2]);
        int removed = queue.dequeue().getRequestId();
        queue.enqueue(requests[3]);
        trace.append("\nCircular capacity=3: removed R").append(removed)
                .append(", enqueued R").append(requests[3].getRequestId())
                .append(" after rear wrapped (front 0->1, rear 2->0); remaining -> ");
        while (!queue.isEmpty()) {
            appendId(trace, queue.dequeue());
            if (!queue.isEmpty()) {
                trace.append(", ");
            }
        }
    }

    private static void appendDeque(StringBuilder trace, ServiceRequest[] requests) {
        Deque<ServiceRequest> deque = new Deque<>();
        trace.append("\nDeque rule (urgency 4-5 front, otherwise rear): ");
        for (int index = 0; index < REQUIRED_REQUESTS; index++) {
            ServiceRequest request = requests[index];
            if (request.getUrgency() >= 4) {
                deque.addFirst(request);
                trace.append("front R").append(request.getRequestId());
            } else {
                deque.addLast(request);
                trace.append("rear R").append(request.getRequestId());
            }
            if (index + 1 < REQUIRED_REQUESTS) {
                trace.append("; ");
            }
        }
        trace.append("; dispatch -> ");
        while (!deque.isEmpty()) {
            appendId(trace, deque.removeFirst());
            if (!deque.isEmpty()) {
                trace.append(", ");
            }
        }
    }

    private static void appendHeap(StringBuilder trace, ServiceRequest[] requests) {
        Comparator<ServiceRequest> priority = (left, right) -> {
            int urgency = Integer.compare(right.getUrgency(), left.getUrgency());
            if (urgency != 0) {
                return urgency;
            }
            int deadline = left.getDeadline().compareTo(right.getDeadline());
            return deadline != 0
                    ? deadline
                    : Integer.compare(left.getRequestId(), right.getRequestId());
        };
        BinaryHeap<ServiceRequest> heap = new BinaryHeap<>(priority);
        for (int index = 0; index < REQUIRED_REQUESTS; index++) {
            heap.add(requests[index]);
        }
        trace.append("\nBinary heap priority (urgency desc, deadline, ID): ");
        while (!heap.isEmpty()) {
            ServiceRequest next = heap.poll();
            trace.append("R").append(next.getRequestId())
                    .append("[u=").append(next.getUrgency()).append(']');
            if (!heap.isEmpty()) {
                trace.append(", ");
            }
        }
        trace.append('\n');
    }

    private static void appendDrain(StringBuilder trace, FIFOQueue<ServiceRequest> queue) {
        while (!queue.isEmpty()) {
            appendId(trace, queue.dequeue());
            if (!queue.isEmpty()) {
                trace.append(", ");
            }
        }
    }

    private static void appendId(StringBuilder trace, ServiceRequest request) {
        trace.append('R').append(request.getRequestId());
    }
}
