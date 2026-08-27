package com.devsleuth.pullrequest.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.enums.PullRequestStatus;
import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.github.service.GitHubPullRequestService;
import com.devsleuth.github.service.GitHubPullRequestService.GitHubPRInfo;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.service.RepositoryService;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import com.devsleuth.review.service.ReviewOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PullRequestService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestService.class);

    private final PullRequestRepository pullRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final GitHubPullRequestService gitHubPullRequestService;
    private final RepositoryService repositoryService;

    public PullRequestService(PullRequestRepository pullRequestRepository,
                              ReviewRepository reviewRepository,
                              ReviewOrchestrator reviewOrchestrator,
                              GitHubPullRequestService gitHubPullRequestService,
                              RepositoryService repositoryService) {
        this.pullRequestRepository = pullRequestRepository;
        this.reviewRepository = reviewRepository;
        this.reviewOrchestrator = reviewOrchestrator;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.repositoryService = repositoryService;
    }

    /**
     * List PRs for a repository.
     */
    public List<PullRequest> listByRepository(UUID repositoryId) {
        return pullRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }

    /**
     * Manual trigger: user selects a PR to analyze.
     */
    public Review triggerAnalysis(UUID repositoryId, int prNumber, User user) {
        return triggerAnalysis(repositoryId, prNumber, user, com.devsleuth.experiment.ExperimentMode.HYBRID);
    }

    public Review triggerAnalysis(UUID repositoryId, int prNumber, User user,
                                  com.devsleuth.experiment.ExperimentMode mode) {
        Repository repo = repositoryService.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        // Fetch PR info from GitHub
        GitHubPRInfo info = gitHubPullRequestService.getPullRequest(
                repo.getOwner(), repo.getName(), prNumber, user.getAccessToken());

        // Upsert PR in DB
        PullRequest pr = pullRequestRepository.findByRepositoryIdAndNumber(repositoryId, prNumber)
                .orElseGet(() -> {
                    PullRequest p = new PullRequest();
                    p.setRepository(repo);
                    p.setNumber(prNumber);
                    return p;
                });
        pr.setGithubPrId(info.githubPrId());
        pr.setTitle(info.title());
        pr.setAuthor(info.author());
        pr.setSourceBranch(info.sourceBranch());
        pr.setTargetBranch(info.targetBranch());
        pr.setCommitSha(info.commitSha());
        pr.setStatus(PullRequestStatus.OPEN);
        pullRequestRepository.save(pr);

        // Create review and kick off analysis
        Review review = new Review();
        review.setPullRequest(pr);
        review.setCommitSha(info.commitSha());
        review.setStatus(ReviewStatus.QUEUED);
        reviewRepository.save(review);

        reviewOrchestrator.runReview(review, mode);
        return review;
    }

    /**
     * Webhook-triggered: creates/updates PR and queues review.
     * The webhook handler already returned 200 before this runs (@Async).
     */
    public void handleWebhookPR(String repoFullName, GitHubPRInfo info) {
        Repository repo = repositoryService.findByFullName(repoFullName).orElse(null);
        if (repo == null || !repo.isConnected()) {
            log.debug("Ignoring PR for unconnected repo: {}", repoFullName);
            return;
        }

        PullRequest pr = pullRequestRepository.findByRepositoryIdAndNumber(repo.getId(), info.number())
                .orElseGet(() -> {
                    PullRequest p = new PullRequest();
                    p.setRepository(repo);
                    p.setNumber(info.number());
                    return p;
                });
        pr.setGithubPrId(info.githubPrId());
        pr.setTitle(info.title());
        pr.setAuthor(info.author());
        pr.setSourceBranch(info.sourceBranch());
        pr.setTargetBranch(info.targetBranch());
        pr.setCommitSha(info.commitSha());
        pr.setStatus(PullRequestStatus.OPEN);
        pullRequestRepository.save(pr);

        Review review = new Review();
        review.setPullRequest(pr);
        review.setCommitSha(info.commitSha());
        review.setStatus(ReviewStatus.QUEUED);
        reviewRepository.save(review);

        reviewOrchestrator.runReview(review);
    }
}
