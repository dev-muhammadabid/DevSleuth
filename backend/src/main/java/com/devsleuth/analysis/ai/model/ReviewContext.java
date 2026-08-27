package com.devsleuth.analysis.ai.model;

import java.util.List;

/**
 * Token-efficient context sent to the LLM.
 */
public record ReviewContext(
        String repositoryName,
        String prTitle,
        String commitSha,
        List<FileContext> files
) {
    public record FileContext(
            String filePath,
            String patch,
            String surroundingCode
    ) {}
}
