package org.ugoptimizer.structures.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BinarySearchTreeTest extends OrderedTreeContractTest {

    @Override
    protected OrderedTree<Integer, String> createTree() {
        return new BinarySearchTree<>();
    }

    @Test
    void ascendingInsertionProducesExpectedSkewAndRemainsSearchable() {
        BinarySearchTree<Integer, String> tree = new BinarySearchTree<>();
        for (int key = 0; key < 200; key++) {
            tree.put(key, Integer.toString(key));
        }

        assertEquals(200, tree.height());
        assertEquals("0", tree.get(0).orElseThrow());
        assertEquals("199", tree.get(199).orElseThrow());
        assertAscending(tree.entriesInOrder(), 200);
    }

    @Test
    void descendingInsertionProducesExpectedSkewAndRemainsSearchable() {
        BinarySearchTree<Integer, String> tree = new BinarySearchTree<>();
        for (int key = 199; key >= 0; key--) {
            tree.put(key, Integer.toString(key));
        }

        assertEquals(200, tree.height());
        assertEquals("0", tree.get(0).orElseThrow());
        assertEquals("199", tree.get(199).orElseThrow());
        assertAscending(tree.entriesInOrder(), 200);
    }

    @Test
    void repeatedRootDeletionWorksForSkewedTree() {
        BinarySearchTree<Integer, String> tree = new BinarySearchTree<>();
        for (int key = 0; key < 250; key++) {
            tree.put(key, Integer.toString(key));
        }

        for (int key = 0; key < 250; key++) {
            assertEquals(Integer.toString(key), tree.remove(key).orElseThrow());
            assertFalse(tree.containsKey(key));
            assertEquals(249 - key, tree.size());
        }
        assertTrue(tree.isEmpty());
    }

    @Test
    void largeInputSupportsLookupTraversalAndRemovalWithoutRecursion() {
        BinarySearchTree<Integer, String> tree = new BinarySearchTree<>();
        int size = 5_000;
        for (int key = 0; key < size; key++) {
            tree.put(key, Integer.toString(key));
        }

        assertEquals(Integer.toString(size - 1), tree.get(size - 1).orElseThrow());
        assertAscending(tree.entriesInOrder(), size);
        for (int key = 0; key < size; key += 2) {
            assertTrue(tree.remove(key).isPresent());
        }
        assertEquals(size / 2, tree.size());
        assertEquals(1, tree.firstEntry().orElseThrow().getKey());
        assertEquals(size - 1, tree.lastEntry().orElseThrow().getKey());
    }

    private static void assertAscending(TreeEntry<Integer, String>[] entries, int expectedSize) {
        assertEquals(expectedSize, entries.length);
        for (int index = 0; index < entries.length; index++) {
            assertEquals(index, entries[index].getKey());
            assertEquals(Integer.toString(index), entries[index].getValue());
        }
    }
}
