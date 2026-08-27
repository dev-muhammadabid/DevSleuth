package com.devsleuth.analysis.ai.model;

import java.util.List;

/**
 * Structured response expected from the LLM.
 */
public record AiResponse(List<AiFinding> findings) {

    public record AiFinding(
            String category,
            String severity,
            Double confidence,
            String title,
            String description,
            String recommendation,
            String filePath,
            Integer lineStart,
            Integer lineEnd
    ) {}
}
