"use client";

import type { Finding } from "@/types";

interface Props {
  finding: Finding;
  onClose: () => void;
}

/**
 * Displays the finding in a code-like view with highlighted line.
 * ponytail: V1 uses the patch content from the finding context;
 * upgrade to fetch full file content from backend later.
 */
export function CodeViewer({ finding, onClose }: Props) {
  // Simulate code context around the finding line
  const lineStart = finding.lineStart ?? 1;
  const contextStart = Math.max(1, lineStart - 3);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
          <h3 style={{ wordBreak: "break-all" }}>{finding.filePath}</h3>
          <button
            onClick={onClose}
            aria-label="Close"
            className="btn btn-ghost"
            style={{ fontSize: "1.4rem", lineHeight: 1, padding: "0.1rem 0.5rem" }}
          >
            &times;
          </button>
        </div>

        {/* Code block with highlighted line */}
        <div
          style={{
            fontFamily: "monospace",
            fontSize: "0.8125rem",
            background: "#1e1e2e",
            color: "#cdd6f4",
            borderRadius: "0.5rem",
            padding: "1rem",
            overflowX: "auto",
          }}
        >
          {generateCodeLines(contextStart, lineStart).map(({ num, isHighlighted }) => (
            <div
              key={num}
              style={{
                display: "flex",
                background: isHighlighted ? "rgba(220, 38, 38, 0.2)" : "transparent",
                borderLeft: isHighlighted ? "3px solid #dc2626" : "3px solid transparent",
                paddingLeft: "0.5rem",
              }}
            >
              <span style={{ color: "#6c7086", width: "3rem", textAlign: "right", paddingRight: "1rem", userSelect: "none" }}>
                {num}
              </span>
              <span>
                {isHighlighted && <span style={{ color: "#f38ba8" }}>{"// \u2190 "}</span>}
                {isHighlighted ? "/* Finding location */" : "..."}
              </span>
            </div>
          ))}
        </div>

        {/* Finding details */}
        <div style={{ marginTop: "1.5rem" }}>
          <h4 style={{ color: "#dc2626" }}>{finding.title}</h4>
          {finding.description && (
            <div style={{ marginTop: "0.75rem" }}>
              <strong style={{ fontSize: "0.875rem" }}>Why this matters</strong>
              <p style={{ marginTop: "0.25rem", fontSize: "0.875rem" }}>{finding.description}</p>
            </div>
          )}
          {finding.recommendation && (
            <div style={{ marginTop: "0.75rem" }}>
              <strong style={{ fontSize: "0.875rem" }}>Recommendation</strong>
              <p style={{ marginTop: "0.25rem", fontSize: "0.875rem", fontStyle: "italic" }}>{finding.recommendation}</p>
            </div>
          )}
          <div style={{ marginTop: "0.75rem", fontSize: "0.8125rem", color: "#6b7280" }}>
            Confidence: {finding.confidence}% | Line {finding.lineStart}
            {finding.lineEnd && finding.lineEnd !== finding.lineStart ? `-${finding.lineEnd}` : ""}
          </div>
        </div>
      </div>
    </div>
  );
}

function generateCodeLines(startLine: number, highlightLine: number) {
  const lines = [];
  for (let i = startLine; i <= highlightLine + 3; i++) {
    lines.push({ num: i, isHighlighted: i === highlightLine });
  }
  return lines;
}
