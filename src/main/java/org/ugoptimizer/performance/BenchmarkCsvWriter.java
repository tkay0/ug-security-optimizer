package org.ugoptimizer.performance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Exports raw and averaged measurements in chart-ready CSV form. */
public final class BenchmarkCsvWriter {

    private static final String HEADER = "experiment,algorithm,input_size,trial,runtime_ns,"
            + "average_runtime_ns,memory_kb_approx,seed,parameters,result_metric,date_run";

    public void write(Path directory, BenchmarkReport report) throws IOException {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(report, "report cannot be null");
        Files.createDirectories(directory);
        writeCsv(directory.resolve("benchmark-results.csv"), report.getRecords());
        Files.writeString(directory.resolve("environment.txt"), report.getEnvironment(),
                StandardCharsets.UTF_8);
    }

    private static void writeCsv(Path path, BenchmarkRecord[] records) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (BenchmarkRecord record : records) {
                writer.write(csv(record.experiment()));
                writer.write(',');
                writer.write(csv(record.algorithm()));
                writer.write(',');
                writer.write(Integer.toString(record.inputSize()));
                writer.write(',');
                writer.write(Integer.toString(record.trial()));
                writer.write(',');
                writer.write(Long.toString(record.runtimeNs()));
                writer.write(',');
                writer.write(Long.toString(record.averageRuntimeNs()));
                writer.write(',');
                writer.write(Long.toString(record.memoryKb()));
                writer.write(',');
                writer.write(Long.toString(record.seed()));
                writer.write(',');
                writer.write(csv(record.parameters()));
                writer.write(',');
                writer.write(csv(record.resultMetric()));
                writer.write(',');
                writer.write(record.dateRun().toString());
                writer.newLine();
            }
        }
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
