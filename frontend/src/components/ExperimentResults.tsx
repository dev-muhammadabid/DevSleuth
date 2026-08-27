"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { ExperimentRun } from "@/types";

interface Props {
  experimentId: string;
}

export function ExperimentResults({ experimentId }: Props) {
  const [runs, setRuns] = useState<ExperimentRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRuns = useCallback(async () => {
    try {
      const data = await api.experiments.getRuns(experimentId);
      setRuns(data);
      setError(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load runs");
    } finally {
      setLoading(false);
    }
  }, [experimentId]);

  // Initial fetch
  useEffect(() => {
    fetchRuns();
  }, [fetchRuns]);

  // Poll while any run is RUNNING
  const hasRunning = runs.some((r) => r.status === "RUNNING");

  useEffect(() => {
    if (!hasRunning) return;
    const timer = setInterval(fetchRuns, 3000);
    return () => clearInterval(timer);
  }, [hasRunning, fetchRuns]);

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading results...</span>
      </div>
    );
  }

  if (error) {
    return <p style={{ color: "#dc2626" }}>{error}</p>;
  }

  const completedRuns = runs.filter((r) => r.status === "COMPLETED" && r.metrics);
  const runningRuns = runs.filter((r) => r.status === "RUNNING");
  const failedRuns = runs.filter((r) => r.status === "FAILED");

  if (completedRuns.length === 0 && runningRuns.length === 0 && failedRuns.length === 0) {
    return (
      <div className="empty-state">
        <p style={{ fontWeight: 600, color: "var(--text)" }}>No experiment results yet</p>
        <p style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
          Use the Run button above to start an experiment.
        </p>
      </div>
    );
  }

  return (
    <div className="fade-in">
      {/* Running indicator */}
      {runningRuns.length > 0 && (
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "1rem" }}>
          <span className="spinner" />
          <span style={{ fontSize: "0.875rem" }}>
            {runningRuns.length} run{runningRuns.length > 1 ? "s" : ""} in progress...
          </span>
        </div>
      )}

      {/* Failed runs */}
      {failedRuns.length > 0 && (
        <div style={{ marginBottom: "1rem" }}>
          {failedRuns.map((r) => (
            <div key={r.id} style={{ color: "#dc2626", fontSize: "0.875rem", marginBottom: "0.25rem" }}>
              <span className="badge" style={{ backgroundColor: "#dc2626" }}>FAILED</span>{" "}
              {r.mode.replace(/_/g, " ")} &mdash; {r.errorMessage || "Unknown error"}
            </div>
          ))}
        </div>
      )}

      {/* Comparison table */}
      {completedRuns.length > 0 && (
        <>
          <h3 style={{ marginBottom: "0.75rem" }}>Results Comparison</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Mode</th>
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
                {completedRuns.map((r) => {
                  const m = r.metrics!;
                  return (
                    <tr key={r.id}>
                      <td style={{ fontWeight: 600 }}>{r.mode.replace(/_/g, " ")}</td>
                      <td>{pct(m.precisionScore)}</td>
                      <td>{pct(m.recallScore)}</td>
                      <td style={{ fontWeight: 600 }}>{pct(m.f1Score)}</td>
                      <td style={{ color: "#16a34a" }}>{m.truePositives}</td>
                      <td style={{ color: "#dc2626" }}>{m.falsePositives}</td>
                      <td style={{ color: "#ca8a04" }}>{m.falseNegatives}</td>
                      <td>{(m.analysisTimeMs / 1000).toFixed(1)}s</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Visual bars grouped by mode */}
          <h3 style={{ marginTop: "1.5rem", marginBottom: "0.75rem" }}>Metric Comparison</h3>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", maxWidth: "600px" }}>
            {completedRuns.map((r) => {
              const m = r.metrics!;
              return (
                <div key={r.id}>
                  <div style={{ fontSize: "0.875rem", marginBottom: "0.25rem" }}>
                    {r.mode.replace(/_/g, " ")}
                  </div>
                  <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
                    <MetricBar label="P" value={m.precisionScore} color="#2563eb" />
                    <MetricBar label="R" value={m.recallScore} color="#7c3aed" />
                    <MetricBar label="F1" value={m.f1Score} color="#059669" />
                  </div>
                </div>
              );
            })}
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
