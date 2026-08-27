package com.devsleuth.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a project ".env" file into the Spring environment so secrets like
 * GITHUB_CLIENT_SECRET and ENCRYPTION_KEY can live in a git-ignored .env instead of
 * being exported in the shell on every run.
 *
 * <p>Standard-library only (no dotenv dependency). The .env source is added last, so
 * real OS environment variables and JVM system properties still take precedence, which
 * matches conventional dotenv behaviour. Missing/unreadable .env is non-fatal.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = locate();
        if (envFile == null) {
            return;
        }
        try {
            Map<String, Object> values = parseLines(Files.readAllLines(envFile));
            if (!values.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
            }
        } catch (IOException e) {
            // Non-fatal: fall back to OS env vars / configured defaults.
            System.err.println("[dotenv] Could not read " + envFile + ": " + e.getMessage());
        }
    }

    /**
     * Looks for .env in the working directory, then the parent directory (the repo root
     * when the backend is started from the backend/ folder).
     */
    private Path locate() {
        for (Path candidate : List.of(Paths.get(".env"), Paths.get("..", ".env"))) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Parses KEY=VALUE lines, skipping blanks/comments, tolerating {@code export} and quotes. */
    static Map<String, Object> parseLines(List<String> lines) {
        Map<String, Object> map = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = stripQuotes(line.substring(eq + 1).strip());
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static String stripQuotes(String v) {
        if (v.length() >= 2
                && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}
