package org.ugoptimizer.evidence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorrectnessEvidenceGeneratorTest {

    @Test
    void generatesVerifiedEvidenceForRequiredStructuresAndAlgorithms() {
        String evidence = new CorrectnessEvidenceGenerator().generate();

        assertTrue(evidence.contains("CUSTOM STRUCTURE TRACE"));
        assertTrue(evidence.contains("SCHEDULING STRUCTURE DEMONSTRATION"));
        assertTrue(evidence.contains("REQUEST INDEX DEMONSTRATION"));
        assertTrue(evidence.contains("BINARY SEARCH TRACE"));
        assertTrue(evidence.contains("INSERTION SORT TRACE"));
        assertTrue(evidence.contains("MERGE / QUICK SORT TRACE"));
        assertTrue(evidence.contains("DIJKSTRA TRACE"));
        assertTrue(evidence.contains("KRUSKAL / PRIM TRACE"));
        assertTrue(evidence.contains("Prim weight=4.0, Kruskal weight=4.0, equal=true"));
        assertTrue(evidence.contains("DYNAMIC PROGRAMMING TRACE"));
        assertTrue(evidence.contains("Verified optimum benefit: 7"));
    }
}
