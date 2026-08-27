import type { ExperimentMetrics } from "@/types";

/**
 * Pure presentational component that renders experiment metrics as a comparison table.
 * Displays precision, recall, F1, and analysis time for each metric entry.
 */
export function ExperimentResultsTable({ metrics }: { metrics: ExperimentMetrics[] }) {
  if (metrics.length === 0) {
    return (
      <div className="empty-state">
        <p>No results yet. Run an experiment to see metrics here.</p>
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Mode</th>
            <th>Precision</th>
            <th>Recall</th>
            <th>F1</th>
            <th>Time (ms)</th>
          </tr>
        </thead>
        <tbody>
          {metrics.map((m) => (
            <tr key={m.runId} data-testid={`metrics-row-${m.runId}`}>
              <td>{m.mode ?? "—"}</td>
              <td>{m.precisionScore.toFixed(3)}</td>
              <td>{m.recallScore.toFixed(3)}</td>
              <td>{m.f1Score.toFixed(3)}</td>
              <td>{m.analysisTimeMs}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
