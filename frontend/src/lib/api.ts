import type {
  DashboardSummary,
  Finding,
  PullRequest,
  Repository,
  Review,
  ReviewComparison,
  User,
} from "@/types";

const BASE = "/api";

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { credentials: "include" });
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`);
  return res.json();
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`);
  // Logout and some POSTs return no body; guard against empty-body JSON parse.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  auth: {
    me: () => get<User>("/auth/me"),
    getAuthUrl: () => get<{ url: string }>("/auth/github"),
    logout: () => post<void>("/auth/logout"),
  },
  dashboard: {
    summary: () => get<DashboardSummary>("/dashboard/summary"),
  },
  repositories: {
    list: () => get<Repository[]>("/repositories"),
    connect: (id: string) => post<Repository>(`/repositories/${id}/connect`),
  },
  pullRequests: {
    list: (repoId: string) => get<PullRequest[]>(`/repositories/${repoId}/pull-requests`),
    analyze: (repoId: string, number: number) =>
      post<{ reviewId: string; status: string }>(`/repositories/${repoId}/pull-requests/${number}/analyze`),
  },
  reviews: {
    get: (id: string) => get<Review>(`/reviews/${id}`),
    findings: (id: string) => get<Finding[]>(`/reviews/${id}/findings`),
    compare: (baseId: string, compareId: string) =>
      get<ReviewComparison>(`/reviews/${baseId}/compare/${compareId}`),
  },
  findings: {
    get: (id: string) => get<Finding>(`/findings/${id}`),
  },
};
