package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MergeSort}.
 *
 * <p>Covers the scenarios required by Team 3: empty array, single element,
 * already sorted, reverse sorted, duplicates, and random input, plus null
 * safety and confirmation that the algorithm modifies the input array in
 * place rather than returning a new one.</p>
 */
class MergeSortTest {

    @Test
    void sortEmptyArrayDoesNotThrow() {
        Integer[] values = {};
        assertDoesNotThrow(() -> MergeSort.sort(values));
        assertArrayEquals(new Integer[]{}, values);
    }

    @Test
    void sortSingleElementArrayDoesNotThrow() {
        Integer[] values = {42};
        MergeSort.sort(values);
        assertArrayEquals(new Integer[]{42}, values);
    }

    @Test
    void sortAlreadySortedArrayLeavesItUnchanged() {
        Integer[] values = {1, 2, 3, 4, 5};
        MergeSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, values);
    }

    @Test
    void sortReverseSortedArrayAscending() {
        Integer[] values = {9, 7, 5, 3, 1};
        MergeSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 7, 9}, values);
    }

    @Test
    void sortArrayWithDuplicatesOrdering() {
        Integer[] values = {4, 2, 4, 1, 2, 4};
        MergeSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 2, 4, 4, 4}, values);
    }

    @Test
    void sortRandomInputProducesAscendingOrder() {
        Random random = new Random(7);
        Integer[] values = new Integer[500];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(10_000);
        }
        MergeSort.sort(values);
        assertSorted(values);
    }

    @Test
    void sortNullInputDoesNotThrow() {
        assertDoesNotThrow(() -> MergeSort.sort(null));
    }

    @Test
    void sortMutatesInputInPlace() {
        Integer[] values = {6, 3, 5, 1};
        Integer[] originalReference = values;
        MergeSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 6}, values);
        assertSame(originalReference, values, "MergeSort.sort must mutate the caller's array, not return a new one");
    }

    @Test
    void sortIsStableForEqualKeys() {
        // Wrap values so we can tell apart two elements that compare equal
        // but originate from different positions, confirming stability.
        TaggedValue[] values = {
                new TaggedValue(2, "first-2"),
                new TaggedValue(1, "only-1"),
                new TaggedValue(2, "second-2"),
        };
        MergeSort.sort(values);
        assertArrayEquals(
                new TaggedValue[]{
                        new TaggedValue(1, "only-1"),
                        new TaggedValue(2, "first-2"),
                        new TaggedValue(2, "second-2"),
                },
                values
        );
        // Confirm relative order of the two equal-key elements was preserved.
        assertTrue(values[1].tag.equals("first-2") && values[2].tag.equals("second-2"));
    }

    private static void assertSorted(Integer[] values) {
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i - 1].compareTo(values[i]) <= 0,
                    "Array not sorted at index " + i + ": " + values[i - 1] + " > " + values[i]);
        }
    }

    /**
     * Small comparable wrapper used only to test Merge Sort's stability:
     * two elements with the same {@code key} compare equal, but we can
     * still tell them apart via {@code tag} to check their relative order
     * was preserved.
     */
    private static final class TaggedValue implements Comparable<TaggedValue> {
        private final int key;
        private final String tag;

        private TaggedValue(int key, String tag) {
            this.key = key;
            this.tag = tag;
        }

        @Override
        public int compareTo(TaggedValue other) {
            return Integer.compare(this.key, other.key);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TaggedValue)) {
                return false;
            }
            TaggedValue other = (TaggedValue) obj;
            return this.key == other.key && this.tag.equals(other.tag);
        }

        @Override
        public int hashCode() {
            return key * 31 + tag.hashCode();
        }

        @Override
        public String toString() {
            return tag + "(" + key + ")";
        }
    }
}
