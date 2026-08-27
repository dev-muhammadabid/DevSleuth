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
import type { MultiModelResponse, MultiModelSummary } from "@/types";

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

      {/* AI-generated PR summary */}
      {review.summary && (
        <div className="card" style={{ marginBottom: "1.5rem", padding: "1.25rem" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.5rem" }}>
            <span style={{
              padding: "2px 8px",
              borderRadius: "4px",
              fontSize: "0.68rem",
              fontWeight: 800,
              background: "linear-gradient(135deg, var(--brand), #22d3ee)",
              color: "#fff",
              letterSpacing: "0.04em"
            }}>AI Summary</span>
          </div>
          <p style={{ fontSize: "0.92rem", lineHeight: 1.65, color: "var(--text)" }}>
            {review.summary}
          </p>
        </div>
      )}

      {/* Multi-model comparison */}
      <MultiModelPanel reviewId={review.id} />

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
      {f.suggestedFix && (
        <details style={{ marginTop: "0.25rem" }}>
          <summary style={{ fontSize: "0.875rem", color: "var(--brand-600)", cursor: "pointer", fontWeight: 600 }}>
            Suggested Fix
          </summary>
          <pre style={{
            marginTop: "0.35rem",
            padding: "0.6rem",
            fontSize: "0.78rem",
            background: "#1e1e2e",
            color: "#cdd6f4",
            borderRadius: "6px",
            overflow: "auto",
            lineHeight: 1.5,
            fontFamily: "'JetBrains Mono', 'Fira Code', ui-monospace, monospace"
          }}>{f.suggestedFix}</pre>
        </details>
      )}
      <div style={{ marginTop: "0.5rem", fontSize: "0.75rem", color: "#6b7280" }}>
        Source: {f.source === "HYBRID" ? "Static Analysis + AI Analysis" : f.source === "STATIC" ? "Static Analysis" : "AI Analysis"}
      </div>
      <VerdictButtons finding={f} />
      <FindingChat findingId={f.id} />
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

function FindingChat({ findingId }: { findingId: string }) {
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState<{ role: "user" | "ai"; text: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || loading) return;

    const q = question.trim();
    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setQuestion("");
    setLoading(true);

    try {
      const res = await api.findings.chat(findingId, q);
      setMessages((prev) => [...prev, { role: "ai", text: res.answer }]);
    } catch {
      setMessages((prev) => [...prev, { role: "ai", text: "Failed to get a response. Try again." }]);
    } finally {
      setLoading(false);
    }
  };

  if (!open) {
    return (
      <button
        onClick={(e) => { e.stopPropagation(); setOpen(true); }}
        style={{
          marginTop: "0.5rem",
          padding: "3px 10px",
          fontSize: "0.72rem",
          fontWeight: 600,
          border: "1px solid var(--brand-soft)",
          borderRadius: "999px",
          background: "var(--brand-soft)",
          color: "var(--brand-700)",
          cursor: "pointer",
        }}
      >
        Ask AI about this
      </button>
    );
  }

  return (
    <div
      onClick={(e) => e.stopPropagation()}
      style={{
        marginTop: "0.6rem",
        padding: "0.75rem",
        background: "var(--bg)",
        borderRadius: "var(--radius-sm)",
        border: "1px solid var(--border)",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.5rem" }}>
        <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--brand-600)" }}>AI Chat</span>
        <button
          onClick={() => setOpen(false)}
          style={{ fontSize: "0.7rem", color: "var(--text-muted)", cursor: "pointer", background: "none", border: "none" }}
        >
          Close
        </button>
      </div>

      {messages.length > 0 && (
        <div style={{ maxHeight: "200px", overflowY: "auto", marginBottom: "0.5rem" }}>
          {messages.map((m, i) => (
            <div key={i} style={{
              marginBottom: "0.4rem",
              padding: "0.4rem 0.6rem",
              borderRadius: "6px",
              fontSize: "0.82rem",
              lineHeight: 1.5,
              background: m.role === "user" ? "var(--brand-soft)" : "var(--surface)",
              border: m.role === "ai" ? "1px solid var(--border)" : "none",
            }}>
              <span style={{ fontWeight: 600, fontSize: "0.68rem", color: "var(--text-muted)", display: "block", marginBottom: "2px" }}>
                {m.role === "user" ? "You" : "AI"}
              </span>
              {m.text}
            </div>
          ))}
          {loading && <span className="spinner" style={{ marginTop: "0.25rem" }} />}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: "flex", gap: "0.4rem" }}>
        <input
          className="input"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about this finding..."
          style={{ flex: 1, fontSize: "0.82rem", padding: "0.4rem 0.6rem" }}
          disabled={loading}
        />
        <button
          type="submit"
          className="btn btn-primary btn-sm"
          disabled={loading || !question.trim()}
        >
          Send
        </button>
      </form>
    </div>
  );
}

function MultiModelPanel({ reviewId }: { reviewId: string }) {
  const [result, setResult] = useState<MultiModelResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runComparison = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.reviews.multiModel(reviewId);
      setResult(res);
    } catch (e) {
      setError("Failed to run multi-model comparison.");
    } finally {
      setLoading(false);
    }
  };

  if (!result && !loading) {
    return (
      <div style={{ marginBottom: "1.5rem" }}>
        <button onClick={runComparison} className="btn btn-sm" disabled={loading}>
          Compare Models (GPT-4o vs Claude)
        </button>
        {error && <span style={{ marginLeft: "0.5rem", fontSize: "0.8rem", color: "var(--critical)" }}>{error}</span>}
      </div>
    );
  }

  if (loading) {
    return (
      <div className="card" style={{ marginBottom: "1.5rem", padding: "1.25rem" }}>
        <div className="loading-center" style={{ padding: "1rem" }}>
          <span className="spinner" />
          <span style={{ fontSize: "0.85rem" }}>Running analysis on both models...</span>
        </div>
      </div>
    );
  }

  if (!result) return null;

  return (
    <div className="card" style={{ marginBottom: "1.5rem", padding: "1.25rem" }}>
      <h3 style={{ marginBottom: "1rem" }}>Multi-Model Comparison</h3>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        <ModelColumn model={result.openai} />
        <ModelColumn model={result.anthropic} />
      </div>
    </div>
  );
}

function ModelColumn({ model }: { model: MultiModelSummary }) {
  const providerLabel = model.provider === "openai" ? "GPT-4o" : "Claude";
  return (
    <div style={{ background: "var(--bg)", borderRadius: "var(--radius-sm)", padding: "1rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.75rem" }}>
        <strong style={{ fontSize: "0.9rem" }}>{providerLabel}</strong>
        <span style={{ fontSize: "0.72rem", color: "var(--text-muted)" }}>
          {model.durationMs > 0 ? `${(model.durationMs / 1000).toFixed(1)}s` : "—"}
        </span>
      </div>
      {model.error ? (
        <p style={{ fontSize: "0.82rem", color: "var(--critical)" }}>{model.error}</p>
      ) : (
        <>
          <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", marginBottom: "0.5rem" }}>
            {model.findingCount} finding{model.findingCount !== 1 ? "s" : ""}
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
            {model.findings.slice(0, 8).map((f, i) => (
              <div key={i} style={{
                padding: "0.35rem 0.5rem",
                borderRadius: "4px",
                fontSize: "0.75rem",
                background: "var(--surface)",
                border: "1px solid var(--border)",
              }}>
                <span style={{
                  display: "inline-block",
                  width: "6px", height: "6px",
                  borderRadius: "50%",
                  marginRight: "6px",
                  background: f.severity === "CRITICAL" ? "var(--critical)" :
                              f.severity === "HIGH" ? "var(--high)" :
                              f.severity === "MEDIUM" ? "var(--medium)" : "var(--low)",
                }} />
                <strong>{f.title}</strong>
                <span style={{ color: "var(--text-muted)", marginLeft: "0.4rem" }}>{f.filePath}:{f.lineStart}</span>
              </div>
            ))}
            {model.findingCount > 8 && (
              <span style={{ fontSize: "0.72rem", color: "var(--text-muted)" }}>
                +{model.findingCount - 8} more
              </span>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function VerdictButtons({ finding }: { finding: Finding }) {
  const [verdict, setVerdict] = useState<string | null>(finding.userVerdict);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (v: "CONFIRMED" | "DISMISSED") => {
    setSubmitting(true);
    try {
      await api.findings.verdict(finding.id, v);
      setVerdict(v);
    } catch { /* ignore */ }
    finally { setSubmitting(false); }
  };

  if (verdict) {
    return (
      <div style={{ marginTop: "0.4rem", fontSize: "0.72rem" }} onClick={(e) => e.stopPropagation()}>
        <span style={{
          padding: "2px 8px",
          borderRadius: "999px",
          fontWeight: 700,
          background: verdict === "CONFIRMED" ? "#dcfce7" : "#fef2f2",
          color: verdict === "CONFIRMED" ? "#16a34a" : "#dc2626",
        }}>
          {verdict === "CONFIRMED" ? "Confirmed" : "Dismissed"}
        </span>
      </div>
    );
  }

  return (
    <div style={{ marginTop: "0.4rem", display: "flex", gap: "0.4rem" }} onClick={(e) => e.stopPropagation()}>
      <button
        onClick={() => submit("CONFIRMED")}
        disabled={submitting}
        style={{
          padding: "2px 8px",
          fontSize: "0.7rem",
          fontWeight: 600,
          borderRadius: "999px",
          border: "1px solid #bbf7d0",
          background: "#f0fdf4",
          color: "#16a34a",
          cursor: "pointer",
        }}
      >
        Confirm
      </button>
      <button
        onClick={() => submit("DISMISSED")}
        disabled={submitting}
        style={{
          padding: "2px 8px",
          fontSize: "0.7rem",
          fontWeight: 600,
          borderRadius: "999px",
          border: "1px solid #fecaca",
          background: "#fef2f2",
          color: "#dc2626",
          cursor: "pointer",
        }}
      >
        Dismiss
      </button>
    </div>
  );
}
