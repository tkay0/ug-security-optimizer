package org.ugoptimizer.util;

/**
 * Project-level parameters reproducibly derived from the index numbers of the
 * 15 project members.
 *
 * <p>The complete derivation is documented in
 * {@code docs/INDEX_DERIVED_PARAMETERS.md}.</p>
 */
public final class ProjectParameters {

    /**
     * Weight reserved for incident/request priority calculations.
     *
     * <p>Derived as {@code 1 + (897 mod 5)}.</p>
     */
    public static final int PRIORITY_WEIGHT = 3;

    /**
     * Budget reserved for constrained DP and brute-force optimization.
     *
     * <p>Derived as {@code 50 + (897 mod 51)}.</p>
     */
    public static final int OPTIMIZATION_BUDGET = 80;

    /**
     * Deterministic seed reserved for reproducible benchmark input generation.
     *
     * <p>Derived from the sum of the final three digits of all member index
     * numbers.</p>
     */
    public static final long BENCHMARK_SEED = 7497L;

    private ProjectParameters() {
        // Utility class.
    }
}
