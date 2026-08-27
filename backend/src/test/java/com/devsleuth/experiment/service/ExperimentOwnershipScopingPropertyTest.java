package com.devsleuth.experiment.service;

import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.common.security.EncryptedStringConverter;
import com.devsleuth.experiment.entity.Experiment;
import com.devsleuth.experiment.repository.ExperimentRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pull-requests-and-experiments, Property 6: Experiment ownership scoping
 *
 * Property 6: For any user, listing experiments SHALL return only experiments where
 * experiment.userId equals the requesting user's ID; no experiment owned by a different
 * user SHALL appear in the results.
 *
 * Seam: a real persistence test against Postgres via {@code @DataJpaTest}. The only
 * ownership-filtering logic lives in the Spring Data derived query
 * {@code findByUserIdOrderByCreatedAtDesc(userId)} that ExperimentService.listByUser
 * delegates to, so a mock or in-memory fake would only exercise the fake's own filter.
 * Persisting real Experiment rows (including the JSONB dataset column) and querying them
 * back is the seam that actually validates the property. jqwik generates 120 random
 * distributions of experiments across a small user pool.
 *
 * Validates: Requirements 5.4
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ExperimentService.class, EncryptedStringConverter.class})
class ExperimentOwnershipScopingPropertyTest {

    @Autowired
    private ExperimentService experimentService;
    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private UserRepository userRepository;

    // Monotonic so generated users never collide on the unique github_user_id, regardless
    // of when Hibernate flushes deletes between iterations.
    private static final AtomicLong GITHUB_ID_SEQ = new AtomicLong(1);

    @Test
    void listByUserReturnsOnlyOwnersExperiments() {
        Arbitrary<Scenario> scenarios = Arbitraries.integers().between(2, 5).flatMap(userCount ->
                Arbitraries.integers().between(0, userCount - 1)
                        .list().ofMinSize(0).ofMaxSize(30)
                        .map(owners -> new Scenario(userCount, owners)));

        scenarios.sampleStream().limit(120).forEach(this::checkScenario);
    }

    private void checkScenario(Scenario scenario) {
        // Independent starting state for each generated distribution.
        experimentRepository.deleteAll();
        userRepository.deleteAll();

        List<UUID> userIds = new ArrayList<>();
        Map<UUID, Set<UUID>> expectedByUser = new HashMap<>();
        for (int i = 0; i < scenario.userCount(); i++) {
            UUID id = persistUser().getId();
            userIds.add(id);
            expectedByUser.put(id, new HashSet<>());
        }

        for (int ownerIndex : scenario.ownerIndices()) {
            UUID ownerId = userIds.get(ownerIndex);
            Experiment saved = experimentRepository.save(makeExperiment(ownerId));
            expectedByUser.get(ownerId).add(saved.getId());
        }

        for (UUID userId : userIds) {
            List<Experiment> result = experimentService.listByUser(userId);

            // No experiment from a different owner leaks in.
            assertThat(result).allSatisfy(e -> assertThat(e.getUserId()).isEqualTo(userId));

            // The user sees exactly their own experiments.
            Set<UUID> resultIds = new HashSet<>();
            result.forEach(e -> resultIds.add(e.getId()));
            assertThat(resultIds)
                    .as("experiments returned for user %s", userId)
                    .isEqualTo(expectedByUser.get(userId));
        }
    }

    private User persistUser() {
        User user = new User();
        user.setGithubUserId(GITHUB_ID_SEQ.getAndIncrement());
        user.setUsername("user-" + UUID.randomUUID());
        return userRepository.save(user);
    }

    private Experiment makeExperiment(UUID userId) {
        Experiment experiment = new Experiment();
        experiment.setUserId(userId);
        experiment.setName("exp-" + UUID.randomUUID());
        // dataset is NOT NULL; a single file change is enough to exercise the JSONB column.
        experiment.setDataset(List.of(new FileChange("src/Main.java", "@@ -1 +1 @@", null)));
        return experiment;
    }

    private record Scenario(int userCount, List<Integer> ownerIndices) {}
}
