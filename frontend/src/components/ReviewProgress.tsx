"use client";

import type { Review } from "@/types";

const STEPS = [
  { status: "QUEUED", label: "Queued" },
  { status: "FETCHING", label: "Fetching PR" },
  { status: "STATIC_ANALYSIS", label: "Running static analysis" },
  { status: "AI_ANALYSIS", label: "Running AI analysis" },
  { status: "NORMALIZING", label: "Normalizing findings" },
  { status: "DEDUPLICATING", label: "Deduplicating findings" },
  { status: "COMPLETED", label: "Report ready" },
] as const;

export function ReviewProgress({ review }: { review: Review }) {
  if (review.status === "FAILED") {
    return (
      <div className="alert alert-error" style={{ maxWidth: 520 }}>
        <strong>Analysis failed</strong>
        {review.errorMessage && <p style={{ marginTop: "0.5rem" }}>{review.errorMessage}</p>}
      </div>
    );
  }

  const currentIndex = STEPS.findIndex((s) => s.status === review.status);

  return (
    <div className="card" style={{ maxWidth: 520 }}>
      <div className="row" style={{ marginBottom: "1.1rem" }}>
        <span className="spinner" />
        <h3 style={{ margin: 0 }}>Analyzing pull request…</h3>
      </div>
      <div className="stack" style={{ gap: "0.35rem" }}>
        {STEPS.map((step, i) => {
          const state = i < currentIndex ? "done" : i === currentIndex ? "active" : "pending";
          return (
            <div key={step.status} className="progress-step">
              <span className={`progress-icon ${state}`}>
                {state === "done" ? "\u2713" : state === "active" ? "\u25CF" : "\u25CB"}
              </span>
              <span
                style={{
                  color: i <= currentIndex ? "var(--text)" : "var(--text-subtle)",
                  fontWeight: state === "active" ? 650 : 400,
                }}
              >
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
