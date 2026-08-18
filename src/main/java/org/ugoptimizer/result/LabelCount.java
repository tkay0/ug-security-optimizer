package org.ugoptimizer.result;

import java.util.*;

/** Immutable label/count pair for GUI tables and reports. */
public final class LabelCount {
  private final String label;
  private final int count;

  public LabelCount(String l, int c) {
    label = Objects.requireNonNull(l);
    if (c < 0) throw new IllegalArgumentException("count cannot be negative");
    count = c;
  }

  public String getLabel() {
    return label;
  }

  public int getCount() {
    return count;
  }
}
