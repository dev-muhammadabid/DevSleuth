package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.repository.FindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates AI-powered code fix suggestions for findings.
 * Calls the LLM with the finding context + surrounding code and asks for a minimal fix.
 */
@Service
public class FixSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(FixSuggestionService.class);

    private static final String SYSTEM_PROMPT = """
            You are a senior Java developer. Given a code finding (bug, security issue, or quality problem)
            and the surrounding source code, produce a MINIMAL code fix.

            Rules:
            - Show ONLY the fixed code snippet (the lines that need to change).
            - Use a unified diff format: lines starting with - are removed, + are added.
            - Keep the fix as small as possible — change only what's necessary.
            - Do NOT explain the fix (that's already in the finding description).
            - Do NOT include file headers, package statements, or unrelated code.
            - If you cannot produce a confident fix, respond with exactly: NO_FIX
            """;

    private final LlmClient llmClient;
    private final FindingRepository findingRepository;

    public FixSuggestionService(LlmClient llmClient, FindingRepository findingRepository) {
        this.llmClient = llmClient;
        this.findingRepository = findingRepository;
    }

    /**
     * Generate fix suggestions for a batch of findings, using the analysis input for code context.
     * Updates findings in-place and persists them.
     */
    public void generateFixes(List<Finding> findings, AnalysisInput input) {
        if (!llmClient.isConfigured()) {
            log.debug("LLM not configured, skipping fix suggestions");
            return;
        }

        // Only generate fixes for HIGH and CRITICAL findings to conserve API calls
        List<Finding> highPriority = findings.stream()
                .filter(f -> f.getSeverity().ordinal() <= 1) // CRITICAL=0, HIGH=1
                .toList();

        if (highPriority.isEmpty()) {
            log.debug("No high-priority findings, skipping fix suggestions");
            return;
        }

        log.info("Generating fix suggestions for {} high-priority findings", highPriority.size());

        for (Finding finding : highPriority) {
            try {
                String fix = generateFix(finding, input);
                if (fix != null && !fix.equals("NO_FIX")) {
                    finding.setSuggestedFix(fix);
                }
            } catch (Exception e) {
                log.warn("Failed to generate fix for finding {}: {}", finding.getId(), e.getMessage());
            }
        }

        findingRepository.saveAll(highPriority);
        log.info("Saved fix suggestions for {} findings", highPriority.stream()
                .filter(f -> f.getSuggestedFix() != null).count());
    }

    private String generateFix(Finding finding, AnalysisInput input) {
        String codeContext = extractContext(finding, input);
        if (codeContext == null) return null;

        String userPrompt = buildUserPrompt(finding, codeContext);
        String response = llmClient.call(SYSTEM_PROMPT, userPrompt);

        if (response == null || response.isBlank() || response.trim().equals("NO_FIX")) {
            return null;
        }
        return response.trim();
    }

    private String buildUserPrompt(Finding finding, String codeContext) {
        return String.format("""
                **Finding:** %s
                **Severity:** %s
                **Category:** %s
                **File:** %s (lines %d-%d)
                **Description:** %s

                **Surrounding code:**
                ```java
                %s
                ```

                Produce the minimal fix:
                """,
                finding.getTitle(),
                finding.getSeverity(),
                finding.getCategory(),
                finding.getFilePath(),
                finding.getLineStart() != null ? finding.getLineStart() : 0,
                finding.getLineEnd() != null ? finding.getLineEnd() : 0,
                finding.getDescription() != null ? finding.getDescription() : "",
                codeContext);
    }

    /**
     * Extract ~20 lines of code context around the finding's line range from the analysis input.
     */
    private String extractContext(Finding finding, AnalysisInput input) {
        FileChange file = input.files().stream()
                .filter(f -> f.filePath().equals(finding.getFilePath()))
                .findFirst()
                .orElse(null);

        if (file == null || file.fullContent() == null) {
            // Fall back to the patch if full content isn't available
            if (file != null && file.patch() != null) {
                return file.patch();
            }
            return null;
        }

        String[] lines = file.fullContent().split("\n");
        int start = Math.max(0, (finding.getLineStart() != null ? finding.getLineStart() : 1) - 10);
        int end = Math.min(lines.length, (finding.getLineEnd() != null ? finding.getLineEnd() : start + 1) + 10);

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(String.format("%4d | %s\n", i + 1, lines[i]));
        }
        return sb.toString();
    }
}
