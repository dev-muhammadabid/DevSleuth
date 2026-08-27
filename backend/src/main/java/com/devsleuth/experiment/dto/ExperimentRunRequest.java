package com.devsleuth.experiment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for starting an experiment run. {@code mode} is the analysis mode
 * (STATIC_ONLY / AI_ONLY / HYBRID); it is parsed into {@link com.devsleuth.experiment.ExperimentMode}
 * by the controller, which returns 400 on an unrecognized value.
 */
public record ExperimentRunRequest(
        @NotBlank String mode
) {}
