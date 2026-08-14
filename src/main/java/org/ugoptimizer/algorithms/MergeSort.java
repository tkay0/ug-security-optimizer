package org.ugoptimizer.algorithms;

/**
 * Reusable Merge Sort implementation used by Team 3 (Workflow, Trees &amp;
 * Optimization).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * Several Team 3 workflows (case-history reviews, batch report generation,
 * archived-incident audits) need a sort with <b>guaranteed</b> {@code O(n log
 * n)} performance regardless of how the input is already ordered, and one
 * that is <b>stable</b> so that records with equal keys keep their original
 * relative order (important when secondary fields, such as timestamp, must
 * be preserved). Merge Sort satisfies both requirements, at the cost of extra
 * memory.
 * </p>
 *
 * <p><b>How Merge Sort Works</b></p>
 * <p>
 * Merge Sort is a classic divide-and-conquer algorithm:
 * </p>
 * <ol>
 *   <li><b>Divide:</b> split the array into two halves around the midpoint.</li>
 *   <li><b>Conquer:</b> recursively sort each half.</li>
 *   <li><b>Combine:</b> merge the two sorted halves back together by
 *       repeatedly taking the smaller of the two front elements into a
 *       temporary buffer, then copying that buffer back into the original
 *       array.</li>
 * </ol>
 * <p>The recursion bottoms out at sub-arrays of length 0 or 1, which are
 * trivially sorted.</p>
 *
 * <p>This implementation sorts the array <b>in place</b> in the sense that
 * the original array reference passed by the caller ends up holding the
 * sorted result (no new array is returned), but it is <b>not</b> an in-place
 * algorithm in the strict algorithmic sense: merging requires an auxiliary
 * buffer proportional to the input size. A single buffer array is allocated
 * once and reused across all merge steps, rather than allocating a new array
 * per recursive call, to reduce garbage-collector pressure. The
 * implementation is written from scratch and does <b>not</b> call
 * {@code Arrays.sort}, {@code Collections.sort}, Streams, or any external
 * sorting library.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of elements.</p>
 * <ul>
 *   <li><b>Best Case: {@code O(n log n)}</b> — even an already-sorted array
 *       is still fully divided into {@code log n} levels and every level
 *       still performs a full {@code O(n)} merge pass; Merge Sort has no
 *       "already sorted" shortcut.</li>
 *   <li><b>Average Case: {@code O(n log n)}</b> — the array is split into
 *       {@code log n} levels of recursion, and each level does {@code O(n)}
 *       work merging, for a total of {@code O(n log n)}.</li>
 *   <li><b>Worst Case: {@code O(n log n)}</b> — unlike Quick Sort, the split
 *       point is always the midpoint regardless of data order, so the
 *       recursion tree is always balanced; there is no pathological input
 *       that degrades performance.</li>
 *   <li><b>Space Complexity: {@code O(n)}</b> — one auxiliary buffer array of
 *       size {@code n} is allocated for merging, plus {@code O(log n)}
 *       recursion-stack space for the balanced call tree.</li>
 * </ul>
 *
 * <p><b>Does it modify the input?</b> Yes. The caller's array reference is
 * mutated so that, after {@link #sort(Comparable[])} returns, it contains
 * the sorted elements. No copy is returned; the original array object is
 * reused.</p>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Input: {@code [6, 3, 5, 1]}</p>
 * <pre>
 * Divide:
 *   [6, 3, 5, 1]
 *   -&gt; [6, 3]        [5, 1]
 *   -&gt; [6] [3]       [5] [1]
 *
 * Merge back up:
 *   [6] + [3]  -&gt; [3, 6]
 *   [5] + [1]  -&gt; [1, 5]
 *   [3, 6] + [1, 5] -&gt; compare 3 vs 1 -&gt; take 1
 *                   -&gt; compare 3 vs 5 -&gt; take 3
 *                   -&gt; compare 6 vs 5 -&gt; take 5
 *                   -&gt; take remaining 6
 *                   -&gt; [1, 3, 5, 6]
 *
 * Final result: [1, 3, 5, 6]
 * </pre>
 */
public final class MergeSort {

    private MergeSort() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Sorts {@code array} in ascending order using Merge Sort. The original
     * array reference is mutated in place to hold the sorted result.
     *
     * <p><b>Precondition:</b> the array must not contain {@code null}
     * elements, since ordering is determined by {@code compareTo}. A
     * {@code null} array, an empty array, and a single-element array are
     * handled safely as no-ops.</p>
     *
     * @param array the array to sort; its contents are mutated in place
     * @param <T>   the type of elements, comparable with itself
     */
    public static <T extends Comparable<T>> void sort(T[] array) {
        if (array == null || array.length < 2) {
            return;
        }
        @SuppressWarnings("unchecked")
        T[] buffer = (T[]) new Comparable[array.length];
        mergeSort(array, buffer, 0, array.length - 1);
    }

    private static <T extends Comparable<T>> void mergeSort(T[] array, T[] buffer, int left, int right) {
        if (left >= right) {
            return;
        }
        int mid = left + (right - left) / 2;
        mergeSort(array, buffer, left, mid);
        mergeSort(array, buffer, mid + 1, right);
        merge(array, buffer, left, mid, right);
    }

    private static <T extends Comparable<T>> void merge(T[] array, T[] buffer, int left, int mid, int right) {
        for (int i = left; i <= right; i++) {
            buffer[i] = array[i];
        }

        int leftIndex = left;
        int rightIndex = mid + 1;
        int destination = left;

        while (leftIndex <= mid && rightIndex <= right) {
            if (buffer[leftIndex].compareTo(buffer[rightIndex]) <= 0) {
                array[destination++] = buffer[leftIndex++];
            } else {
                array[destination++] = buffer[rightIndex++];
            }
        }
        while (leftIndex <= mid) {
            array[destination++] = buffer[leftIndex++];
        }
        while (rightIndex <= right) {
            array[destination++] = buffer[rightIndex++];
        }
    }
}
