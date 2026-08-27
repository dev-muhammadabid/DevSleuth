"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { useApi } from "@/hooks/useApi";
import { api } from "@/lib/api";
import type { Experiment, ExperimentMode, ExperimentRun } from "@/types";

const MODES: ExperimentMode[] = ["STATIC_ONLY", "AI_ONLY", "HYBRID"];

export default function ExperimentDetailPage() {
  const params = useParams<{ id: string }>();
  const { data: experiment, loading, error } = useApi<Experiment>(
    () => api.experiments.get(params.id),
    [params.id]
  );
  const { data: runs, refetch: refetchRuns } = useApi<ExperimentRun[]>(
    () => api.experiments.getRuns(params.id),
    [params.id]
  );

  const [mode, setMode] = useState<ExperimentMode>("HYBRID");
  const [runError, setRunError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  const startRun = async () => {
    setStarting(true);
    setRunError(null);
    try {
      await api.experiments.startRun(params.id, mode);
      refetchRuns();
    } catch (e: unknown) {
      setRunError(e instanceof Error ? e.message : "Failed to start run.");
    } finally {
      setStarting(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading experiment…</span>
      </div>
    );
  }

  if (error || !experiment) {
    return <div className="empty-state">{error ?? "Experiment not found."}</div>;
  }

  const runList = runs ?? [];

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "0.25rem" }}>{experiment.name}</h1>
      {experiment.description && (
        <p style={{ color: "#6b7280", marginBottom: "1rem" }}>{experiment.description}</p>
      )}

      {/* Dataset & ground truth summary */}
      <div style={{ display: "flex", gap: "1.5rem", marginBottom: "1.5rem", flexWrap: "wrap" }}>
        <Stat label="Dataset files" value={experiment.datasetSummary.fileCount} />
        <Stat label="Ground truth entries" value={experiment.groundTruthCount} />
        <Stat label="Runs" value={runList.length} />
      </div>

      {/* Run controls */}
      <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", marginBottom: "1rem", flexWrap: "wrap" }}>
        <select
          className="select"
          value={mode}
          onChange={(e) => setMode(e.target.value as ExperimentMode)}
          style={{ maxWidth: 200 }}
        >
          {MODES.map((m) => (
            <option key={m} value={m}>{m.replace("_", " ")}</option>
          ))}
        </select>
        <button className="btn btn-primary" onClick={startRun} disabled={starting}>
          {starting ? (
            <>
              <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} />
              Starting…
            </>
          ) : (
            "Run Experiment"
          )}
        </button>
      </div>

      {runError && (
        <div className="empty-state" style={{ color: "var(--danger, #e5484d)", marginBottom: "1rem" }}>
          {runError}
        </div>
      )}

      {/* Runs table */}
      {runList.length === 0 ? (
        <div className="empty-state">
          No runs yet. Select a mode and click &quot;Run Experiment&quot; to start.
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Mode</th>
                <th>Status</th>
                <th>Started</th>
                <th>Completed</th>
                <th>Error</th>
              </tr>
            </thead>
            <tbody>
              {runList.map((run, i) => (
                <tr key={run.id}>
                  <td className="muted">{i + 1}</td>
                  <td>{run.mode.replace("_", " ")}</td>
                  <td>
                    <span className={`badge badge-soft ${statusColor(run.status)}`}>
                      {run.status}
                    </span>
                  </td>
                  <td className="muted">{formatDate(run.startedAt)}</td>
                  <td className="muted">{run.completedAt ? formatDate(run.completedAt) : "—"}</td>
                  <td style={{ color: "var(--danger, #e5484d)", fontSize: "0.875rem" }}>
                    {run.errorMessage ?? ""}
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

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <div style={{ fontSize: "0.75rem", color: "#6b7280", textTransform: "uppercase" }}>{label}</div>
      <div style={{ fontSize: "1.5rem", fontWeight: 700 }}>{value}</div>
    </div>
  );
}

function statusColor(status: string): string {
  switch (status) {
    case "COMPLETED": return "badge-success";
    case "FAILED": return "badge-danger";
    case "RUNNING": return "badge-info";
    default: return "";
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
  });
}
