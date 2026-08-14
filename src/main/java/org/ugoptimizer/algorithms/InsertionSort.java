package org.ugoptimizer.algorithms;

/**
 * Reusable Insertion Sort implementation used by Team 2 (Incident Management
 * and Emergency Dispatch System).
 *
 * <p><b>Purpose</b></p>
 * <p>
 * The system works with a continuously growing incident log: new incidents
 * (medical emergencies, fire outbreaks, theft reports) arrive over time. Once
 * the existing IDs are ordered, re-sorting the whole dataset after every new
 * arrival is wasteful. Insertion Sort is ideal for this pattern because it
 * efficiently inserts each incoming record into its correct position within an
 * already ordered list, and it performs very well on data that is already
 * nearly sorted (e.g. IDs that are mostly in order with a few new entries
 * sprinkled in).
 * </p>
 *
 * <p><b>How Insertion Sort Works</b></p>
 * <p>
 * Insertion Sort maintains a <b>sorted section</b> at the start of the array.
 * The first element is trivially sorted. Each subsequent element (starting from
 * the second) is picked up and inserted into its correct place inside the
 * sorted section by shifting larger elements one position to the right. The
 * algorithm proceeds as follows for every element from index {@code 1} to
 * {@code n-1}:
 * </p>
 * <ol>
 *   <li>Save the current element as the "key".</li>
 *   <li>Look leftwards through the sorted section, shifting every element
 *       that is larger than the key one position to the right.</li>
 *   <li>Insert the key into the gap left behind.</li>
 * </ol>
 * <p>Once every element has been inserted, the whole array is sorted.</p>
 *
 * <p>Sorting is done <b>in place</b> with element shifts; no array
 * proportional to the input is allocated. The implementation is written from
 * scratch and does <b>not</b> call {@code Arrays.sort},
 * {@code Collections.sort}, Streams, or any external sorting library.</p>
 *
 * <p><b>Complexity Analysis</b></p>
 * <p>Let {@code n} be the number of elements. The cost is driven by the number
 * of shifts performed, which depends on how ordered the input already is:</p>
 * <ul>
 *   <li><b>Best Case: {@code O(n)}</b> — when the array is already sorted,
 *       each key is larger than everything before it, so only one comparison
 *       is made per element and no shifts occur; the loop simply walks the
 *       array once.</li>
 *   <li><b>Average Case: {@code O(n²)}</b> — for randomly ordered data, each
 *       key must travel roughly halfway through the sorted section on average,
 *       giving about {@code n²/4} shifts and comparisons in total.</li>
 *   <li><b>Worst Case: {@code O(n²)}</b> — a reverse-sorted array forces every
 *       key to shift all preceding elements, producing {@code 1 + 2 + ... +
 *       (n-1) = n(n-1)/2} shifts and comparisons.</li>
 *   <li><b>Space Complexity: {@code O(1)}</b> — only a loop counter and the
 *       single temporary "key" variable are used; sorting is done in place with
 *       element shifts, with no recursion stack.</li>
 * </ul>
 *
 * <p><b>Example Execution Trace</b></p>
 * <p>Input: {@code [6, 3, 5, 1]}</p>
 * <pre>
 * Initial:         6   3   5   1
 *
 * Pass 1 (key = 3, array[1]):
 *   Compare 3 &lt; 6   -> shift 6 right
 *   Insert 3 at index 0
 *   Array:          3   6   5   1
 *
 * Pass 2 (key = 5, array[2]):
 *   Compare 5 &lt; 6   -> shift 6 right
 *   Compare 5 &gt; 3   -> stop
 *   Insert 5 at index 1
 *   Array:          3   5   6   1
 *
 * Pass 3 (key = 1, array[3]):
 *   Compare 1 &lt; 6   -> shift 6 right
 *   Compare 1 &lt; 5   -> shift 5 right
 *   Compare 1 &lt; 3   -> shift 3 right
 *   Insert 1 at index 0
 *   Array:          1   3   5   6
 *
 * Final result:     1   3   5   6
 * </pre>
 */
public final class InsertionSort {

    private InsertionSort() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Sorts {@code array} in place in ascending order using Insertion Sort.
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
        for (int i = 1; i < array.length; i++) {
            T key = array[i];
            int position = i - 1;
            while (position >= 0 && array[position].compareTo(key) > 0) {
                array[position + 1] = array[position];
                position--;
            }
            array[position + 1] = key;
        }
    }
}
