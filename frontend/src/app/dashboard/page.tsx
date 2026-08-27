"use client";

import { useApi } from "@/hooks/useApi";
import { api } from "@/lib/api";
import { StatusBadge } from "@/components/StatusBadge";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import type { DashboardSummary } from "@/types";

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardContent />
    </ProtectedRoute>
  );
}

function DashboardContent() {
  const { data, loading, error } = useApi<DashboardSummary>(() => api.dashboard.summary(), []);

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading dashboard…</span>
      </div>
    );
  }
  if (error) return <div className="alert alert-error">Failed to load dashboard: {error}</div>;
  if (!data) return null;

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "1.5rem" }}>Dashboard</h1>

      <div className="grid-3" style={{ marginBottom: "2rem" }}>
        <StatCard label="Reviews" value={data.totalReviews} accent="var(--brand)" delay={0} />
        <StatCard label="Findings" value={data.totalFindings} accent="#0891b2" delay={0.06} />
        <StatCard label="High Risk" value={data.highRiskFindings} accent="var(--critical)" delay={0.12} valueColor="var(--critical)" />
      </div>

      <h2 style={{ marginBottom: "1rem" }}>Recent Reviews</h2>
      {data.recentReviews.length === 0 ? (
        <div className="empty-state">
          <p style={{ fontWeight: 600, color: "var(--text)" }}>No reviews yet</p>
          <p style={{ marginTop: "0.35rem" }}>
            Connect a repository and analyze a PR to get started.
          </p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>PR</th>
                <th>Repository</th>
                <th>Status</th>
                <th>Findings</th>
              </tr>
            </thead>
            <tbody>
              {data.recentReviews.map((r) => (
                <tr key={r.reviewId}>
                  <td>
                    <a href={`/reviews/${r.reviewId}`}>
                      #{r.prNumber} {r.prTitle}
                    </a>
                  </td>
                  <td className="muted" style={{ fontSize: "0.875rem" }}>{r.repoFullName}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>
                    <span style={{ fontWeight: r.findingCount > 0 ? 700 : 400 }}>
                      {r.findingCount} {r.findingCount === 1 ? "issue" : "issues"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  accent,
  delay,
  valueColor,
}: {
  label: string;
  value: number;
  accent: string;
  delay: number;
  valueColor?: string;
}) {
  return (
    <div className="stat-card" style={{ ["--accent" as string]: accent, animationDelay: `${delay}s` }}>
      <div className="stat-value" style={{ color: valueColor }}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}
