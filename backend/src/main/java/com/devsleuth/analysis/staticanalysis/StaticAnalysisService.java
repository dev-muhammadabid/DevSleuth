package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.finding.entity.Finding;
import com.devsleuth.review.entity.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class StaticAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(StaticAnalysisService.class);

    /**
     * Runs Semgrep and SpotBugs on the PR diff/files.
     * Returns normalized findings.
     */
    @Async("analysisExecutor")
    public CompletableFuture<List<Finding>> analyze(Review review) {
        log.info("Running static analysis for review {}", review.getId());
        // TODO: invoke Semgrep CLI, parse JSON output, map to Finding entities
        // TODO: invoke SpotBugs, parse XML output, map to Finding entities
        return CompletableFuture.completedFuture(List.of());
    }
}
