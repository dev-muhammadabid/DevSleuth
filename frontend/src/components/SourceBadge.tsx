import type { Finding } from "@/types";

const colors: Record<Finding["source"], string> = {
  STATIC: "#7c3aed",
  AI: "#0891b2",
  HYBRID: "#059669",
};

export function SourceBadge({ source }: { source: Finding["source"] }) {
  return (
    <span className="badge" style={{ backgroundColor: colors[source] }}>
      {source}
    </span>
  );
}
