package com.devsleuth.analysis.staticanalysis;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.analysis.model.RawFinding;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Adapter interface for static analysis tools.
 * Each implementation wraps a specific tool (SpotBugs, Checkstyle, Semgrep).
 */
public interface StaticAnalyzer {

    /** Human-readable name for logging/metrics. */
    String name();

    /** Run analysis on the given input and return raw findings. */
    List<RawFinding> analyze(AnalysisInput input);

    /** Whether this analyzer is available (tool installed, configured). */
    boolean isAvailable();

    /**
     * Writes PR file contents into a workspace directory for analysis.
     *
     * <p>{@code filePath} comes from an untrusted GitHub PR diff, so it is validated to
     * stay within {@code workDir}. Without this check a path like {@code ../../etc/cron.d/x}
     * (or an absolute path) would let a malicious PR write arbitrary files on the host.
     * Entries that escape the workspace are skipped rather than written.
     */
    default void writeSourceFiles(List<FileChange> files, Path workDir) throws IOException {
        Path root = workDir.toAbsolutePath().normalize();
        for (FileChange file : files) {
            if (file.fullContent() == null || file.filePath() == null) continue;

            Path target = root.resolve(file.filePath()).normalize();
            if (!target.startsWith(root)) {
                LoggerFactory.getLogger(getClass())
                        .warn("Skipping file outside workspace (path traversal attempt): {}", file.filePath());
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.fullContent());
        }
    }
}
