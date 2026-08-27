package com.devsleuth.experiment.service;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.analysis.ai.AiAnalysisService;
import com.devsleuth.analysis.staticanalysis.StaticAnalysisEngine;
import com.devsleuth.experiment.ExperimentMode;
import com.devsleuth.experiment.model.EvaluationMetrics;
import com.devsleuth.experiment.model.GroundTruthEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Runs an experiment: feeds a dataset through the pipeline in a given mode,
 * compares results against ground truth, produces evaluation metrics.
 */
@Service
public class ExperimentRunner {

    private static final Logger log = LoggerFactory.getLogger(ExperimentRunner.class);
    private static final int LINE_TOLERANCE = 3;

    private final StaticAnalysisEngine staticEngine;
    private final AiAnalysisService aiService;

    public ExperimentRunner(StaticAnalysisEngine staticEngine, AiAnalysisService aiService) {
        this.staticEngine = staticEngine;
        this.aiService = aiService;
    }

    /**
     * Run an experiment.
     *
     * @param dataset list of file changes to analyze
     * @param groundTruth expected findings
     * @param mode STATIC_ONLY, AI_ONLY, or HYBRID
     * @return evaluation metrics
     */
    public EvaluationMetrics run(List<FileChange> dataset, List<GroundTruthEntry> groundTruth, ExperimentMode mode) {
        AnalysisInput input = new AnalysisInput(UUID.randomUUID(), "experiment", "HEAD", dataset);

        long start = System.currentTimeMillis();

        List<RawFinding> findings = new ArrayList<>();

        if (mode != ExperimentMode.AI_ONLY) {
            findings.addAll(staticEngine.runAll(input));
        }
        if (mode != ExperimentMode.STATIC_ONLY) {
            findings.addAll(aiService.analyze(input));
        }

        long elapsed = System.currentTimeMillis() - start;

        // Match findings against ground truth
        Set<Integer> matchedTruth = new HashSet<>();
        int tp = 0;
        int fp = 0;

        for (RawFinding f : findings) {
            boolean matched = false;
            for (int i = 0; i < groundTruth.size(); i++) {
                if (matchedTruth.contains(i)) continue;
                if (matches(f, groundTruth.get(i))) {
                    matchedTruth.add(i);
                    matched = true;
                    tp++;
                    break;
                }
            }
            if (!matched) {
                fp++;
            }
        }

        int fn = groundTruth.size() - matchedTruth.size();

        EvaluationMetrics metrics = EvaluationMetrics.compute(tp, fp, fn, elapsed);
        log.info("Experiment [{}]: TP={}, FP={}, FN={}, P={}, R={}, F1={}, time={}ms",
                mode, tp, fp, fn, metrics.precision(), metrics.recall(), metrics.f1(), elapsed);

        return metrics;
    }

    /**
     * A finding matches ground truth if: same file, nearby line, same category.
     */
    private boolean matches(RawFinding finding, GroundTruthEntry truth) {
        if (!finding.filePath().endsWith(truth.filePath()) && !truth.filePath().endsWith(finding.filePath())) {
            return false;
        }
        if (finding.category() != truth.category()) {
            return false;
        }
        if (finding.lineStart() == null) return false;
        return Math.abs(finding.lineStart() - truth.lineStart()) <= LINE_TOLERANCE;
    }
}
