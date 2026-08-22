package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.AlgorithmRun;

/** Swing-facing algorithm measurement persistence backed by PerformanceService. */
public interface ReportService {
  AlgorithmRun record(AlgorithmRun run);

  default org.ugoptimizer.result.SystemReport generateSystemReport() {
    throw new UnsupportedOperationException("System reports require the backend service");
  }

  List<AlgorithmRun> findAll();
}
