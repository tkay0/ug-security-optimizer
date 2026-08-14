package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.AlgorithmRun;

/**
 * Records and retrieves algorithm benchmark runs. A real implementation wraps
 * {@code AlgorithmRunDao}; {@code InMemoryReportService} exists for
 * development before that lands.
 *
 * <p>{@code ReportMenu} still performs the actual timed sort itself (that is
 * the point of the tab) and only hands the finished {@link AlgorithmRun} to
 * {@link #record} instead of appending to a private list.</p>
 */
public interface ReportService {

    AlgorithmRun record(AlgorithmRun run);

    List<AlgorithmRun> findAll();
}
