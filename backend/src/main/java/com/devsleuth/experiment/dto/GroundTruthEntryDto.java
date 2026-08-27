package com.devsleuth.experiment.dto;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A ground-truth entry describing an expected finding, used to score a run.
 * Mapped to {@link com.devsleuth.experiment.model.GroundTruthEntry} by the service.
 */
public record GroundTruthEntryDto(
        @NotBlank String filePath,
        @NotNull Integer lineStart,
        Integer lineEnd,
        @NotNull FindingCategory category,
        Severity severity,
        String title
) {}
