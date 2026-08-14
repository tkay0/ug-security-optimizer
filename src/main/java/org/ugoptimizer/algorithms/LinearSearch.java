package org.ugoptimizer.algorithms;

/**
 * Reusable Linear Search implementation used by Team 2 (Incident Management and
 * Emergency Dispatch System).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * The system stores campus security incidents (medical emergencies, fire
 * outbreaks, theft reports, lost property, security alerts). Incident
 * collections are not guaranteed to be sorted, so a Binary Search cannot
 * always be applied. Linear Search inspects the collection from the first
 * element to the last until the target incident is found, which works on both
 * sorted and unsorted data.
 * </p>
 *
 * <p><b>How Linear Search Works</b></p>
 * <ol>
 *   <li>Start at the first element (index {@code 0}).</li>
 *   <li>Compare the current element with the target using the element's
 *       {@code equals} semantics.</li>
 *   <li>If they are equal, return the current index immediately.</li>
 *   <li>Otherwise, move to the next element and repeat.</li>
 *   <li>If the end of the array is reached without a match, return
 *       {@code -1} to indicate the target is not present.</li>
 * </ol>
 *
 * <p>This implementation is deliberately written from scratch: it performs a
 * manual element-by-element comparison and does <b>not</b> delegate to
 * {@code java.util.Arrays} search helpers, Java Collections, Streams, or any
 * external library. Because elements are compared with their own
 * {@code equals} method, the same generic method supports any type, including
 * incident IDs represented as {@code String}.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>The cost is dominated by the number of comparisons between elements and
 * the target. Let {@code n} be the number of elements in the array. Each
 * comparison and index increment runs in constant time, so:</p>
 * <ul>
 *   <li><b>Best Case: {@code O(1)}</b> — the target is the first element
 *       (index {@code 0}); only one comparison is needed and the loop exits
 *       immediately.</li>
 *   <li><b>Average Case: {@code O(n)}</b> — for a randomly placed target, the
 *       expected number of comparisons is roughly {@code n/2}, which grows
 *       linearly with {@code n}.</li>
 *   <li><b>Worst Case: {@code O(n)}</b> — the target is the last element or
 *       absent; every one of the {@code n} elements must be compared before a
 *       result is returned.</li>
 *   <li><b>Space Complexity: {@code O(1)}</b> — only the loop counter and a
 *       couple of local references are used; no additional data structures
 *       proportional to the input size are allocated.</li>
 * </ul>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Dataset: {@code [INC001, INC050, INC120, INC300]}, target:
 * {@code INC120}.</p>
 * <pre>
 * Step 1: Compare INC001 with INC120  -> Not equal
 * Step 2: Compare INC050 with INC120  -> Not equal
 * Step 3: Compare INC120 with INC120  -> Found
 * Return: Index 2
 * </pre>
 */
public final class LinearSearch {

    private LinearSearch() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Searches for {@code target} by inspecting every element of
     * {@code array} in order.
     *
     * <p>When the array contains duplicate values, the <b>first</b> occurrence
     * (lowest index) is returned, because the scan stops as soon as a match is
     * found. If the target appears only once, its single index is returned. A
     * {@code null} array or an array without the target yields {@code -1};
     * a {@code null} target matches the first {@code null} element, if any.</p>
     *
     * @param array  the array to search; may be {@code null}
     * @param target the element to look for; may be {@code null}
     * @param <T>    the type of elements in the array
     * @return the index of the first matching element, or {@code -1} if the
     *         target is not found, the array is empty, or the array is
     *         {@code null}
     */
    public static <T> int search(T[] array, T target) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (areEqual(array[i], target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compares two elements safely, returning {@code true} when they are
     * considered equal. Guards against comparing {@code null} values directly
     * through {@code equals}.
     *
     * @param left  the element from the array; may be {@code null}
     * @param right the target being searched for; may be {@code null}
     * @return {@code true} if both are equal (or both {@code null})
     */
    private static boolean areEqual(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }
}
