"use client";

import { useParams } from "next/navigation";
import { useApi } from "@/hooks/useApi";
import { api } from "@/lib/api";
import { SeverityBadge } from "@/components/SeverityBadge";
import { SourceBadge } from "@/components/SourceBadge";
import type { Finding, ReviewComparison } from "@/types";

export default function ReviewComparisonPage() {
  const params = useParams<{ id: string; compareId: string }>();

  const { data, loading, error } = useApi<ReviewComparison>(
    () => api.reviews.compare(params.id, params.compareId),
    [params.id, params.compareId]
  );

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading comparison…</span>
      </div>
    );
  }
  if (error) return <div className="alert alert-error">Error: {error}</div>;
  if (!data) return null;

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "1.5rem" }}>Review Comparison</h1>

      <div className="grid-3" style={{ marginBottom: "2rem" }}>
        <StatCard label="New Findings" value={data.newFindings.length} color="#dc2626" />
        <StatCard label="Resolved" value={data.resolvedFindings.length} color="#16a34a" />
        <StatCard label="Remaining" value={data.remainingFindings.length} color="#6b7280" />
      </div>

      {data.newFindings.length > 0 && (
        <Section title="New Findings" subtitle="Introduced since the previous review" findings={data.newFindings} color="#fef2f2" />
      )}

      {data.resolvedFindings.length > 0 && (
        <Section title="Resolved" subtitle="Fixed since the previous review" findings={data.resolvedFindings} color="#f0fdf4" />
      )}

      {data.remainingFindings.length > 0 && (
        <Section title="Remaining" subtitle="Still present" findings={data.remainingFindings} color="#f9fafb" />
      )}
    </div>
  );
}

function Section({ title, subtitle, findings, color }: { title: string; subtitle: string; findings: Finding[]; color: string }) {
  return (
    <div style={{ marginBottom: "2rem" }}>
      <h2 style={{ marginBottom: "0.25rem" }}>{title} ({findings.length})</h2>
      <p style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.75rem" }}>{subtitle}</p>
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {findings.map((f) => (
          <div
            key={f.id}
            style={{ padding: "0.75rem", border: "1px solid #e5e7eb", borderRadius: "0.5rem", background: color }}
          >
            <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
              <SeverityBadge severity={f.severity} />
              <SourceBadge source={f.source} />
              <strong>{f.title}</strong>
            </div>
            <div style={{ fontSize: "0.8125rem", color: "#6b7280", marginTop: "0.25rem" }}>
              {f.filePath}:{f.lineStart} | {f.category} | {f.confidence}%
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="stat-card" style={{ ["--accent" as string]: color }}>
      <div className="stat-value" style={{ color }}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}
