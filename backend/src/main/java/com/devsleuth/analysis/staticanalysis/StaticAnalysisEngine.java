package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates all available static analyzers.
 */
@Service
public class StaticAnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(StaticAnalysisEngine.class);

    private final List<StaticAnalyzer> analyzers;

    public StaticAnalysisEngine(List<StaticAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    public List<RawFinding> runAll(AnalysisInput input) {
        List<RawFinding> allFindings = new ArrayList<>();

        for (StaticAnalyzer analyzer : analyzers) {
            if (!analyzer.isAvailable()) {
                log.info("Skipping {} (not available)", analyzer.name());
                continue;
            }
            log.info("Running {}", analyzer.name());
            try {
                List<RawFinding> findings = analyzer.analyze(input);
                log.info("{} produced {} findings", analyzer.name(), findings.size());
                allFindings.addAll(findings);
            } catch (Exception e) {
                log.error("{} failed unexpectedly", analyzer.name(), e);
            }
        }

        return allFindings;
    }
}
