# Complexity Notes

## Asymptotic notation

- `O(f(n))` is an eventual upper bound; it describes growth no faster than a constant multiple of
  `f(n)`.
- `Ω(f(n))` is an eventual lower bound.
- `Θ(f(n))` gives both bounds and therefore a tight growth class.

For example, Linear Search is `O(n)`, `Ω(1)`, and `Θ(n)` in the worst case. Binary Search on sorted
input is `O(log n)`, `Ω(1)`, and `Θ(log n)` in the worst case. Actual nanoseconds include JVM
warm-up, allocation, caching and operating-system noise, so empirical curves need not match a
smooth theoretical function at every size.

## Primitive-operation count: Selection Sort

For `n` items, the inner comparison executes

```text
(n - 1) + (n - 2) + ... + 1 = n(n - 1) / 2
```

times for sorted, random and reverse inputs. It performs at most `n - 1` swaps. Thus its comparison
count is `Θ(n²)` in best, average and worst ordering cases, with `Θ(1)` auxiliary space.

## Primitive-operation count: Binary Search

After `k` unsuccessful comparisons, at most `n / 2^k` candidates remain. The loop stops once this
quantity is below one, so `k` is at most `floor(log2(n)) + 1`. Best case is one comparison; average
and worst cases are `Θ(log n)`, with `Θ(1)` auxiliary space.

## Expected empirical relationships

- Linear Search should grow roughly linearly; Binary Search should grow slowly after its untimed
  sorted-input setup.
- Selection/Insertion should eventually separate from Merge/Quick because quadratic work grows
  faster than `n log n` work.
- Ordered BST insertion/search exposes height `n`; red-black height remains logarithmic.
- Hash collisions generally rise with load factor, but resizing and the concrete key distribution
  can make short-range results non-monotonic.
- Heap insertion/extraction should exhibit logarithmic per-operation growth.
- Graph timings depend on both vertex and edge counts and on result validation costs documented by
  each algorithm, not only the headline textbook kernel.
