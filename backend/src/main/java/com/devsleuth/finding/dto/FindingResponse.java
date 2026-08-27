package com.devsleuth.finding.dto;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;

import java.util.UUID;

public record FindingResponse(
        UUID id,
        FindingSource source,
        FindingCategory category,
        Severity severity,
        Integer confidence,
        String title,
        String description,
        String recommendation,
        String suggestedFix,
        String userVerdict,
        String filePath,
        Integer lineStart,
        Integer lineEnd
) {
    public static FindingResponse from(Finding f) {
        return new FindingResponse(
                f.getId(), f.getSource(), f.getCategory(), f.getSeverity(),
                f.getConfidence(), f.getTitle(), f.getDescription(),
                f.getRecommendation(), f.getSuggestedFix(), f.getUserVerdict(),
                f.getFilePath(), f.getLineStart(), f.getLineEnd()
        );
    }
}
