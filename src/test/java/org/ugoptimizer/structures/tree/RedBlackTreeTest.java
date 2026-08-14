package org.ugoptimizer.structures.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedBlackTreeTest extends OrderedTreeContractTest {

    @Override
    protected OrderedTree<Integer, String> createTree() {
        return new RedBlackTree<>();
    }

    @Test
    void rootIsBlackAfterInsertionsAndRemovals() {
        RedBlackTree<Integer, String> tree = populatedTree(41);
        assertTrue(tree.hasBlackRoot());

        for (int key = 0; key < 41; key += 3) {
            tree.remove(key);
            assertTrue(tree.hasBlackRoot());
        }
    }

    @Test
    void noRedNodeHasRedChild() {
        RedBlackTree<Integer, String> tree = populatedTree(200);
        assertTrue(tree.hasNoRedRedViolation());

        for (int key = 1; key < 200; key += 2) {
            tree.remove(key);
            assertTrue(tree.hasNoRedRedViolation());
        }
    }

    @Test
    void everyRootToNullPathHasEqualBlackHeight() {
        RedBlackTree<Integer, String> tree = populatedTree(200);
        assertTrue(tree.hasConsistentBlackHeight());

        for (int key = 0; key < 200; key += 4) {
            tree.remove(key);
            assertTrue(tree.hasConsistentBlackHeight());
        }
    }

    @Test
    void ascendingTripleTriggersLeftRotationAndPreservesInvariants() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.put(10, "ten");
        tree.put(20, "twenty");
        tree.put(30, "thirty");

        assertEquals(2, tree.height());
        assertValid(tree);
    }

    @Test
    void descendingTripleTriggersRightRotationAndPreservesInvariants() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.put(30, "thirty");
        tree.put(20, "twenty");
        tree.put(10, "ten");

        assertEquals(2, tree.height());
        assertValid(tree);
    }

    @Test
    void redUncleCaseRecoloursWithoutViolatingProperties() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        int[] keys = {10, 5, 15, 1};
        for (int key : keys) {
            tree.put(key, Integer.toString(key));
        }

        assertValid(tree);
        assertEquals(3, tree.height());
    }

    @Test
    void ascendingAndDescendingInsertionsRemainLogarithmicallyBounded() {
        RedBlackTree<Integer, String> ascending = new RedBlackTree<>();
        RedBlackTree<Integer, String> descending = new RedBlackTree<>();
        int size = 2_000;
        for (int key = 0; key < size; key++) {
            ascending.put(key, Integer.toString(key));
            descending.put(size - key - 1, Integer.toString(size - key - 1));
        }

        assertHeightBound(ascending, size);
        assertHeightBound(descending, size);
        assertValid(ascending);
        assertValid(descending);
    }

    @Test
    void repeatedInsertUpdateAndRemovePreservesAllInvariants() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        int modulus = 257;

        for (int index = 0; index < modulus; index++) {
            int key = (index * 73) % modulus;
            assertEquals(OrderedTree.PutResult.INSERTED, tree.put(key, "v" + key));
            assertValid(tree);
        }
        for (int index = 0; index < modulus; index++) {
            int key = (index * 91) % modulus;
            assertEquals(OrderedTree.PutResult.UPDATED, tree.put(key, "updated" + key));
            assertValid(tree);
        }
        for (int index = 0; index < modulus; index++) {
            int key = (index * 137) % modulus;
            assertEquals("updated" + key, tree.remove(key).orElseThrow());
            assertValid(tree);
            assertHeightBound(tree, tree.size());
        }

        assertTrue(tree.isEmpty());
    }

    @Test
    void logarithmicHeightBoundHoldsAcrossProgressiveDeletion() {
        RedBlackTree<Integer, String> tree = populatedTree(1_024);

        for (int key = 0; key < 1_024; key++) {
            if ((key & 31) == 0) {
                assertHeightBound(tree, tree.size());
                assertValid(tree);
            }
            tree.remove(key);
        }
        assertEquals(0, tree.height());
        assertValid(tree);
    }

    private static RedBlackTree<Integer, String> populatedTree(int size) {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        for (int index = 0; index < size; index++) {
            int key = (index * 37) % size;
            tree.put(key, Integer.toString(key));
        }
        return tree;
    }

    private static void assertValid(RedBlackTree<Integer, String> tree) {
        assertTrue(tree.hasBlackRoot(), "root must be black");
        assertTrue(tree.hasNoRedRedViolation(), "red node cannot have a red child");
        assertTrue(tree.hasConsistentBlackHeight(), "black heights must match");
        assertTrue(tree.hasValidOrdering(), "BST ordering must remain valid");
    }

    private static void assertHeightBound(RedBlackTree<Integer, String> tree, int size) {
        if (size == 0) {
            assertEquals(0, tree.height());
            return;
        }
        int maximumHeight = (int) Math.ceil(2.0d * (Math.log(size + 1.0d) / Math.log(2.0d)));
        assertTrue(
                tree.height() <= maximumHeight,
                "height " + tree.height() + " exceeds red-black bound " + maximumHeight);
    }
}
