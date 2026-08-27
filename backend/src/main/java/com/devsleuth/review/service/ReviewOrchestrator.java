package com.devsleuth.review.service;

import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.FindingService;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import com.devsleuth.analysis.staticanalysis.StaticAnalysisService;
import com.devsleuth.analysis.ai.AiAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrator.class);

    private final ReviewRepository reviewRepository;
    private final FindingService findingService;
    private final StaticAnalysisService staticAnalysisService;
    private final AiAnalysisService aiAnalysisService;
    private final DeduplicationService deduplicationService;

    public ReviewOrchestrator(
            ReviewRepository reviewRepository,
            FindingService findingService,
            StaticAnalysisService staticAnalysisService,
            AiAnalysisService aiAnalysisService,
            DeduplicationService deduplicationService) {
        this.reviewRepository = reviewRepository;
        this.findingService = findingService;
        this.staticAnalysisService = staticAnalysisService;
        this.aiAnalysisService = aiAnalysisService;
        this.deduplicationService = deduplicationService;
    }

    /**
     * Entry point from webhook. Parses the PR event payload and kicks off analysis.
     * ponytail: payload parsing is naive JSON extraction for now; upgrade to a typed DTO when event complexity grows.
     */
    @Async("analysisExecutor")
    public void handlePullRequestEvent(String payload) {
        // TODO: parse payload, resolve/create PR entity, then call runReview(review)
        log.info("Received PR event, queuing analysis");
    }

    public void runReview(Review review) {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setStartedAt(Instant.now());
        reviewRepository.save(review);

        try {
            // Run static + AI in parallel
            CompletableFuture<List<Finding>> staticFuture = staticAnalysisService.analyze(review);
            CompletableFuture<List<Finding>> aiFuture = aiAnalysisService.analyze(review);

            List<Finding> staticFindings = staticFuture.join();
            List<Finding> aiFindings = aiFuture.join();

            // Merge and deduplicate
            List<Finding> all = new ArrayList<>(staticFindings);
            all.addAll(aiFindings);
            List<Finding> deduplicated = deduplicationService.deduplicate(all);

            // Persist
            findingService.saveAll(deduplicated);

            // Update review stats
            review.setStaticFindingCount(staticFindings.size());
            review.setAiFindingCount(aiFindings.size());
            review.setFinalFindingCount(deduplicated.size());
            review.setStatus(ReviewStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Review failed", e);
            review.setStatus(ReviewStatus.FAILED);
            review.setErrorMessage(e.getMessage());
        } finally {
            review.setCompletedAt(Instant.now());
            review.setDurationMs(Duration.between(review.getStartedAt(), review.getCompletedAt()).toMillis());
            reviewRepository.save(review);
        }
    }
}
