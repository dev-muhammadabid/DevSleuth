import type { Finding } from "@/types";

const colors: Record<Finding["severity"], string> = {
  CRITICAL: "#dc2626",
  HIGH: "#ea580c",
  MEDIUM: "#ca8a04",
  LOW: "#2563eb",
  INFO: "#64748b",
};

export function SeverityBadge({ severity }: { severity: Finding["severity"] }) {
  return (
    <span className="badge" style={{ backgroundColor: colors[severity] }}>
      {severity}
    </span>
  );
}
