package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.AiResponse;
import com.devsleuth.analysis.ai.model.AiResponse.AiFinding;
import com.devsleuth.analysis.ai.model.ReviewContext;
import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI analysis engine. Builds context, calls LLM, parses and validates structured JSON response.
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final int MAX_RETRIES = 2;

    private final String apiKey;
    private final String provider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiPromptService promptService = new AiPromptService();
    private final AiResponseParser responseParser = new AiResponseParser();
    private final AiResponseValidator responseValidator = new AiResponseValidator();
    private final ReviewContextBuilder contextBuilder;
    private final AiInputSanitizer inputSanitizer;

    public AiAnalysisService(
            @Value("${devsleuth.ai.api-key:}") String apiKey,
            @Value("${devsleuth.ai.provider:openai}") String provider,
            ReviewContextBuilder contextBuilder,
            AiInputSanitizer inputSanitizer) {
        this.apiKey = apiKey;
        this.provider = provider;
        this.contextBuilder = contextBuilder;
        this.inputSanitizer = inputSanitizer;
    }

    public List<RawFinding> analyze(AnalysisInput input) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API key not configured, skipping AI analysis");
            return List.of();
        }

        ReviewContext context = contextBuilder.build(input);
        ReviewContext sanitized = inputSanitizer.sanitize(context);
        String systemPrompt = promptService.buildSystemPrompt();
        String userPrompt = promptService.buildUserPrompt(sanitized);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String rawResponse = callLlm(systemPrompt, userPrompt);
                Optional<AiResponse> parsed = responseParser.parse(rawResponse);

                if (parsed.isEmpty()) {
                    log.warn("AI response parse failed (attempt {})", attempt);
                    continue;
                }

                List<AiFinding> validated = responseValidator.validate(parsed.get(), input);
                log.info("AI analysis produced {} valid findings (attempt {})", validated.size(), attempt);
                return validated.stream().map(this::toRawFinding).toList();

            } catch (Exception e) {
                log.error("AI analysis attempt {} failed: {}", attempt, e.getMessage());
            }
        }

        log.warn("AI analysis failed after {} attempts", MAX_RETRIES);
        return List.of();
    }

    private String callLlm(String systemPrompt, String userPrompt) {
        if ("anthropic".equalsIgnoreCase(provider)) {
            return callAnthropic(systemPrompt, userPrompt);
        }
        return callOpenAi(systemPrompt, userPrompt);
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o",
                "temperature", 0.0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

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

    private RawFinding toRawFinding(AiFinding f) {
        return new RawFinding(
                FindingSource.AI,
                FindingCategory.valueOf(f.category().toUpperCase()),
                Severity.valueOf(f.severity().toUpperCase()),
                (int) (f.confidence() * 100),
                f.title(),
                f.description(),
                f.recommendation(),
                f.filePath(),
                f.lineStart(),
                f.lineEnd() != null ? f.lineEnd() : f.lineStart()
        );
    }
}
