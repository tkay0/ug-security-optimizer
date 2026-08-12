package org.ugoptimizer.gui.util;

import java.time.Instant;
import java.util.function.Function;
import org.ugoptimizer.algorithms.BinarySearch;
import org.ugoptimizer.algorithms.InsertionSort;
import org.ugoptimizer.algorithms.LinearSearch;
import org.ugoptimizer.algorithms.MergeSort;
import org.ugoptimizer.algorithms.QuickSort;
import org.ugoptimizer.algorithms.SelectionSort;
import org.ugoptimizer.model.ServiceRequest;

/**
 * Search and sort glue that delegates to the project's existing algorithm
 * implementations instead of reimplementing them.
 *
 * <p>Linear and binary search operate on the request-ID array. The four
 * project sorting algorithms are invoked on comparable key projections of
 * service requests so the incident table can be re-ordered by a chosen field.</p>
 */
public final class SearchSortEngine {

    /** Existing project sort algorithms exposed to the incident table. */
    public enum SortAlgorithm {
        SELECTION_SORT("Selection Sort"),
        INSERTION_SORT("Insertion Sort"),
        MERGE_SORT("Merge Sort"),
        QUICK_SORT("Quick Sort");

        private final String label;

        SortAlgorithm(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Incident fields that can order the incident table. */
    public enum SortKey {
        REQUEST_ID("Request ID"),
        URGENCY("Urgency"),
        TIME_SUBMITTED("Submitted"),
        DEADLINE("Deadline"),
        STATUS("Status"),
        CATEGORY("Category"),
        DESTINATION("Destination");

        private final String label;

        SortKey(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private SearchSortEngine() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    // ------------------------------------------------------------------
    // Search — existing LinearSearch and BinarySearch
    // ------------------------------------------------------------------

    /** Index of {@code targetId} via the existing LinearSearch, or -1. */
    public static int findIndexLinear(Integer[] requestIds, int targetId) {
        return LinearSearch.search(requestIds, targetId);
    }

    /** Index of {@code targetId} via the existing BinarySearch, or -1. */
    public static int findIndexBinary(Integer[] requestIds, int targetId) {
        return BinarySearch.search(requestIds, targetId);
    }

    /** Whether the id array satisfies the BinarySearch sorted precondition. */
    public static boolean idsSorted(Integer[] requestIds) {
        return BinarySearch.isSorted(requestIds);
    }

    // ------------------------------------------------------------------
    // Sort — existing Selection/Insertion/Merge/QuickSort
    // ------------------------------------------------------------------

    /**
     * Re-orders {@code requests} by {@code key} using the existing
     * {@code algorithm}. A new array is returned; the input is not mutated.
     *
     * @param locationName resolves location ids to names for location-based keys
     */
    public static ServiceRequest[] sort(
            ServiceRequest[] requests,
            SortKey key,
            boolean ascending,
            SortAlgorithm algorithm,
            Function<Integer, String> locationName) {
        if (requests == null || requests.length == 0) {
            return requests == null ? new ServiceRequest[0] : requests.clone();
        }
        Keyed[] keyed = new Keyed[requests.length];
        for (int index = 0; index < requests.length; index++) {
            keyed[index] = new Keyed(requests[index], keyOf(requests[index], key, locationName));
        }

        switch (algorithm) {
            case SELECTION_SORT -> SelectionSort.sort(keyed);
            case INSERTION_SORT -> InsertionSort.sort(keyed);
            case MERGE_SORT -> MergeSort.sort(keyed);
            case QUICK_SORT -> QuickSort.sort(keyed);
            default -> throw new AssertionError("Unsupported sort algorithm: " + algorithm);
        }

        ServiceRequest[] result = new ServiceRequest[requests.length];
        for (int index = 0; index < requests.length; index++) {
            result[ascending ? index : requests.length - 1 - index] = keyed[index].request;
        }
        return result;
    }

    private static Comparable<?> keyOf(
            ServiceRequest request, SortKey key, Function<Integer, String> locationName) {
        return switch (key) {
            case REQUEST_ID -> request.getRequestId();
            case URGENCY -> request.getUrgency();
            case TIME_SUBMITTED -> request.getTimeSubmitted();
            case DEADLINE -> request.getDeadline();
            case STATUS -> request.getStatus();
            case CATEGORY -> request.getCategory();
            case DESTINATION -> locationName.apply(request.getDestinationLocationId());
        };
    }

    /** Comparable projection used so the generic project sorts can order requests. */
    private static final class Keyed implements Comparable<Keyed> {

        private final ServiceRequest request;
        private final Comparable<?> key;

        Keyed(ServiceRequest request, Comparable<?> key) {
            this.request = request;
            this.key = key;
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(Keyed other) {
            return ((Comparable<Object>) key).compareTo(other.key);
        }
    }
}
