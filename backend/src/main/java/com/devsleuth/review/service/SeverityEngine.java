package com.devsleuth.review.service;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Policy-based severity adjustment. The LLM does not control severity alone —
 * this engine applies documented rules to enforce consistent severity assignment.
 *
 * Severity = "how serious is the problem?"
 * Confidence = "how certain are we the finding is real?"
 *
 * Policy:
 * - SECURITY + exploitable keywords → minimum HIGH
 * - SECURITY (general) → minimum MEDIUM
 * - BUG + null/NPE/resource leak → minimum MEDIUM
 * - PERFORMANCE → cap at MEDIUM unless explicit critical pattern
 * - QUALITY → cap at LOW
 * - AI-only findings with low confidence (<60%) → demote one level
 * - HYBRID findings → boost confidence (already done in HybridEngine)
 * - Static findings always keep 100% confidence (deterministic)
 */
@Service
public class SeverityEngine {

    private static final Set<String> EXPLOITABLE_KEYWORDS = Set.of(
            "injection", "sql injection", "xss", "cross-site", "rce",
            "remote code execution", "deserialization", "ssrf", "path traversal",
            "command injection", "authentication bypass", "privilege escalation"
    );

    private static final Set<String> HIGH_BUG_KEYWORDS = Set.of(
            "null pointer", "null dereference", "npe", "resource leak",
            "use after free", "infinite loop", "deadlock", "data loss",
            "race condition", "concurrent modification"
    );

    /**
     * Applies severity policy to all findings. Mutates in place.
     */
    public void apply(List<Finding> findings) {
        for (Finding f : findings) {
            adjustSeverity(f);
            adjustConfidence(f);
        }
    }

    private void adjustSeverity(Finding f) {
        String titleLower = f.getTitle() != null ? f.getTitle().toLowerCase() : "";
        String descLower = f.getDescription() != null ? f.getDescription().toLowerCase() : "";
        String combined = titleLower + " " + descLower;

        switch (f.getCategory()) {
            case SECURITY -> {
                if (containsAny(combined, EXPLOITABLE_KEYWORDS)) {
                    // Exploitable security issue: minimum HIGH
                    if (f.getSeverity().ordinal() > Severity.HIGH.ordinal()) {
                        f.setSeverity(Severity.HIGH);
                    }
                } else {
                    // General security: minimum MEDIUM
                    if (f.getSeverity().ordinal() > Severity.MEDIUM.ordinal()) {
                        f.setSeverity(Severity.MEDIUM);
                    }
                }
            }
            case BUG -> {
                if (containsAny(combined, HIGH_BUG_KEYWORDS)) {
                    // Serious bug patterns: minimum MEDIUM
                    if (f.getSeverity().ordinal() > Severity.MEDIUM.ordinal()) {
                        f.setSeverity(Severity.MEDIUM);
                    }
                }
            }
            case PERFORMANCE -> {
                // Cap at MEDIUM unless already CRITICAL/HIGH from a static tool
                if (f.getSource() == FindingSource.AI && f.getSeverity().ordinal() < Severity.MEDIUM.ordinal()) {
                    f.setSeverity(Severity.MEDIUM);
                }
            }
            case QUALITY -> {
                // Cap at LOW
                if (f.getSeverity().ordinal() < Severity.LOW.ordinal()) {
                    f.setSeverity(Severity.LOW);
                }
            }
        }
    }

    private void adjustConfidence(Finding f) {
        // Static findings are deterministic: always 100%
        if (f.getSource() == FindingSource.STATIC) {
            f.setConfidence(100);
            return;
        }

        // AI-only findings with low confidence: demote severity one level
        if (f.getSource() == FindingSource.AI && f.getConfidence() < 60) {
            Severity current = f.getSeverity();
            if (current.ordinal() < Severity.INFO.ordinal()) {
                f.setSeverity(Severity.values()[current.ordinal() + 1]);
            }
        }
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
