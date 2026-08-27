package com.devsleuth.analysis.ai;

import com.devsleuth.finding.entity.Finding;
import com.devsleuth.review.entity.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    /**
     * Sends the PR diff to the configured LLM and parses structured findings.
     */
    @Async("analysisExecutor")
    public CompletableFuture<List<Finding>> analyze(Review review) {
        log.info("Running AI analysis for review {}", review.getId());
        // TODO: build prompt from PR diff, call OpenAI/Anthropic, parse response into Finding entities
        return CompletableFuture.completedFuture(List.of());
    }
}
