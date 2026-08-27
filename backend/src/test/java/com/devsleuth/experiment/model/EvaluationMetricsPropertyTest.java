package com.devsleuth.experiment.model;

import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Feature: pull-requests-and-experiments, Property 7: Metrics computation correctness
 *
 * Property 7: For any non-negative integers tp, fp, fn where tp+fp > 0 and tp+fn > 0,
 * EvaluationMetrics.compute(tp, fp, fn, timeMs) SHALL produce:
 *   - precision == tp / (tp + fp)
 *   - recall    == tp / (tp + fn)
 *   - f1        == 2 * precision * recall / (precision + recall)  (0 when precision+recall == 0)
 *   - analysisTimeMs == timeMs
 *
 * compute() is a pure static factory over primitive inputs, so the property is validated
 * by calling it directly with jqwik-generated (tp, fp, fn, timeMs) tuples. Counts are
 * constrained so the two denominators are positive, and timeMs is any non-negative long.
 *
 * Validates: Requirements 6.2
 */
class EvaluationMetricsPropertyTest {

    private static final double EPSILON = 1e-9;

    @Property(tries = 200)
    void metricsComputationMatchesFormulas(
            @ForAll("validCounts") Counts counts,
            @ForAll @LongRange(min = 0L, max = 3_600_000L) long timeMs) {
        int tp = counts.tp();
        int fp = counts.fp();
        int fn = counts.fn();

        EvaluationMetrics metrics = EvaluationMetrics.compute(tp, fp, fn, timeMs);

        double expectedPrecision = (double) tp / (tp + fp);
        double expectedRecall = (double) tp / (tp + fn);

        assertThat(metrics.precision()).isCloseTo(expectedPrecision, within(EPSILON));
        assertThat(metrics.recall()).isCloseTo(expectedRecall, within(EPSILON));

        double denom = expectedPrecision + expectedRecall;
        double expectedF1 = denom > 0 ? 2 * expectedPrecision * expectedRecall / denom : 0.0;
        assertThat(metrics.f1()).isCloseTo(expectedF1, within(EPSILON));

        // f1 is 0 exactly when both precision and recall are 0 (i.e. tp == 0).
        if (tp == 0) {
            assertThat(metrics.f1()).isEqualTo(0.0);
        }

        assertThat(metrics.analysisTimeMs()).isEqualTo(timeMs);
        // Raw counts are echoed back unchanged.
        assertThat(metrics.truePositives()).isEqualTo(tp);
        assertThat(metrics.falsePositives()).isEqualTo(fp);
        assertThat(metrics.falseNegatives()).isEqualTo(fn);
    }

    /**
     * Non-negative (tp, fp, fn) constrained so tp+fp > 0 and tp+fn > 0. When tp == 0 both
     * fp and fn are forced to be >= 1 so neither denominator is zero.
     */
    @Provide
    Arbitrary<Counts> validCounts() {
        Arbitrary<Integer> tp = Arbitraries.integers().between(0, 10_000);
        Arbitrary<Integer> fp = Arbitraries.integers().between(0, 10_000);
        Arbitrary<Integer> fn = Arbitraries.integers().between(0, 10_000);
        return Combinators.combine(tp, fp, fn)
                .as(Counts::new)
                .filter(c -> (c.tp() + c.fp()) > 0 && (c.tp() + c.fn()) > 0);
    }

    record Counts(int tp, int fp, int fn) {}
}
