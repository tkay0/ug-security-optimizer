package org.ugoptimizer.demo;

import java.util.Objects;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.structures.index.BTree;
import org.ugoptimizer.structures.tree.BinarySearchTree;
import org.ugoptimizer.structures.tree.RedBlackTree;
import org.ugoptimizer.structures.tree.TreeEntry;

/** Builds search and balancing evidence from the project's custom ordered indexes. */
public final class IndexingDemonstration {

    private static final int DISPLAY_LIMIT = 20;

    /**
     * Indexes current requests by ID in a BST, red-black tree, and B-tree, then searches all
     * three structures for {@code requestId}. Comparisons made by each real search are traced.
     */
    public String demonstrate(ServiceRequest[] requests, int requestId) {
        Objects.requireNonNull(requests, "requests cannot be null");
        BinarySearchTree<TracingKey, ServiceRequest> bst = new BinarySearchTree<>();
        RedBlackTree<TracingKey, ServiceRequest> redBlack = new RedBlackTree<>();
        BTree<TracingKey, ServiceRequest> btree = new BTree<>();
        for (int index = 0; index < requests.length; index++) {
            ServiceRequest request = Objects.requireNonNull(
                    requests[index], "requests[" + index + "] cannot be null");
            TracingKey key = new TracingKey(request.getRequestId(), null);
            bst.put(key, request);
            redBlack.put(key, request);
            btree.insert(key, request);
        }

        SearchTrace bstTrace = new SearchTrace(requests.length * 3 + 8);
        SearchTrace redBlackTrace = new SearchTrace(requests.length * 3 + 8);
        SearchTrace btreeTrace = new SearchTrace(requests.length * 3 + 8);
        ServiceRequest bstMatch = bst.get(new TracingKey(requestId, bstTrace)).orElse(null);
        ServiceRequest redBlackMatch = redBlack.get(
                new TracingKey(requestId, redBlackTrace)).orElse(null);
        ServiceRequest btreeMatch = btree.search(new TracingKey(requestId, btreeTrace));

        StringBuilder report = new StringBuilder("REQUEST INDEX DEMONSTRATION\n")
                .append("Records indexed: ").append(requests.length).append('\n')
                .append("BST search path: ").append(bstTrace).append('\n')
                .append("Red-black search path: ").append(redBlackTrace).append('\n')
                .append("B-tree search path: ").append(btreeTrace).append('\n')
                .append("Match agreement: ")
                .append(sameMatch(bstMatch, redBlackMatch, btreeMatch) ? "YES" : "NO")
                .append(" (request ").append(requestId).append(")\n")
                .append("Found: ").append(bstMatch != null).append('\n')
                .append("BST height: ").append(bst.height()).append('\n')
                .append("Red-black height: ").append(redBlack.height())
                .append(" (balanced under ordered insertion)\n")
                .append("Red-black balancing events: left rotations=")
                .append(redBlack.getLeftRotationCount())
                .append(", right rotations=").append(redBlack.getRightRotationCount())
                .append(", insertion recolours=").append(redBlack.getInsertionRecolorCount())
                .append('\n')
                .append("B-tree height: ").append(btree.height())
                .append(", root keys: ").append(btree.rootKeyCount())
                .append(", root split occurred: ").append(!btree.isRootLeaf()).append('\n')
                .append("BST in-order IDs: ");
        appendInOrder(report, bst.entriesInOrder());
        return report.toString();
    }

    private static boolean sameMatch(ServiceRequest first, ServiceRequest second, ServiceRequest third) {
        if (first == null || second == null || third == null) {
            return first == null && second == null && third == null;
        }
        return first.getRequestId() == second.getRequestId()
                && first.getRequestId() == third.getRequestId();
    }

    private static void appendInOrder(
            StringBuilder report, TreeEntry<TracingKey, ServiceRequest>[] entries) {
        int displayed = Math.min(entries.length, DISPLAY_LIMIT);
        for (int index = 0; index < displayed; index++) {
            if (index > 0) {
                report.append(", ");
            }
            report.append(entries[index].getKey().value);
        }
        if (displayed < entries.length) {
            report.append(" ... (").append(entries.length).append(" total)");
        }
        report.append('\n');
    }

    private static final class TracingKey implements Comparable<TracingKey> {
        private final int value;
        private final SearchTrace trace;

        private TracingKey(int value, SearchTrace trace) {
            this.value = value;
            this.trace = trace;
        }

        @Override
        public int compareTo(TracingKey other) {
            if (trace != null) {
                trace.add(other.value);
            }
            return Integer.compare(value, other.value);
        }
    }

    private static final class SearchTrace {
        private final int[] comparedIds;
        private int count;

        private SearchTrace(int capacity) {
            comparedIds = new int[capacity];
        }

        private void add(int requestId) {
            if (count < comparedIds.length) {
                comparedIds[count++] = requestId;
            }
        }

        @Override
        public String toString() {
            if (count == 0) {
                return "(empty index)";
            }
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < count; index++) {
                if (index > 0) {
                    result.append(" -> ");
                }
                result.append(comparedIds[index]);
            }
            return result.toString();
        }
    }
}
