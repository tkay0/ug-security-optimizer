package org.ugoptimizer.algorithms;

/**
 * Reusable Binary Search implementation used by Team 2 (Incident Management
 * and Emergency Dispatch System).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * The system manages hundreds of campus security incidents (medical
 * emergencies, fire outbreaks, theft reports, lost property, security alerts),
 * each identified by an ID such as {@code INC001}, {@code INC020},
 * {@code INC050}. Scanning every record sequentially (Linear Search) becomes
 * inefficient as the incident log grows. When the incident IDs are held in a
 * sorted array, Binary Search locates a target in logarithmic time by
 * repeatedly discarding the half of the array that cannot contain the target.
 * </p>
 *
 * <p><b>How Binary Search Works (Divide and Conquer)</b></p>
 * <ol>
 *   <li>Maintain two pointers: {@code low} (start) and {@code high} (end) of
 *       the current search space.</li>
 *   <li>Compute the middle index {@code mid = low + (high - low) / 2}.</li>
 *   <li>Compare the element at {@code mid} with the target:
 *       <ul>
 *         <li>Equal: the target is found, return {@code mid}.</li>
 *         <li>Element smaller than target: the target must lie in the right
 *             half, so set {@code low = mid + 1}.</li>
 *         <li>Element larger than target: the target must lie in the left
 *             half, so set {@code high = mid - 1}.</li>
 *       </ul>
 *   </li>
 *   <li>Repeat until the target is found or the search space collapses
 *       ({@code low > high}), in which case the target is absent and
 *       {@code -1} is returned.</li>
 * </ol>
 *
 * <p><b>IMPORTANT — Input Must Be Sorted</b></p>
 * <p>
 * Binary Search only produces correct results when the array is sorted in
 * <b>non-decreasing order</b> according to the elements' natural ordering.
 * On unsorted data the pointer movement assumptions break and the result is
 * meaningless. Callers must therefore guarantee the precondition. When the
 * sortedness of an input array is in doubt, use {@link #isSorted(Comparable[])}
 * to verify it before searching.
 * </p>
 *
 * <p>The algorithm is implemented from scratch: {@code low}/{@code high}
 * pointers, an overflow-safe middle calculation, and explicit
 * {@code compareTo} comparison logic. It does <b>not</b> delegate to
 * {@code Arrays.binarySearch}, {@code Collections.binarySearch}, Streams, or
 * any external library.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of elements. Every comparison halves the
 * remaining search space, so the number of elements inspected is the number of
 * times {@code n} can be divided by two before one element remains:</p>
 * <ul>
 *   <li><b>Best Case: {@code O(1)}</b> — the target is exactly the element at
 *       the first computed middle position; only one comparison is needed.</li>
 *   <li><b>Average Case: {@code O(log n)}</b> — on average the target is found
 *       after a few halvings; the expected number of comparisons is
 *       proportional to {@code log2(n)}.</li>
 *   <li><b>Worst Case: {@code O(log n)}</b> — when the target is absent (or at
 *       the extreme ends), every halving step must run until the search space
 *       is empty, producing roughly {@code log2(n)} comparisons.</li>
 *   <li><b>Space Complexity: {@code O(1)}</b> — the iterative version uses only
 *       the three integer pointers ({@code low}, {@code high}, {@code mid})
 *       and one comparison result; no storage proportional to the input is
 *       allocated and no recursion stack is used.</li>
 * </ul>
 *
 * <p><b>Example Execution Trace — Target Found</b></p>
 * <p>Array: {@code [INC001, INC020, INC050, INC100, INC200, INC300, INC500]},
 * target: {@code INC100}.</p>
 * <pre>
 * Step 1:
 *   low = 0, high = 6
 *   mid = low + (high - low) / 2 = 0 + (6 - 0) / 2 = 3
 *   array[3] = INC100
 *   Compare INC100 with INC100  -> Equal
 * Result: Found at index 3
 * </pre>
 *
 * <p><b>Example Execution Trace — Target Not Found</b></p>
 * <p>Same array, target: {@code INC900}.</p>
 * <pre>
 * Step 1: low = 0, high = 6, mid = 3, array[3] = INC100
 *         Compare INC100 with INC900 -> INC100 &lt; INC900 -> search right, low = 4
 * Step 2: low = 4, high = 6, mid = 5, array[5] = INC300
 *         Compare INC300 with INC900 -> INC300 &lt; INC900 -> search right, low = 6
 * Step 3: low = 6, high = 6, mid = 6, array[6] = INC500
 *         Compare INC500 with INC900 -> INC500 &lt; INC900 -> search right, low = 7
 * Step 4: low = 7, high = 6 -> low &gt; high, search space is empty
 * Result: Not found, return -1
 * </pre>
 */
public final class BinarySearch {

    private BinarySearch() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Searches {@code array} for {@code target} using the Binary Search
     * algorithm.
     *
     * <p><b>Precondition:</b> {@code array} must be sorted in non-decreasing
     * order according to the natural ordering of {@code T}; otherwise the
     * result is undefined. Use {@link #isSorted(Comparable[])} to verify the
     * precondition when in doubt. Array elements and the target must be
     * non-null for the {@code compareTo} comparison to be meaningful.</p>
     *
     * <p>When duplicates exist, a <b>valid</b> index of an element equal to
     * the target is returned (not necessarily the first or last occurrence,
     * since the scan converges on the first match in the halving process).</p>
     *
     * <p>An array containing {@code null} elements does not satisfy the
     * sortedness precondition. Instead of throwing a
     * {@code NullPointerException} when the scan reaches a {@code null}
     * element, {@code -1} is returned to indicate the target is not present.</p>
     *
     * @param array  the sorted array to search; may be {@code null}
     * @param target the element to look for; may be {@code null}
     * @param <T>    the type of elements, comparable with itself
     * @return the index of an occurrence of {@code target}, or {@code -1} if
     *         the target is absent, the array is empty, the array is
     *         {@code null}, the target is {@code null}, or the array contains
     *         a {@code null} element
     */
    public static <T extends Comparable<T>> int search(T[] array, T target) {
        if (array == null || target == null) {
            return -1;
        }
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            T element = array[mid];
            if (element == null) {
                return -1;
            }
            int comparison = element.compareTo(target);
            if (comparison == 0) {
                return mid;
            } else if (comparison < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    /**
     * Checks whether {@code array} is sorted in non-decreasing order according
     * to the natural ordering of {@code T}.
     *
     * <p>An empty array or an array with a single element is vacuously
     * sorted. A {@code null} array is treated as sorted so callers can run the
     * check unconditionally before invoking {@link #search(Comparable[],
     * Comparable)}. An array containing {@code null} elements is not sorted:
     * the natural ordering of {@code T} does not apply to {@code null}, so
     * {@code false} is returned instead of throwing a
     * {@code NullPointerException}.</p>
     *
     * @param array the array to inspect; may be {@code null}
     * @param <T>   the type of elements, comparable with itself
     * @return {@code true} if the array is {@code null}, empty, has fewer than
     *         two elements, or is sorted in non-decreasing order; {@code false}
     *         if the array contains any {@code null} element
     */
    public static <T extends Comparable<T>> boolean isSorted(T[] array) {
        if (array == null || array.length < 2) {
            return true;
        }
        for (int i = 1; i < array.length; i++) {
            if (array[i] == null || array[i - 1] == null) {
                return false;
            }
            if (array[i].compareTo(array[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }
}
