package com.devsleuth.experiment;

/**
 * Analysis modes for experimental comparison.
 *
 * STATIC_ONLY: Only static analysis tools (Semgrep, SpotBugs, Checkstyle).
 * AI_ONLY: Only LLM-based analysis.
 * HYBRID: Static + AI + deduplication + correlation (default, full pipeline).
 */
public enum ExperimentMode {
    STATIC_ONLY,
    AI_ONLY,
    HYBRID
}
