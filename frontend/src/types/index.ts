export interface User {
  id: string;
  username: string;
  email: string | null;
  avatarUrl: string | null;
}

export interface Repository {
  id: string;
  githubRepositoryId: number;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string | null;
  language: string | null;
  connected: boolean;
}

export interface PullRequest {
  id: string;
  number: number;
  title: string;
  author: string;
  sourceBranch: string;
  targetBranch: string;
  commitSha: string;
  status: string;
}

export interface Review {
  id: string;
  pullRequestId: string;
  commitSha: string;
  status: string;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  staticFindingCount: number | null;
  aiFindingCount: number | null;
  finalFindingCount: number | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface Finding {
  id: string;
  source: "STATIC" | "AI" | "HYBRID";
  category: "BUG" | "SECURITY" | "PERFORMANCE" | "QUALITY";
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO";
  confidence: number;
  title: string;
  description: string | null;
  recommendation: string | null;
  filePath: string;
  lineStart: number | null;
  lineEnd: number | null;
}

export interface DashboardSummary {
  totalReviews: number;
  totalFindings: number;
  highRiskFindings: number;
  recentReviews: RecentReview[];
}

export interface RecentReview {
  reviewId: string;
  prNumber: number;
  prTitle: string;
  repoFullName: string;
  status: string;
  findingCount: number;
  createdAt: string;
}


export interface ReviewComparison {
  baseReviewId: string;
  compareReviewId: string;
  newFindings: Finding[];
  resolvedFindings: Finding[];
  remainingFindings: Finding[];
}
