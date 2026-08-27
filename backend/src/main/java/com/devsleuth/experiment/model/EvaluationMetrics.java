package com.devsleuth.experiment.model;

/**
 * Evaluation metrics for an experiment run against a ground-truth dataset.
 */
public record EvaluationMetrics(
        int truePositives,
        int falsePositives,
        int falseNegatives,
        double precision,
        double recall,
        double f1,
        long analysisTimeMs
) {
    public static EvaluationMetrics compute(int tp, int fp, int fn, long timeMs) {
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;
        return new EvaluationMetrics(tp, fp, fn, precision, recall, f1, timeMs);
    }
}
