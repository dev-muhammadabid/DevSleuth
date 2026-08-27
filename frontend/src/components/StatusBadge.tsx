const colors: Record<string, string> = {
  COMPLETED: "#16a34a",
  FAILED: "#dc2626",
  QUEUED: "#64748b",
  FETCHING: "#2563eb",
  STATIC_ANALYSIS: "#7c3aed",
  AI_ANALYSIS: "#0891b2",
  NORMALIZING: "#ca8a04",
  DEDUPLICATING: "#ea580c",
};

const ANIMATED = new Set([
  "FETCHING",
  "STATIC_ANALYSIS",
  "AI_ANALYSIS",
  "NORMALIZING",
  "DEDUPLICATING",
  "QUEUED",
]);

export function StatusBadge({ status }: { status: string }) {
  const bg = colors[status] || "#64748b";
  const inProgress = ANIMATED.has(status);
  return (
    <span className="badge" style={{ backgroundColor: bg }}>
      {inProgress && (
        <span
          className="badge-dot"
          style={{ animation: "pulseGlow 1.4s ease-in-out infinite" }}
          aria-hidden
        />
      )}
      {status.replace(/_/g, " ")}
    </span>
  );
}
