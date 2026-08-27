package com.devsleuth.experiment.entity;

import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "experiment_metrics")
public class ExperimentMetric extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "true_positives", nullable = false)
    private int truePositives;

    @Column(name = "false_positives", nullable = false)
    private int falsePositives;

    @Column(name = "false_negatives", nullable = false)
    private int falseNegatives;

    @Column(name = "precision_score", nullable = false)
    private double precisionScore;

    @Column(name = "recall_score", nullable = false)
    private double recallScore;

    @Column(name = "f1_score", nullable = false)
    private double f1Score;

    @Column(name = "analysis_time_ms", nullable = false)
    private long analysisTimeMs;

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public int getTruePositives() { return truePositives; }
    public void setTruePositives(int truePositives) { this.truePositives = truePositives; }
    public int getFalsePositives() { return falsePositives; }
    public void setFalsePositives(int falsePositives) { this.falsePositives = falsePositives; }
    public int getFalseNegatives() { return falseNegatives; }
    public void setFalseNegatives(int falseNegatives) { this.falseNegatives = falseNegatives; }
    public double getPrecisionScore() { return precisionScore; }
    public void setPrecisionScore(double precisionScore) { this.precisionScore = precisionScore; }
    public double getRecallScore() { return recallScore; }
    public void setRecallScore(double recallScore) { this.recallScore = recallScore; }
    public double getF1Score() { return f1Score; }
    public void setF1Score(double f1Score) { this.f1Score = f1Score; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public void setAnalysisTimeMs(long analysisTimeMs) { this.analysisTimeMs = analysisTimeMs; }
}
