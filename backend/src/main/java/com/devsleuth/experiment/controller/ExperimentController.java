package com.devsleuth.experiment.controller;

import com.devsleuth.experiment.entity.ExperimentMetric;
import com.devsleuth.experiment.entity.ExperimentRun;
import com.devsleuth.experiment.repository.ExperimentMetricRepository;
import com.devsleuth.experiment.repository.ExperimentRunRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentRunRepository runRepository;
    private final ExperimentMetricRepository metricRepository;

    public ExperimentController(ExperimentRunRepository runRepository,
                                ExperimentMetricRepository metricRepository) {
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
    }

    @GetMapping("/{experimentId}/runs")
    public ResponseEntity<List<ExperimentRun>> getRuns(@PathVariable UUID experimentId) {
        return ResponseEntity.ok(runRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId));
    }

    @GetMapping("/runs/{runId}/metrics")
    public ResponseEntity<ExperimentMetric> getMetrics(@PathVariable UUID runId) {
        return metricRepository.findByRunId(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/results")
    public ResponseEntity<List<ExperimentMetric>> getAllMetrics() {
        return ResponseEntity.ok(metricRepository.findAll());
    }
}
