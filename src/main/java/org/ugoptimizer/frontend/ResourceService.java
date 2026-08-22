package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.Resource;

/** Swing-facing resource contract with no DAO or JDBC exposure. */
public interface ResourceService {
  List<Resource> findAll();

  int nextResourceId();

  Resource add(Resource resource);
}
