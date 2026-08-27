// Feature: pull-requests-and-experiments, Property 8: Results comparison rendering completeness
// **Validates: Requirements 8.1**

import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import fc from "fast-check";
import { ExperimentResultsTable } from "./ExperimentResultsTable";
import type { ExperimentMetrics, ExperimentMode } from "@/types";

const modeArb: fc.Arbitrary<ExperimentMode> = fc.constantFrom(
  "STATIC_ONLY",
  "AI_ONLY",
  "HYBRID"
);

const metricsArb: fc.Arbitrary<ExperimentMetrics> = fc.record({
  runId: fc.uuid(),
  mode: modeArb,
  truePositives: fc.nat({ max: 1000 }),
  falsePositives: fc.nat({ max: 1000 }),
  falseNegatives: fc.nat({ max: 1000 }),
  precisionScore: fc.double({ min: 0, max: 1, noNaN: true }),
  recallScore: fc.double({ min: 0, max: 1, noNaN: true }),
  f1Score: fc.double({ min: 0, max: 1, noNaN: true }),
  analysisTimeMs: fc.integer({ min: 1, max: 100_000 }),
});

describe("ExperimentResultsTable — Property 8", () => {
  it("renders precision, recall, F1, and time for every metric entry", () => {
    fc.assert(
      fc.property(
        fc.array(metricsArb, { minLength: 1, maxLength: 10 }),
        (metricsList) => {
          const { unmount } = render(
            <ExperimentResultsTable metrics={metricsList} />
          );

          for (const m of metricsList) {
            const row = screen.getByTestId(`metrics-row-${m.runId}`);
            expect(row).toBeInTheDocument();

            const cells = row.querySelectorAll("td");
            const text = row.textContent ?? "";

            // Precision, recall, F1 rendered as .toFixed(3)
            expect(text).toContain(m.precisionScore.toFixed(3));
            expect(text).toContain(m.recallScore.toFixed(3));
            expect(text).toContain(m.f1Score.toFixed(3));
            // Analysis time rendered as integer
            expect(text).toContain(String(m.analysisTimeMs));
          }

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
