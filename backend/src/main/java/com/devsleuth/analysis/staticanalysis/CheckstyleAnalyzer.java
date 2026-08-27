package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.analysis.staticanalysis.parser.CheckstyleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs Checkstyle on Java source files.
 */
@Component
public class CheckstyleAnalyzer implements StaticAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(CheckstyleAnalyzer.class);
    private static final long ANALYSIS_TIMEOUT_SECONDS = 120;

    private final CheckstyleParser parser = new CheckstyleParser();

    @Override
    public String name() { return "Checkstyle"; }

    @Override
    public List<RawFinding> analyze(AnalysisInput input) {
        try {
            Path workDir = Files.createTempDirectory("devsleuth-checkstyle-");
            writeSourceFiles(input.files(), workDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", "checkstyle.jar",
                    "-c", "/google_checks.xml",
                    "-f", "xml",
                    workDir.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("Checkstyle timed out after {}s", ANALYSIS_TIMEOUT_SECONDS);
                return List.of();
            }

            return parser.parse(output, input);
        } catch (IOException | InterruptedException e) {
            log.error("Checkstyle analysis failed", e);
            return List.of();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Process p = new ProcessBuilder("java", "-jar", "checkstyle.jar", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
