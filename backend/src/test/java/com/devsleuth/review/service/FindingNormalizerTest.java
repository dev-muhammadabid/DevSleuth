package com.devsleuth.review.service;

import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.review.entity.Review;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FindingNormalizerTest {

    private final FindingNormalizer normalizer = new FindingNormalizer();

    @Test
    void normalizesRawFindingToEntity() {
        Review review = new Review();
        RawFinding raw = new RawFinding(
                FindingSource.STATIC, FindingCategory.SECURITY, Severity.HIGH, 95,
                "SQL Injection", "User input in query", "Use parameterized queries",
                "src/Repo.java", 42, 42
        );

        List<Finding> result = normalizer.normalize(List.of(raw), review);

        assertEquals(1, result.size());
        Finding f = result.get(0);
        assertEquals(FindingSource.STATIC, f.getSource());
        assertEquals(FindingCategory.SECURITY, f.getCategory());
        assertEquals(Severity.HIGH, f.getSeverity());
        assertEquals(95, f.getConfidence());
        assertEquals("SQL Injection", f.getTitle());
        assertEquals("src/Repo.java", f.getFilePath());
        assertEquals(42, f.getLineStart());
        assertSame(review, f.getReview());
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertEquals(0, normalizer.normalize(List.of(), new Review()).size());
    }
}
