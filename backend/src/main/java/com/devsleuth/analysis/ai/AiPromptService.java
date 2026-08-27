package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.ReviewContext;

/**
 * Builds the system and user prompts for the LLM code review.
 */
public class AiPromptService {

    private static final String SYSTEM_PROMPT = """
            You are an expert Java code reviewer. You analyze ONLY the supplied code diff and context.
            
            Look for:
            1. Bugs (null dereferences, logic errors, resource leaks, race conditions)
            2. Security vulnerabilities (injection, auth bypass, sensitive data exposure)
            3. Performance problems (unnecessary allocations, O(n²) where O(n) is possible, blocking calls)
            4. Code quality issues with real impact (dead code, misleading names that cause bugs)
            
            Do NOT report:
            - Stylistic preferences without functional impact
            - Issues unsupported by evidence in the provided code
            - Duplicate findings (report each issue once)
            
            Respond ONLY with a JSON object matching this exact schema:
            {
              "findings": [
                {
                  "category": "BUG" | "SECURITY" | "PERFORMANCE" | "QUALITY",
                  "severity": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO",
                  "confidence": <number between 0.0 and 1.0>,
                  "title": "<short title>",
                  "description": "<explanation of why this is a problem>",
                  "recommendation": "<how to fix it>",
                  "filePath": "<exact file path from the diff>",
                  "lineStart": <line number>,
                  "lineEnd": <line number>
                }
              ]
            }
            
            If there are no findings, respond with: {"findings": []}
            Do not include any text outside the JSON object.
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(ReviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("PR: ").append(context.prTitle()).append("\n");
        sb.append("Repository: ").append(context.repositoryName()).append("\n\n");

        for (ReviewContext.FileContext file : context.files()) {
            sb.append("--- File: ").append(file.filePath()).append(" ---\n");
            if (file.patch() != null && !file.patch().isEmpty()) {
                sb.append("Diff:\n```diff\n").append(file.patch()).append("\n```\n");
            }
            if (file.surroundingCode() != null && !file.surroundingCode().isEmpty()) {
                sb.append("Context:\n```java\n").append(file.surroundingCode()).append("\n```\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
