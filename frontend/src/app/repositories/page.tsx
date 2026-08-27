"use client";

import { useEffect, useState } from "react";

interface Repository {
  id: string;
  githubRepositoryId: number;
  owner: string;
  name: string;
  fullName: string;
  language: string | null;
  connected: boolean;
}

export default function RepositoriesPage() {
  const [repos, setRepos] = useState<Repository[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/repositories", { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then(setRepos)
      .catch(() => setRepos([]))
      .finally(() => setLoading(false));
  }, []);

  const connect = async (id: string) => {
    const res = await fetch(`/api/repositories/${id}/connect`, { method: "POST", credentials: "include" });
    if (res.ok) {
      setRepos((prev) =>
        prev.map((r) => (r.id === id ? { ...r, connected: true } : r))
      );
    }
  };

  const filtered = repos.filter(
    (r) =>
      r.fullName.toLowerCase().includes(search.toLowerCase()) ||
      (r.language && r.language.toLowerCase().includes(search.toLowerCase()))
  );

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Loading repositories…</span>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <h1 style={{ marginBottom: "1rem" }}>GitHub Repositories</h1>

      <input
        type="text"
        className="input"
        placeholder="Search repositories…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ maxWidth: "400px", marginBottom: "1.25rem" }}
      />

      <div style={{ display: "grid", gap: "1rem", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))" }}>
        {filtered.map((repo, i) => (
          <div
            key={repo.id}
            className="card card-hover"
            style={{ animation: "fadeInUp 0.35s var(--ease) both", animationDelay: `${Math.min(i * 0.04, 0.4)}s` }}
          >
            <strong style={{ wordBreak: "break-word" }}>{repo.fullName}</strong>
            <div className="muted" style={{ fontSize: "0.875rem", marginTop: "0.15rem" }}>
              {repo.language || "Unknown language"}
            </div>
            <div style={{ marginTop: "0.85rem" }}>
              {repo.connected ? (
                <span className="badge" style={{ backgroundColor: "var(--success)" }}>
                  <span className="badge-dot" aria-hidden />
                  Connected
                </span>
              ) : (
                <button onClick={() => connect(repo.id)} className="btn btn-primary btn-sm">
                  Analyze Repository
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
      {filtered.length === 0 && (
        <div className="empty-state" style={{ marginTop: "1rem" }}>No repositories found.</div>
      )}
    </div>
  );
}
