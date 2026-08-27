package com.devsleuth.experiment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A single file change in an experiment dataset.
 * Mapped to {@link com.devsleuth.analysis.model.AnalysisInput.FileChange} by the service.
 */
public record FileChangeDto(
        @NotBlank String filename,
        @NotBlank String status,  // added, modified, deleted
        String patch
) {}
