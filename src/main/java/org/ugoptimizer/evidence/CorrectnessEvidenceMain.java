package org.ugoptimizer.evidence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Command-line exporter for the deterministic correctness-evidence text. */
public final class CorrectnessEvidenceMain {

    private CorrectnessEvidenceMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: CorrectnessEvidenceMain [output-file]");
        }
        Path output = args.length == 1
                ? Path.of(args[0])
                : Path.of("results", "correctness-evidence.txt");
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, new CorrectnessEvidenceGenerator().generate(), StandardCharsets.UTF_8);
    }
}
