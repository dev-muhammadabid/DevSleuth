package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generates a plain-English summary of what a PR does using the LLM.
 */
@Service
public class PrSummaryService {

    private static final Logger log = LoggerFactory.getLogger(PrSummaryService.class);

    private static final String SYSTEM_PROMPT = """
            You are a senior software engineer. Given a code diff, write a concise summary
            of what the Pull Request does. Focus on:
            - What functionality was added, changed, or removed
            - The motivation/intent behind the changes (infer from context)
            - Any notable architectural decisions

            Rules:
            - Write 2-4 sentences maximum.
            - Use plain, direct language (no marketing speak).
            - Do NOT list individual files or line numbers.
            - Do NOT suggest improvements or mention bugs.
            - Just describe WHAT the code change does.
            """;

    private final LlmClient llmClient;

    public PrSummaryService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Generate a summary for the given analysis input (PR diff).
     * Returns null if AI is not configured or the call fails.
     */
    public String summarize(AnalysisInput input) {
        if (!llmClient.isConfigured()) {
            log.debug("LLM not configured, skipping PR summary");
            return null;
        }

        String userPrompt = buildUserPrompt(input);
        try {
            String summary = llmClient.call(SYSTEM_PROMPT, userPrompt);
            if (summary != null && !summary.isBlank()) {
                log.info("Generated PR summary for review={} ({} chars)", input.reviewId(), summary.length());
                return summary.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to generate PR summary for review={}: {}", input.reviewId(), e.getMessage());
        }
        return null;
    }

    private String buildUserPrompt(AnalysisInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Repository: ").append(input.repositoryFullName()).append("\n");
        sb.append("Commit: ").append(input.commitSha()).append("\n");
        sb.append("Files changed: ").append(input.files().size()).append("\n\n");

        for (FileChange file : input.files()) {
            sb.append("--- ").append(file.filePath()).append(" ---\n");
            if (file.patch() != null) {
                // Truncate very long patches to stay within token limits
                String patch = file.patch();
                if (patch.length() > 3000) {
                    patch = patch.substring(0, 3000) + "\n... (truncated)";
                }
                sb.append(patch).append("\n\n");
            }
        }

        return sb.toString();
    }
}
