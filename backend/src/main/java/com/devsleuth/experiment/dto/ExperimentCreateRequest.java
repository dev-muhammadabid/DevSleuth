package com.devsleuth.experiment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for creating an experiment.
 */
public record ExperimentCreateRequest(
        @NotBlank String name,
        String description,
        @NotEmpty @Valid List<FileChangeDto> dataset,
        @NotEmpty @Valid List<GroundTruthEntryDto> groundTruth
) {}
