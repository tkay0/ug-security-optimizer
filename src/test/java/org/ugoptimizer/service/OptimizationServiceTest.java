package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.*;
import org.ugoptimizer.algorithms.optimization.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.result.*;

class OptimizationServiceTest {
  private final OptimizationService service = new OptimizationService();

  @Test
  void dpAndBruteForceMatchAtApprovedBudget() {
    RequestOptimizationCandidate[] c = {
      candidate(1, 40, 70), candidate(2, 30, 50), candidate(3, 50, 80)
    };
    OptimizationComparison x = service.compareExact(c);
    assertTrue(x.hasSameOptimum());
    assertEquals(80, x.getDynamicProgramming().getObjective().getCapacity());
    assertEquals(2, x.getDynamicProgramming().getSelectedRequests().length);
  }

  @Test
  void zeroCapacityAndInvalidCapacityAreHandled() {
    OptimizationItem[] items = {new OptimizationItem(1, 1, 2)};
    assertTrue(service.runDynamicProgramming(items, 0).isEmpty());
    assertTrue(service.runBruteForce(items, 0).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> service.runDynamicProgramming(items, -1));
  }

  @Test
  void bruteForceGuardIsPreserved() {
    OptimizationItem[] items =
        new OptimizationItem[BruteForceIncidentSelector.MAX_SUPPORTED_ITEMS + 1];
    for (int i = 0; i < items.length; i++) items[i] = new OptimizationItem(i, 1, 1);
    assertThrows(IllegalArgumentException.class, () -> service.runBruteForce(items));
  }

  private static RequestOptimizationCandidate candidate(int id, int cost, int benefit) {
    Instant t = Instant.parse("2026-08-14T00:00:00Z");
    return new RequestOptimizationCandidate(
        new ServiceRequest(
            id,
            1,
            2,
            "SECURITY_ESCORT",
            3,
            t,
            t.plusSeconds(60),
            "PENDING",
            "PATROL_OFFICER",
            null),
        cost,
        benefit);
  }
}
