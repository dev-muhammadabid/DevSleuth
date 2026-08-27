package com.devsleuth.pullrequest.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.enums.PullRequestStatus;
import com.devsleuth.github.service.GitHubPullRequestService;
import com.devsleuth.github.service.GitHubPullRequestService.GitHubPRInfo;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.service.RepositoryService;
import com.devsleuth.review.repository.ReviewRepository;
import com.devsleuth.review.service.ReviewOrchestrator;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: pull-requests-and-experiments, Property 1: PR upsert preserves all fields
 *
 * upsertPullRequest is a private method, so it is exercised through the reachable
 * public path listByRepository(): GitHub returns a generated GitHubPRInfo, the service
 * upserts it, and we then round-trip by querying the (in-memory) repository by
 * repository ID and PR number, asserting every persisted field matches the input.
 */
class PullRequestServicePropertyTest {

    @Property(tries = 100)
    void upsertPreservesAllFields(
            @ForAll @LongRange(min = 1L, max = 1_000_000_000L) long githubPrId,
            @ForAll @IntRange(min = 1, max = 100_000) int number,
            @ForAll @NotBlank @StringLength(min = 1, max = 200) String title,
            @ForAll @NotBlank @StringLength(min = 1, max = 60) String author,
            @ForAll @NotBlank @StringLength(min = 1, max = 100) String sourceBranch,
            @ForAll @NotBlank @StringLength(min = 1, max = 100) String targetBranch,
            @ForAll @NotBlank @StringLength(min = 1, max = 40) String commitSha) {

        GitHubPRInfo info = new GitHubPRInfo(
                githubPrId, number, title, author,
                sourceBranch, targetBranch, commitSha, "open");

        Repository repo = new Repository();
        repo.setId(UUID.randomUUID());
        repo.setOwner("acme");
        repo.setName("widgets");
        repo.setFullName("acme/widgets");

        // In-memory stand-in for the JPA repository, keyed by (repositoryId, number).
        Map<String, PullRequest> db = new HashMap<>();
        PullRequestRepository prRepo = mock(PullRequestRepository.class);
        when(prRepo.findByRepositoryIdAndNumber(any(), any())).thenAnswer(inv -> {
            UUID rid = inv.getArgument(0);
            Integer num = inv.getArgument(1);
            return Optional.ofNullable(db.get(rid + "#" + num));
        });
        when(prRepo.save(any(PullRequest.class))).thenAnswer(inv -> {
            PullRequest p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            db.put(p.getRepository().getId() + "#" + p.getNumber(), p);
            return p;
        });
        when(prRepo.findByRepositoryIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> {
            UUID rid = inv.getArgument(0);
            List<PullRequest> out = new ArrayList<>();
            for (PullRequest p : db.values()) {
                if (p.getRepository().getId().equals(rid)) {
                    out.add(p);
                }
            }
            return out;
        });

        RepositoryService repositoryService = mock(RepositoryService.class);
        when(repositoryService.findById(repo.getId())).thenReturn(Optional.of(repo));

        GitHubPullRequestService gitHub = mock(GitHubPullRequestService.class);
        when(gitHub.listOpenPullRequests(any(), any(), any())).thenReturn(List.of(info));

        PullRequestService service = new PullRequestService(
                prRepo,
                mock(ReviewRepository.class),
                mock(ReviewOrchestrator.class),
                gitHub,
                repositoryService);

        User user = new User();
        user.setAccessToken("gh-token");

        // Exercise upsert via the reachable public path.
        service.listByRepository(repo.getId(), user);

        // Round-trip: query by repository ID and number.
        PullRequest persisted = prRepo.findByRepositoryIdAndNumber(repo.getId(), number)
                .orElseThrow(() -> new AssertionError("PR was not persisted"));

        assertThat(persisted.getGithubPrId()).isEqualTo(githubPrId);
        assertThat(persisted.getNumber()).isEqualTo(number);
        assertThat(persisted.getTitle()).isEqualTo(title);
        assertThat(persisted.getAuthor()).isEqualTo(author);
        assertThat(persisted.getSourceBranch()).isEqualTo(sourceBranch);
        assertThat(persisted.getTargetBranch()).isEqualTo(targetBranch);
        assertThat(persisted.getCommitSha()).isEqualTo(commitSha);
        assertThat(persisted.getStatus()).isEqualTo(PullRequestStatus.OPEN);
    }
}
