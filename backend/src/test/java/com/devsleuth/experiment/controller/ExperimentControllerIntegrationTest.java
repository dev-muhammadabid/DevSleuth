package com.devsleuth.experiment.controller;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.experiment.entity.Experiment;
import com.devsleuth.experiment.entity.ExperimentRun;
import com.devsleuth.experiment.model.EvaluationMetrics;
import com.devsleuth.experiment.repository.ExperimentRepository;
import com.devsleuth.experiment.repository.ExperimentRunRepository;
import com.devsleuth.experiment.service.ExperimentRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature: pull-requests-and-experiments, Integration tests for experiment endpoints
 *
 * Full integration tests for Experiment CRUD and run lifecycle.
 * Validates: Requirements 5.1, 5.4, 6.1, 6.3, 6.4, 9.1, 9.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentRunRepository runRepository;

    @MockBean
    private ExperimentRunner experimentRunner;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        experimentRepository.deleteAll();
        userRepository.deleteAll();

        userA = persistUser("user-a", 100L);
        userB = persistUser("user-b", 200L);

        // Mock the ExperimentRunner to avoid actual analysis calls and return deterministic metrics.
        when(experimentRunner.run(any(), any(), any()))
                .thenReturn(EvaluationMetrics.compute(5, 2, 1, 150L));
    }

    // --- Create experiment ---

    @Test
    void createExperiment_happyPath() throws Exception {
        MockHttpSession session = sessionFor(userA);

        mockMvc.perform(post("/api/experiments")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("My Experiment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("My Experiment"))
                .andExpect(jsonPath("$.datasetSummary.fileCount").value(1))
                .andExpect(jsonPath("$.groundTruthCount").value(1));
    }

    @Test
    void createExperiment_blankName_returns400() throws Exception {
        MockHttpSession session = sessionFor(userA);

        String body = """
                {
                  "name": "",
                  "dataset": [{"filename": "src/A.java", "status": "modified", "patch": "@@ -1 +1 @@"}],
                  "groundTruth": [{"filePath": "src/A.java", "lineStart": 10, "category": "BUG"}]
                }
                """;

        mockMvc.perform(post("/api/experiments")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- List experiments ---

    @Test
    void listExperiments_returnsOnlyAuthenticatedUsersExperiments() throws Exception {
        // Create experiments for both users directly
        createExperimentForUser(userA, "Exp A1");
        createExperimentForUser(userA, "Exp A2");
        createExperimentForUser(userB, "Exp B1");

        MockHttpSession sessionA = sessionFor(userA);

        mockMvc.perform(get("/api/experiments").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", everyItem(startsWith("Exp A"))));
    }

    // --- Get single experiment ---

    @Test
    void getExperiment_returnsExperimentWithCorrectFields() throws Exception {
        UUID expId = createExperimentViaApi(userA, "Detail Experiment");

        MockHttpSession session = sessionFor(userA);

        mockMvc.perform(get("/api/experiments/" + expId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expId.toString()))
                .andExpect(jsonPath("$.name").value("Detail Experiment"))
                .andExpect(jsonPath("$.datasetSummary.fileCount").value(1))
                .andExpect(jsonPath("$.groundTruthCount").value(1));
    }

    // --- Ownership scoping: 404 for non-owner ---

    @Test
    void getExperiment_nonOwner_returns404() throws Exception {
        UUID expId = createExperimentViaApi(userA, "Private Experiment");

        MockHttpSession sessionB = sessionFor(userB);

        mockMvc.perform(get("/api/experiments/" + expId).session(sessionB))
                .andExpect(status().isNotFound());
    }

    // --- Start run ---

    @Test
    void startRun_returnsRunningStatus() throws Exception {
        UUID expId = createExperimentViaApi(userA, "Run Experiment");
        MockHttpSession session = sessionFor(userA);

        mockMvc.perform(post("/api/experiments/" + expId + "/runs")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.mode").value("HYBRID"));
    }

    // --- List runs ---

    @Test
    void listRuns_returnsRunsForExperiment() throws Exception {
        UUID expId = createExperimentViaApi(userA, "Runs Experiment");
        MockHttpSession session = sessionFor(userA);

        // Start a run
        mockMvc.perform(post("/api/experiments/" + expId + "/runs")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "STATIC_ONLY"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/experiments/" + expId + "/runs").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mode").value("STATIC_ONLY"));
    }

    // --- Run lifecycle: RUNNING → COMPLETED ---

    @Test
    void runLifecycle_eventuallyCompletes() throws Exception {
        UUID expId = createExperimentViaApi(userA, "Lifecycle Experiment");
        MockHttpSession session = sessionFor(userA);

        MvcResult startResult = mockMvc.perform(post("/api/experiments/" + expId + "/runs")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String runId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id").asText();

        // Poll until the async execution completes the run.
        await().atMost(10, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            ExperimentRun run = runRepository.findById(UUID.fromString(runId)).orElseThrow();
            assertThat(run.getStatus()).isIn("COMPLETED", "FAILED");
        });

        // Verify the run status endpoint returns COMPLETED with metrics.
        mockMvc.perform(get("/api/experiments/runs/" + runId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.metrics").isNotEmpty())
                .andExpect(jsonPath("$.metrics.precisionScore").isNumber())
                .andExpect(jsonPath("$.metrics.recallScore").isNumber())
                .andExpect(jsonPath("$.metrics.f1Score").isNumber());
    }

    // --- Unauthenticated access ---

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        // No session → controller returns 401
        mockMvc.perform(get("/api/experiments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("No Auth")))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private User persistUser(String username, long githubId) {
        User user = new User();
        user.setGithubUserId(githubId);
        user.setUsername(username);
        return userRepository.save(user);
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        return session;
    }

    private void createExperimentForUser(User user, String name) {
        Experiment exp = new Experiment();
        exp.setUserId(user.getId());
        exp.setName(name);
        exp.setDataset(java.util.List.of(
                new com.devsleuth.analysis.model.AnalysisInput.FileChange("src/Main.java", "@@ -1 +1 @@", null)));
        exp.setGroundTruth(java.util.List.of(
                new com.devsleuth.experiment.model.GroundTruthEntry(
                        "src/Main.java", 10, 10,
                        com.devsleuth.common.enums.FindingCategory.BUG, null, "Bug here")));
        experimentRepository.save(exp);
    }

    private UUID createExperimentViaApi(User user, String name) throws Exception {
        MockHttpSession session = sessionFor(user);
        MvcResult result = mockMvc.perform(post("/api/experiments")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody(name)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String validCreateBody(String name) {
        return """
                {
                  "name": "%s",
                  "description": "Test experiment",
                  "dataset": [{"filename": "src/Main.java", "status": "modified", "patch": "@@ -1 +1 @@"}],
                  "groundTruth": [{"filePath": "src/Main.java", "lineStart": 10, "category": "BUG", "severity": "HIGH", "title": "Bug here"}]
                }
                """.formatted(name);
    }
}
