package org.ugoptimizer.performance;

import java.time.Instant;
import java.util.Random;
import org.ugoptimizer.algorithms.BinarySearch;
import org.ugoptimizer.algorithms.GreedyAssignment;
import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.LinearSearch;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.SelectionSort;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.algorithms.mst.Kruskal;
import org.ugoptimizer.algorithms.mst.Prim;
import org.ugoptimizer.algorithms.optimization.BruteForceIncidentSelector;
import org.ugoptimizer.algorithms.optimization.DynamicProgrammingIncidentSelector;
import org.ugoptimizer.algorithms.optimization.OptimizationItem;
import org.ugoptimizer.algorithms.shortestpath.Dijkstra;
import org.ugoptimizer.algorithms.traversal.BreadthFirstSearch;
import org.ugoptimizer.algorithms.traversal.DepthFirstSearch;
import org.ugoptimizer.model.Resource;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.MSTResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.hash.HashTable;
import org.ugoptimizer.structures.heap.BinaryHeap;
import org.ugoptimizer.structures.tree.BinarySearchTree;
import org.ugoptimizer.structures.tree.RedBlackTree;
import org.ugoptimizer.util.ProjectParameters;

/**
 * Reproducible performance lab for the assessed algorithms and structures.
 *
 * <p>Every input is generated from {@link ProjectParameters#BENCHMARK_SEED}; setup and cloning
 * occur before {@link System#nanoTime()} starts. Each algorithm/size group records at least three
 * raw trials and the arithmetic mean. Memory is a consistently sampled, non-negative approximate
 * used-heap delta and is labelled as approximate in exported CSV.</p>
 */
public final class EfficiencyLab {

    public static final int MINIMUM_TRIALS = 3;
    private static final int[] SEARCH_AND_SORT_SIZES = {100, 500, 1_000, 5_000, 10_000};
    private static final int[] HASH_AND_HEAP_SIZES = {100, 500, 1_000, 5_000, 10_000, 20_000};
    private static final int[] TREE_SIZES = {100, 500, 1_000, 5_000, 10_000};
    private static final int[] GRAPH_SIZES = {50, 100, 200, 500};
    private static final int[] OPTIMIZATION_SIZES = {8, 12, 16};

    public static int[] searchAndSortSizes() {
        return SEARCH_AND_SORT_SIZES.clone();
    }

    public static int[] hashAndHeapSizes() {
        return HASH_AND_HEAP_SIZES.clone();
    }

    public static int[] treeSizes() {
        return TREE_SIZES.clone();
    }

    public static int[] graphSizes() {
        return GRAPH_SIZES.clone();
    }

    public static int[] optimizationSizes() {
        return OPTIMIZATION_SIZES.clone();
    }

    /** Runs the complete lecturer-scale plan with three trials per group. */
    public BenchmarkReport runFull() {
        return runPlan(
                SEARCH_AND_SORT_SIZES, HASH_AND_HEAP_SIZES, TREE_SIZES,
                GRAPH_SIZES, OPTIMIZATION_SIZES, MINIMUM_TRIALS);
    }

    /** Runs a small genuine smoke benchmark using the same timing and validation pipeline. */
    public BenchmarkReport runRepresentative() {
        return runPlan(
                new int[]{100, 500}, new int[]{100, 500}, new int[]{100, 500},
                new int[]{50, 100}, new int[]{8, 12}, MINIMUM_TRIALS);
    }

    BenchmarkReport runPlan(
            int[] searchSortSizes,
            int[] hashHeapSizes,
            int[] treeSizes,
            int[] graphSizes,
            int[] optimizationSizes,
            int trials) {
        if (trials < MINIMUM_TRIALS) {
            throw new IllegalArgumentException("At least three trials are required");
        }
        RecordBuffer records = new RecordBuffer();
        benchmarkSearch(records, searchSortSizes, trials);
        benchmarkSort(records, searchSortSizes, trials);
        benchmarkHash(records, hashHeapSizes, trials);
        benchmarkTrees(records, treeSizes, trials);
        benchmarkHeap(records, hashHeapSizes, trials);
        benchmarkGraphs(records, graphSizes, trials);
        benchmarkOptimization(records, optimizationSizes, trials);
        benchmarkGreedy(records, hashHeapSizes, trials);
        return new BenchmarkReport(records.toArray(), environment());
    }

    private static void benchmarkSearch(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            measure(output, "search", "LinearSearch", size, trials,
                    "target=last generated value; input=unsorted", trial -> {
                        Integer[] values = randomValues(size, seedFor("search", size, trial));
                        Integer target = values[size - 1];
                        return () -> "index=" + LinearSearch.search(values, target);
                    });
            measure(output, "search", "BinarySearch", size, trials,
                    "target=same generated value; precondition=sorted clone", trial -> {
                        Integer[] values = randomValues(size, seedFor("search", size, trial));
                        Integer target = values[size - 1];
                        MergeSort.sort(values);
                        if (!BinarySearch.isSorted(values)) {
                            throw new IllegalStateException("Binary-search input is not sorted");
                        }
                        return () -> "index=" + BinarySearch.search(values, target) + ";sorted=true";
                    });
        }
    }

    private static void benchmarkSort(RecordBuffer output, int[] sizes, int trials) {
        String[] algorithms = {"SelectionSort", "InsertionSort", "MergeSort", "QuickSort"};
        for (int size : sizes) {
            for (String algorithm : algorithms) {
                measureWithPostMetric(output, "sort", algorithm, size, trials,
                        "equivalent deterministic clone; random order", trial -> {
                            Integer[] values = randomValues(size, seedFor("sort", size, trial));
                            return new PostMeasuredTask() {
                                @Override
                                public void run() {
                                    switch (algorithm) {
                                        case "SelectionSort" -> SelectionSort.sort(values);
                                        case "InsertionSort" -> InsertionSort.sort(values);
                                        case "MergeSort" -> MergeSort.sort(values);
                                        case "QuickSort" -> QuickSort.sort(values);
                                        default -> throw new IllegalStateException("Unknown sort");
                                    }
                                }

                                @Override
                                public String metric() {
                                    return "sorted=" + BinarySearch.isSorted(values);
                                }
                            };
                        });
            }
        }
    }

    private static void benchmarkHash(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            int[] requestedCapacities = {
                Math.max(16, size / 4), Math.max(16, size / 2), Math.max(16, size)
            };
            for (int initialCapacity : requestedCapacities) {
                measure(output, "hash", "HashTable", size, trials,
                        "initial_capacity=" + initialCapacity + ";threshold=1.0", trial -> {
                            Integer[] keys = randomValues(size, seedFor("hash", size, trial));
                            return () -> {
                                HashTable<Integer, Integer> table = new HashTable<>(initialCapacity, 1.0d);
                                for (int index = 0; index < keys.length; index++) {
                                    table.put(keys[index], index);
                                }
                                return "unique=" + table.size() + ";capacity=" + table.getCapacity()
                                        + ";load=" + table.currentLoadFactor()
                                        + ";collisions=" + table.getCollisionCount();
                            };
                        });
            }
        }
    }

    private static void benchmarkTrees(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            measureWithPostMetric(output, "tree", "BST_INSERT", size, trials,
                    "ordered IDs expose unbalanced degradation", trial -> new PostMeasuredTask() {
                        private final BinarySearchTree<Integer, Integer> tree = new BinarySearchTree<>();

                        @Override
                        public void run() {
                            for (int key = 0; key < size; key++) tree.put(key, key);
                        }

                        @Override
                        public String metric() {
                            return "height=" + tree.height();
                        }
                    });
            measureWithPostMetric(output, "tree", "RED_BLACK_INSERT", size, trials,
                    "same ordered IDs; balancing active", trial -> new PostMeasuredTask() {
                        private final RedBlackTree<Integer, Integer> tree = new RedBlackTree<>();

                        @Override
                        public void run() {
                            for (int key = 0; key < size; key++) tree.put(key, key);
                        }

                        @Override
                        public String metric() {
                            return "height=" + tree.height();
                        }
                    });
            measure(output, "tree", "BST_SEARCH", size, trials,
                    "ordered IDs; target=last", trial -> {
                        BinarySearchTree<Integer, Integer> tree = new BinarySearchTree<>();
                        for (int key = 0; key < size; key++) tree.put(key, key);
                        int height = tree.height();
                        return () -> "found=" + tree.get(size - 1).isPresent() + ";height=" + height;
                    });
            measure(output, "tree", "RED_BLACK_SEARCH", size, trials,
                    "ordered IDs; target=last", trial -> {
                        RedBlackTree<Integer, Integer> tree = new RedBlackTree<>();
                        for (int key = 0; key < size; key++) tree.put(key, key);
                        int height = tree.height();
                        return () -> "found=" + tree.get(size - 1).isPresent() + ";height=" + height;
                    });
        }
    }

    private static void benchmarkHeap(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            measure(output, "heap", "BinaryHeap_INSERT", size, trials,
                    "deterministic integer priorities", trial -> {
                        Integer[] values = randomValues(size, seedFor("heap", size, trial));
                        return () -> {
                            BinaryHeap<Integer> heap = new BinaryHeap<>();
                            for (Integer value : values) heap.add(value);
                            return "size=" + heap.size() + ";minimum=" + heap.peek();
                        };
                    });
            measure(output, "heap", "BinaryHeap_EXTRACT", size, trials,
                    "same priorities; heap construction excluded", trial -> {
                        Integer[] values = randomValues(size, seedFor("heap", size, trial));
                        BinaryHeap<Integer> heap = new BinaryHeap<>();
                        for (Integer value : values) heap.add(value);
                        return () -> {
                            int extracted = 0;
                            while (!heap.isEmpty()) {
                                heap.poll();
                                extracted++;
                            }
                            return "extracted=" + extracted;
                        };
                    });
        }
    }

    private static void benchmarkGraphs(RecordBuffer output, int[] sizes, int trials) {
        String[] algorithms = {"BFS", "DFS", "Dijkstra", "Prim", "Kruskal"};
        for (int size : sizes) {
            for (String algorithm : algorithms) {
                measure(output, "graph", algorithm, size, trials,
                        "connected deterministic graph; vertices=" + size, trial -> {
                            AdjacencyListGraph graph = connectedGraph(size, trial);
                            MSTResult primCheck = new Prim().compute(graph);
                            MSTResult kruskalCheck = new Kruskal().compute(graph);
                            if (Math.abs(primCheck.getTotalWeight() - kruskalCheck.getTotalWeight()) > 1.0e-9d) {
                                throw new IllegalStateException("Prim and Kruskal MST weights differ");
                            }
                            return () -> switch (algorithm) {
                                case "BFS" -> "visited=" + new BreadthFirstSearch()
                                        .traverse(graph, 0).getVisitedCount();
                                case "DFS" -> "visited=" + new DepthFirstSearch()
                                        .traverse(graph, 0).getVisitedCount();
                                case "Dijkstra" -> "weight=" + new Dijkstra()
                                        .shortestPath(graph, 0, size - 1).getTotalWeight().orElseThrow();
                                case "Prim" -> "mst_weight=" + new Prim().compute(graph).getTotalWeight()
                                        + ";matches_kruskal=true";
                                case "Kruskal" -> "mst_weight=" + new Kruskal().compute(graph).getTotalWeight()
                                        + ";matches_prim=true";
                                default -> throw new IllegalStateException("Unknown graph algorithm");
                            };
                        });
            }
        }
    }

    private static void benchmarkOptimization(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            for (String algorithm : new String[]{"DynamicProgramming", "BruteForce"}) {
                measure(output, "optimization", algorithm, size, trials,
                        "identical items; capacity=" + ProjectParameters.OPTIMIZATION_BUDGET, trial -> {
                            OptimizationItem[] items = optimizationItems(size, trial);
                            long dpBenefit = new DynamicProgrammingIncidentSelector()
                                    .optimize(items).getTotalBenefit();
                            long bruteBenefit = new BruteForceIncidentSelector()
                                    .optimize(items).getTotalBenefit();
                            if (dpBenefit != bruteBenefit) {
                                throw new IllegalStateException("DP and brute force disagree");
                            }
                            return () -> {
                                long benefit = algorithm.equals("DynamicProgramming")
                                        ? new DynamicProgrammingIncidentSelector().optimize(items).getTotalBenefit()
                                        : new BruteForceIncidentSelector().optimize(items).getTotalBenefit();
                                return "benefit=" + benefit + ";exact_match=true";
                            };
                        });
            }
        }
    }

    private static void benchmarkGreedy(RecordBuffer output, int[] sizes, int trials) {
        for (int size : sizes) {
            measure(output, "assignment", "GreedyAssignment", size, trials,
                    "actual eligible resources and response metrics", trial -> {
                        AssignmentCandidate[] candidates = candidates(size, trial);
                        ServiceRequest request = benchmarkRequest();
                        return () -> {
                            AssignmentCandidate chosen = GreedyAssignment.assignBestResource(request, candidates);
                            return "resource_id=" + chosen.getResource().getResourceId()
                                    + ";response_time=" + chosen.getResponseTime();
                        };
                    });
        }
    }

    private static void measure(
            RecordBuffer output,
            String experiment,
            String algorithm,
            int inputSize,
            int trials,
            String parameters,
            TaskFactory factory) {
        long[] runtimes = new long[trials];
        long[] memory = new long[trials];
        String[] metrics = new String[trials];
        Instant[] dates = new Instant[trials];
        long sum = 0L;
        for (int trial = 1; trial <= trials; trial++) {
            MeasuredTask task = factory.prepare(trial);
            long beforeMemory = usedMemory();
            long started = System.nanoTime();
            metrics[trial - 1] = task.run();
            long elapsed = System.nanoTime() - started;
            long afterMemory = usedMemory();
            runtimes[trial - 1] = elapsed;
            memory[trial - 1] = Math.max(0L, afterMemory - beforeMemory) / 1024L;
            dates[trial - 1] = Instant.now();
            sum = Math.addExact(sum, elapsed);
        }
        appendMeasurements(output, experiment, algorithm, inputSize, parameters,
                runtimes, memory, metrics, dates, sum / trials);
    }

    private static void measureWithPostMetric(
            RecordBuffer output,
            String experiment,
            String algorithm,
            int inputSize,
            int trials,
            String parameters,
            PostTaskFactory factory) {
        long[] runtimes = new long[trials];
        long[] memory = new long[trials];
        String[] metrics = new String[trials];
        Instant[] dates = new Instant[trials];
        long sum = 0L;
        for (int trial = 1; trial <= trials; trial++) {
            PostMeasuredTask task = factory.prepare(trial);
            long beforeMemory = usedMemory();
            long started = System.nanoTime();
            task.run();
            long elapsed = System.nanoTime() - started;
            long afterMemory = usedMemory();
            metrics[trial - 1] = task.metric();
            runtimes[trial - 1] = elapsed;
            memory[trial - 1] = Math.max(0L, afterMemory - beforeMemory) / 1024L;
            dates[trial - 1] = Instant.now();
            sum = Math.addExact(sum, elapsed);
        }
        appendMeasurements(output, experiment, algorithm, inputSize, parameters,
                runtimes, memory, metrics, dates, sum / trials);
    }

    private static void appendMeasurements(
            RecordBuffer output,
            String experiment,
            String algorithm,
            int inputSize,
            String parameters,
            long[] runtimes,
            long[] memory,
            String[] metrics,
            Instant[] dates,
            long average) {
        for (int trial = 1; trial <= runtimes.length; trial++) {
            output.add(new BenchmarkRecord(
                    experiment, algorithm, inputSize, trial, runtimes[trial - 1], average,
                    memory[trial - 1], ProjectParameters.BENCHMARK_SEED,
                    parameters, metrics[trial - 1], dates[trial - 1]));
        }
    }

    private static Integer[] randomValues(int size, long seed) {
        Random random = new Random(seed);
        Integer[] values = new Integer[size];
        for (int index = 0; index < size; index++) {
            values[index] = random.nextInt();
        }
        return values;
    }

    private static AdjacencyListGraph connectedGraph(int size, int trial) {
        AdjacencyListGraph graph = new AdjacencyListGraph(size);
        for (int vertex = 0; vertex < size; vertex++) graph.addVertex(vertex);
        for (int vertex = 0; vertex + 1 < size; vertex++) {
            graph.addEdge(vertex, vertex + 1, 1.0d + ((vertex + trial) % 9));
        }
        for (int vertex = 0; vertex + 2 < size; vertex++) {
            graph.addEdge(vertex, vertex + 2, 2.0d + ((vertex * 3 + trial) % 11));
        }
        return graph;
    }

    private static OptimizationItem[] optimizationItems(int size, int trial) {
        Random random = new Random(seedFor("optimization", size, trial));
        OptimizationItem[] items = new OptimizationItem[size];
        for (int index = 0; index < size; index++) {
            items[index] = new OptimizationItem(index + 1, 1 + random.nextInt(20),
                    1 + random.nextInt(100));
        }
        return items;
    }

    private static AssignmentCandidate[] candidates(int size, int trial) {
        Random random = new Random(seedFor("greedy", size, trial));
        AssignmentCandidate[] candidates = new AssignmentCandidate[size];
        for (int index = 0; index < size; index++) {
            Resource resource = new Resource(index + 1, "PATROL_OFFICER", 1, 1,
                    "AVAILABLE", 1, null, null);
            candidates[index] = new AssignmentCandidate(resource,
                    1.0d + random.nextDouble() * 30.0d, index % 4);
        }
        return candidates;
    }

    private static ServiceRequest benchmarkRequest() {
        Instant submitted = Instant.parse("2026-01-01T00:00:00Z");
        return new ServiceRequest(1, 1, 2, "SECURITY_ESCORT", 5, submitted,
                submitted.plusSeconds(600), "PENDING", "PATROL_OFFICER", "Benchmark request");
    }

    private static long seedFor(String experiment, int size, int trial) {
        long hash = 0L;
        for (int index = 0; index < experiment.length(); index++) {
            hash = 31L * hash + experiment.charAt(index);
        }
        return ProjectParameters.BENCHMARK_SEED + hash + 1_000_003L * size + trial;
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String environment() {
        Runtime runtime = Runtime.getRuntime();
        return "Benchmark seed: " + ProjectParameters.BENCHMARK_SEED + '\n'
                + "OS: " + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + '\n'
                + "Architecture: " + System.getProperty("os.arch") + '\n'
                + "Java: " + System.getProperty("java.version") + '\n'
                + "JVM: " + System.getProperty("java.vm.name") + '\n'
                + "Available processors: " + runtime.availableProcessors() + '\n'
                + "Maximum JVM heap bytes: " + runtime.maxMemory() + '\n'
                + "Hardware model/CPU/RAM: record manually on the final benchmark machine.\n"
                + "Memory column: approximate used-heap delta; setup excluded; negative deltas clamped to zero.\n";
    }

    @FunctionalInterface
    private interface TaskFactory {
        MeasuredTask prepare(int trial);
    }

    @FunctionalInterface
    private interface MeasuredTask {
        String run();
    }

    @FunctionalInterface
    private interface PostTaskFactory {
        PostMeasuredTask prepare(int trial);
    }

    private interface PostMeasuredTask {
        void run();

        String metric();
    }

    private static final class RecordBuffer {
        private BenchmarkRecord[] records = new BenchmarkRecord[64];
        private int size;

        private void add(BenchmarkRecord record) {
            if (size == records.length) {
                BenchmarkRecord[] grown = new BenchmarkRecord[records.length * 2];
                System.arraycopy(records, 0, grown, 0, size);
                records = grown;
            }
            records[size++] = record;
        }

        private BenchmarkRecord[] toArray() {
            BenchmarkRecord[] snapshot = new BenchmarkRecord[size];
            System.arraycopy(records, 0, snapshot, 0, size);
            return snapshot;
        }
    }
}
