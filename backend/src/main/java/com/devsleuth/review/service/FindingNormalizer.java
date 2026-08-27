package com.devsleuth.review.service;

import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.review.entity.Review;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Converts RawFinding records (from analyzers/AI) into Finding JPA entities.
 */
@Service
public class FindingNormalizer {

    public List<Finding> normalize(List<RawFinding> rawFindings, Review review) {
        return rawFindings.stream()
                .map(raw -> toEntity(raw, review))
                .toList();
    }

    private Finding toEntity(RawFinding raw, Review review) {
        Finding f = new Finding();
        f.setReview(review);
        f.setSource(raw.source());
        f.setCategory(raw.category());
        f.setSeverity(raw.severity());
        f.setConfidence(raw.confidence());
        f.setTitle(raw.title());
        f.setDescription(raw.description());
        f.setRecommendation(raw.recommendation());
        f.setFilePath(raw.filePath());
        f.setLineStart(raw.lineStart());
        f.setLineEnd(raw.lineEnd());
        // fingerprint set during deduplication
        f.setFingerprint("");
        return f;
    }
}
