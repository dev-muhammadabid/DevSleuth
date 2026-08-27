package com.devsleuth.finding.service;

import com.devsleuth.finding.dto.CalibrationResponse;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.repository.FindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Tracks user feedback (confirm/dismiss) on AI findings and computes accuracy metrics.
 * Uses historical feedback to provide calibration stats that help users understand
 * how reliable AI findings are for their codebase.
 */
@Service
public class CalibrationService {

    private static final Logger log = LoggerFactory.getLogger(CalibrationService.class);

    private final FindingRepository findingRepository;

    public CalibrationService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    /**
     * Submit a verdict (CONFIRMED or DISMISSED) for a finding.
     */
    public void submitVerdict(UUID findingId, String verdict) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new RuntimeException("Finding not found: " + findingId));
        finding.setUserVerdict(verdict);
        findingRepository.save(finding);
        log.info("Verdict submitted for finding={}: {}", findingId, verdict);
    }

    /**
     * Compute calibration stats for findings accessible to a given user.
     */
    public CalibrationResponse getCalibrationStats(UUID userId) {
        List<Finding> allWithFeedback = findingRepository.findWithVerdictForUser(userId);

        if (allWithFeedback.isEmpty()) {
            return new CalibrationResponse(0, 0, 0, 0.0, 0.0, 0.0);
        }

        long confirmed = allWithFeedback.stream()
                .filter(f -> "CONFIRMED".equals(f.getUserVerdict())).count();
        long dismissed = allWithFeedback.stream()
                .filter(f -> "DISMISSED".equals(f.getUserVerdict())).count();
        long total = confirmed + dismissed;

        double accuracy = total > 0 ? (double) confirmed / total : 0.0;

        // Per-source accuracy
        double aiAccuracy = computeAccuracyForSource(allWithFeedback, "AI");
        double staticAccuracy = computeAccuracyForSource(allWithFeedback, "STATIC");

        return new CalibrationResponse(total, confirmed, dismissed, accuracy, aiAccuracy, staticAccuracy);
    }

    private double computeAccuracyForSource(List<Finding> findings, String sourcePrefix) {
        List<Finding> sourceFindings = findings.stream()
                .filter(f -> f.getSource().name().startsWith(sourcePrefix)
                        || "HYBRID".equals(f.getSource().name()))
                .toList();

        if (sourceFindings.isEmpty()) return 0.0;

        long confirmed = sourceFindings.stream()
                .filter(f -> "CONFIRMED".equals(f.getUserVerdict())).count();
        return (double) confirmed / sourceFindings.size();
    }
}
