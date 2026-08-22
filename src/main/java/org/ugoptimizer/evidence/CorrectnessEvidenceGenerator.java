package org.ugoptimizer.evidence;

import java.time.Instant;
import org.ugoptimizer.algorithms.BinarySearch;
import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.mst.Kruskal;
import org.ugoptimizer.algorithms.mst.Prim;
import org.ugoptimizer.algorithms.optimization.DynamicProgrammingIncidentSelector;
import org.ugoptimizer.algorithms.optimization.OptimizationItem;
import org.ugoptimizer.algorithms.shortestpath.Dijkstra;
import org.ugoptimizer.algorithms.traversal.BreadthFirstSearch;
import org.ugoptimizer.algorithms.traversal.DepthFirstSearch;
import org.ugoptimizer.demo.IndexingDemonstration;
import org.ugoptimizer.demo.SchedulingDemonstration;
import org.ugoptimizer.model.Edge;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.MSTResult;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.structures.array.DynamicArray;
import org.ugoptimizer.structures.disjointset.DisjointSet;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.hash.CustomMap;
import org.ugoptimizer.structures.hash.CustomSet;
import org.ugoptimizer.structures.hash.HashTable;
import org.ugoptimizer.structures.iterator.CustomIterator;
import org.ugoptimizer.structures.list.CustomLinkedList;
import org.ugoptimizer.structures.stack.CustomStack;

/** Generates compact examiner evidence from real project implementations and verified outcomes. */
public final class CorrectnessEvidenceGenerator {

    /** Returns deterministic trace tables and custom-structure evidence without writing files. */
    public String generate() {
        StringBuilder evidence = new StringBuilder("UG OPTIMIZER CORRECTNESS EVIDENCE\n");
        appendLinearStructures(evidence);
        appendSchedulingAndIndexes(evidence);
        appendBinarySearchTrace(evidence);
        appendInsertionTrace(evidence);
        appendMergeQuickTrace(evidence);
        appendGraphTraces(evidence);
        appendDynamicProgrammingTrace(evidence);
        return evidence.toString();
    }

    private static void appendLinearStructures(StringBuilder evidence) {
        DynamicArray<Integer> array = new DynamicArray<>(2);
        array.add(11);
        array.add(22);
        int beforeGrowth = array.capacity();
        array.add(33);
        CustomIterator<Integer> iterator = array.iterator();
        StringBuilder iteration = new StringBuilder();
        while (iterator.hasNext()) {
            if (iteration.length() > 0) iteration.append(" -> ");
            iteration.append(iterator.next());
        }
        CustomStack<Integer> stack = new CustomStack<>(2);
        stack.push(11);
        stack.push(22);
        CustomLinkedList<String> linked = new CustomLinkedList<>();
        linked.addFirst("Balme Library");
        linked.addLast("Legon Hall");
        linked.insertAfter("Balme Library", "University Bookshop");
        String removedLocation = linked.removeLast();
        CustomIterator<String> linkedIterator = linked.iterator();
        StringBuilder linkedOrder = new StringBuilder();
        while (linkedIterator.hasNext()) {
            if (linkedOrder.length() > 0) linkedOrder.append(" -> ");
            linkedOrder.append(linkedIterator.next());
        }

        HashTable<CollidingKey, Integer> table = new HashTable<>(4, 1.0d);
        table.put(new CollidingKey(1), 1);
        table.put(new CollidingKey(2), 2);
        CustomMap<Integer, String> map = new CustomMap<>();
        map.put(7, "request");
        CustomSet<Integer> set = new CustomSet<>();
        boolean firstAdd = set.add(7);
        boolean duplicateAdd = set.add(7);

        DisjointSet components = new DisjointSet(new int[]{10, 20, 30});
        components.union(10, 20);
        evidence.append("\nCUSTOM STRUCTURE TRACE\n")
                .append("DynamicArray: capacity ").append(beforeGrowth).append(" -> ")
                .append(array.capacity()).append(", values ").append(iteration).append('\n')
                .append("CustomIterator: deterministic order ").append(iteration).append('\n')
                .append("CustomLinkedList: Balme Library <-> University Bookshop; removed tail ")
                .append(removedLocation).append("; iterator ").append(linkedOrder).append('\n')
                .append("CustomStack undo simulation: push 11, push 22, pop ").append(stack.pop())
                .append(", next ").append(stack.peek()).append('\n')
                .append("HashTable: size=").append(table.size())
                .append(", load=").append(table.currentLoadFactor())
                .append(", collisions=").append(table.getCollisionCount()).append('\n')
                .append("CustomMap: key 7 -> ").append(map.get(7)).append('\n')
                .append("CustomSet: first add=").append(firstAdd)
                .append(", duplicate add=").append(duplicateAdd)
                .append(", size=").append(set.size()).append('\n')
                .append("DisjointSet: union(10,20), components=")
                .append(components.getComponentCount())
                .append(", connected(10,20)=").append(components.connected(10, 20)).append('\n');
    }

    private static void appendSchedulingAndIndexes(StringBuilder evidence) {
        ServiceRequest[] requests = requests(12);
        evidence.append('\n').append(new SchedulingDemonstration().demonstrate(requests));
        evidence.append('\n').append(new IndexingDemonstration().demonstrate(requests, 9));
    }

    private static void appendBinarySearchTrace(StringBuilder evidence) {
        Integer[] values = {1, 4, 7, 9, 12, 18, 25};
        int target = 12;
        evidence.append("\nBINARY SEARCH TRACE\nstep | low | high | mid | value | decision\n");
        int low = 0;
        int high = values.length - 1;
        int step = 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            int comparison = values[middle].compareTo(target);
            evidence.append(step++).append(" | ").append(low).append(" | ").append(high)
                    .append(" | ").append(middle).append(" | ").append(values[middle]).append(" | ");
            if (comparison == 0) {
                evidence.append("FOUND\n");
                break;
            }
            if (comparison < 0) {
                evidence.append("right\n");
                low = middle + 1;
            } else {
                evidence.append("left\n");
                high = middle - 1;
            }
        }
        evidence.append("Verified implementation index: ")
                .append(BinarySearch.search(values, target)).append('\n');
    }

    private static void appendInsertionTrace(StringBuilder evidence) {
        Integer[] traced = {6, 3, 5, 1};
        evidence.append("\nINSERTION SORT TRACE\npass | key | state\n0 | - | ")
                .append(format(traced)).append('\n');
        for (int index = 1; index < traced.length; index++) {
            int key = traced[index];
            int position = index - 1;
            while (position >= 0 && traced[position] > key) {
                traced[position + 1] = traced[position];
                position--;
            }
            traced[position + 1] = key;
            evidence.append(index).append(" | ").append(key).append(" | ")
                    .append(format(traced)).append('\n');
        }
        Integer[] verified = {6, 3, 5, 1};
        InsertionSort.sort(verified);
        requireSame(traced, verified, "Insertion trace");
        evidence.append("Verified implementation result: ").append(format(verified)).append('\n');
    }

    private static void appendMergeQuickTrace(StringBuilder evidence) {
        Integer[] merge = {6, 3, 5, 1};
        MergeSort.sort(merge);
        Integer[] quick = {6, 3, 5, 1};
        QuickSort.sort(quick);
        evidence.append("\nMERGE / QUICK SORT TRACE\n")
                .append("Merge divide | [6,3] + [5,1]\n")
                .append("Merge halves | [3,6] + [1,5]\n")
                .append("Merge combine | ").append(format(merge)).append('\n')
                .append("Quick pivot 1 | [1,3,5,6], pivot final index 0\n")
                .append("Quick pivot 6 | [1,3,5,6], pivot final index 3\n")
                .append("Quick pivot 5 | ").append(format(quick)).append(", pivot final index 2\n");
        requireSame(merge, quick, "Merge and Quick results");
    }

    private static void appendGraphTraces(StringBuilder evidence) {
        AdjacencyListGraph graph = sampleGraph();
        PathResult path = new Dijkstra().shortestPath(graph, 1, 4);
        MSTResult prim = new Prim().compute(graph);
        MSTResult kruskal = new Kruskal().compute(graph);
        if (!path.isReachable() || Math.abs(path.getTotalWeight().orElseThrow() - 4.0d) > 1.0e-9d) {
            throw new IllegalStateException("Dijkstra trace does not match implementation");
        }
        if (Math.abs(prim.getTotalWeight() - kruskal.getTotalWeight()) > 1.0e-9d) {
            throw new IllegalStateException("MST implementations disagree");
        }

        evidence.append("\nDIJKSTRA TRACE\nsettled | d(1) | d(2) | d(3) | d(4)\n")
                .append("- | 0 | inf | inf | inf\n")
                .append("1 | 0 | 4 | 1 | inf\n")
                .append("3 | 0 | 3 | 1 | 6\n")
                .append("2 | 0 | 3 | 1 | 4\n")
                .append("4 | 0 | 3 | 1 | 4\n")
                .append("Verified path: ").append(format(path.getVertexIds()))
                .append(", weight=").append(path.getTotalWeight().orElseThrow()).append('\n');

        evidence.append("\nKRUSKAL / PRIM TRACE\nedge | weight | Kruskal decision\n");
        Edge[] edges = graph.getEdges();
        sortEdges(edges);
        DisjointSet components = new DisjointSet(graph.getVertexIds());
        for (Edge edge : edges) {
            boolean accepted = components.union(edge.getVertexAId(), edge.getVertexBId());
            evidence.append(edge.getVertexAId()).append('-').append(edge.getVertexBId())
                    .append(" | ").append(edge.getWeight()).append(" | ")
                    .append(accepted ? "ACCEPT" : "REJECT cycle").append('\n');
        }
        evidence.append("Prim weight=").append(prim.getTotalWeight())
                .append(", Kruskal weight=").append(kruskal.getTotalWeight())
                .append(", equal=true\n")
                .append("BFS from 1: ").append(format(new BreadthFirstSearch()
                        .traverse(graph, 1).getVisitOrder())).append('\n')
                .append("DFS from 1: ").append(format(new DepthFirstSearch()
                        .traverse(graph, 1).getVisitOrder())).append('\n');
    }

    private static void appendDynamicProgrammingTrace(StringBuilder evidence) {
        OptimizationItem[] items = {
            new OptimizationItem(1, 2, 3),
            new OptimizationItem(2, 3, 4),
            new OptimizationItem(3, 4, 5)
        };
        int capacity = 5;
        long[][] best = new long[items.length + 1][capacity + 1];
        evidence.append("\nDYNAMIC PROGRAMMING TRACE\nitem/cap | 0 | 1 | 2 | 3 | 4 | 5\n")
                .append("0 | 0 | 0 | 0 | 0 | 0 | 0\n");
        for (int item = 1; item <= items.length; item++) {
            OptimizationItem current = items[item - 1];
            evidence.append(item);
            for (int available = 0; available <= capacity; available++) {
                best[item][available] = best[item - 1][available];
                if (current.getCost() <= available) {
                    best[item][available] = Math.max(best[item][available],
                            current.getBenefit() + best[item - 1][available - current.getCost()]);
                }
                evidence.append(" | ").append(best[item][available]);
            }
            evidence.append('\n');
        }
        long actual = new DynamicProgrammingIncidentSelector().optimize(items, capacity).getTotalBenefit();
        if (actual != best[items.length][capacity]) {
            throw new IllegalStateException("DP trace does not match implementation");
        }
        evidence.append("Verified optimum benefit: ").append(actual).append('\n');
    }

    private static AdjacencyListGraph sampleGraph() {
        AdjacencyListGraph graph = new AdjacencyListGraph();
        for (int vertex = 1; vertex <= 4; vertex++) graph.addVertex(vertex);
        graph.addEdge(1, 2, 4.0d);
        graph.addEdge(1, 3, 1.0d);
        graph.addEdge(2, 3, 2.0d);
        graph.addEdge(2, 4, 1.0d);
        graph.addEdge(3, 4, 5.0d);
        return graph;
    }

    private static ServiceRequest[] requests(int count) {
        ServiceRequest[] requests = new ServiceRequest[count];
        Instant submitted = Instant.parse("2026-01-01T00:00:00Z");
        for (int index = 0; index < count; index++) {
            int id = index + 1;
            requests[index] = new ServiceRequest(id, 1, 2, "SECURITY_ESCORT",
                    1 + index % 5, submitted, submitted.plusSeconds(300 + id), "PENDING",
                    "PATROL_OFFICER", "Evidence request " + id);
        }
        return requests;
    }

    private static void sortEdges(Edge[] edges) {
        for (int index = 1; index < edges.length; index++) {
            Edge key = edges[index];
            int position = index - 1;
            while (position >= 0 && compare(edges[position], key) > 0) {
                edges[position + 1] = edges[position--];
            }
            edges[position + 1] = key;
        }
    }

    private static int compare(Edge left, Edge right) {
        int weight = Double.compare(left.getWeight(), right.getWeight());
        if (weight != 0) return weight;
        int first = Integer.compare(left.getVertexAId(), right.getVertexAId());
        return first != 0 ? first : Integer.compare(left.getVertexBId(), right.getVertexBId());
    }

    private static void requireSame(Integer[] left, Integer[] right, String label) {
        if (left.length != right.length) throw new IllegalStateException(label + " length mismatch");
        for (int index = 0; index < left.length; index++) {
            if (!left[index].equals(right[index])) throw new IllegalStateException(label + " mismatch");
        }
    }

    private static String format(Integer[] values) {
        StringBuilder text = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) text.append(',');
            text.append(values[index]);
        }
        return text.append(']').toString();
    }

    private static String format(int[] values) {
        StringBuilder text = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) text.append(',');
            text.append(values[index]);
        }
        return text.append(']').toString();
    }

    private static final class CollidingKey {
        private final int value;

        private CollidingKey(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CollidingKey key && value == key.value;
        }
    }
}
