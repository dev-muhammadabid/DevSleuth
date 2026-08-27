"use client";

import { useEffect, useState } from "react";

interface PullRequest {
  id: string;
  number: number;
  title: string;
  author: string;
  status: string;
}

export default function PullRequestsPage() {
  const [prs, setPrs] = useState<PullRequest[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // TODO: pass repositoryId from context/selection
    fetch("/api/dashboard/pull-requests?repositoryId=placeholder")
      .then((res) => (res.ok ? res.json() : []))
      .then(setPrs)
      .catch(() => setPrs([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading...</p>;

  return (
    <main style={{ padding: "2rem", fontFamily: "system-ui, sans-serif" }}>
      <h1>Pull Requests</h1>
      {prs.length === 0 ? (
        <p>No pull requests found.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Title</th>
              <th>Author</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {prs.map((pr) => (
              <tr key={pr.id}>
                <td>{pr.number}</td>
                <td>
                  <a href={`/pull-requests/${pr.id}`}>{pr.title}</a>
                </td>
                <td>{pr.author}</td>
                <td>{pr.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
