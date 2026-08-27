// Feature: pull-requests-and-experiments, Property 2: PR row rendering completeness
import { render } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import * as fc from "fast-check";
import { PullRequestRow } from "./PullRequestRow";
import type { PullRequest } from "@/types";

/**
 * **Validates: Requirements 2.1**
 *
 * Property 2: For any PullRequestResponse with non-null fields, rendering it
 * as a table row SHALL produce output containing the PR number, title, author,
 * target branch, and status.
 */

/** Arbitrary that generates valid PullRequest objects. */
const arbPullRequest: fc.Arbitrary<PullRequest> = fc.record({
  id: fc.uuid(),
  number: fc.integer({ min: 1, max: 99999 }),
  title: fc.string({ minLength: 1, maxLength: 100 }).filter((s) => s.trim().length > 0),
  author: fc.string({ minLength: 1, maxLength: 40 }).filter((s) => s.trim().length > 0),
  sourceBranch: fc.string({ minLength: 1, maxLength: 50 }).filter((s) => s.trim().length > 0),
  targetBranch: fc.string({ minLength: 1, maxLength: 50 }).filter((s) => s.trim().length > 0),
  commitSha: fc.string({ minLength: 40, maxLength: 40 }),
  status: fc.constantFrom("OPEN", "CLOSED", "MERGED"),
  latestReview: fc.oneof(
    fc.constant(null),
    fc.constant(undefined),
    fc.record({
      reviewId: fc.uuid(),
      status: fc.constantFrom("COMPLETED", "FAILED", "RUNNING"),
      finalFindingCount: fc.oneof(fc.constant(null), fc.integer({ min: 0, max: 100 })),
    })
  ),
});

describe("PullRequestRow property test", () => {
  it("rendered row contains number, title, author, target branch, and status for any valid PR", () => {
    fc.assert(
      fc.property(arbPullRequest, (pr) => {
        const { container } = render(
          <table>
            <tbody>
              <tr>
                <PullRequestRow pr={pr} />
              </tr>
            </tbody>
          </table>
        );

        const text = container.textContent ?? "";

        // PR number must appear
        expect(text).toContain(String(pr.number));
        // Title must appear
        expect(text).toContain(pr.title);
        // Author must appear
        expect(text).toContain(pr.author);
        // Target branch must appear
        expect(text).toContain(pr.targetBranch);
        // Status must appear
        expect(text).toContain(pr.status);
      }),
      { numRuns: 100 }
    );
  });
});
