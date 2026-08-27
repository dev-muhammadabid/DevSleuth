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

export interface LatestReviewInfo {
  reviewId: string;
  status: string;
  finalFindingCount: number | null;
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
  latestReview?: LatestReviewInfo | null;
}

// Backend returns an enriched PR payload; alias mirrors the API DTO name.
export type PullRequestResponse = PullRequest;

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
  summary: string | null;
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
  suggestedFix: string | null;
  userVerdict: "CONFIRMED" | "DISMISSED" | null;
  filePath: string;
  lineStart: number | null;
  lineEnd: number | null;
}

export interface CalibrationStats {
  totalFeedback: number;
  confirmed: number;
  dismissed: number;
  accuracy: number;
  aiAccuracy: number;
  staticAccuracy: number;
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

export interface MultiModelFinding {
  category: string;
  severity: string;
  confidence: number;
  title: string;
  description: string;
  recommendation: string;
  filePath: string;
  lineStart: number;
  lineEnd: number | null;
}

export interface MultiModelSummary {
  provider: string;
  findingCount: number;
  durationMs: number;
  error: string | null;
  findings: MultiModelFinding[];
}

export interface MultiModelResponse {
  openai: MultiModelSummary;
  anthropic: MultiModelSummary;
}

export type ExperimentMode = "STATIC_ONLY" | "AI_ONLY" | "HYBRID";

export interface Experiment {
  id: string;
  name: string;
  description: string | null;
  datasetSummary: { fileCount: number };
  groundTruthCount: number;
  createdAt: string;
}

export interface ExperimentRun {
  id: string;
  experimentId: string;
  mode: ExperimentMode;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
  metrics?: ExperimentMetrics | null;
}

export interface ExperimentMetrics {
  runId: string;
  // Optional: the backend metric entity does not carry mode; it comes from the
  // parent run (ExperimentRunResponse.mode). Kept optional to align with the API.
  mode?: ExperimentMode;
  truePositives: number;
  falsePositives: number;
  falseNegatives: number;
  precisionScore: number;
  recallScore: number;
  f1Score: number;
  analysisTimeMs: number;
}

export interface FileChangeInput {
  filename: string;
  status: string;
  patch?: string | null;
}

export interface GroundTruthEntryInput {
  filePath: string;
  lineStart: number;
  lineEnd?: number | null;
  category: string;
  severity?: string | null;
  title?: string | null;
}

export interface ExperimentCreateRequest {
  name: string;
  description?: string | null;
  dataset: FileChangeInput[];
  groundTruth: GroundTruthEntryInput[];
}
