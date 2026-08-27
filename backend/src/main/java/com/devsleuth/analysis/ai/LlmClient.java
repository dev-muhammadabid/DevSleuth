package com.devsleuth.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Shared low-level LLM HTTP client. Supports OpenAI and Anthropic.
 * Used by AiAnalysisService, PrSummaryService, and any future LLM features.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final String apiKey;
    private final String provider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmClient(
            @Value("${devsleuth.ai.api-key:}") String apiKey,
            @Value("${devsleuth.ai.provider:openai}") String provider) {
        this.apiKey = apiKey;
        this.provider = provider;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getProvider() {
        return provider;
    }

    /**
     * Call the configured LLM with system + user prompts.
     * Returns the raw text response content.
     */
    public String call(String systemPrompt, String userPrompt) {
        return call(systemPrompt, userPrompt, false);
    }

    /**
     * Call the configured LLM. If jsonMode is true, requests JSON output format (OpenAI only).
     */
    public String call(String systemPrompt, String userPrompt, boolean jsonMode) {
        if ("anthropic".equalsIgnoreCase(provider)) {
            return callAnthropic(systemPrompt, userPrompt);
        }
        return callOpenAi(systemPrompt, userPrompt, jsonMode);
    }

    /**
     * Call a specific provider (for multi-model comparison).
     */
    public String callProvider(String providerName, String systemPrompt, String userPrompt, boolean jsonMode) {
        if ("anthropic".equalsIgnoreCase(providerName)) {
            return callAnthropic(systemPrompt, userPrompt);
        }
        return callOpenAi(systemPrompt, userPrompt, jsonMode);
    }

    private String callOpenAi(String systemPrompt, String userPrompt, boolean jsonMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var bodyMap = new java.util.LinkedHashMap<String, Object>();
        bodyMap.put("model", "gpt-4o");
        bodyMap.put("temperature", 0.0);
        if (jsonMode) {
            bodyMap.put("response_format", Map.of("type", "json_object"));
        }
        bodyMap.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST, new HttpEntity<>(bodyMap, headers), String.class);

        return extractOpenAiContent(response.getBody());
    }

    private String callAnthropic(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        return extractAnthropicContent(response.getBody());
    }

    private String extractOpenAiContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract OpenAI response content", e);
        }
    }

    private String extractAnthropicContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").path(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Anthropic response content", e);
        }
    }
}
