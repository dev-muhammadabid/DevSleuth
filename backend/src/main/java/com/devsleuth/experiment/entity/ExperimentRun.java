package com.devsleuth.experiment.entity;

import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.experiment.ExperimentMode;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experiment_runs")
public class ExperimentRun extends BaseEntity {

    @Column(name = "experiment_id", nullable = false)
    private UUID experimentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentMode mode;

    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getExperimentId() { return experimentId; }
    public void setExperimentId(UUID experimentId) { this.experimentId = experimentId; }
    public ExperimentMode getMode() { return mode; }
    public void setMode(ExperimentMode mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
