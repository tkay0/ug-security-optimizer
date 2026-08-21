package org.ugoptimizer.performance;

import java.nio.file.Path;

/** Command-line entry point for reproducible benchmark export. */
public final class EfficiencyLabMain {

    private EfficiencyLabMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 2 || (args.length > 0
                && !"--quick".equals(args[0]) && !"--full".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: EfficiencyLabMain [--quick|--full] [output-directory]");
        }
        boolean full = args.length > 0 && "--full".equals(args[0]);
        Path output = args.length == 2
                ? Path.of(args[1])
                : Path.of("results", full ? "full-efficiency-lab" : "representative-efficiency-lab");
        EfficiencyLab lab = new EfficiencyLab();
        BenchmarkReport report = full ? lab.runFull() : lab.runRepresentative();
        new BenchmarkCsvWriter().write(output, report);
    }
}
