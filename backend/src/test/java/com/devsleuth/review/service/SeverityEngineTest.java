package com.devsleuth.review.service;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeverityEngineTest {

    private final SeverityEngine engine = new SeverityEngine();

    @Test
    void securityWithInjectionGetsMinHigh() {
        Finding f = makeFinding(FindingCategory.SECURITY, Severity.LOW, FindingSource.AI, 80, "SQL injection risk");
        engine.apply(List.of(f));
        assertEquals(Severity.HIGH, f.getSeverity());
    }

    @Test
    void securityGeneralGetsMinMedium() {
        Finding f = makeFinding(FindingCategory.SECURITY, Severity.INFO, FindingSource.STATIC, 100, "Weak hash algorithm");
        engine.apply(List.of(f));
        assertEquals(Severity.MEDIUM, f.getSeverity());
    }

    @Test
    void bugWithNullDereferenceGetsMinMedium() {
        Finding f = makeFinding(FindingCategory.BUG, Severity.LOW, FindingSource.STATIC, 100, "Possible null pointer dereference");
        engine.apply(List.of(f));
        assertEquals(Severity.MEDIUM, f.getSeverity());
    }

    @Test
    void qualityCapsAtLow() {
        Finding f = makeFinding(FindingCategory.QUALITY, Severity.CRITICAL, FindingSource.STATIC, 100, "Naming convention");
        engine.apply(List.of(f));
        // QUALITY should not be above LOW, but policy only caps upward from INFO
        // CRITICAL is already above LOW, so no change (policy only floors, not caps for severity)
        // Actually, re-reading: "Cap at LOW" means severity should not be higher than LOW for QUALITY
        assertEquals(Severity.LOW, f.getSeverity());
    }

    @Test
    void lowConfidenceAiGetsDemoted() {
        Finding f = makeFinding(FindingCategory.BUG, Severity.MEDIUM, FindingSource.AI, 50, "Maybe a bug");
        engine.apply(List.of(f));
        assertEquals(Severity.LOW, f.getSeverity()); // demoted one level
    }

    @Test
    void staticFindingsAlwaysKeep100Confidence() {
        Finding f = makeFinding(FindingCategory.BUG, Severity.MEDIUM, FindingSource.STATIC, 75, "Bug");
        engine.apply(List.of(f));
        assertEquals(100, f.getConfidence());
    }

    private Finding makeFinding(FindingCategory cat, Severity sev, FindingSource src, int confidence, String title) {
        Finding f = new Finding();
        f.setCategory(cat);
        f.setSeverity(sev);
        f.setSource(src);
        f.setConfidence(confidence);
        f.setTitle(title);
        f.setDescription(title);
        f.setFilePath("Test.java");
        f.setLineStart(1);
        f.setFingerprint("test");
        return f;
    }
}
