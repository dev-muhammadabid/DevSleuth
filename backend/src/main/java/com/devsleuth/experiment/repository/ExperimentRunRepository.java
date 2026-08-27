package com.devsleuth.experiment.repository;

import com.devsleuth.experiment.entity.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, UUID> {
    List<ExperimentRun> findByExperimentIdOrderByCreatedAtDesc(UUID experimentId);
}
