package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.AiResponse;
import com.devsleuth.analysis.ai.model.AiResponse.AiFinding;
import com.devsleuth.analysis.model.AnalysisInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates AI response findings against known constraints:
 * - Valid enum values for category and severity
 * - Confidence in [0, 1]
 * - File path exists in the analysis input
 * - Line numbers are positive
 * - Required fields are non-blank
 */
public class AiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(AiResponseValidator.class);

    private static final Set<String> VALID_CATEGORIES = Set.of("BUG", "SECURITY", "PERFORMANCE", "QUALITY");
    private static final Set<String> VALID_SEVERITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");

    /**
     * Returns only valid findings; invalid ones are logged and dropped.
     */
    public List<AiFinding> validate(AiResponse response, AnalysisInput input) {
        if (response == null || response.findings() == null) {
            return List.of();
        }

        Set<String> knownFiles = new java.util.HashSet<>();
        for (var file : input.files()) {
            knownFiles.add(file.filePath());
        }

        List<AiFinding> valid = new ArrayList<>();
        for (AiFinding f : response.findings()) {
            List<String> errors = new ArrayList<>();

            if (f.category() == null || !VALID_CATEGORIES.contains(f.category().toUpperCase())) {
                errors.add("invalid category: " + f.category());
            }
            if (f.severity() == null || !VALID_SEVERITIES.contains(f.severity().toUpperCase())) {
                errors.add("invalid severity: " + f.severity());
            }
            if (f.confidence() == null || f.confidence() < 0.0 || f.confidence() > 1.0) {
                errors.add("invalid confidence: " + f.confidence());
            }
            if (f.title() == null || f.title().isBlank()) {
                errors.add("missing title");
            }
            if (f.filePath() == null || f.filePath().isBlank()) {
                errors.add("missing filePath");
            } else if (!knownFiles.contains(f.filePath())) {
                errors.add("filePath not in PR: " + f.filePath());
            }
            if (f.lineStart() == null || f.lineStart() < 1) {
                errors.add("invalid lineStart: " + f.lineStart());
            }

            if (errors.isEmpty()) {
                valid.add(f);
            } else {
                log.debug("Dropping invalid AI finding '{}': {}", f.title(), errors);
            }
        }

        return valid;
    }
}
