package com.devsleuth.pullrequest.controller;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.github.service.GitHubPullRequestService;
import com.devsleuth.github.service.GitHubPullRequestService.GitHubPRInfo;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.repository.RepositoryRepository;
import com.devsleuth.review.service.ReviewOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Feature: pull-requests-and-experiments, Integration tests for PR endpoints
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PullRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @MockBean
    private GitHubPullRequestService gitHubPullRequestService;

    @MockBean
    private ReviewOrchestrator reviewOrchestrator;

    private User member;
    private User nonMember;
    private Repository repo;

    @BeforeEach
    void setUp() {
        member = new User();
        member.setGithubUserId(1001L);
        member.setUsername("member-user");
        member.setAccessToken("ghp_test_token");
        member = userRepository.save(member);

        nonMember = new User();
        nonMember.setGithubUserId(1002L);
        nonMember.setUsername("outsider");
        nonMember.setAccessToken("ghp_other_token");
        nonMember = userRepository.save(nonMember);

        repo = new Repository();
        repo.setGithubRepositoryId(9999L);
        repo.setOwner("acme");
        repo.setName("widgets");
        repo.setFullName("acme/widgets");
        repo.setConnected(true);
        repo.setMembers(Set.of(member));
        repo = repositoryRepository.save(repo);
    }

    // --- PR List endpoint ---

    @Test
    void listPRs_happyPath_fetchesFromGitHubAndReturnsUpserted() throws Exception {
        GitHubPRInfo prInfo = new GitHubPRInfo(
                42L, 7, "Fix null pointer", "alice",
                "feature/fix-npe", "main", "abc123def", "open");

        when(gitHubPullRequestService.listOpenPullRequests(eq("acme"), eq("widgets"), any()))
                .thenReturn(List.of(prInfo));

        MockHttpSession session = sessionFor(member);

        mockMvc.perform(get("/api/repositories/{repoId}/pull-requests", repo.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].number").value(7))
                .andExpect(jsonPath("$[0].title").value("Fix null pointer"))
                .andExpect(jsonPath("$[0].author").value("alice"))
                .andExpect(jsonPath("$[0].sourceBranch").value("feature/fix-npe"))
                .andExpect(jsonPath("$[0].targetBranch").value("main"))
                .andExpect(jsonPath("$[0].commitSha").value("abc123def"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void listPRs_nonMember_returns404() throws Exception {
        MockHttpSession session = sessionFor(nonMember);

        mockMvc.perform(get("/api/repositories/{repoId}/pull-requests", repo.getId())
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPRs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/repositories/{repoId}/pull-requests", repo.getId()))
                .andExpect(status().isUnauthorized());
    }

    // --- Analyze endpoint ---

    @Test
    void analyze_happyPath_returnsReviewIdAndQueuedStatus() throws Exception {
        GitHubPRInfo prInfo = new GitHubPRInfo(
                42L, 7, "Fix null pointer", "alice",
                "feature/fix-npe", "main", "abc123def", "open");

        when(gitHubPullRequestService.getPullRequest(eq("acme"), eq("widgets"), eq(7), any()))
                .thenReturn(prInfo);
        doNothing().when(reviewOrchestrator).runReview(any(), any());

        MockHttpSession session = sessionFor(member);

        mockMvc.perform(post("/api/repositories/{repoId}/pull-requests/{number}/analyze",
                        repo.getId(), 7)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").exists())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void analyze_nonMember_returns404() throws Exception {
        MockHttpSession session = sessionFor(nonMember);

        mockMvc.perform(post("/api/repositories/{repoId}/pull-requests/{number}/analyze",
                        repo.getId(), 7)
                        .session(session))
                .andExpect(status().isNotFound());
    }

    // --- Helper ---

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        return session;
    }
}
