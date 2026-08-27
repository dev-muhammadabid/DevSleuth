"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

interface Finding {
  id: string;
  source: string;
  category: string;
  severity: string;
  confidence: number;
  title: string;
  description: string;
  recommendation: string;
  filePath: string;
  lineStart: number;
  lineEnd: number;
}

interface Review {
  id: string;
  commitSha: string;
  status: string;
  finalFindingCount: number;
}

const severityColor: Record<string, string> = {
  CRITICAL: "#dc2626",
  HIGH: "#ea580c",
  MEDIUM: "#ca8a04",
  LOW: "#2563eb",
  INFO: "#6b7280",
};

export default function PRDetailPage() {
  const params = useParams<{ id: string }>();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [selectedReview, setSelectedReview] = useState<string | null>(null);

  useEffect(() => {
    fetch(`/api/dashboard/pull-requests/${params.id}/reviews`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Review[]) => {
        setReviews(data);
        if (data.length > 0) setSelectedReview(data[0].id);
      });
  }, [params.id]);

  useEffect(() => {
    if (!selectedReview) return;
    fetch(`/api/reviews/${selectedReview}/findings`)
      .then((res) => (res.ok ? res.json() : []))
      .then(setFindings);
  }, [selectedReview]);

  return (
    <main style={{ padding: "2rem", fontFamily: "system-ui, sans-serif" }}>
      <h1>PR Review</h1>

      <section>
        <h2>Reviews</h2>
        {reviews.map((r) => (
          <button
            key={r.id}
            onClick={() => setSelectedReview(r.id)}
            style={{
              marginRight: "0.5rem",
              fontWeight: selectedReview === r.id ? "bold" : "normal",
            }}
          >
            {r.commitSha.slice(0, 7)} ({r.status}) — {r.finalFindingCount}{" "}
            findings
          </button>
        ))}
      </section>

      <section style={{ marginTop: "1rem" }}>
        <h2>Findings</h2>
        {findings.length === 0 ? (
          <p>No findings.</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0 }}>
            {findings.map((f) => (
              <li
                key={f.id}
                style={{
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.5rem",
                  padding: "1rem",
                  marginBottom: "0.75rem",
                }}
              >
                <strong style={{ color: severityColor[f.severity] || "#000" }}>
                  [{f.severity}]
                </strong>{" "}
                <span>{f.title}</span>
                <div style={{ fontSize: "0.875rem", color: "#6b7280" }}>
                  {f.filePath}:{f.lineStart} | {f.category} | {f.source} |
                  Confidence: {f.confidence}%
                </div>
                {f.description && <p>{f.description}</p>}
                {f.recommendation && (
                  <p style={{ fontStyle: "italic" }}>{f.recommendation}</p>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
