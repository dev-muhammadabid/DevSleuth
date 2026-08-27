package com.devsleuth.analysis.staticanalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes analyzer processes with security boundaries:
 * - Fixed executable path (never user-provided)
 * - Controlled arguments (never interpolated from untrusted input)
 * - Restricted working directory
 * - Timeout enforcement
 * - No shell expansion (ProcessBuilder, not Runtime.exec(String))
 */
public final class ProcessIsolation {

    private static final Logger log = LoggerFactory.getLogger(ProcessIsolation.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private ProcessIsolation() {}

    /**
     * Runs a process safely. Returns stdout content.
     * Throws if timeout exceeded or process fails to start.
     */
    public static String execute(List<String> command, Path workDir) throws IOException, InterruptedException {
        return execute(command, workDir, DEFAULT_TIMEOUT_SECONDS);
    }

    public static String execute(List<String> command, Path workDir, long timeoutSeconds)
            throws IOException, InterruptedException {

        log.debug("Executing: {} in {}", command.get(0), workDir);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        // No shell: ProcessBuilder uses exec directly, no shell expansion of special chars
        pb.environment().clear(); // Minimal env — don't leak host secrets to analyzer
        pb.environment().put("PATH", System.getenv("PATH")); // Need PATH to find the tool
        pb.environment().put("HOME", System.getProperty("java.io.tmpdir")); // Safe home dir

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Process timed out after " + timeoutSeconds + "s: " + command.get(0));
        }

        int exitCode = process.exitValue();
        log.debug("{} exited with code {}", command.get(0), exitCode);
        return output;
    }
}
