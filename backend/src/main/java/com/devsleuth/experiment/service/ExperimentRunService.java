package com.devsleuth.experiment.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.experiment.ExperimentMode;
import com.devsleuth.experiment.entity.Experiment;
import com.devsleuth.experiment.entity.ExperimentMetric;
import com.devsleuth.experiment.entity.ExperimentRun;
import com.devsleuth.experiment.model.EvaluationMetrics;
import com.devsleuth.experiment.repository.ExperimentMetricRepository;
import com.devsleuth.experiment.repository.ExperimentRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates experiment execution: validates ownership, creates a RUNNING run,
 * hands off to an async worker that runs the pipeline via {@link ExperimentRunner},
 * persists metrics, and records COMPLETED/FAILED status.
 */
@Service
public class ExperimentRunService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentRunService.class);

    private final ExperimentService experimentService;
    private final ExperimentRunner experimentRunner;
    private final ExperimentRunRepository runRepository;
    private final ExperimentMetricRepository metricRepository;
    // Self-reference so the @Async proxy applies to the worker method (self-invocation
    // through 'this' would bypass the proxy). ObjectProvider avoids constructor cycle.
    private final ObjectProvider<ExperimentRunService> self;

    public ExperimentRunService(ExperimentService experimentService,
                                ExperimentRunner experimentRunner,
                                ExperimentRunRepository runRepository,
                                ExperimentMetricRepository metricRepository,
                                ObjectProvider<ExperimentRunService> self) {
        this.experimentService = experimentService;
        this.experimentRunner = experimentRunner;
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.self = self;
    }

    /**
     * Start a run for an experiment in the given mode. Validates that the experiment
     * belongs to the requesting user, persists a RUNNING run, and delegates execution
     * to an async worker. Returns immediately with the RUNNING run.
     */
    public ExperimentRun startRun(UUID experimentId, ExperimentMode mode, User user) {
        Experiment experiment = experimentService.findByIdAndUser(experimentId, user.getId())
                .orElseThrow(() -> new DevSleuthException("Experiment not found", HttpStatus.NOT_FOUND));

        ExperimentRun run = new ExperimentRun();
        run.setExperimentId(experiment.getId());
        run.setMode(mode);
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now());
        run = runRepository.save(run);

        // Hand off through the proxy so @Async takes effect.
        self.getObject().execute(run.getId());
        return run;
    }

    /**
     * Async worker. Reloads the run by id (avoids detached-entity issues on the async
     * thread), runs the pipeline, persists metrics, and records the terminal status.
     * Catches all exceptions and records FAILED rather than propagating.
     */
    @Async("analysisExecutor")
    public void execute(UUID runId) {
        ExperimentRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Experiment run {} vanished before execution", runId);
            return;
        }
        try {
            Experiment experiment = experimentService.findById(run.getExperimentId())
                    .orElseThrow(() -> new DevSleuthException("Experiment not found", HttpStatus.NOT_FOUND));

            EvaluationMetrics metrics = experimentRunner.run(
                    experiment.getDataset(), experiment.getGroundTruth(), run.getMode());

            ExperimentMetric metric = new ExperimentMetric();
            metric.setRunId(run.getId());
            metric.setTruePositives(metrics.truePositives());
            metric.setFalsePositives(metrics.falsePositives());
            metric.setFalseNegatives(metrics.falseNegatives());
            metric.setPrecisionScore(metrics.precision());
            metric.setRecallScore(metrics.recall());
            metric.setF1Score(metrics.f1());
            metric.setAnalysisTimeMs(metrics.analysisTimeMs());
            metricRepository.save(metric);

            run.setStatus("COMPLETED");
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            log.info("Experiment run {} completed [{}]", run.getId(), run.getMode());
        } catch (Exception e) {
            log.error("Experiment run {} failed", run.getId(), e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
        }
    }

    /**
     * Get a run's status/metrics by id.
     */
    public Optional<ExperimentRun> getRunStatus(UUID runId) {
        return runRepository.findById(runId);
    }

    /**
     * List an experiment's runs, most recent first. Ownership-checked: returns 404 if
     * the experiment does not belong to the requesting user.
     */
    public List<ExperimentRun> listRuns(UUID experimentId, User user) {
        experimentService.findByIdAndUser(experimentId, user.getId())
                .orElseThrow(() -> new DevSleuthException("Experiment not found", HttpStatus.NOT_FOUND));
        return runRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    /**
     * All metrics for runs owned by the given user, scoped so one user never sees
     * another's results. Three bounded queries (experiments -> runs -> metrics); no N+1.
     */
    public List<ExperimentMetric> listMetricsForUser(UUID userId) {
        List<UUID> experimentIds = experimentService.listByUser(userId).stream()
                .map(Experiment::getId)
                .toList();
        if (experimentIds.isEmpty()) return List.of();

        List<UUID> runIds = runRepository.findByExperimentIdIn(experimentIds).stream()
                .map(ExperimentRun::getId)
                .toList();
        if (runIds.isEmpty()) return List.of();

        return metricRepository.findByRunIdIn(runIds);
    }
}
