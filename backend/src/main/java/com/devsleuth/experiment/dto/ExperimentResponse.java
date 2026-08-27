package com.devsleuth.experiment.dto;

import com.devsleuth.experiment.entity.Experiment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response describing an experiment for list/detail views.
 */
public record ExperimentResponse(
        UUID id,
        String name,
        String description,
        DatasetSummary datasetSummary,
        int groundTruthCount,
        Instant createdAt
) {
    public record DatasetSummary(int fileCount) {}

    public static ExperimentResponse from(Experiment experiment) {
        int fileCount = experiment.getDataset() == null ? 0 : experiment.getDataset().size();
        List<?> gt = experiment.getGroundTruth();
        int groundTruthCount = gt == null ? 0 : gt.size();
        return new ExperimentResponse(
                experiment.getId(),
                experiment.getName(),
                experiment.getDescription(),
                new DatasetSummary(fileCount),
                groundTruthCount,
                experiment.getCreatedAt()
        );
    }
}
