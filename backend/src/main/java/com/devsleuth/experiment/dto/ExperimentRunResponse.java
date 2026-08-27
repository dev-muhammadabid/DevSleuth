package com.devsleuth.experiment.dto;

import com.devsleuth.experiment.ExperimentMode;
import com.devsleuth.experiment.entity.ExperimentRun;

import java.time.Instant;
import java.util.UUID;

/**
 * Response describing an experiment run and, when COMPLETED, its metrics.
 */
public record ExperimentRunResponse(
        UUID id,
        UUID experimentId,
        ExperimentMode mode,
        String status,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        ExperimentMetricResponse metrics  // nullable, populated when COMPLETED
) {
    public static ExperimentRunResponse from(ExperimentRun run, ExperimentMetricResponse metrics) {
        return new ExperimentRunResponse(
                run.getId(),
                run.getExperimentId(),
                run.getMode(),
                run.getStatus(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getCompletedAt(),
                metrics
        );
    }

    public static ExperimentRunResponse from(ExperimentRun run) {
        return from(run, null);
    }
}
