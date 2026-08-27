package com.devsleuth.analysis.model;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;

/**
 * Common finding model. Every analyzer (static or AI) produces these.
 * This is the central contract of DevSleuth — all tool-specific formats
 * get normalized into this before deduplication and persistence.
 */
public record RawFinding(
        FindingSource source,
        FindingCategory category,
        Severity severity,
        int confidence,
        String title,
        String description,
        String recommendation,
        String filePath,
        Integer lineStart,
        Integer lineEnd
) {}
