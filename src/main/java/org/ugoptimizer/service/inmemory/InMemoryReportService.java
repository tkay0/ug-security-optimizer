package org.ugoptimizer.service.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ugoptimizer.model.AlgorithmRun;
import org.ugoptimizer.frontend.ReportService;

/**
 * In-memory {@link ReportService}. Replace with a real DAO-backed
 * implementation once the database team's work lands.
 */
public final class InMemoryReportService implements ReportService {

    private final List<AlgorithmRun> runs = new ArrayList<>();

    @Override
    public AlgorithmRun record(AlgorithmRun run) {
        runs.add(run);
        return run;
    }

    @Override
    public List<AlgorithmRun> findAll() {
        return Collections.unmodifiableList(runs);
    }
}
