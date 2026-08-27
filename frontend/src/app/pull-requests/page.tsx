"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PullRequest, Repository } from "@/types";
import { useApi } from "@/hooks/useApi";

export default function PullRequestsPage() {
  const { data: repos } = useApi<Repository[]>(() => api.repositories.list(), []);
  const [selectedRepo, setSelectedRepo] = useState<string | null>(null);
  const [prs, setPrs] = useState<PullRequest[]>([]);
  const [loading, setLoading] = useState(false);

  const connectedRepos = repos?.filter((r) => r.connected) ?? [];

  useEffect(() => {
    if (!selectedRepo) return;
    setLoading(true);
    api.pullRequests
      .list(selectedRepo)
      .then(setPrs)
      .catch(() => setPrs([]))
      .finally(() => setLoading(false));
  }, [selectedRepo]);

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "1rem" }}>Pull Requests</h1>

      {connectedRepos.length === 0 ? (
        <div className="empty-state">
          No connected repositories. <a href="/repositories">Connect one first</a>.
        </div>
      ) : (
        <div className="row" style={{ marginBottom: "1.25rem" }}>
          <label style={{ fontSize: "0.875rem" }} className="muted">Repository</label>
          <select
            className="select"
            onChange={(e) => setSelectedRepo(e.target.value || null)}
            value={selectedRepo ?? ""}
            style={{ maxWidth: 320, width: "auto" }}
          >
            <option value="">Select…</option>
            {connectedRepos.map((r) => (
              <option key={r.id} value={r.id}>{r.fullName}</option>
            ))}
          </select>
        </div>
      )}

      {loading && (
        <div className="loading-center">
          <span className="spinner" />
          <span>Loading pull requests…</span>
        </div>
      )}

      {!loading && prs.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Title</th>
                <th>Author</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {prs.map((pr) => (
                <tr key={pr.id}>
                  <td className="muted">{pr.number}</td>
                  <td>{pr.title}</td>
                  <td className="muted">{pr.author}</td>
                  <td><span className="badge badge-soft">{pr.status}</span></td>
                  <td>
                    <AnalyzeButton repoId={selectedRepo!} prNumber={pr.number} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && selectedRepo && prs.length === 0 && (
        <div className="empty-state">No pull requests found for this repository.</div>
      )}
    </div>
  );
}

function AnalyzeButton({ repoId, prNumber }: { repoId: string; prNumber: number }) {
  const [state, setState] = useState<"idle" | "loading" | "done">("idle");

  const analyze = async () => {
    setState("loading");
    try {
      const result = await api.pullRequests.analyze(repoId, prNumber);
      setState("done");
      window.location.href = `/reviews/${result.reviewId}`;
    } catch {
      setState("idle");
    }
  };

  return (
    <button onClick={analyze} disabled={state !== "idle"} className="btn btn-primary btn-sm">
      {state === "loading" ? (
        <>
          <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} />
          Analyzing…
        </>
      ) : (
        "Analyze"
      )}
    </button>
  );
}
