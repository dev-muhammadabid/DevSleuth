package com.devsleuth.review.repository;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.common.enums.PullRequestStatus;
import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.common.security.EncryptedStringConverter;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.repository.RepositoryRepository;
import com.devsleuth.review.entity.Review;
import jakarta.persistence.EntityManager;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pull-requests-and-experiments, Property 3: Reviews ordered by recency
 *
 * Property 3: For any set of reviews belonging to a pull request, the list returned by
 * findByPullRequestIdOrderByCreatedAtDesc SHALL be ordered such that for consecutive
 * elements reviews[i] and reviews[i+1], reviews[i].createdAt >= reviews[i+1].createdAt.
 *
 * Seam: a real persistence test against Postgres via {@code @DataJpaTest}. The ordering
 * guarantee lives in the Spring Data derived query method name. jqwik generates 100
 * random timestamp distributions, persists reviews with those timestamps via native SQL
 * override, and verifies the query result ordering.
 *
 * Validates: Requirements 4.1
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(EncryptedStringConverter.class)
class ReviewOrderingPropertyTest {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private PullRequestRepository pullRequestRepository;
    @Autowired
    private RepositoryRepository repositoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private static final AtomicLong GITHUB_ID_SEQ = new AtomicLong(100_000);

    private UUID pullRequestId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        pullRequestRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setGithubUserId(GITHUB_ID_SEQ.getAndIncrement());
        user.setUsername("test-user-" + UUID.randomUUID());
        user = userRepository.save(user);

        Repository repo = new Repository();
        repo.setGithubRepositoryId(GITHUB_ID_SEQ.getAndIncrement());
        repo.setOwner("test-owner");
        repo.setName("test-repo");
        repo.setFullName("test-owner/test-repo");
        repo.setConnected(true);
        repo.getMembers().add(user);
        repo = repositoryRepository.save(repo);

        PullRequest pr = new PullRequest();
        pr.setRepository(repo);
        pr.setGithubPrId(GITHUB_ID_SEQ.getAndIncrement());
        pr.setNumber(1);
        pr.setTitle("Test PR");
        pr.setAuthor("author");
        pr.setSourceBranch("feature");
        pr.setTargetBranch("main");
        pr.setCommitSha("abc123");
        pr.setStatus(PullRequestStatus.OPEN);
        pr = pullRequestRepository.save(pr);

        pullRequestId = pr.getId();
    }

    @Test
    void reviewsReturnedInDescendingCreatedAtOrder() {
        // Generate random lists of epoch-second offsets to produce distinct timestamps.
        Arbitrary<List<Long>> offsetLists = Arbitraries.longs()
                .between(0, 365L * 24 * 3600) // up to ~1 year of seconds
                .list().ofMinSize(2).ofMaxSize(20);

        offsetLists.sampleStream().limit(100).forEach(this::checkOrdering);
    }

    private void checkOrdering(List<Long> secondOffsets) {
        reviewRepository.deleteAll();
        entityManager.flush();

        Instant baseTime = Instant.parse("2024-01-01T00:00:00Z");

        // Insert reviews and override their created_at to the generated timestamps.
        for (Long offset : secondOffsets) {
            Review review = new Review();
            review.setPullRequest(entityManager.getReference(PullRequest.class, pullRequestId));
            review.setCommitSha("sha-" + UUID.randomUUID());
            review.setStatus(ReviewStatus.COMPLETED);
            review = reviewRepository.save(review);
            entityManager.flush();

            Instant ts = baseTime.plus(offset, ChronoUnit.SECONDS);
            entityManager.createNativeQuery("UPDATE reviews SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", ts)
                    .setParameter("id", review.getId())
                    .executeUpdate();
        }

        entityManager.flush();
        entityManager.clear(); // evict cache so the query reads fresh DB state

        List<Review> result = reviewRepository.findByPullRequestIdOrderByCreatedAtDesc(pullRequestId);

        assertThat(result).hasSameSizeAs(secondOffsets);

        // Verify descending order: each element's createdAt >= the next one's.
        for (int i = 0; i < result.size() - 1; i++) {
            Instant current = result.get(i).getCreatedAt();
            Instant next = result.get(i + 1).getCreatedAt();
            assertThat(current)
                    .as("reviews[%d].createdAt (%s) should be >= reviews[%d].createdAt (%s)",
                            i, current, i + 1, next)
                    .isAfterOrEqualTo(next);
        }
    }
}
