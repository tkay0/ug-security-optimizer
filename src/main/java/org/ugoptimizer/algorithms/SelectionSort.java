package org.ugoptimizer.algorithms;

/**
 * Reusable Selection Sort implementation used by Team 2 (Incident Management
 * and Emergency Dispatch System).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * The system manages many campus security incidents and supporting records
 * (incident IDs, resource availability, report rows). Sorting organizes this
 * data so it can be displayed in reports, prepared for dispatch, or made
 * ready for faster Binary Search. Selection Sort is a simple comparison-based
 * sort chosen because it performs the minimum possible number
 * of writes, which matters when the records are large objects.
 * </p>
 *
 * <p><b>How Selection Sort Works</b></p>
 * <p>
 * The array is conceptually divided into two portions at every step:
 * </p>
 * <ul>
 *   <li><b>Sorted portion</b> — the left part, which is already in its final
 *       order.</li>
 *   <li><b>Unsorted portion</b> — the right part, still to be processed.</li>
 * </ul>
 * <p>The algorithm then repeats:</p>
 * <ol>
 *   <li>Scan the unsorted portion to find the index of its <b>smallest</b>
 *       element.</li>
 *   <li>Swap that smallest element into the first position of the unsorted
 *       portion, growing the sorted portion by one.</li>
 *   <li>Stop when only one element remains in the unsorted portion; a single
 *       element is already in place.</li>
 * </ol>
 *
 * <p><b>Important — elements must be non-null.</b> Every comparison relies on
 * {@link Comparable#compareTo(Object)}, so the array must not contain
 * {@code null} elements. The array itself, however, may be {@code null} and is
 * handled as a no-op.</p>
 *
 * <p>Sorting is done <b>in place</b>: elements are rearranged by swapping
 * within the same array, so no additional array proportional to the input is
 * allocated. The implementation is written from scratch with explicit loop and
 * swap logic and does <b>not</b> call {@code Arrays.sort},
 * {@code Collections.sort}, Streams, or any external sorting library.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of elements. The algorithm always performs a
 * full scan of the remaining unsorted portion on every pass, regardless of
 * how ordered the input already is:</p>
 * <ul>
 *   <li><b>Best Case: {@code O(n²)}</b> — even an already sorted array is
 *       scanned in full ({@code n-1} comparisons, then {@code n-2}, and so
 *       on), so the comparison count is the same as the worst case.</li>
 *   <li><b>Average Case: {@code O(n²)}</b> — the sum of the decreasing scans
 *       {@code (n-1) + (n-2) + ... + 1} equals {@code n(n-1)/2}, which grows
 *       quadratically.</li>
 *   <li><b>Worst Case: {@code O(n²)}</b> — a reverse-sorted array still costs
 *       {@code n(n-1)/2} comparisons; Selection Sort never benefits from
 *       existing order.</li>
 *   <li><b>Space Complexity: {@code O(1)}</b> — only two loop counters, a
 *       variable holding the index of the current minimum, and a temporary
 *       variable used by the swap are needed. No auxiliary array or recursion
 *       stack is used.</li>
 * </ul>
 * <p>One useful property: Selection Sort performs at most {@code n-1} swaps
 * regardless of input, which is the fewest among the simple sorts.</p>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Input: {@code [7, 4, 5, 2]}</p>
 * <pre>
 * Initial:         7   4   5   2
 *
 * Pass 1 (i = 0, unsorted = {7, 4, 5, 2}):
 *   Scan {7, 4, 5, 2}: 7&gt;4, 4&lt;5, 2 is smallest -> min index 3
 *   Swap 7 with 2
 *   Array:            2   4   5   7
 *
 * Pass 2 (i = 1, unsorted = {4, 5, 7}):
 *   Scan {4, 5, 7}: 4 is already the smallest -> min index 1
 *   No swap needed
 *   Array:            2   4   5   7
 *
 * Pass 3 (i = 2, unsorted = {5, 7}):
 *   Scan {5, 7}: 5 is already the smallest -> min index 2
 *   No swap needed
 *   Array:            2   4   5   7
 *
 * Final result:      2   4   5   7
 * </pre>
 */
public final class SelectionSort {

    private SelectionSort() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Sorts {@code array} in place in ascending order using Selection Sort.
     *
     * <p><b>Precondition:</b> the array must not contain {@code null}
     * elements, since ordering is determined by {@code compareTo}. A
     * {@code null} array, an empty array, and a single-element array are
     * handled safely as no-ops.</p>
     *
     * @param array the array to sort in place; may be {@code null}
     * @param <T>   the type of elements, comparable with itself
     */
    public static <T extends Comparable<T>> void sort(T[] array) {
        if (array == null || array.length < 2) {
            return;
        }
        for (int i = 0; i < array.length - 1; i++) {
            int minIndex = findMinimumIndex(array, i);
            if (minIndex != i) {
                swap(array, i, minIndex);
            }
        }
    }

    /**
     * Finds the index of the smallest element in {@code array} from
     * {@code fromIndex} (inclusive) to the end of the array.
     *
     * @param array     the array being sorted; never {@code null}
     * @param fromIndex the first index to consider; never negative
     * @param <T>       the type of elements, comparable with itself
     * @return the index of the smallest element
     */
    private static <T extends Comparable<T>> int findMinimumIndex(T[] array, int fromIndex) {
        int minIndex = fromIndex;
        for (int j = fromIndex + 1; j < array.length; j++) {
            if (array[j].compareTo(array[minIndex]) < 0) {
                minIndex = j;
            }
        }
        return minIndex;
    }

    /**
     * Swaps the elements at {@code left} and {@code right} in {@code array}.
     *
     * @param array the array containing the elements; never {@code null}
     * @param left  the first index to swap
     * @param right the second index to swap
     */
    private static <T> void swap(T[] array, int left, int right) {
        T temporary = array[left];
        array[left] = array[right];
        array[right] = temporary;
    }
}
