package org.ugoptimizer.result;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TraversalResultTest {

    @Test
    void representsCompleteTraversal() {
        TraversalResult result = TraversalResult.traversed(4, 3, new int[]{4, -1, 8});

        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertTrue(result.isComplete());
        assertEquals(3, result.getVisitedCount());
        assertEquals(3, result.getTotalVertexCount());
        assertTrue(result.containsVertex(-1));
    }

    @Test
    void representsPartialTraversal() {
        TraversalResult result = TraversalResult.traversed(2, 5, new int[]{2, 9});

        assertEquals(TraversalResult.Status.PARTIAL, result.getStatus());
        assertFalse(result.isComplete());
        assertFalse(result.containsVertex(5));
    }

    @Test
    void representsMissingStartWithEmptyVisitOrder() {
        TraversalResult result = TraversalResult.missingStart(-9, 4);

        assertEquals(TraversalResult.Status.MISSING_START, result.getStatus());
        assertEquals(-9, result.getStartVertexId());
        assertEquals(0, result.getVisitedCount());
        assertArrayEquals(new int[0], result.getVisitOrder());
    }

    @Test
    void defensivelyCopiesInputAndOutputArrays() {
        int[] input = {1, 2};
        TraversalResult result = TraversalResult.traversed(1, 2, input);
        input[0] = 99;

        int[] output = result.getVisitOrder();
        output[1] = 99;

        assertArrayEquals(new int[]{1, 2}, result.getVisitOrder());
    }

    @Test
    void rejectsNullOrderAndNegativeTotalCount() {
        assertThrows(NullPointerException.class,
                () -> TraversalResult.traversed(1, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.traversed(1, -1, new int[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.missingStart(1, -1));
    }

    @Test
    void rejectsDuplicatesAndTooManyVisitedVertices() {
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.traversed(1, 3, new int[]{1, 2, 1}));
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.traversed(1, 1, new int[]{1, 2}));
    }

    @Test
    void rejectsEmptyTraversalAndIncorrectFirstVertex() {
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.traversed(1, 1, new int[0]));
        assertThrows(IllegalArgumentException.class,
                () -> TraversalResult.traversed(1, 2, new int[]{2}));
    }
}
