"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { useApi } from "@/hooks/useApi";
import { usePolling } from "@/hooks/usePolling";
import { api } from "@/lib/api";
import { SeverityBadge } from "@/components/SeverityBadge";
import { SourceBadge } from "@/components/SourceBadge";
import { StatusBadge } from "@/components/StatusBadge";
import { CodeViewer } from "@/components/CodeViewer";
import { FindingFilters, type Filters } from "@/components/FindingFilters";
import { ReviewProgress } from "@/components/ReviewProgress";
import type { Finding, Review } from "@/types";

const TERMINAL_STATUSES = ["COMPLETED", "FAILED"];
const SEVERITY_ORDER = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"] as const;

export default function ReviewDetailPage() {
  const params = useParams<{ id: string }>();
  const [selectedFinding, setSelectedFinding] = useState<Finding | null>(null);
  const [filters, setFilters] = useState<Filters>({
    severity: [],
    category: [],
    source: [],
    file: null,
    minConfidence: 0,
  });

  const { data: review, loading: loadingReview } = usePolling<Review>(
    () => api.reviews.get(params.id),
    (r) => TERMINAL_STATUSES.includes(r.status),
    3000
  );
  const { data: findings, loading: loadingFindings } = useApi<Finding[]>(
    () => api.reviews.findings(params.id),
    [review?.status]
  );

  if (loadingReview) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading review…</span>
      </div>
    );
  }
  if (!review) return <div className="empty-state">Review not found.</div>;

  // Show progress indicator while review is in progress
  if (!TERMINAL_STATUSES.includes(review.status)) {
    return <ReviewProgress review={review} />;
  }

  const filtered = applyFilters(findings ?? [], filters);
  const grouped = groupBySeverity(filtered);
  const severityCounts = countBySeverity(filtered);

  return (
    <div className="fade-in">
      {/* Header */}
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ marginBottom: "0.25rem" }}>
          PR Review
        </h1>
        <div style={{ display: "flex", gap: "1rem", alignItems: "center", flexWrap: "wrap" }}>
          <StatusBadge status={review.status} />
          <span style={{ fontSize: "0.875rem", color: "#6b7280" }}>
            Commit: {review.commitSha?.slice(0, 7)}
          </span>
          {review.durationMs != null && (
            <span style={{ fontSize: "0.875rem", color: "#6b7280" }}>
              Duration: {(review.durationMs / 1000).toFixed(1)}s
            </span>
          )}
        </div>
      </div>

      {review.errorMessage && (
        <div className="alert alert-error" style={{ marginBottom: "1rem" }}>
          {review.errorMessage}
        </div>
      )}

      {/* Severity summary */}
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1.5rem", flexWrap: "wrap" }}>
        {SEVERITY_ORDER.map((s) => (
          <SeverityCount key={s} severity={s} count={severityCounts[s] || 0} />
        ))}
      </div>

      {/* Filters + Findings */}
      <div style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: "1.5rem" }}>
        <FindingFilters
          filters={filters}
          onChange={setFilters}
          findings={findings ?? []}
        />

        <div>
          {filtered.length === 0 ? (
            <p style={{ color: "#6b7280" }}>No findings match the current filters.</p>
          ) : (
            SEVERITY_ORDER.map((sev) => {
              const items = grouped[sev];
              if (!items || items.length === 0) return null;
              return (
                <div key={sev} style={{ marginBottom: "1.5rem" }}>
                  <h3 style={{ marginBottom: "0.5rem" }}>{sev} ({items.length})</h3>
                  <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                    {items.map((f) => (
                      <FindingCard
                        key={f.id}
                        finding={f}
                        onSelect={() => setSelectedFinding(f)}
                        isSelected={selectedFinding?.id === f.id}
                      />
                    ))}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Code viewer modal */}
      {selectedFinding && (
        <CodeViewer finding={selectedFinding} onClose={() => setSelectedFinding(null)} />
      )}
    </div>
  );
}

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: "#dc2626",
  HIGH: "#ea580c",
  MEDIUM: "#ca8a04",
  LOW: "#2563eb",
  INFO: "#64748b",
};

function FindingCard({ finding: f, onSelect, isSelected }: { finding: Finding; onSelect: () => void; isSelected: boolean }) {
  return (
    <div
      onClick={onSelect}
      className={`finding-card${isSelected ? " selected" : ""}`}
      style={{ ["--accent" as string]: SEVERITY_COLORS[f.severity] ?? "var(--border-strong)" }}
    >
      <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", marginBottom: "0.5rem" }}>
        <SeverityBadge severity={f.severity} />
        <SourceBadge source={f.source} />
        <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>{f.category}</span>
      </div>
      <strong>{f.title}</strong>
      <div style={{ fontSize: "0.875rem", color: "#6b7280", marginTop: "0.25rem" }}>
        {f.filePath}:{f.lineStart} — Confidence: {f.confidence}%
      </div>
      {f.description && (
        <details style={{ marginTop: "0.5rem" }}>
          <summary style={{ fontSize: "0.875rem", color: "#374151", cursor: "pointer" }}>
            Why this matters
          </summary>
          <p style={{ marginTop: "0.25rem", fontSize: "0.875rem" }}>{f.description}</p>
        </details>
      )}
      {f.recommendation && (
        <details style={{ marginTop: "0.25rem" }}>
          <summary style={{ fontSize: "0.875rem", color: "#374151", cursor: "pointer" }}>
            Recommendation
          </summary>
          <p style={{ marginTop: "0.25rem", fontSize: "0.875rem", fontStyle: "italic" }}>{f.recommendation}</p>
        </details>
      )}
      <div style={{ marginTop: "0.5rem", fontSize: "0.75rem", color: "#6b7280" }}>
        Source: {f.source === "HYBRID" ? "Static Analysis + AI Analysis" : f.source === "STATIC" ? "Static Analysis" : "AI Analysis"}
      </div>
    </div>
  );
}

function SeverityCount({ severity, count }: { severity: string; count: number }) {
  const icons: Record<string, string> = { CRITICAL: "\uD83D\uDD34", HIGH: "\uD83D\uDD34", MEDIUM: "\uD83D\uDFE0", LOW: "\uD83D\uDFE1", INFO: "\u26AA" };
  return (
    <span style={{ fontSize: "0.875rem" }}>
      {icons[severity] || ""} {count} {severity.charAt(0) + severity.slice(1).toLowerCase()}
    </span>
  );
}

function applyFilters(findings: Finding[], filters: Filters): Finding[] {
  return findings.filter((f) => {
    if (filters.severity.length > 0 && !filters.severity.includes(f.severity)) return false;
    if (filters.category.length > 0 && !filters.category.includes(f.category)) return false;
    if (filters.source.length > 0 && !filters.source.includes(f.source)) return false;
    if (filters.file && f.filePath !== filters.file) return false;
    if (f.confidence < filters.minConfidence) return false;
    return true;
  });
}

function groupBySeverity(findings: Finding[]): Record<string, Finding[]> {
  const groups: Record<string, Finding[]> = {};
  for (const f of findings) {
    (groups[f.severity] ??= []).push(f);
  }
  return groups;
}

function countBySeverity(findings: Finding[]): Record<string, number> {
  const counts: Record<string, number> = {};
  for (const f of findings) {
    counts[f.severity] = (counts[f.severity] || 0) + 1;
  }
  return counts;
}
