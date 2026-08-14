package org.ugoptimizer.structures.disjointset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisjointSetTest {

    @Test
    void emptySetIsSupported() {
        DisjointSet set = new DisjointSet(new int[0]);

        assertEquals(0, set.size());
        assertEquals(0, set.getComponentCount());
        assertFalse(set.contains(0));
    }

    @Test
    void oneElementStartsAsItsOwnComponent() {
        DisjointSet set = new DisjointSet(new int[]{42});

        assertEquals(1, set.size());
        assertEquals(1, set.getComponentCount());
        assertEquals(42, set.find(42));
        assertTrue(set.connected(42, 42));
        assertFalse(set.union(42, 42));
    }

    @Test
    void multipleElementsInitiallyBelongToSeparateComponents() {
        DisjointSet set = new DisjointSet(new int[]{30, -4, 8});

        assertEquals(3, set.size());
        assertEquals(3, set.getComponentCount());
        assertFalse(set.connected(-4, 8));
        assertFalse(set.connected(8, 30));
    }

    @Test
    void unionReducesComponentCountAndRepeatedUnionReturnsFalse() {
        DisjointSet set = new DisjointSet(new int[]{1, 2, 3});

        assertTrue(set.union(1, 2));
        assertEquals(2, set.getComponentCount());
        assertTrue(set.connected(1, 2));
        assertFalse(set.union(2, 1));
        assertEquals(2, set.getComponentCount());
    }

    @Test
    void transitiveUnionsReturnConsistentRepresentative() {
        DisjointSet set = new DisjointSet(new int[]{10, 20, 30, 40});
        set.union(10, 20);
        set.union(30, 40);
        set.union(20, 30);

        int representative = set.find(10);
        assertEquals(representative, set.find(20));
        assertEquals(representative, set.find(30));
        assertEquals(representative, set.find(40));
        assertEquals(1, set.getComponentCount());
    }

    @Test
    void duplicateElementIdsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DisjointSet(new int[]{7, -2, 7}));
    }

    @Test
    void nullElementArrayIsRejected() {
        assertThrows(NullPointerException.class, () -> new DisjointSet(null));
    }

    @Test
    void unknownIdsAreRejectedConsistently() {
        DisjointSet set = new DisjointSet(new int[]{1, 2});

        assertThrows(IllegalArgumentException.class, () -> set.find(99));
        assertThrows(IllegalArgumentException.class, () -> set.connected(1, 99));
        assertThrows(IllegalArgumentException.class, () -> set.union(99, 2));
        assertEquals(2, set.getComponentCount());
    }

    @Test
    void supportsNegativeAndNonContiguousIds() {
        DisjointSet set = new DisjointSet(new int[]{500, -100, 7});

        assertTrue(set.contains(-100));
        assertTrue(set.contains(500));
        assertTrue(set.union(-100, 500));
        assertTrue(set.connected(500, -100));
        assertFalse(set.connected(7, -100));
    }

    @Test
    void supportsExtremeIntegerIds() {
        DisjointSet set = new DisjointSet(
                new int[]{Integer.MAX_VALUE, 0, Integer.MIN_VALUE});

        assertTrue(set.union(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertTrue(set.connected(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, set.find(Integer.MAX_VALUE));
    }

    @Test
    void equalRankUnionKeepsSmallerRootId() {
        DisjointSet set = new DisjointSet(new int[]{4, 3, 2, 1});
        set.union(4, 3);
        set.union(2, 1);

        assertEquals(3, set.find(4));
        assertEquals(1, set.find(2));

        set.union(3, 1);

        assertEquals(1, set.find(1));
        assertEquals(1, set.find(2));
        assertEquals(1, set.find(3));
        assertEquals(1, set.find(4));
    }

    @Test
    void longerUnionFindSequenceRemainsConsistentAfterCompression() {
        int[] ids = new int[32];
        for (int index = 0; index < ids.length; index++) {
            ids[index] = index * 10 - 100;
        }
        DisjointSet set = new DisjointSet(ids);

        for (int step = 1; step < ids.length; step *= 2) {
            for (int start = 0; start + step < ids.length; start += step * 2) {
                set.union(ids[start], ids[start + step]);
            }
        }

        assertEquals(1, set.getComponentCount());
        int representative = set.find(ids[0]);
        for (int id : ids) {
            assertTrue(set.connected(ids[0], id));
            assertEquals(representative, set.find(id));
        }
    }

    @Test
    void constructorDefensivelyCopiesInputIds() {
        int[] ids = new int[]{9, -1, 4};
        DisjointSet set = new DisjointSet(ids);

        ids[0] = 1000;
        ids[1] = 1001;
        ids[2] = 1002;

        assertTrue(set.contains(-1));
        assertTrue(set.contains(4));
        assertTrue(set.contains(9));
        assertFalse(set.contains(1000));
        assertEquals(3, set.getComponentCount());
    }
}
