package com.devsleuth.review.service;

import com.devsleuth.analysis.ai.AiAnalysisService;
import com.devsleuth.analysis.ai.FixSuggestionService;
import com.devsleuth.analysis.ai.PrSummaryService;
import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.analysis.service.DiffExtractionService;
import com.devsleuth.analysis.staticanalysis.StaticAnalysisEngine;
import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.FindingService;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrator.class);

    private final ReviewRepository reviewRepository;
    private final FindingService findingService;
    private final DiffExtractionService diffExtractionService;
    private final StaticAnalysisEngine staticAnalysisEngine;
    private final AiAnalysisService aiAnalysisService;
    private final PrSummaryService prSummaryService;
    private final FixSuggestionService fixSuggestionService;
    private final HybridEngine hybridEngine;
    private final SeverityEngine severityEngine;
    private final FindingNormalizer findingNormalizer;
    private final UserRepository userRepository;

    public ReviewOrchestrator(
            ReviewRepository reviewRepository,
            FindingService findingService,
            DiffExtractionService diffExtractionService,
            StaticAnalysisEngine staticAnalysisEngine,
            AiAnalysisService aiAnalysisService,
            PrSummaryService prSummaryService,
            FixSuggestionService fixSuggestionService,
            HybridEngine hybridEngine,
            SeverityEngine severityEngine,
            FindingNormalizer findingNormalizer,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.findingService = findingService;
        this.diffExtractionService = diffExtractionService;
        this.staticAnalysisEngine = staticAnalysisEngine;
        this.aiAnalysisService = aiAnalysisService;
        this.prSummaryService = prSummaryService;
        this.fixSuggestionService = fixSuggestionService;
        this.hybridEngine = hybridEngine;
        this.severityEngine = severityEngine;
        this.findingNormalizer = findingNormalizer;
        this.userRepository = userRepository;
    }

    @Async("analysisExecutor")
    public void runReview(Review review) {
        runReview(review, com.devsleuth.experiment.ExperimentMode.HYBRID);
    }

    @Async("analysisExecutor")
    public void runReview(Review review, com.devsleuth.experiment.ExperimentMode mode) {
        review.setStartedAt(Instant.now());
        ReviewTimer timer = new ReviewTimer(review.getId());

        try {
            // FETCHING
            timer.startStep("FETCHING");
            updateStatus(review, ReviewStatus.FETCHING);
            String accessToken = resolveAccessToken(review);
            AnalysisInput input = com.devsleuth.common.exception.RetryStrategy.execute(
                    2, () -> diffExtractionService.extract(review, accessToken), "DiffExtraction");
            timer.endCurrentStep();

            if (input.files().isEmpty()) {
                log.info("review={} event=NO_SUPPORTED_FILES", review.getId());
                review.setStaticFindingCount(0);
                review.setAiFindingCount(0);
                review.setFinalFindingCount(0);
                updateStatus(review, ReviewStatus.COMPLETED);
                return;
            }

            // SUMMARIZE (non-blocking — failure doesn't stop the pipeline)
            try {
                String summary = prSummaryService.summarize(input);
                if (summary != null) {
                    review.setSummary(summary);
                    reviewRepository.save(review);
                }
            } catch (Exception e) {
                log.warn("PR summary generation failed for review={}: {}", review.getId(), e.getMessage());
            }

            // STATIC_ANALYSIS
            List<RawFinding> staticFindings = List.of();
            if (mode != com.devsleuth.experiment.ExperimentMode.AI_ONLY) {
                timer.startStep("STATIC_ANALYSIS");
                updateStatus(review, ReviewStatus.STATIC_ANALYSIS);
                staticFindings = staticAnalysisEngine.runAll(input);
                timer.endCurrentStep();
            }

            // AI_ANALYSIS
            List<RawFinding> aiFindings = List.of();
            if (mode != com.devsleuth.experiment.ExperimentMode.STATIC_ONLY) {
                timer.startStep("AI_ANALYSIS");
                updateStatus(review, ReviewStatus.AI_ANALYSIS);
                aiFindings = aiAnalysisService.analyze(input);
                timer.endCurrentStep();
            }

            // NORMALIZING
            timer.startStep("NORMALIZING");
            updateStatus(review, ReviewStatus.NORMALIZING);
            List<RawFinding> allRaw = new ArrayList<>(staticFindings);
            allRaw.addAll(aiFindings);
            List<Finding> normalized = findingNormalizer.normalize(allRaw, review);
            severityEngine.apply(normalized);
            timer.endCurrentStep();

            // DEDUPLICATING (+ correlate + rank via hybrid engine)
            timer.startStep("DEDUPLICATING");
            updateStatus(review, ReviewStatus.DEDUPLICATING);
            List<Finding> finalFindings = hybridEngine.process(normalized);
            timer.endCurrentStep();

            // PERSIST
            timer.startStep("DATABASE");
            findingService.saveAll(finalFindings);
            timer.endCurrentStep();

            // FIX SUGGESTIONS (non-blocking — failure doesn't stop the pipeline)
            try {
                fixSuggestionService.generateFixes(finalFindings, input);
            } catch (Exception e) {
                log.warn("Fix suggestion generation failed for review={}: {}", review.getId(), e.getMessage());
            }

            review.setStaticFindingCount(staticFindings.size());
            review.setAiFindingCount(aiFindings.size());
            review.setFinalFindingCount(finalFindings.size());
            updateStatus(review, ReviewStatus.COMPLETED);

        } catch (com.devsleuth.common.exception.ReviewException e) {
            log.error("Review {} failed [{}]: {}", review.getId(), e.getErrorCode(), e.getMessage());
            review.setErrorMessage(e.getErrorCode() + ": " + e.getMessage());
            updateStatus(review, ReviewStatus.FAILED);
        } catch (Exception e) {
            log.error("Review {} failed", review.getId(), e);
            review.setErrorMessage(e.getMessage());
            updateStatus(review, ReviewStatus.FAILED);
        } finally {
            timer.endCurrentStep(); // in case we failed mid-step
            timer.logSummary();
            review.setCompletedAt(Instant.now());
            if (review.getStartedAt() != null) {
                review.setDurationMs(Duration.between(review.getStartedAt(), review.getCompletedAt()).toMillis());
            }
            reviewRepository.save(review);
        }
    }

    private void updateStatus(Review review, ReviewStatus status) {
        review.setStatus(status);
        reviewRepository.save(review);
        log.info("Review {} → {}", review.getId(), status);
    }

    private String resolveAccessToken(Review review) {
        // ponytail: use any member's token to fetch the diff. Upgrade to GitHub App
        // installation tokens later. Queried (not walked off the detached entity) so it
        // works from the @Async analysis thread.
        List<String> memberTokens = reviewRepository.findMemberAccessTokens(
                review.getId(), org.springframework.data.domain.PageRequest.of(0, 1));
        if (!memberTokens.isEmpty()) {
            return memberTokens.get(0);
        }
        // fallback: any user with a token
        return userRepository.findAll().stream()
                .filter(u -> u.getAccessToken() != null)
                .map(User::getAccessToken)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No access token available for review"));
    }
}
