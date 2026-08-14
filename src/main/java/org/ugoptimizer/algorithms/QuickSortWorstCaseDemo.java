package org.ugoptimizer.algorithms;

import java.util.Random;

/**
 * Runnable demonstration of {@link QuickSort}'s {@code O(n²)} worst case.
 *
 * <p><b>Why this class exists</b></p>
 * <p>
 * {@link QuickSort} always picks the <b>last element</b> of the current
 * sub-array as its pivot. That choice means an already-sorted (or already
 * reverse-sorted) array is the worst possible input: every partition step
 * splits the array into one empty side and one side of size {@code n-1},
 * so the recursion never balances and the algorithm degrades from
 * {@code O(n log n)} to {@code O(n²)}.
 * </p>
 * <p>
 * This class contains a comparison-counting copy of the same Lomuto
 * partition logic used in {@link QuickSort#sort(Comparable[])} (kept
 * separate so the production algorithm stays free of instrumentation), and
 * a {@link #main(String[])} method that prints a side-by-side comparison
 * count for sorted input (worst case) versus random input (average case)
 * at increasing sizes. Run it directly to see the quadratic growth.
 * </p>
 *
 * <p>For an automated, deterministic check of the same behaviour (rather
 * than console output), see {@code QuickSortWorstCaseDemoTest}, which
 * asserts the exact closed-form comparison count {@code n(n-1)/2} for
 * sorted input of several sizes.</p>
 */
public final class QuickSortWorstCaseDemo {

    private QuickSortWorstCaseDemo() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Sorts {@code array} in place, identically to
     * {@link QuickSort#sort(Comparable[])}, and returns the number of
     * element comparisons performed.
     *
     * @param array the array to sort in place; may be {@code null}
     * @param <T>   the type of elements, comparable with itself
     * @return the total number of {@code compareTo} calls made while sorting
     */
    public static <T extends Comparable<T>> long sortCountingComparisons(T[] array) {
        if (array == null || array.length < 2) {
            return 0L;
        }
        long[] comparisons = {0L};
        quickSort(array, 0, array.length - 1, comparisons);
        return comparisons[0];
    }

    private static <T extends Comparable<T>> void quickSort(T[] array, int low, int high, long[] comparisons) {
        if (low >= high) {
            return;
        }
        int pivotIndex = partition(array, low, high, comparisons);
        quickSort(array, low, pivotIndex - 1, comparisons);
        quickSort(array, pivotIndex + 1, high, comparisons);
    }

    private static <T extends Comparable<T>> int partition(T[] array, int low, int high, long[] comparisons) {
        T pivot = array[high];
        int boundary = low - 1;

        for (int i = low; i < high; i++) {
            comparisons[0]++;
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

    /**
     * Builds an ascending sorted {@code Integer[]} of {@code 0..size-1},
     * the worst-case input for this pivot strategy.
     */
    static Integer[] sortedInput(int size) {
        Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++) {
            values[i] = i;
        }
        return values;
    }

    /**
     * Builds a shuffled {@code Integer[]} of {@code 0..size-1}, a
     * representative average-case input.
     */
    static Integer[] randomInput(int size, Random random) {
        Integer[] values = sortedInput(size);
        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Integer temp = values[i];
            values[i] = values[j];
            values[j] = temp;
        }
        return values;
    }

    /**
     * Prints a table comparing worst-case (sorted input) versus average-case
     * (random input) comparison counts as {@code n} grows, demonstrating the
     * {@code O(n²)} vs {@code O(n log n)} gap described in
     * {@link QuickSort}'s class-level Javadoc.
     */
    public static void main(String[] args) {
        Random random = new Random(42);
        int[] sizes = {10, 100, 1_000, 5_000, 10_000};

        System.out.println("QuickSort worst case (sorted input, last-element pivot)");
        System.out.println("vs. average case (shuffled random input)");
        System.out.println();
        System.out.printf("%10s %20s %20s %25s%n", "n", "sorted comparisons", "random comparisons", "n(n-1)/2 (worst case)");

        for (int n : sizes) {
            long sortedComparisons = sortCountingComparisons(sortedInput(n));
            long randomComparisons = sortCountingComparisons(randomInput(n, random));
            long expectedWorstCase = (long) n * (n - 1) / 2;

            System.out.printf("%10d %20d %20d %25d%n", n, sortedComparisons, randomComparisons, expectedWorstCase);
        }

        System.out.println();
        System.out.println("Observation: comparisons on sorted input match n(n-1)/2 exactly (O(n^2)),");
        System.out.println("while comparisons on random input grow much more slowly (~O(n log n)).");
    }
}
