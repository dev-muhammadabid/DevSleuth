package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.AiResponse;
import com.devsleuth.analysis.ai.model.AiResponse.AiFinding;
import com.devsleuth.analysis.ai.model.ReviewContext;
import com.devsleuth.analysis.model.AnalysisInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Runs the same code context through multiple LLM providers and returns
 * raw findings from each for side-by-side comparison.
 */
@Service
public class MultiModelService {

    private static final Logger log = LoggerFactory.getLogger(MultiModelService.class);

    private final LlmClient llmClient;
    private final AiPromptService promptService = new AiPromptService();
    private final AiResponseParser responseParser = new AiResponseParser();
    private final AiResponseValidator responseValidator = new AiResponseValidator();
    private final ReviewContextBuilder contextBuilder;
    private final AiInputSanitizer inputSanitizer;

    public MultiModelService(LlmClient llmClient,
                             ReviewContextBuilder contextBuilder,
                             AiInputSanitizer inputSanitizer) {
        this.llmClient = llmClient;
        this.contextBuilder = contextBuilder;
        this.inputSanitizer = inputSanitizer;
    }

    public record ModelResult(String provider, List<AiFinding> findings, long durationMs, String error) {}
    public record ComparisonResult(ModelResult openai, ModelResult anthropic) {}

    /**
     * Run the analysis input through both OpenAI and Anthropic and return raw findings from each.
     */
    public ComparisonResult compare(AnalysisInput input) {
        if (!llmClient.isConfigured()) {
            return new ComparisonResult(
                    new ModelResult("openai", List.of(), 0, "AI not configured"),
                    new ModelResult("anthropic", List.of(), 0, "AI not configured")
            );
        }

        ReviewContext context = contextBuilder.build(input);
        ReviewContext sanitized = inputSanitizer.sanitize(context);
        String systemPrompt = promptService.buildSystemPrompt();
        String userPrompt = promptService.buildUserPrompt(sanitized);

        ModelResult openaiResult = runProvider("openai", systemPrompt, userPrompt, input);
        ModelResult anthropicResult = runProvider("anthropic", systemPrompt, userPrompt, input);

        log.info("Multi-model comparison: OpenAI={} findings, Anthropic={} findings",
                openaiResult.findings().size(), anthropicResult.findings().size());

        return new ComparisonResult(openaiResult, anthropicResult);
    }

    private ModelResult runProvider(String provider, String systemPrompt, String userPrompt, AnalysisInput input) {
        long start = System.currentTimeMillis();
        try {
            String rawResponse = llmClient.callProvider(provider, systemPrompt, userPrompt, true);
            long duration = System.currentTimeMillis() - start;

            Optional<AiResponse> parsed = responseParser.parse(rawResponse);
            if (parsed.isEmpty()) {
                return new ModelResult(provider, List.of(), duration, "Failed to parse response");
            }

            List<AiFinding> validated = responseValidator.validate(parsed.get(), input);
            return new ModelResult(provider, validated, duration, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Multi-model {} failed: {}", provider, e.getMessage());
            return new ModelResult(provider, List.of(), duration, e.getMessage());
        }
    }
}
