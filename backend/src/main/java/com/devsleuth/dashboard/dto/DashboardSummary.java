package com.devsleuth.dashboard.dto;

import java.util.List;

public record DashboardSummary(
        long totalReviews,
        long totalFindings,
        long highRiskFindings,
        List<RecentReview> recentReviews
) {
    public record RecentReview(
            String reviewId,
            int prNumber,
            String prTitle,
            String repoFullName,
            String status,
            int findingCount,
            String createdAt
    ) {}
}
