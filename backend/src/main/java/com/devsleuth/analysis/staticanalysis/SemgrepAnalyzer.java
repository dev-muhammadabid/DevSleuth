package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.analysis.staticanalysis.parser.SemgrepParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs Semgrep on source files. Primary static analyzer for V1.
 */
@Component
public class SemgrepAnalyzer implements StaticAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(SemgrepAnalyzer.class);
    private static final long ANALYSIS_TIMEOUT_SECONDS = 120;

    private final String semgrepPath;
    private final SemgrepParser parser = new SemgrepParser();

    public SemgrepAnalyzer(@Value("${devsleuth.analysis.static-analyzer-path:semgrep}") String semgrepPath) {
        this.semgrepPath = semgrepPath;
    }

    @Override
    public String name() { return "Semgrep"; }

    @Override
    public List<RawFinding> analyze(AnalysisInput input) {
        try {
            Path workDir = Files.createTempDirectory("devsleuth-semgrep-");
            writeSourceFiles(input.files(), workDir);

            ProcessBuilder pb = new ProcessBuilder(
                    semgrepPath, "scan",
                    "--config", "auto",
                    "--json",
                    "--no-git-ignore",
                    workDir.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("Semgrep timed out after {}s", ANALYSIS_TIMEOUT_SECONDS);
                return List.of();
            }

            return parser.parse(output, input);
        } catch (IOException | InterruptedException e) {
            log.error("Semgrep analysis failed", e);
            return List.of();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Process p = new ProcessBuilder(semgrepPath, "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
