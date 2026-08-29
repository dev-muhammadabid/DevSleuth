package com.devsleuth.experiment.controller;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.service.AuthService;
import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.experiment.ExperimentMode;
import com.devsleuth.experiment.dto.ExperimentCreateRequest;
import com.devsleuth.experiment.dto.ExperimentMetricResponse;
import com.devsleuth.experiment.dto.ExperimentResponse;
import com.devsleuth.experiment.dto.ExperimentRunRequest;
import com.devsleuth.experiment.dto.ExperimentRunResponse;
import com.devsleuth.experiment.entity.Experiment;
import com.devsleuth.experiment.entity.ExperimentRun;
import com.devsleuth.experiment.repository.ExperimentMetricRepository;
import com.devsleuth.experiment.service.ExperimentRunService;
import com.devsleuth.experiment.service.ExperimentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;
    private final ExperimentRunService experimentRunService;
    private final ExperimentMetricRepository metricRepository;
    private final AuthService authService;

    public ExperimentController(ExperimentService experimentService,
                                ExperimentRunService experimentRunService,
                                ExperimentMetricRepository metricRepository,
                                AuthService authService) {
        this.experimentService = experimentService;
        this.experimentRunService = experimentRunService;
        this.metricRepository = metricRepository;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ExperimentResponse> create(@Valid @RequestBody ExperimentCreateRequest request,
                                                      HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        Experiment experiment = experimentService.create(request, user);
        return ResponseEntity.ok(ExperimentResponse.from(experiment));
    }

    @GetMapping
    public ResponseEntity<List<ExperimentResponse>> list(HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        List<ExperimentResponse> experiments = experimentService.listByUser(user.getId()).stream()
                .map(ExperimentResponse::from)
                .toList();
        return ResponseEntity.ok(experiments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentResponse> get(@PathVariable UUID id, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        Experiment experiment = experimentService.findByIdAndUser(id, user.getId())
                .orElseThrow(() -> new DevSleuthException("Experiment not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(ExperimentResponse.from(experiment));
    }

    @PostMapping("/{id}/runs")
    public ResponseEntity<ExperimentRunResponse> startRun(@PathVariable UUID id,
                                                          @Valid @RequestBody ExperimentRunRequest request,
                                                          HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        ExperimentMode mode = parseMode(request.mode());
        ExperimentRun run = experimentRunService.startRun(id, mode, user);
        return ResponseEntity.ok(ExperimentRunResponse.from(run));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<List<ExperimentRunResponse>> getRuns(@PathVariable UUID id, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        List<ExperimentRunResponse> runs = experimentRunService.listRuns(id, user).stream()
                .map(ExperimentRunResponse::from)
                .toList();
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ExperimentRunResponse> getRunStatus(@PathVariable UUID runId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();

        ExperimentRun run = experimentRunService.getRunStatus(runId)
                .orElseThrow(() -> new DevSleuthException("Run not found", HttpStatus.NOT_FOUND));
        // Ownership check: the run must belong to an experiment the user owns.
        experimentService.findByIdAndUser(run.getExperimentId(), user.getId())
                .orElseThrow(() -> new DevSleuthException("Run not found", HttpStatus.NOT_FOUND));

        if ("COMPLETED".equals(run.getStatus())) {
            ExperimentMetricResponse metrics = metricRepository.findByRunId(runId)
                    .map(ExperimentMetricResponse::from)
                    .orElse(null);
            return ResponseEntity.ok(ExperimentRunResponse.from(run, metrics));
        }
        return ResponseEntity.ok(ExperimentRunResponse.from(run));
    }

    /**
     * Returns the authenticated user's experiment metrics. Scoped to the caller so one
     * user never sees another's results; returns the DTO (not the raw entity).
     */
    @GetMapping("/results")
    public ResponseEntity<List<ExperimentMetricResponse>> getResults(HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        List<ExperimentMetricResponse> results = experimentRunService.listMetricsForUser(user.getId()).stream()
                .map(ExperimentMetricResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    private ExperimentMode parseMode(String mode) {
        try {
            return ExperimentMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DevSleuthException(
                    "Invalid mode '" + mode + "'. Expected one of STATIC_ONLY, AI_ONLY, HYBRID.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private User getUser(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) return null;
        return authService.findById(userId).orElse(null);
    }
}
