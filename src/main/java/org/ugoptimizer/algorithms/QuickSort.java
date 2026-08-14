package org.ugoptimizer.algorithms;

/**
 * Reusable Quick Sort implementation used by Team 3 (Workflow, Trees &amp;
 * Optimization).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * Quick Sort is used where average-case speed and low memory overhead matter
 * more than worst-case guarantees, for example one-off sorts of in-memory
 * incident batches before they are handed to the tree/heap indexing
 * structures. It sorts <b>in place</b> using swaps, so it needs no auxiliary
 * array the way {@link MergeSort} does.
 * </p>
 *
 * <p><b>How Quick Sort Works</b></p>
 * <p>
 * Quick Sort is a divide-and-conquer algorithm built around partitioning:
 * </p>
 * <ol>
 *   <li><b>Choose a pivot.</b> This implementation deliberately uses the
 *       <b>last element</b> of the current sub-array as the pivot (a
 *       classic Lomuto partition), rather than a random or median-of-three
 *       pivot. This is a conscious choice, not an oversight: it keeps the
 *       algorithm's worst case simple to reproduce and reason about for
 *       {@link QuickSortWorstCaseDemo}. Teams that need Quick Sort to be
 *       resilient against already-sorted input in production should switch
 *       to a randomized or median-of-three pivot instead.</li>
 *   <li><b>Partition.</b> Rearrange the sub-array so every element smaller
 *       than the pivot ends up to its left, and every element greater ends
 *       up to its right; the pivot lands in its final sorted position.</li>
 *   <li><b>Recurse.</b> Apply the same process to the sub-array left of the
 *       pivot and the sub-array right of the pivot.</li>
 * </ol>
 *
 * <p>Sorting is done <b>in place</b> using element swaps; no array
 * proportional to the input is allocated. The implementation is written
 * from scratch and does <b>not</b> call {@code Arrays.sort},
 * {@code Collections.sort}, Streams, or any external sorting library.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of elements.</p>
 * <ul>
 *   <li><b>Best Case: {@code O(n log n)}</b> — the pivot happens to split
 *       the sub-array into two roughly equal halves at every level, giving a
 *       balanced recursion tree of depth {@code log n}, each level doing
 *       {@code O(n)} partitioning work.</li>
 *   <li><b>Average Case: {@code O(n log n)}</b> — for randomly ordered
 *       input, partitions are unbalanced but not consistently so; the
 *       expected recursion depth is {@code O(log n)}.</li>
 *   <li><b>Worst Case: {@code O(n²)}</b> — with a last-element pivot, an
 *       already sorted (or already reverse-sorted) array always produces
 *       the most unbalanced possible partition: one side has {@code n-1}
 *       elements and the other has {@code 0}. The recursion depth becomes
 *       {@code n} instead of {@code log n}, and total comparisons are
 *       {@code (n-1) + (n-2) + ... + 1 = n(n-1)/2}, i.e. {@code O(n²)}. See
 *       {@link QuickSortWorstCaseDemo} for a runnable demonstration and
 *       {@code QuickSortTest} for a test that pins down the exact
 *       comparison count on sorted input.</li>
 *   <li><b>Space Complexity: {@code O(log n)} average / {@code O(n)}
 *       worst case</b> — no auxiliary array is used, but recursion uses
 *       stack space proportional to recursion depth: {@code O(log n)} for
 *       balanced partitions, degrading to {@code O(n)} in the worst case
 *       described above.</li>
 * </ul>
 *
 * <p><b>Does it modify the input?</b> Yes. Elements are swapped directly
 * within the caller's array; no copy is made or returned.</p>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Input: {@code [6, 3, 5, 1]}, pivot = last element</p>
 * <pre>
 * Partition [6, 3, 5, 1], pivot = 1 (index 3):
 *   Nothing is smaller than 1 -&gt; pivot swaps into index 0
 *   Array:            [1, 3, 5, 6]   (pivot 1 now fixed at index 0)
 *
 * Recurse right on [3, 5, 6], pivot = 6 (index 3):
 *   3 &lt; 6 and 5 &lt; 6 -&gt; both stay left of the swap point
 *   pivot swaps into its own position (already last)
 *   Array:            [1, 3, 5, 6]   (pivot 6 now fixed at index 3)
 *
 * Recurse on [3, 5], pivot = 5 (index 2):
 *   3 &lt; 5 -&gt; stays left; pivot swaps into index 2 (already there)
 *   Array:            [1, 3, 5, 6]   (pivot 5 now fixed at index 2)
 *
 * Final result:       [1, 3, 5, 6]
 * </pre>
 */
public final class QuickSort {

    private QuickSort() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Sorts {@code array} in place in ascending order using Quick Sort with
     * a last-element (Lomuto) pivot.
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
        quickSort(array, 0, array.length - 1);
    }

    private static <T extends Comparable<T>> void quickSort(T[] array, int low, int high) {
        if (low >= high) {
            return;
        }
        int pivotIndex = partition(array, low, high);
        quickSort(array, low, pivotIndex - 1);
        quickSort(array, pivotIndex + 1, high);
    }

    /**
     * Lomuto partition scheme: uses {@code array[high]} as the pivot,
     * rearranges {@code array[low..high]} so everything smaller than the
     * pivot comes first, then places the pivot right after it.
     *
     * @return the final, sorted index of the pivot element
     */
    private static <T extends Comparable<T>> int partition(T[] array, int low, int high) {
        T pivot = array[high];
        int boundary = low - 1;

        for (int i = low; i < high; i++) {
            if (array[i].compareTo(pivot) < 0) {
                boundary++;
                swap(array, boundary, i);
            }
        }
        swap(array, boundary + 1, high);
        return boundary + 1;
    }

    private static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
