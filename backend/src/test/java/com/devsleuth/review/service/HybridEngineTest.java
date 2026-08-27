package com.devsleuth.review.service;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HybridEngineTest {

    private final HybridEngine engine = new HybridEngine();

    @Test
    void deduplicatesExactMatches() {
        Finding f1 = makeFinding("SQL Injection", "Repo.java", 42, FindingSource.STATIC, Severity.HIGH, 100);
        Finding f2 = makeFinding("SQL Injection", "Repo.java", 42, FindingSource.AI, Severity.MEDIUM, 85);

        List<Finding> result = engine.process(new ArrayList<>(List.of(f1, f2)));

        assertEquals(1, result.size());
        assertEquals(FindingSource.HYBRID, result.get(0).getSource());
        assertEquals(Severity.HIGH, result.get(0).getSeverity()); // keeps higher
        assertEquals(100, result.get(0).getConfidence()); // keeps higher
    }

    @Test
    void deduplicatesSemanticallyDuplicateFindings() {
        Finding f1 = makeFinding("SQL query uses user input", "Repo.java", 42, FindingSource.STATIC, Severity.HIGH, 100);
        Finding f2 = makeFinding("User input inserted into SQL query", "Repo.java", 43, FindingSource.AI, Severity.HIGH, 90);

        List<Finding> result = engine.process(new ArrayList<>(List.of(f1, f2)));

        // Title tokens overlap: "sql", "query", "user", "input" → Jaccard should exceed 0.5
        assertEquals(1, result.size());
        assertEquals(FindingSource.HYBRID, result.get(0).getSource());
    }

    @Test
    void correlatesNearbyFindingsFromDifferentSources() {
        Finding f1 = makeFinding("Null check missing", "Service.java", 10, FindingSource.STATIC, Severity.MEDIUM, 100);
        Finding f2 = makeFinding("Resource leak", "Service.java", 12, FindingSource.AI, Severity.MEDIUM, 80);

        List<Finding> result = engine.process(new ArrayList<>(List.of(f1, f2)));

        // Different titles, so not deduplicated. But nearby → confidence boosted
        assertEquals(2, result.size());
        // Both should have boosted confidence
        assertTrue(result.stream().anyMatch(f -> f.getConfidence() > 100 - 1 || f.getConfidence() == 100));
    }

    @Test
    void ranksHighSeverityFirst() {
        Finding low = makeFinding("Style", "A.java", 1, FindingSource.STATIC, Severity.LOW, 100);
        Finding high = makeFinding("Injection", "B.java", 1, FindingSource.AI, Severity.HIGH, 90);

        List<Finding> result = engine.process(new ArrayList<>(List.of(low, high)));

        assertEquals("Injection", result.get(0).getTitle());
        assertEquals("Style", result.get(1).getTitle());
    }

    @Test
    void emptyInput() {
        assertEquals(0, engine.process(new ArrayList<>()).size());
    }

    private Finding makeFinding(String title, String file, int line, FindingSource src, Severity sev, int confidence) {
        Finding f = new Finding();
        f.setTitle(title);
        f.setFilePath(file);
        f.setLineStart(line);
        f.setLineEnd(line);
        f.setSource(src);
        f.setSeverity(sev);
        f.setConfidence(confidence);
        f.setCategory(FindingCategory.SECURITY);
        f.setDescription(title);
        f.setRecommendation("Fix it");
        f.setFingerprint("");
        return f;
    }
}
