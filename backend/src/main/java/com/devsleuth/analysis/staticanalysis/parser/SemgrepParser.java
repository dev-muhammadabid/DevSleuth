package com.devsleuth.analysis.staticanalysis.parser;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Semgrep JSON output into RawFindings.
 *
 * Semgrep JSON structure:
 * { "results": [ { "check_id", "path", "start": {"line"}, "end": {"line"},
 *                   "extra": { "message", "severity", "metadata": {"category"} } } ] }
 */
public class SemgrepParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public List<RawFinding> parse(String jsonOutput, AnalysisInput input) {
        List<RawFinding> findings = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonOutput);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return findings;

            for (JsonNode r : results) {
                String checkId = r.get("check_id").asText();
                String path = r.get("path").asText();
                int lineStart = r.path("start").path("line").asInt(0);
                int lineEnd = r.path("end").path("line").asInt(lineStart);

                JsonNode extra = r.get("extra");
                String message = extra != null ? extra.path("message").asText("") : "";
                String severityStr = extra != null ? extra.path("severity").asText("WARNING") : "WARNING";

                findings.add(new RawFinding(
                        FindingSource.STATIC,
                        mapCategory(checkId, extra),
                        mapSeverity(severityStr),
                        100, // Deterministic tool = full confidence
                        checkId,
                        message,
                        "Review and fix according to rule: " + checkId,
                        relativizePath(path, input),
                        lineStart,
                        lineEnd
                ));
            }
        } catch (Exception e) {
            // If output is malformed, return empty
        }
        return findings;
    }

    private Severity mapSeverity(String s) {
        return switch (s.toUpperCase()) {
            case "ERROR" -> Severity.HIGH;
            case "WARNING" -> Severity.MEDIUM;
            case "INFO" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private FindingCategory mapCategory(String checkId, JsonNode extra) {
        if (checkId.contains("security") || checkId.contains("injection") || checkId.contains("xss")) {
            return FindingCategory.SECURITY;
        }
        if (extra != null && extra.has("metadata")) {
            String cat = extra.path("metadata").path("category").asText("");
            if (cat.contains("security")) return FindingCategory.SECURITY;
            if (cat.contains("performance")) return FindingCategory.PERFORMANCE;
        }
        return FindingCategory.BUG;
    }

    private String relativizePath(String absolutePath, AnalysisInput input) {
        // Temp dir paths: extract the relative part matching input file paths
        for (var file : input.files()) {
            if (absolutePath.endsWith(file.filePath())) {
                return file.filePath();
            }
        }
        return absolutePath;
    }
}
