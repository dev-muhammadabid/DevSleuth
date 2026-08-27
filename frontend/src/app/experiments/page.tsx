"use client";

import { useState } from "react";
import { api } from "@/lib/api";
import { useApi } from "@/hooks/useApi";
import type { Experiment, ExperimentCreateRequest } from "@/types";

export default function ExperimentsPage() {
  const { data: experiments, loading, error, refetch } = useApi<Experiment[]>(
    () => api.experiments.list(),
    []
  );

  const [showForm, setShowForm] = useState(false);

  return (
    <div className="fade-in">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <h1>Experiments</h1>
        <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New Experiment"}
        </button>
      </div>

      {showForm && (
        <CreateExperimentForm
          onCreated={() => {
            setShowForm(false);
            refetch();
          }}
        />
      )}

      {loading && (
        <div className="loading-center">
          <span className="spinner" />
          <span>Loading experiments…</span>
        </div>
      )}

      {!loading && error && (
        <div className="empty-state" style={{ color: "var(--danger, #e5484d)" }}>
          {error}
        </div>
      )}

      {!loading && !error && experiments && experiments.length === 0 && (
        <div className="empty-state">
          <p style={{ fontWeight: 600, color: "var(--text)" }}>No experiments yet</p>
          <p style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
            Create one to evaluate detection quality across analysis modes.
          </p>
        </div>
      )}

      {!loading && !error && experiments && experiments.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Files</th>
                <th>Ground Truth</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {experiments.map((exp) => (
                <tr key={exp.id}>
                  <td style={{ fontWeight: 600 }}>{exp.name}</td>
                  <td className="muted">{exp.datasetSummary?.fileCount ?? 0} files</td>
                  <td className="muted">{exp.groundTruthCount} entries</td>
                  <td className="muted">{new Date(exp.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function CreateExperimentForm({ onCreated }: { onCreated: () => void }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [datasetJson, setDatasetJson] = useState("");
  const [groundTruthJson, setGroundTruthJson] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validate = (): string | null => {
    if (!name.trim()) return "Name is required.";
    try {
      const dataset = JSON.parse(datasetJson);
      if (!Array.isArray(dataset) || dataset.length === 0) return "Dataset must be a non-empty JSON array.";
    } catch {
      return "Dataset is not valid JSON.";
    }
    try {
      const gt = JSON.parse(groundTruthJson);
      if (!Array.isArray(gt) || gt.length === 0) return "Ground truth must be a non-empty JSON array.";
    } catch {
      return "Ground truth is not valid JSON.";
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      const body: ExperimentCreateRequest = {
        name: name.trim(),
        description: description.trim() || null,
        dataset: JSON.parse(datasetJson),
        groundTruth: JSON.parse(groundTruthJson),
      };
      await api.experiments.create(body);
      onCreated();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create experiment.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginBottom: "2rem", display: "flex", flexDirection: "column", gap: "1rem", maxWidth: "600px" }}>
      <div>
        <label style={{ fontSize: "0.875rem", display: "block", marginBottom: "0.25rem" }}>Name *</label>
        <input
          className="input"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. Baseline security evaluation"
        />
      </div>

      <div>
        <label style={{ fontSize: "0.875rem", display: "block", marginBottom: "0.25rem" }}>Description</label>
        <input
          className="input"
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Optional description"
        />
      </div>

      <div>
        <label style={{ fontSize: "0.875rem", display: "block", marginBottom: "0.25rem" }}>Dataset (JSON array of file changes) *</label>
        <textarea
          className="input"
          rows={5}
          value={datasetJson}
          onChange={(e) => setDatasetJson(e.target.value)}
          placeholder={'[\n  { "filename": "src/main.java", "status": "modified", "patch": "..." }\n]'}
          style={{ fontFamily: "monospace", fontSize: "0.8rem" }}
        />
      </div>

      <div>
        <label style={{ fontSize: "0.875rem", display: "block", marginBottom: "0.25rem" }}>Ground Truth (JSON array of entries) *</label>
        <textarea
          className="input"
          rows={5}
          value={groundTruthJson}
          onChange={(e) => setGroundTruthJson(e.target.value)}
          placeholder={'[\n  { "filePath": "src/main.java", "lineStart": 42, "category": "BUG" }\n]'}
          style={{ fontFamily: "monospace", fontSize: "0.8rem" }}
        />
      </div>

      {error && (
        <div style={{ color: "var(--danger, #e5484d)", fontSize: "0.875rem" }}>{error}</div>
      )}

      <button type="submit" className="btn btn-primary" disabled={submitting} style={{ alignSelf: "flex-start" }}>
        {submitting ? (
          <>
            <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} />
            Creating…
          </>
        ) : (
          "Create Experiment"
        )}
      </button>
    </form>
  );
}
