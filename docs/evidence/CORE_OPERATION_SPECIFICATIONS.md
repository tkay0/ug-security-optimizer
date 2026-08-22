# Core Operation Specifications

These five examiner-facing operations define the primary inputs, outputs, assumptions, and
failure outcomes. They correspond directly to executable Swing tabs and backend contracts.

## 1. Schedule current requests

- Input: at least four canonical `ServiceRequest` snapshots.
- Output: FIFO order, circular wrap trace, urgent deque order, and heap priority order.
- Assumptions: urgency is 1–5; heap ties use deadline then request ID.
- Failure: null input/elements or fewer than four requests is rejected; inputs are not mutated.

```text
enqueue requests in FIFO and circular queue
dequeue one circular item; enqueue fourth item to force wrap
add urgency 4–5 to deque front, others to rear
insert all into heap ordered by urgency desc, deadline, ID
drain each structure and return trace
```

## 2. Search request indexes

- Input: current request snapshots and target request ID.
- Output: BST/RB/B-tree search paths, agreement, in-order IDs, height and balancing/split evidence.
- Assumptions: request IDs are unique persisted identifiers.
- Failure: null input/elements is rejected; an absent target is a normal `Found: false` result.

```text
insert every request into BST, red-black tree and B-tree by request ID
search each tree with a comparison-recording key
compare found results
return paths, ordered IDs, heights, rotations/recolours and split state
```

## 3. Find a route

- Input: immutable `WeightedGraph`, source ID, destination ID, optional persisted road scenario.
- Output: `PathResult` with status, vertex path, edges and total weight.
- Assumptions: weights are finite and non-negative; graph snapshots are deterministic.
- Failure: missing endpoints and unreachable destinations have explicit result statuses.

```text
load baseline or scenario graph from SQLite
map external IDs to dense indexes
run Dijkstra with the custom binary heap
reconstruct predecessor edges if destination settles
return explicit found/missing/unreachable result
```

## 4. Build a minimum connection network

- Input: immutable undirected `WeightedGraph`.
- Output: `MSTResult` containing an MST or minimum spanning forest, component count and weight.
- Assumptions: every undirected edge is emitted once by the graph contract.
- Failure: disconnected input is represented as `DISCONNECTED`, not as an exception.

```text
sort edges by weight and endpoint IDs
create one disjoint-set component per vertex
accept each edge only when union joins two components
return selected edges, remaining components and total weight
```

## 5. Select incidents under a budget

- Input: immutable optimization items `(ID, cost, benefit)` and non-negative capacity.
- Output: selected item snapshots, total cost and total benefit.
- Assumptions: each item may be chosen at most once.
- Failure: invalid/null inputs are rejected; brute force rejects more than its safe item bound.

```text
for each item from last to first
  for each available capacity
    compare exclude with include + best remaining state
    store deterministic winning decision
reconstruct selected items from stored decisions
return immutable optimum
```
