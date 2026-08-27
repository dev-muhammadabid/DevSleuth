"use client";

import { useApi } from "@/hooks/useApi";

interface ExperimentMetric {
  id: string;
  runId: string;
  truePositives: number;
  falsePositives: number;
  falseNegatives: number;
  precisionScore: number;
  recallScore: number;
  f1Score: number;
  analysisTimeMs: number;
}

export default function ExperimentsPage() {
  const { data: metrics, loading } = useApi<ExperimentMetric[]>(
    () => fetch("/api/experiments/results", { credentials: "include" }).then((r) => r.ok ? r.json() : []),
    []
  );

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading experiment results…</span>
      </div>
    );
  }

  // Group by mode (embedded in runId for now; in a real scenario we'd join with runs)
  // For the research dashboard, show all results in a comparison table
  const results = metrics ?? [];

  // Aggregate by mode — ponytail: V1 shows raw results; add grouping when runs carry mode metadata via API
  const modeLabels = ["STATIC_ONLY", "AI_ONLY", "HYBRID"];

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "1.5rem" }}>Experiments</h1>

      {results.length === 0 ? (
        <div className="empty-state">
          <p style={{ fontWeight: 600, color: "var(--text)" }}>No experiment results yet</p>
          <p style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
            Run experiments using the API: POST /api/repositories/{"{id}"}/pull-requests/{"{number}"}/analyze?mode=STATIC_ONLY
          </p>
        </div>
      ) : (
        <>
          {/* Comparison table */}
          <h2 style={{ marginBottom: "1rem" }}>Results Comparison</h2>
          <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Run</th>
                <th>Precision</th>
                <th>Recall</th>
                <th>F1</th>
                <th>TP</th>
                <th>FP</th>
                <th>FN</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {results.map((m, i) => (
                <tr key={m.id}>
                  <td style={{ fontWeight: 600 }}>Run {i + 1}</td>
                  <td>{pct(m.precisionScore)}</td>
                  <td>{pct(m.recallScore)}</td>
                  <td style={{ fontWeight: 600 }}>{pct(m.f1Score)}</td>
                  <td style={{ color: "#16a34a" }}>{m.truePositives}</td>
                  <td style={{ color: "#dc2626" }}>{m.falsePositives}</td>
                  <td style={{ color: "#ca8a04" }}>{m.falseNegatives}</td>
                  <td>{(m.analysisTimeMs / 1000).toFixed(1)}s</td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>

          {/* Visual bars */}
          <h2 style={{ marginTop: "2rem", marginBottom: "1rem" }}>Metric Comparison</h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", maxWidth: "600px" }}>
            {results.map((m, i) => (
              <div key={m.id}>
                <div style={{ fontSize: "0.875rem", marginBottom: "0.25rem" }}>Run {i + 1}</div>
                <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
                  <MetricBar label="P" value={m.precisionScore} color="#2563eb" />
                  <MetricBar label="R" value={m.recallScore} color="#7c3aed" />
                  <MetricBar label="F1" value={m.f1Score} color="#059669" />
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function MetricBar({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ flex: 1 }}>
      <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>{label}</div>
      <div className="metric-track">
        <div
          className="metric-fill"
          style={{ background: color, width: `${Math.round(value * 100)}%` }}
        />
        <span style={{ position: "absolute", right: "6px", top: "2px", fontSize: "0.7rem", color: "#fff", fontWeight: 600 }}>
          {pct(value)}
        </span>
      </div>
    </div>
  );
}

function pct(v: number): string {
  return `${Math.round(v * 100)}%`;
}
