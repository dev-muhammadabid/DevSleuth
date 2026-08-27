package com.devsleuth.analysis.model;

import java.util.List;
import java.util.UUID;

/**
 * Input to the analysis pipeline: the relevant Java files changed in a PR,
 * with their patches and (optionally) full content for context.
 */
public record AnalysisInput(
        UUID reviewId,
        String repositoryFullName,
        String commitSha,
        List<FileChange> files
) {

    public record FileChange(
            String filePath,
            String patch,
            String fullContent
    ) {}
}
