package com.devsleuth.experiment.repository;

import com.devsleuth.experiment.entity.ExperimentMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentMetricRepository extends JpaRepository<ExperimentMetric, UUID> {
    Optional<ExperimentMetric> findByRunId(UUID runId);
    List<ExperimentMetric> findByRunIdIn(List<UUID> runIds);
}
