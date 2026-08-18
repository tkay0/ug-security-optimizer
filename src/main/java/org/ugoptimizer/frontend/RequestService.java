package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.ServiceRequest;

/** Swing-facing request contract backed by canonical request and workflow services. */
public interface RequestService {
  List<ServiceRequest> findAll();

  int nextRequestId();

  ServiceRequest add(ServiceRequest request);

  ServiceRequest updateStatus(int requestId, String newStatus);
}
