package com.devsleuth.experiment.model;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.Severity;

/**
 * A single ground-truth finding expected from the test corpus.
 */
public record GroundTruthEntry(
        String filePath,
        int lineStart,
        int lineEnd,
        FindingCategory category,
        Severity severity,
        String title
) {}
