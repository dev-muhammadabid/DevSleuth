package com.devsleuth.review.dto;

import com.devsleuth.analysis.ai.MultiModelService.ComparisonResult;
import com.devsleuth.analysis.ai.MultiModelService.ModelResult;
import com.devsleuth.analysis.ai.model.AiResponse.AiFinding;

import java.util.List;

public record MultiModelResponse(
        ModelSummary openai,
        ModelSummary anthropic
) {
    public record ModelSummary(
            String provider,
            int findingCount,
            long durationMs,
            String error,
            List<AiFinding> findings
    ) {}

    public static MultiModelResponse from(ComparisonResult result) {
        return new MultiModelResponse(
                toSummary(result.openai()),
                toSummary(result.anthropic())
        );
    }

    private static ModelSummary toSummary(ModelResult r) {
        return new ModelSummary(
                r.provider(),
                r.findings().size(),
                r.durationMs(),
                r.error(),
                r.findings()
        );
    }
}
