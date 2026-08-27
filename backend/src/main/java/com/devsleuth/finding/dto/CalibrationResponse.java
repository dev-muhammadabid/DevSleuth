package com.devsleuth.finding.dto;

public record CalibrationResponse(
        long totalFeedback,
        long confirmed,
        long dismissed,
        double accuracy,
        double aiAccuracy,
        double staticAccuracy
) {}
