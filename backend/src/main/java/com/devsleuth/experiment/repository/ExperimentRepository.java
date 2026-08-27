package com.devsleuth.experiment.repository;

import com.devsleuth.experiment.entity.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

    List<Experiment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Experiment> findByIdAndUserId(UUID id, UUID userId);
}
