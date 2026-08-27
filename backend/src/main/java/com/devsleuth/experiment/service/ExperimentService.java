package com.devsleuth.experiment.service;

import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.auth.entity.User;
import com.devsleuth.experiment.dto.ExperimentCreateRequest;
import com.devsleuth.experiment.dto.FileChangeDto;
import com.devsleuth.experiment.dto.GroundTruthEntryDto;
import com.devsleuth.experiment.entity.Experiment;
import com.devsleuth.experiment.model.GroundTruthEntry;
import com.devsleuth.experiment.repository.ExperimentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD for experiments. All reads are scoped to the owning user; field-level validation
 * of the request (@NotBlank/@NotEmpty) is enforced at the controller via @Valid.
 */
@Service
public class ExperimentService {

    private final ExperimentRepository experimentRepository;

    public ExperimentService(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    public Experiment create(ExperimentCreateRequest req, User user) {
        Experiment experiment = new Experiment();
        experiment.setUserId(user.getId());
        experiment.setName(req.name());
        experiment.setDescription(req.description());
        experiment.setDataset(mapDataset(req.dataset()));
        experiment.setGroundTruth(mapGroundTruth(req.groundTruth()));
        return experimentRepository.save(experiment);
    }

    public List<Experiment> listByUser(UUID userId) {
        return experimentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Experiment> findByIdAndUser(UUID id, UUID userId) {
        return experimentRepository.findByIdAndUserId(id, userId);
    }

    /** Load by id without an ownership check — for internal use after ownership is verified. */
    public Optional<Experiment> findById(UUID id) {
        return experimentRepository.findById(id);
    }

    private List<FileChange> mapDataset(List<FileChangeDto> dataset) {
        return dataset.stream()
                .map(dto -> new FileChange(dto.filename(), dto.patch(), null))
                .toList();
    }

    private List<GroundTruthEntry> mapGroundTruth(List<GroundTruthEntryDto> groundTruth) {
        return groundTruth.stream()
                .map(dto -> new GroundTruthEntry(
                        dto.filePath(),
                        dto.lineStart(),
                        dto.lineEnd() != null ? dto.lineEnd() : dto.lineStart(),
                        dto.category(),
                        dto.severity(),
                        dto.title()))
                .toList();
    }
}
