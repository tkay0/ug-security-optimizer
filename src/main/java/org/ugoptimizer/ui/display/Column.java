package org.ugoptimizer.ui.display;

import java.util.function.Function;

/**
 * One column of a {@link DataTablePanel}: a header label and how to read its
 * display value from a row object. Every screen defines its own columns
 * without writing a new table model.
 */
public record Column<T>(String header, Function<T, String> extractor) {
}
