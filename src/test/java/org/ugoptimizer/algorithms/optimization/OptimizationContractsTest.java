package org.ugoptimizer.algorithms.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptimizationContractsTest {

    @Test
    void itemRejectsNegativeCostAndBenefit() {
        assertThrows(IllegalArgumentException.class,
                () -> new OptimizationItem(1, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new OptimizationItem(1, 0, -1));
    }

    @Test
    void itemAllowsExtremeIdentifiersAndZeroValues() {
        OptimizationItem minimum = new OptimizationItem(Integer.MIN_VALUE, 0, 0);
        OptimizationItem maximum = new OptimizationItem(Integer.MAX_VALUE, 0, 0);

        assertEquals(Integer.MIN_VALUE, minimum.getItemId());
        assertEquals(Integer.MAX_VALUE, maximum.getItemId());
        assertEquals(0, minimum.getCost());
        assertEquals(0, minimum.getBenefit());
    }

    @Test
    void resultDefensivelyCopiesSuppliedAndReturnedArrays() {
        OptimizationItem first = new OptimizationItem(10, 2, 4);
        OptimizationItem second = new OptimizationItem(20, 3, 5);
        OptimizationItem[] supplied = {first, second};
        OptimizationResult result = new OptimizationResult(supplied, 5);

        supplied[0] = new OptimizationItem(99, 0, 0);
        OptimizationItem[] returnedItems = result.getSelectedItems();
        int[] returnedIds = result.getSelectedItemIds();
        returnedItems[0] = supplied[0];
        returnedIds[0] = 99;

        assertNotSame(supplied, result.getSelectedItems());
        assertArrayEquals(new OptimizationItem[] {first, second}, result.getSelectedItems());
        assertArrayEquals(new int[] {10, 20}, result.getSelectedItemIds());
        assertEquals(2, result.getSelectedCount());
        assertEquals(5L, result.getTotalCost());
        assertEquals(9L, result.getTotalBenefit());
        assertEquals(5, result.getCapacity());
    }

    @Test
    void resultRejectsInvalidConstruction() {
        OptimizationItem item = new OptimizationItem(1, 2, 3);

        assertThrows(NullPointerException.class,
                () -> new OptimizationResult(null, 2));
        assertThrows(NullPointerException.class,
                () -> new OptimizationResult(new OptimizationItem[] {null}, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new OptimizationResult(new OptimizationItem[] {item}, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new OptimizationResult(new OptimizationItem[] {item}, 1));
    }

    @Test
    void resultUsesLongAggregatesWithoutIntOverflow() {
        OptimizationItem[] selected = {
            new OptimizationItem(1, 0, Integer.MAX_VALUE),
            new OptimizationItem(2, 0, Integer.MAX_VALUE)
        };

        OptimizationResult result = new OptimizationResult(selected, 0);

        assertEquals(4_294_967_294L, result.getTotalBenefit());
        assertEquals(0L, result.getTotalCost());
    }
}
