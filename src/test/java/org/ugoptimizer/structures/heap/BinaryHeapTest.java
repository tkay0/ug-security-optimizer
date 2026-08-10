package org.ugoptimizer.structures.heap;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryHeapTest {

    /** Larger than the heap's initial backing-array capacity, so growth is exercised. */
    private static final int LARGE_COUNT = 1000;

    @Test
    void pollReturnsElementsInAscendingOrderByDefault() {
        BinaryHeap<Integer> heap = heapOf(50, 10, 40, 20, 30);

        assertEquals(List.of(10, 20, 30, 40, 50), drain(heap));
    }

    @Test
    void peekReturnsSmallestWithoutRemovingIt() {
        BinaryHeap<Integer> heap = heapOf(50, 10, 40);

        assertEquals(10, heap.peek());
        assertEquals(10, heap.peek());
        assertEquals(3, heap.size());
    }

    @Test
    void singleElementHeapPeeksAndPollsThatElement() {
        BinaryHeap<Integer> heap = heapOf(7);

        assertEquals(7, heap.peek());
        assertEquals(7, heap.poll());
        assertTrue(heap.isEmpty());
    }

    @Test
    void alreadyAscendingInputStillPollsInOrder() {
        BinaryHeap<Integer> heap = heapOf(1, 2, 3, 4, 5, 6);

        assertEquals(List.of(1, 2, 3, 4, 5, 6), drain(heap));
    }

    @Test
    void descendingInputStillPollsInOrder() {
        BinaryHeap<Integer> heap = heapOf(6, 5, 4, 3, 2, 1);

        assertEquals(List.of(1, 2, 3, 4, 5, 6), drain(heap));
    }

    @Test
    void duplicateValuesAreAllRetainedAndPolledInOrder() {
        BinaryHeap<Integer> heap = heapOf(5, 1, 5, 1, 3);

        assertEquals(5, heap.size());
        assertEquals(List.of(1, 1, 3, 5, 5), drain(heap));
    }

    @Test
    void interleavedAddAndPollMaintainsHeapOrder() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();

        heap.add(30);
        heap.add(20);
        assertEquals(20, heap.poll());

        heap.add(10);
        heap.add(25);
        assertEquals(10, heap.poll());
        assertEquals(25, heap.poll());
        assertEquals(30, heap.poll());
        assertTrue(heap.isEmpty());
    }

    @Test
    void comparatorProducesMaxHeapOrdering() {
        BinaryHeap<Integer> heap = new BinaryHeap<>(Comparator.reverseOrder());
        for (int value : new int[]{10, 50, 20, 40, 30}) {
            heap.add(value);
        }

        assertEquals(50, heap.peek());
        assertEquals(List.of(50, 40, 30, 20, 10), drain(heap));
    }

    @Test
    void comparatorOrdersDomainObjectsBySeverity() {
        BinaryHeap<Incident> heap = new BinaryHeap<>(Comparator.comparingInt(Incident::severity).reversed());
        heap.add(new Incident("INC001", 2));
        heap.add(new Incident("INC002", 9));
        heap.add(new Incident("INC003", 5));

        assertEquals("INC002", heap.poll().id());
        assertEquals("INC003", heap.poll().id());
        assertEquals("INC001", heap.poll().id());
    }

    @Test
    void removeRootReordersRemainingElements() {
        BinaryHeap<Integer> heap = heapOf(10, 20, 30, 40, 50);

        assertTrue(heap.remove(10));
        assertEquals(4, heap.size());
        assertEquals(List.of(20, 30, 40, 50), drain(heap));
    }

    @Test
    void removeInteriorElementSiftsReplacementDown() {
        BinaryHeap<Integer> heap = heapOf(10, 20, 30, 40, 50, 5);

        assertTrue(heap.remove(20));
        assertEquals(List.of(5, 10, 30, 40, 50), drain(heap));
    }

    @Test
    void removeInteriorElementSiftsReplacementUp() {
        // Layout is [5, 25, 15, 45, 35, 50, 20]; removing 45 moves the tail value 20
        // into a slot whose parent (25) is larger, forcing the sift-up branch. If the
        // replacement is sifted down instead, the drain comes out as [5, 15, 25, 20, ...].
        BinaryHeap<Integer> heap = heapOf(35, 45, 50, 5, 25, 20, 15);

        assertTrue(heap.remove(45));
        assertEquals(List.of(5, 15, 20, 25, 35, 50), drain(heap));
    }

    @Test
    void removeLastSlotElementLeavesHeapIntact() {
        // 4 occupies the final array slot, so no replacement or sifting is needed.
        BinaryHeap<Integer> heap = heapOf(1, 10, 2, 11, 12, 3, 4);

        assertTrue(heap.remove(4));
        assertEquals(List.of(1, 2, 3, 10, 11, 12), drain(heap));
    }

    @Test
    void removeOnlyElementEmptiesHeap() {
        BinaryHeap<Integer> heap = heapOf(42);

        assertTrue(heap.remove(42));
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    void removeDuplicateDropsExactlyOneOccurrence() {
        BinaryHeap<Integer> heap = heapOf(5, 5, 7);

        assertTrue(heap.remove(5));
        assertEquals(2, heap.size());
        assertEquals(List.of(5, 7), drain(heap));
    }

    @Test
    void removeMissingValueReturnsFalseAndLeavesHeapUnchanged() {
        BinaryHeap<Integer> heap = heapOf(10, 20, 30);

        assertFalse(heap.remove(99));
        assertEquals(3, heap.size());
        assertEquals(List.of(10, 20, 30), drain(heap));
    }

    @Test
    void removeFromEmptyHeapReturnsFalse() {
        assertFalse(new BinaryHeap<Integer>().remove(1));
    }

    @Test
    void removeNullReturnsFalseWithoutCrashing() {
        BinaryHeap<Integer> heap = heapOf(10, 20, 30);

        assertFalse(heap.remove(null));
        assertEquals(3, heap.size());
    }

    @Test
    void addNullIsRejectedAndLeavesHeapUnchanged() {
        BinaryHeap<Integer> heap = heapOf(30, 10, 20);

        NullPointerException error = assertThrows(NullPointerException.class, () -> heap.add(null));
        assertEquals("value cannot be null", error.getMessage());

        assertEquals(3, heap.size());
        assertEquals(10, heap.peek());
        assertEquals(List.of(10, 20, 30), drain(heap));
    }

    @Test
    void addNullToEmptyHeapLeavesItEmpty() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();

        assertThrows(NullPointerException.class, () -> heap.add(null));
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
        assertThrows(NoSuchElementException.class, heap::peek);
    }

    /** A rejected insertion must not leave the null parked in the backing array either. */
    @Test
    void rejectedNullAdditionDoesNotTouchBackingArray() throws Exception {
        BinaryHeap<Integer> heap = heapOf(30, 10, 20);

        assertThrows(NullPointerException.class, () -> heap.add(null));
        assertUnusedSlotsAreNull(heap);
        assertEquals(3, heap.size());
    }

    @Test
    void newHeapIsEmpty() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();

        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    void sizeTracksAddsAndPolls() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();

        heap.add(1);
        heap.add(2);
        assertEquals(2, heap.size());
        assertFalse(heap.isEmpty());

        heap.poll();
        assertEquals(1, heap.size());

        heap.poll();
        assertEquals(0, heap.size());
        assertTrue(heap.isEmpty());
    }

    @Test
    void clearEmptiesHeapAndAllowsReuse() {
        BinaryHeap<Integer> heap = heapOf(30, 10, 20);

        heap.clear();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());

        heap.add(99);
        assertEquals(1, heap.size());
        assertEquals(99, heap.peek());
    }

    @Test
    void clearOnEmptyHeapIsANoOp() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();

        heap.clear();
        assertTrue(heap.isEmpty());
    }

    @Test
    void peekOnEmptyHeapThrows() {
        NoSuchElementException error = assertThrows(NoSuchElementException.class, () -> new BinaryHeap<Integer>().peek());
        assertEquals("Heap is empty", error.getMessage());
    }

    @Test
    void pollOnEmptyHeapThrows() {
        NoSuchElementException error = assertThrows(NoSuchElementException.class, () -> new BinaryHeap<Integer>().poll());
        assertEquals("Heap is empty", error.getMessage());
    }

    @Test
    void pollAfterDrainingThrowsAgain() {
        BinaryHeap<Integer> heap = heapOf(1);

        heap.poll();
        assertThrows(NoSuchElementException.class, heap::poll);
    }

    @Test
    void backingArrayGrowsBeyondInitialCapacity() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();
        for (int value = LARGE_COUNT; value >= 1; value--) {
            heap.add(value);
        }

        assertEquals(LARGE_COUNT, heap.size());
        for (int expected = 1; expected <= LARGE_COUNT; expected++) {
            assertEquals(expected, heap.poll());
        }
        assertTrue(heap.isEmpty());
    }

    @Test
    void randomisedWorkloadMatchesPriorityQueue() {
        Random random = new Random(20260809L);
        BinaryHeap<Integer> heap = new BinaryHeap<>();
        PriorityQueue<Integer> reference = new PriorityQueue<>();

        for (int i = 0; i < 2000; i++) {
            int value = random.nextInt(500);
            heap.add(value);
            reference.add(value);
        }

        for (int i = 0; i < 500; i++) {
            Integer value = random.nextInt(500);
            assertEquals(reference.remove(value), heap.remove(value), "remove(" + value + ") disagreed");
        }

        assertEquals(reference.size(), heap.size());
        while (!reference.isEmpty()) {
            assertEquals(reference.poll(), heap.poll());
        }
        assertTrue(heap.isEmpty());
    }

    /**
     * The vacated slots of the backing array must be nulled so the heap does not keep
     * dead elements alive. That is invisible through the public API, so this test reads
     * the backing array reflectively and is deliberately coupled to the field name.
     */
    @Test
    void vacatedBackingArraySlotsAreCleared() throws Exception {
        BinaryHeap<Integer> heap = heapOf(30, 10, 20, 40, 50);

        heap.poll();
        assertUnusedSlotsAreNull(heap);

        heap.remove(40);
        assertUnusedSlotsAreNull(heap);

        heap.clear();
        assertUnusedSlotsAreNull(heap);
    }

    private static void assertUnusedSlotsAreNull(BinaryHeap<?> heap) throws Exception {
        Field field = BinaryHeap.class.getDeclaredField("heap");
        field.setAccessible(true);
        Object[] backing = (Object[]) field.get(heap);

        for (int i = heap.size(); i < backing.length; i++) {
            assertNull(backing[i], "Backing array slot " + i + " still holds a reference");
        }
    }

    private static BinaryHeap<Integer> heapOf(int... values) {
        BinaryHeap<Integer> heap = new BinaryHeap<>();
        for (int value : values) {
            heap.add(value);
        }

        return heap;
    }

    private static <T> List<T> drain(BinaryHeap<T> heap) {
        List<T> drained = new ArrayList<>();
        while (!heap.isEmpty()) {
            drained.add(heap.poll());
        }

        return drained;
    }

    private record Incident(String id, int severity) {
    }
}
