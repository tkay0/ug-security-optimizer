package org.ugoptimizer.structures.stack;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomStackTest {

    @Test
    void newStackIsEmpty() {
        CustomStack<Integer> stack = new CustomStack<>();
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    void pushIncreasesSize() {
        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.size());
        assertTrue(!stack.isEmpty());
    }

    @Test
    void popReturnsItemsInLifoOrder() {
        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void peekReturnsTopWithoutRemoving() {
        CustomStack<String> stack = new CustomStack<>();
        stack.push("a");
        stack.push("b");

        assertEquals("b", stack.peek());
        assertEquals(2, stack.size());
    }

    @Test
    void popOnEmptyStackThrows() {
        CustomStack<Integer> stack = new CustomStack<>();
        assertThrows(NoSuchElementException.class, stack::pop);
    }

    @Test
    void peekOnEmptyStackThrows() {
        CustomStack<Integer> stack = new CustomStack<>();
        assertThrows(NoSuchElementException.class, stack::peek);
    }

    @Test
    void stackGrowsBeyondInitialCapacity() {
        CustomStack<Integer> stack = new CustomStack<>(2);
        for (int i = 0; i < 50; i++) {
            stack.push(i);
        }
        assertEquals(50, stack.size());
        for (int i = 49; i >= 0; i--) {
            assertEquals(i, stack.pop());
        }
        assertTrue(stack.isEmpty());
    }

    @Test
    void invalidInitialCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CustomStack<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new CustomStack<Integer>(-5));
    }

    @Test
    void pushAfterEmptyingWorksCorrectly() {
        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(1);
        stack.pop();
        assertTrue(stack.isEmpty());

        stack.push(2);
        assertEquals(2, stack.peek());
        assertEquals(1, stack.size());
    }
}
