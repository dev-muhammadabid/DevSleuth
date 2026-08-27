"use client";

import type { Finding } from "@/types";

export interface Filters {
  severity: Finding["severity"][];
  category: Finding["category"][];
  source: Finding["source"][];
  file: string | null;
  minConfidence: number;
}

interface Props {
  filters: Filters;
  onChange: (f: Filters) => void;
  findings: Finding[];
}

export function FindingFilters({ filters, onChange, findings }: Props) {
  const files = Array.from(new Set(findings.map((f) => f.filePath))).sort();

  const toggle = <T extends string>(list: T[], value: T): T[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value];

  return (
    <div style={{ fontSize: "0.875rem" }}>
      <h4 style={{ marginBottom: "0.5rem" }}>Severity</h4>
      {(["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"] as const).map((s) => (
        <label key={s} style={{ display: "block", marginBottom: "0.25rem", cursor: "pointer" }}>
          <input
            type="checkbox"
            checked={filters.severity.includes(s)}
            onChange={() => onChange({ ...filters, severity: toggle(filters.severity, s) })}
            style={{ marginRight: "0.5rem" }}
          />
          {s}
        </label>
      ))}

      <h4 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>Category</h4>
      {(["SECURITY", "BUG", "PERFORMANCE", "QUALITY"] as const).map((c) => (
        <label key={c} style={{ display: "block", marginBottom: "0.25rem", cursor: "pointer" }}>
          <input
            type="checkbox"
            checked={filters.category.includes(c)}
            onChange={() => onChange({ ...filters, category: toggle(filters.category, c) })}
            style={{ marginRight: "0.5rem" }}
          />
          {c}
        </label>
      ))}

      <h4 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>Source</h4>
      {(["STATIC", "AI", "HYBRID"] as const).map((s) => (
        <label key={s} style={{ display: "block", marginBottom: "0.25rem", cursor: "pointer" }}>
          <input
            type="checkbox"
            checked={filters.source.includes(s)}
            onChange={() => onChange({ ...filters, source: toggle(filters.source, s) })}
            style={{ marginRight: "0.5rem" }}
          />
          {s}
        </label>
      ))}

      <h4 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>File</h4>
      <select
        value={filters.file ?? ""}
        onChange={(e) => onChange({ ...filters, file: e.target.value || null })}
        style={{ width: "100%", padding: "0.25rem", border: "1px solid #d1d5db", borderRadius: "0.25rem" }}
      >
        <option value="">All files</option>
        {files.map((f) => (
          <option key={f} value={f}>{f.split("/").pop()}</option>
        ))}
      </select>

      <h4 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>Min Confidence</h4>
      <input
        type="range"
        min={0}
        max={100}
        value={filters.minConfidence}
        onChange={(e) => onChange({ ...filters, minConfidence: Number(e.target.value) })}
        style={{ width: "100%" }}
      />
      <span>{filters.minConfidence}%</span>
    </div>
  );
}
