package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.ServiceRequest;

/** Swing-facing dispatch queue ordered by the canonical BinaryHeap policy. */
public interface PriorityService {
  List<ServiceRequest> priorityOrder();
}
