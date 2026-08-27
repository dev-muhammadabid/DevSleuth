package com.devsleuth.experiment.dto;

import com.devsleuth.experiment.entity.ExperimentMetric;

import java.util.UUID;

/**
 * Metrics produced by a completed experiment run.
 */
public record ExperimentMetricResponse(
        UUID runId,
        int truePositives,
        int falsePositives,
        int falseNegatives,
        double precisionScore,
        double recallScore,
        double f1Score,
        long analysisTimeMs
) {
    public static ExperimentMetricResponse from(ExperimentMetric metric) {
        return new ExperimentMetricResponse(
                metric.getRunId(),
                metric.getTruePositives(),
                metric.getFalsePositives(),
                metric.getFalseNegatives(),
                metric.getPrecisionScore(),
                metric.getRecallScore(),
                metric.getF1Score(),
                metric.getAnalysisTimeMs()
        );
    }
}
