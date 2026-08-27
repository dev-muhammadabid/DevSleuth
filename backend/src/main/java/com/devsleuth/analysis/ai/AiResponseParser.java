package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.AiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Parses the LLM raw text response into a structured AiResponse.
 * Handles JSON extraction from possibly markdown-wrapped output.
 */
public class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public Optional<AiResponse> parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return Optional.empty();
        }

        String json = extractJson(rawResponse);
        try {
            AiResponse response = mapper.readValue(json, AiResponse.class);
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts JSON from possibly markdown-wrapped response (```json ... ```).
     */
    private String extractJson(String raw) {
        String trimmed = raw.strip();
        // Strip markdown code fences if present
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        // Find the first { and last } to extract JSON object
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
