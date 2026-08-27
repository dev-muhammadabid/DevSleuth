package com.devsleuth.finding.service;

import com.devsleuth.analysis.ai.LlmClient;
import com.devsleuth.finding.entity.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles follow-up questions about a specific finding using LLM context.
 */
@Service
public class FindingChatService {

    private static final Logger log = LoggerFactory.getLogger(FindingChatService.class);

    private static final String SYSTEM_PROMPT = """
            You are a senior Java developer helping a teammate understand a code review finding.
            You have full context about the finding (title, description, severity, file, line range,
            recommendation, and suggested fix if available).

            Rules:
            - Answer the user's question concisely and accurately.
            - Stay focused on THIS specific finding — don't invent unrelated issues.
            - If asked "how to fix", provide a concrete code example.
            - If asked "why is this important", explain the real-world impact.
            - If asked "is this a false positive", give an honest assessment based on the context.
            - Keep answers to 2-5 sentences unless a code example is needed.
            - Use plain language, not academic jargon.
            """;

    private final LlmClient llmClient;

    public FindingChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public String chat(Finding finding, String question) {
        if (!llmClient.isConfigured()) {
            return "AI is not configured. Please set the AI_API_KEY environment variable.";
        }

        String userPrompt = buildUserPrompt(finding, question);

        try {
            String answer = llmClient.call(SYSTEM_PROMPT, userPrompt);
            log.info("Chat response generated for finding={} ({} chars)", finding.getId(), answer.length());
            return answer.trim();
        } catch (Exception e) {
            log.error("Chat failed for finding={}: {}", finding.getId(), e.getMessage());
            return "Sorry, I couldn't generate an answer. Please try again.";
        }
    }

    private String buildUserPrompt(Finding finding, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Finding Context\n");
        sb.append("**Title:** ").append(finding.getTitle()).append("\n");
        sb.append("**Severity:** ").append(finding.getSeverity()).append("\n");
        sb.append("**Category:** ").append(finding.getCategory()).append("\n");
        sb.append("**Source:** ").append(finding.getSource()).append("\n");
        sb.append("**File:** ").append(finding.getFilePath());
        if (finding.getLineStart() != null) {
            sb.append(" (lines ").append(finding.getLineStart());
            if (finding.getLineEnd() != null && !finding.getLineEnd().equals(finding.getLineStart())) {
                sb.append("-").append(finding.getLineEnd());
            }
            sb.append(")");
        }
        sb.append("\n");
        sb.append("**Confidence:** ").append(finding.getConfidence()).append("%\n");

        if (finding.getDescription() != null) {
            sb.append("**Description:** ").append(finding.getDescription()).append("\n");
        }
        if (finding.getRecommendation() != null) {
            sb.append("**Recommendation:** ").append(finding.getRecommendation()).append("\n");
        }
        if (finding.getSuggestedFix() != null) {
            sb.append("**Suggested Fix:**\n```\n").append(finding.getSuggestedFix()).append("\n```\n");
        }

        sb.append("\n## User Question\n");
        sb.append(question);

        return sb.toString();
    }
}
