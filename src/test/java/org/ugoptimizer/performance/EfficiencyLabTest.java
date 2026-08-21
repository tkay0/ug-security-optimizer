package org.ugoptimizer.performance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.util.ProjectParameters;

class EfficiencyLabTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void officialPlanUsesRequiredSizesAndDefensiveSnapshots() {
        assertArrayEquals(new int[]{100, 500, 1_000, 5_000, 10_000},
                EfficiencyLab.searchAndSortSizes());
        assertArrayEquals(new int[]{100, 500, 1_000, 5_000, 10_000, 20_000},
                EfficiencyLab.hashAndHeapSizes());
        assertArrayEquals(new int[]{100, 500, 1_000, 5_000, 10_000}, EfficiencyLab.treeSizes());
        assertArrayEquals(new int[]{50, 100, 200, 500}, EfficiencyLab.graphSizes());
        int[] snapshot = EfficiencyLab.graphSizes();
        snapshot[0] = -1;
        assertEquals(50, EfficiencyLab.graphSizes()[0]);
    }

    @Test
    void smallPlanProducesThreeRealTrialsAveragesAndValidatedMetrics() {
        BenchmarkReport report = new EfficiencyLab().runPlan(
                new int[]{10}, new int[]{10}, new int[]{10}, new int[]{5}, new int[]{4}, 3);

        BenchmarkRecord[] records = report.getRecords();
        assertTrue(records.length > 30);
        for (BenchmarkRecord record : records) {
            assertEquals(ProjectParameters.BENCHMARK_SEED, record.seed());
            assertTrue(record.trial() >= 1 && record.trial() <= 3);
            assertTrue(record.runtimeNs() >= 0);
            assertTrue(record.averageRuntimeNs() >= 0);
            assertFalse(record.resultMetric().isBlank());
        }
        assertTrue(hasMetric(records, "BinarySearch", "sorted=true"));
        assertTrue(hasMetric(records, "Prim", "matches_kruskal=true"));
        assertTrue(hasMetric(records, "DynamicProgramming", "exact_match=true"));
        assertTrue(hasMetric(records, "BruteForce", "exact_match=true"));
        assertTrue(hasMetric(records, "GreedyAssignment", "resource_id="));
        assertTrue(hasMetric(records, "HashTable", "collisions="));
        assertTrue(hasMetric(records, "BinaryHeap_INSERT", "top_request="));
        assertTrue(hasParameter(records, "BFS", "edges=7"));
        assertThreeTrialAverages(records);
    }

    @Test
    void csvExportContainsChartReadyHeaderRawRowsAndEnvironment() throws Exception {
        BenchmarkReport report = new EfficiencyLab().runPlan(
                new int[]{5}, new int[]{5}, new int[]{5}, new int[]{3}, new int[]{3}, 3);
        new BenchmarkCsvWriter().write(temporaryDirectory, report);

        String csv = Files.readString(temporaryDirectory.resolve("benchmark-results.csv"));
        assertTrue(csv.startsWith("experiment,algorithm,input_size,trial,runtime_ns,average_runtime_ns"));
        assertTrue(csv.contains("\"search\",\"LinearSearch\",5,1,"));
        String environment = Files.readString(temporaryDirectory.resolve("environment.txt"));
        assertTrue(environment.contains("Benchmark seed: " + ProjectParameters.BENCHMARK_SEED));
        assertTrue(environment.contains("Hardware model/CPU/RAM: record manually"));
    }

    @Test
    void rejectsFewerThanThreeTrials() {
        assertThrows(IllegalArgumentException.class, () -> new EfficiencyLab().runPlan(
                new int[]{5}, new int[]{5}, new int[]{5}, new int[]{3}, new int[]{3}, 2));
    }

    @Test
    void deterministicSeedReproducesParametersAndResultMetrics() {
        EfficiencyLab lab = new EfficiencyLab();
        BenchmarkRecord[] first = lab.runPlan(
                new int[]{8}, new int[]{8}, new int[]{8}, new int[]{5}, new int[]{4}, 3)
                .getRecords();
        BenchmarkRecord[] second = lab.runPlan(
                new int[]{8}, new int[]{8}, new int[]{8}, new int[]{5}, new int[]{4}, 3)
                .getRecords();

        assertEquals(first.length, second.length);
        for (int index = 0; index < first.length; index++) {
            assertEquals(first[index].experiment(), second[index].experiment());
            assertEquals(first[index].algorithm(), second[index].algorithm());
            assertEquals(first[index].inputSize(), second[index].inputSize());
            assertEquals(first[index].trial(), second[index].trial());
            assertEquals(first[index].parameters(), second[index].parameters());
            assertEquals(first[index].resultMetric(), second[index].resultMetric());
        }
    }

    private static boolean hasMetric(BenchmarkRecord[] records, String algorithm, String fragment) {
        for (BenchmarkRecord record : records) {
            if (record.algorithm().equals(algorithm) && record.resultMetric().contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasParameter(
            BenchmarkRecord[] records, String algorithm, String fragment) {
        for (BenchmarkRecord record : records) {
            if (record.algorithm().equals(algorithm) && record.parameters().contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static void assertThreeTrialAverages(BenchmarkRecord[] records) {
        assertEquals(0, records.length % 3);
        for (int start = 0; start < records.length; start += 3) {
            BenchmarkRecord first = records[start];
            long sum = 0L;
            for (int index = start; index < start + 3; index++) {
                assertEquals(first.experiment(), records[index].experiment());
                assertEquals(first.algorithm(), records[index].algorithm());
                assertEquals(first.inputSize(), records[index].inputSize());
                assertEquals(first.parameters(), records[index].parameters());
                assertEquals(index - start + 1, records[index].trial());
                sum += records[index].runtimeNs();
            }
            long average = sum / 3;
            for (int index = start; index < start + 3; index++) {
                assertEquals(average, records[index].averageRuntimeNs());
            }
        }
    }
}
