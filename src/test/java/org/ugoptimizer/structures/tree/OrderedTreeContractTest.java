package org.ugoptimizer.structures.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Reusable behavioral contract for every {@link OrderedTree} implementation. */
abstract class OrderedTreeContractTest {

    protected abstract OrderedTree<Integer, String> createTree();

    @Test
    void newTreeIsEmpty() {
        OrderedTree<Integer, String> tree = createTree();

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertTrue(tree.firstEntry().isEmpty());
        assertTrue(tree.lastEntry().isEmpty());
        assertArrayEquals(entries(), tree.entriesInOrder());
    }

    @Test
    void insertsAndRetrievesValuesByNaturalKeyOrder() {
        OrderedTree<Integer, String> tree = createTree();

        assertEquals(OrderedTree.PutResult.INSERTED, tree.put(20, "twenty"));
        assertEquals(OrderedTree.PutResult.INSERTED, tree.put(10, "ten"));
        assertEquals(OrderedTree.PutResult.INSERTED, tree.put(30, "thirty"));

        assertEquals("ten", tree.get(10).orElseThrow());
        assertEquals("twenty", tree.get(20).orElseThrow());
        assertEquals("thirty", tree.get(30).orElseThrow());
        assertEquals(3, tree.size());
        assertFalse(tree.isEmpty());
    }

    @Test
    void duplicateKeyUpdatesValueWithoutGrowingSize() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(7, "old");

        assertEquals(OrderedTree.PutResult.UPDATED, tree.put(7, "new"));

        assertEquals(1, tree.size());
        assertEquals("new", tree.get(7).orElseThrow());
        assertArrayEquals(entries(entry(7, "new")), tree.entriesInOrder());
    }

    @Test
    void containsDistinguishesExistingAndMissingKeys() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(4, "four");

        assertTrue(tree.containsKey(4));
        assertFalse(tree.containsKey(5));
        assertTrue(tree.get(5).isEmpty());
    }

    @Test
    void firstAndLastEntriesTrackCurrentExtremes() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(5, "five");
        tree.put(-2, "minus two");
        tree.put(19, "nineteen");
        tree.put(7, "seven");

        assertEquals(entry(-2, "minus two"), tree.firstEntry().orElseThrow());
        assertEquals(entry(19, "nineteen"), tree.lastEntry().orElseThrow());
    }

    @Test
    void inOrderSnapshotIsSortedByKey() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(8, "eight");
        tree.put(2, "two");
        tree.put(11, "eleven");
        tree.put(1, "one");
        tree.put(6, "six");

        assertArrayEquals(
                entries(
                        entry(1, "one"),
                        entry(2, "two"),
                        entry(6, "six"),
                        entry(8, "eight"),
                        entry(11, "eleven")),
                tree.entriesInOrder());
    }

    @Test
    void returnedSnapshotsCannotMutateTree() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(1, "one");
        tree.put(2, "two");
        TreeEntry<Integer, String>[] snapshot = tree.entriesInOrder();

        snapshot[0] = entry(99, "changed");

        assertArrayEquals(
                entries(entry(1, "one"), entry(2, "two")),
                tree.entriesInOrder());
        assertEquals(entry(1, "one"), tree.firstEntry().orElseThrow());
    }

    @Test
    void rejectsNullKeysAndValuesConsistently() {
        OrderedTree<Integer, String> tree = createTree();

        assertThrows(NullPointerException.class, () -> tree.put(null, "value"));
        assertThrows(NullPointerException.class, () -> tree.put(1, null));
        assertThrows(NullPointerException.class, () -> tree.get(null));
        assertThrows(NullPointerException.class, () -> tree.containsKey(null));
        assertThrows(NullPointerException.class, () -> tree.remove(null));
        assertEquals(0, tree.size());
    }

    @Test
    void clearRemovesDataAndTreeCanBeReused() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(3, "three");
        tree.put(1, "one");
        tree.put(5, "five");

        tree.clear();

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertArrayEquals(entries(), tree.entriesInOrder());
        assertTrue(tree.get(3).isEmpty());

        assertEquals(OrderedTree.PutResult.INSERTED, tree.put(4, "four"));
        assertEquals("four", tree.get(4).orElseThrow());
        assertEquals(1, tree.size());
    }

    @Test
    void supportsExtremeIntegerKeys() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(0, "zero");
        tree.put(Integer.MAX_VALUE, "maximum");
        tree.put(Integer.MIN_VALUE, "minimum");

        assertEquals("minimum", tree.get(Integer.MIN_VALUE).orElseThrow());
        assertEquals("maximum", tree.get(Integer.MAX_VALUE).orElseThrow());
        assertArrayEquals(
                entries(
                        entry(Integer.MIN_VALUE, "minimum"),
                        entry(0, "zero"),
                        entry(Integer.MAX_VALUE, "maximum")),
                tree.entriesInOrder());
    }

    @Test
    void removingMissingKeyLeavesTreeUnchanged() {
        OrderedTree<Integer, String> tree = createTree();
        tree.put(2, "two");

        assertTrue(tree.remove(99).isEmpty());
        assertEquals(1, tree.size());
        assertEquals("two", tree.get(2).orElseThrow());
    }

    @Test
    void removesLeafNode() {
        OrderedTree<Integer, String> tree = treeWith(10, 5, 15);

        assertEquals("five", tree.remove(5).orElseThrow());

        assertFalse(tree.containsKey(5));
        assertEquals(2, tree.size());
        assertArrayEquals(entries(entry(10, "ten"), entry(15, "fifteen")), tree.entriesInOrder());
    }

    @Test
    void removesNodeWithOneChild() {
        OrderedTree<Integer, String> tree = treeWith(10, 5, 15, 3);

        assertEquals("five", tree.remove(5).orElseThrow());

        assertTrue(tree.containsKey(3));
        assertFalse(tree.containsKey(5));
        assertArrayEquals(
                entries(entry(3, "three"), entry(10, "ten"), entry(15, "fifteen")),
                tree.entriesInOrder());
    }

    @Test
    void removesNodeWithTwoChildren() {
        OrderedTree<Integer, String> tree = treeWith(10, 5, 15, 3, 7, 12, 18);

        assertEquals("five", tree.remove(5).orElseThrow());

        assertFalse(tree.containsKey(5));
        assertTrue(tree.containsKey(3));
        assertTrue(tree.containsKey(7));
        assertArrayEquals(
                entries(
                        entry(3, "three"),
                        entry(7, "seven"),
                        entry(10, "ten"),
                        entry(12, "twelve"),
                        entry(15, "fifteen"),
                        entry(18, "eighteen")),
                tree.entriesInOrder());
    }

    @Test
    void removesRootAndPreservesRemainingAssociations() {
        OrderedTree<Integer, String> tree = treeWith(10, 5, 15, 3, 7, 12, 18);

        assertEquals("ten", tree.remove(10).orElseThrow());

        assertFalse(tree.containsKey(10));
        assertEquals(6, tree.size());
        assertEquals(entry(3, "three"), tree.firstEntry().orElseThrow());
        assertEquals(entry(18, "eighteen"), tree.lastEntry().orElseThrow());
    }

    @Test
    void repeatedRemovalsEventuallyEmptyTree() {
        OrderedTree<Integer, String> tree = treeWith(4, 2, 6, 1, 3, 5, 7);
        int[] removalOrder = {4, 1, 7, 2, 6, 3, 5};

        for (int i = 0; i < removalOrder.length; i++) {
            int key = removalOrder[i];
            assertTrue(tree.remove(key).isPresent());
            assertEquals(removalOrder.length - i - 1, tree.size());
            assertFalse(tree.containsKey(key));
        }

        assertTrue(tree.isEmpty());
        assertTrue(tree.remove(4).isEmpty());
    }

    private OrderedTree<Integer, String> treeWith(int... keys) {
        OrderedTree<Integer, String> tree = createTree();
        for (int key : keys) {
            tree.put(key, word(key));
        }
        return tree;
    }

    private static String word(int key) {
        return switch (key) {
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 10 -> "ten";
            case 12 -> "twelve";
            case 15 -> "fifteen";
            case 18 -> "eighteen";
            default -> Integer.toString(key);
        };
    }

    private static TreeEntry<Integer, String> entry(int key, String value) {
        return new TreeEntry<>(key, value);
    }

    @SafeVarargs
    private static TreeEntry<Integer, String>[] entries(TreeEntry<Integer, String>... entries) {
        return entries;
    }
}
