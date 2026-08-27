import type { PullRequest } from "@/types";

/**
 * Renders the data cells for a single PR table row.
 * Extracted for testability (Property 2: PR row rendering completeness).
 */
export function PullRequestRow({ pr }: { pr: PullRequest }) {
  return (
    <>
      <td className="muted">{pr.number}</td>
      <td>{pr.title}</td>
      <td className="muted">{pr.author}</td>
      <td className="muted">{pr.targetBranch}</td>
      <td>
        <span className="badge badge-soft">{pr.status}</span>
      </td>
      <td>
        {pr.latestReview ? (
          <span className="badge badge-soft">
            {pr.latestReview.status}
            {pr.latestReview.finalFindingCount != null &&
              ` · ${pr.latestReview.finalFindingCount} findings`}
          </span>
        ) : (
          <span className="muted">Not analyzed</span>
        )}
      </td>
    </>
  );
}
