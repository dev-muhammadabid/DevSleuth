package com.devsleuth.pullrequest.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.enums.PullRequestStatus;
import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.common.exception.DevSleuthException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

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
     * List PRs for a repository. Fetches open PRs from GitHub, upserts them locally,
     * then returns the repository's PRs from the local DB (newest first).
     * GitHub failures degrade gracefully into a 502 DevSleuthException.
     */
    public List<PullRequest> listByRepository(UUID repositoryId, User user) {
        Repository repo = repositoryService.findById(repositoryId)
                .orElseThrow(() -> new DevSleuthException("Repository not found", HttpStatus.NOT_FOUND));

        if (user.getAccessToken() == null || user.getAccessToken().isBlank()) {
            throw new DevSleuthException("No GitHub access token. Please re-authenticate.",
                    HttpStatus.BAD_REQUEST);
        }

        List<GitHubPRInfo> ghPRs = fetchOpenPullRequests(repo, user.getAccessToken());
        for (GitHubPRInfo info : ghPRs) {
            upsertPullRequest(repo, info);
        }

        return pullRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }

    /**
     * Fetch open PRs from GitHub, translating client/network failures into a
     * user-facing 502 so the list endpoint fails clearly instead of leaking a raw error.
     */
    private List<GitHubPRInfo> fetchOpenPullRequests(Repository repo, String accessToken) {
        try {
            return gitHubPullRequestService.listOpenPullRequests(
                    repo.getOwner(), repo.getName(), accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new DevSleuthException(
                        "GitHub authentication failed. Reconnect your repository.",
                        HttpStatus.BAD_GATEWAY);
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DevSleuthException("Repository not found on GitHub.",
                        HttpStatus.BAD_GATEWAY);
            }
            log.warn("GitHub PR list failed for {}: {}", repo.getFullName(), e.getStatusCode());
            throw new DevSleuthException("GitHub request failed. Try again later.",
                    HttpStatus.BAD_GATEWAY);
        } catch (RestClientException e) {
            log.warn("GitHub unreachable while listing PRs for {}", repo.getFullName(), e);
            throw new DevSleuthException("GitHub is unreachable. Try again later.",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Manual trigger: user selects a PR to analyze in the given mode.
     */
    public Review triggerAnalysis(UUID repositoryId, int prNumber, User user,
                                  com.devsleuth.experiment.ExperimentMode mode) {
        Repository repo = repositoryService.findById(repositoryId)
                .orElseThrow(() -> new DevSleuthException("Repository not found", HttpStatus.NOT_FOUND));

        // Fetch PR info from GitHub
        GitHubPRInfo info = gitHubPullRequestService.getPullRequest(
                repo.getOwner(), repo.getName(), prNumber, user.getAccessToken());

        // Upsert PR in DB
        PullRequest pr = upsertPullRequest(repo, info);

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

        PullRequest pr = upsertPullRequest(repo, info);

        Review review = new Review();
        review.setPullRequest(pr);
        review.setCommitSha(info.commitSha());
        review.setStatus(ReviewStatus.QUEUED);
        reviewRepository.save(review);

        reviewOrchestrator.runReview(review);
    }

    /**
     * Insert or update a PullRequest for the given repository from GitHub PR info,
     * matching on the PR number. Returns the persisted entity.
     */
    private PullRequest upsertPullRequest(Repository repo, GitHubPRInfo info) {
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
        return pullRequestRepository.save(pr);
    }
}
