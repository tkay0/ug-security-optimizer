package org.ugoptimizer.algorithms.traversal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class DepthFirstSearchTest {

    @Test
    void dfsWorksWithAdjacencyListGraph() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyListGraph());

        TraversalResult result = new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertEquals(4, result.getTotalVertexCount());
        assertEquals(4, result.getVisitedCount());
    }

    @Test
    void dfsWorksWithAdjacencyMatrixGraph() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyMatrixGraph());

        TraversalResult result = new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void dfsHasDeterministicAscendingOrder() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyListGraph());

        DepthFirstSearch dfs = new DepthFirstSearch();

        int[] first = dfs.traverse(graph, 1).getVisitOrder();
        int[] second = dfs.traverse(graph, 1).getVisitOrder();

        assertArrayEquals(first, second);
        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                first);
    }

    @Test
    void dfsHandlesIsolatedStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1},
                result.getVisitOrder());

        assertEquals(
                TraversalResult.Status.PARTIAL,
                result.getStatus());
    }

    @Test
    void dfsHandlesDisconnectedGraph() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(3, 4, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2},
                result.getVisitOrder());

        assertEquals(
                TraversalResult.Status.PARTIAL,
                result.getStatus());
    }

    @Test
    void dfsHandlesMissingStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 99);

        assertEquals(
                TraversalResult.Status.MISSING_START,
                result.getStatus());

        assertEquals(
                0,
                result.getVisitedCount());
    }

    @Test
    void dfsHandlesNegativeAndNonContiguousVertexIds() {
        WeightedGraph graph = new AdjacencyMatrixGraph();

        graph.addVertex(-10);
        graph.addVertex(5);
        graph.addVertex(100);

        graph.addEdge(-10, 5, 1.0);
        graph.addEdge(5, 100, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, -10);

        assertArrayEquals(
                new int[]{-10, 5, 100},
                result.getVisitOrder());
    }

    @Test
    void dfsHandlesExtremeVertexIds() {
        WeightedGraph graph = new AdjacencyListGraph();
        graph.addVertex(Integer.MIN_VALUE);
        graph.addVertex(0);
        graph.addVertex(Integer.MAX_VALUE);
        graph.addEdge(Integer.MIN_VALUE, 0, 1.0);
        graph.addEdge(0, Integer.MAX_VALUE, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, Integer.MIN_VALUE);

        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                result.getVisitOrder());
        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
    }

    @Test
    void dfsHandlesCyclesWithoutRepeatingVertices() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(3, 1, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3},
                result.getVisitOrder());

        assertEquals(
                3,
                result.getVisitedCount());
    }

    @Test
    void dfsRejectsNullGraph() {
        DepthFirstSearch dfs = new DepthFirstSearch();

        assertThrows(
                IllegalArgumentException.class,
                () -> dfs.traverse(null, 1));
    }

    @Test
    void dfsDoesNotModifyAdjacencyListGraph() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyListGraph());

        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();
        int[] vertexIdsBefore = graph.getVertexIds();
        Edge[] edgesBefore = graph.getEdges();

        new DepthFirstSearch().traverse(graph, 1);

        assertEquals(
                vertexCountBefore,
                graph.getVertexCount());

        assertEquals(
                edgeCountBefore,
                graph.getEdgeCount());
        assertArrayEquals(vertexIdsBefore, graph.getVertexIds());
        assertArrayEquals(edgesBefore, graph.getEdges());
    }

    @Test
    void dfsDoesNotModifyAdjacencyMatrixGraph() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyMatrixGraph());

        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();
        int[] vertexIdsBefore = graph.getVertexIds();
        Edge[] edgesBefore = graph.getEdges();

        new DepthFirstSearch().traverse(graph, 1);

        assertEquals(
                vertexCountBefore,
                graph.getVertexCount());

        assertEquals(
                edgeCountBefore,
                graph.getEdgeCount());
        assertArrayEquals(vertexIdsBefore, graph.getVertexIds());
        assertArrayEquals(edgesBefore, graph.getEdges());
    }

    @Test
    void dfsCrossEdgeCaseUsesCorrectDepthFirstOrder() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(2, 4, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void dfsStrongerCrossEdgeCaseUsesCorrectOrder() {
        WeightedGraph graph = new AdjacencyMatrixGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);
        graph.addVertex(5);
        graph.addVertex(6);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 4, 1.0);
        graph.addEdge(2, 5, 1.0);
        graph.addEdge(3, 4, 1.0);
        graph.addEdge(4, 6, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 4, 3, 6, 5},
                result.getVisitOrder());
    }

    @Test
    void dfsRequestsEachVisitedVertexNeighborSnapshotExactlyOnce() {
        WeightedGraph delegate = new AdjacencyListGraph();
        for (int vertexId = 1; vertexId <= 6; vertexId++) {
            delegate.addVertex(vertexId);
        }
        delegate.addEdge(1, 2, 1.0);
        delegate.addEdge(1, 3, 1.0);
        delegate.addEdge(2, 4, 1.0);
        delegate.addEdge(2, 5, 1.0);
        delegate.addEdge(3, 4, 1.0);
        delegate.addEdge(4, 6, 1.0);
        CountingWeightedGraph graph = new CountingWeightedGraph(delegate);

        TraversalResult result = new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(new int[]{1, 2, 4, 3, 6, 5}, result.getVisitOrder());
        for (int vertexId : result.getVisitOrder()) {
            assertEquals(1, graph.getNeighborRequestCount(vertexId));
        }
    }

    @Test
    void dfsHandlesDeepGraphIteratively() {
        final int vertexCount = 5000;
        WeightedGraph graph = new AdjacencyListGraph(0);
        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            graph.addVertex(vertexId);
            if (vertexId > 0) {
                graph.addEdge(vertexId - 1, vertexId, 1.0);
            }
        }

        TraversalResult result = new DepthFirstSearch().traverse(graph, 0);

        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertEquals(vertexCount, result.getTotalVertexCount());
        assertEquals(vertexCount, result.getVisitedCount());
        int[] visitOrder = result.getVisitOrder();
        for (int index = 0; index < visitOrder.length; index++) {
            assertEquals(index, visitOrder[index]);
        }
    }

    @Test
    void repeatedTraversalProducesSameResult() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyMatrixGraph());

        DepthFirstSearch dfs = new DepthFirstSearch();

        TraversalResult first = dfs.traverse(graph, 1);
        TraversalResult second = dfs.traverse(graph, 1);

        assertArrayEquals(
                first.getVisitOrder(),
                second.getVisitOrder());

        assertEquals(
                first.getStatus(),
                second.getStatus());
    }

    private static WeightedGraph createConnectedGraph(
            WeightedGraph graph) {

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(2, 4, 1.0);

        return graph;
    }

    private static final class CountingWeightedGraph implements WeightedGraph {
        private final WeightedGraph delegate;
        private final int[] vertexIds;
        private final int[] neighborRequestCounts;

        private CountingWeightedGraph(WeightedGraph delegate) {
            this.delegate = delegate;
            this.vertexIds = delegate.getVertexIds();
            this.neighborRequestCounts = new int[vertexIds.length];
        }

        private int getNeighborRequestCount(int vertexId) {
            for (int index = 0; index < vertexIds.length; index++) {
                if (vertexIds[index] == vertexId) {
                    return neighborRequestCounts[index];
                }
            }
            return 0;
        }

        @Override
        public int getVertexCount() {
            return delegate.getVertexCount();
        }

        @Override
        public int getEdgeCount() {
            return delegate.getEdgeCount();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean addVertex(int vertexId) {
            return delegate.addVertex(vertexId);
        }

        @Override
        public boolean removeVertex(int vertexId) {
            return delegate.removeVertex(vertexId);
        }

        @Override
        public boolean containsVertex(int vertexId) {
            return delegate.containsVertex(vertexId);
        }

        @Override
        public int[] getVertexIds() {
            return delegate.getVertexIds();
        }

        @Override
        public EdgeUpdate addEdge(int vertexAId, int vertexBId, double weight) {
            return delegate.addEdge(vertexAId, vertexBId, weight);
        }

        @Override
        public boolean removeEdge(int vertexAId, int vertexBId) {
            return delegate.removeEdge(vertexAId, vertexBId);
        }

        @Override
        public boolean containsEdge(int vertexAId, int vertexBId) {
            return delegate.containsEdge(vertexAId, vertexBId);
        }

        @Override
        public OptionalDouble getEdgeWeight(int vertexAId, int vertexBId) {
            return delegate.getEdgeWeight(vertexAId, vertexBId);
        }

        @Override
        public OptionalInt getDegree(int vertexId) {
            return delegate.getDegree(vertexId);
        }

        @Override
        public int[] getNeighborIds(int vertexId) {
            for (int index = 0; index < vertexIds.length; index++) {
                if (vertexIds[index] == vertexId) {
                    neighborRequestCounts[index]++;
                    break;
                }
            }
            return delegate.getNeighborIds(vertexId);
        }

        @Override
        public Edge[] getIncidentEdges(int vertexId) {
            return delegate.getIncidentEdges(vertexId);
        }

        @Override
        public Edge[] getEdges() {
            return delegate.getEdges();
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }
}
