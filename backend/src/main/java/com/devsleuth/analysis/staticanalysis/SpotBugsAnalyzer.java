package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.analysis.staticanalysis.parser.SpotBugsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs SpotBugs on Java source files.
 * ponytail: V1 runs spotbugs CLI on temp files; upgrade to build-integrated analysis for full bytecode support.
 */
@Component
public class SpotBugsAnalyzer implements StaticAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(SpotBugsAnalyzer.class);
    private static final long ANALYSIS_TIMEOUT_SECONDS = 120;

    private final SpotBugsParser parser = new SpotBugsParser();

    @Override
    public String name() { return "SpotBugs"; }

    @Override
    public List<RawFinding> analyze(AnalysisInput input) {
        try {
            Path workDir = Files.createTempDirectory("devsleuth-spotbugs-");
            writeSourceFiles(input.files(), workDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "spotbugs", "-textui", "-xml", "-effort:max",
                    "-sourcepath", workDir.toString(),
                    workDir.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("SpotBugs timed out after {}s", ANALYSIS_TIMEOUT_SECONDS);
                return List.of();
            }

            return parser.parse(output, input);
        } catch (IOException | InterruptedException e) {
            log.error("SpotBugs analysis failed", e);
            return List.of();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Process p = new ProcessBuilder("spotbugs", "-version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
