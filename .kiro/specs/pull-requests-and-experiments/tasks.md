# Implementation Plan: Pull Requests and Experiments

## Overview

This plan implements the two non-functional areas of DevSleuth end-to-end. The backend is Java/Spring Boot with Flyway migrations and PostgreSQL. The frontend is Next.js with TypeScript. Tasks are ordered so that each step builds on the previous: schema first, then entities, services, controllers, frontend types, and finally UI pages.

## Tasks

- [x] 1. Database migrations and entity updates
  - [x] 1.1 Create Flyway migration V8 to alter experiments and experiment_runs tables
    - Add `user_id UUID NOT NULL REFERENCES users(id)` to `experiments`
    - Change `dataset` column to JSONB
    - Add `ground_truth JSONB` column to `experiments`
    - Add `status VARCHAR(20) NOT NULL DEFAULT 'RUNNING'` to `experiment_runs`
    - Add `error_message TEXT` to `experiment_runs`
    - File: `backend/src/main/resources/db/migration/V8__experiments_and_runs_schema.sql`
    - _Requirements: 5.1, 6.3, 6.4, 6.5_

  - [x] 1.2 Update `Experiment` entity to include userId, dataset (JSONB), and groundTruth (JSONB)
    - Add `userId` field with `@Column` mapping
    - Map `dataset` and `groundTruth` as JSONB using a JPA converter or Hibernate Types
    - File: `backend/src/main/java/com/devsleuth/experiment/entity/Experiment.java`
    - _Requirements: 5.1, 5.3_

  - [x] 1.3 Update `ExperimentRun` entity to include status and errorMessage fields
    - Add `status` (String or enum) and `errorMessage` fields
    - Add `startedAt` and `completedAt` Instant fields if not present
    - File: `backend/src/main/java/com/devsleuth/experiment/entity/` (ExperimentRun entity)
    - _Requirements: 6.3, 6.4, 6.5_

- [x] 2. Backend Pull Request service fix and DTOs
  - [x] 2.1 Extract `upsertPullRequest` into a reusable private method in `PullRequestService`
    - Identify the inline upsert logic in `triggerAnalysis`/`handleWebhookPR`
    - Extract to a shared `upsertPullRequest(Repository repo, GitHubPRInfo info)` method
    - _Requirements: 1.2, 10.1_

  - [x] 2.2 Modify `PullRequestService.listByRepository` to fetch from GitHub and upsert
    - Call `gitHubPullRequestService.listOpenPullRequests()` with user's access token
    - Upsert each returned PR, then return from local DB
    - _Requirements: 1.1, 1.2, 10.1_

  - [x] 2.3 Add `PullRequestResponse` DTO with `LatestReviewInfo`
    - Create `PullRequestResponse` record with id, number, title, author, sourceBranch, targetBranch, commitSha, status, latestReview
    - Create `LatestReviewInfo` record with reviewId, status, finalFindingCount
    - File: `backend/src/main/java/com/devsleuth/pullrequest/dto/`
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 2.4 Update `PullRequestController.list` to pass User and return enriched DTOs
    - Inject authenticated user into the service call
    - Query latest review per PR and map to `PullRequestResponse`
    - Return 404 if user is not a member of the repository
    - Handle GitHub API errors with clear error responses
    - _Requirements: 1.1, 1.3, 1.4, 2.1, 2.2, 9.1, 9.2_

  - [x] 2.5 Write property test: PR upsert preserves all fields (Property 1)
    - **Property 1: PR upsert preserves all fields**
    - Use jqwik to generate random `GitHubPRInfo` records, upsert, and verify round-trip
    - **Validates: Requirements 1.2**

- [x] 3. Checkpoint - Verify PR list flow compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Backend Experiment service layer
  - [x] 4.1 Create request/response DTOs for experiments
    - `ExperimentCreateRequest` (name, description, dataset, groundTruth) with Bean Validation
    - `FileChangeDto`, `GroundTruthEntryDto`
    - `ExperimentRunResponse` with metrics
    - File: `backend/src/main/java/com/devsleuth/experiment/` (dto package or model package)
    - _Requirements: 5.1, 5.2, 6.2, 6.3_

  - [x] 4.2 Create `ExperimentService` with create, listByUser, findByIdAndUser
    - Validate ownership scoping (only return user's experiments)
    - Validate required fields, return 400 on invalid input
    - File: `backend/src/main/java/com/devsleuth/experiment/service/ExperimentService.java`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 9.3_

  - [x] 4.3 Create `ExperimentRunService` with startRun, getRunStatus, listRuns
    - Validate experiment ownership before starting run
    - Create run with RUNNING status, delegate to `@Async` method
    - On completion: compute metrics via `ExperimentRunner`, persist, set COMPLETED
    - On failure: set FAILED with error message
    - File: `backend/src/main/java/com/devsleuth/experiment/service/ExperimentRunService.java`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 4.4 Write property test: Invalid experiment input rejected (Property 5)
    - **Property 5: Invalid experiment input rejected**
    - Generate requests with blank name, empty dataset, or empty groundTruth
    - Verify 400 response and no persistence
    - **Validates: Requirements 5.2**

  - [x] 4.5 Write property test: Experiment ownership scoping (Property 6)
    - **Property 6: Experiment ownership scoping**
    - Create experiments for multiple users, verify list isolation
    - **Validates: Requirements 5.4**

- [x] 5. Backend Experiment controller and bug fixes
  - [x] 5.1 Extend `ExperimentController` with CRUD and run endpoints
    - POST `/api/experiments` — create experiment
    - GET `/api/experiments` — list user's experiments
    - GET `/api/experiments/{id}` — get single experiment
    - POST `/api/experiments/{id}/runs` — start a run (body: mode)
    - GET `/api/experiments/{id}/runs` — list runs for experiment
    - GET `/api/experiments/runs/{runId}` — get run status + metrics
    - All endpoints enforce auth and ownership
    - _Requirements: 5.1, 5.4, 6.1, 6.3, 7.1, 9.1, 9.2_

  - [x] 5.2 Fix `ExperimentRunner` SLF4J placeholder bug
    - Replace `{:.3f}` with `{}` in the log statement
    - _Requirements: 10.2_

  - [x] 5.3 Write property test: Metrics computation correctness (Property 7)
    - **Property 7: Metrics computation correctness**
    - Use jqwik to generate random (tp, fp, fn, timeMs) tuples
    - Verify precision = tp/(tp+fp), recall = tp/(tp+fn), f1 = 2*p*r/(p+r)
    - **Validates: Requirements 6.2**

- [x] 6. Checkpoint - Backend compiles and all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Frontend types and API client
  - [x] 7.1 Add TypeScript types for Experiment, ExperimentRun, ExperimentMetrics
    - Add to `frontend/src/types/index.ts`
    - Include `PullRequestResponse` and `LatestReviewInfo` types if not present
    - _Requirements: 2.1, 5.1, 6.2, 8.1_

  - [x] 7.2 Add experiment API client methods to `frontend/src/lib/api.ts`
    - list, get, create, startRun, getRuns, getRunStatus, getResults
    - _Requirements: 5.1, 6.1, 7.1, 8.1_

- [x] 8. Frontend Pull Requests page updates
  - [x] 8.1 Update PR table to show targetBranch and latestReview columns
    - Add target branch column
    - Show latest review status badge and finding count, or "Not analyzed" text
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 8.2 Fix error handling on Pull Requests page
    - Replace silent `catch(() => setPrs([]))` with inline error message display
    - _Requirements: 1.4_

  - [x] 8.3 Write property test: PR row rendering completeness (Property 2)
    - **Property 2: PR row rendering completeness**
    - Verify rendered row contains number, title, author, target branch, status for any valid PR
    - **Validates: Requirements 2.1**

- [x] 9. Frontend Experiments page rewrite
  - [x] 9.1 Implement experiment list view and create form
    - List user's experiments with name and run count
    - Create experiment form: name, dataset (JSON upload/paste), ground truth entries
    - Validate required fields client-side
    - _Requirements: 5.1, 5.2, 7.1_

  - [x] 9.2 Implement experiment detail view with run controls
    - Show dataset summary and ground truth count
    - "Run" button with mode selector (STATIC_ONLY / AI_ONLY / HYBRID)
    - Display error if starting a run fails
    - _Requirements: 6.1, 7.1, 7.3_

  - [x] 9.3 Implement status polling and results comparison view
    - Poll run status at 3s intervals while RUNNING, stop on COMPLETED/FAILED
    - Show metrics comparison: precision, recall, F1, latency per mode (bar chart + table)
    - Show empty state explaining how to run an experiment when no results
    - _Requirements: 7.2, 8.1, 8.2, 8.3_

  - [x] 9.4 Write property test: Results comparison rendering completeness (Property 8)
    - **Property 8: Results comparison rendering completeness**
    - Verify rendered comparison view shows precision, recall, F1, time for any metrics list
    - **Validates: Requirements 8.1**

- [x] 10. Integration tests
  - [x] 10.1 Write integration tests for PR list and analyze endpoints
    - Mock GitHub API, verify fetch → upsert → return cycle
    - Verify 404 for non-member access
    - Verify 401 for unauthenticated access
    - _Requirements: 1.1, 1.3, 3.1, 3.2, 9.1, 9.2_

  - [x] 10.2 Write integration tests for experiment CRUD and run lifecycle
    - Create, list, get experiments with auth
    - Start run → poll RUNNING → COMPLETED with metrics
    - Verify ownership scoping (404 for non-owner)
    - _Requirements: 5.1, 5.4, 6.1, 6.3, 6.4, 9.1, 9.2_

  - [x] 10.3 Write property test: Reviews ordered by recency (Property 3)
    - **Property 3: Reviews ordered by recency**
    - Insert N reviews with random timestamps, verify returned order is descending by createdAt
    - **Validates: Requirements 4.1**

- [x] 11. Final checkpoint - All tests pass, feature complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The SLF4J fix (5.2) is a one-line change but blocks correct logging for experiment runs
- The PR list fix (2.2) is the core defect — wiring `listOpenPullRequests()` into the list flow

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "4.1", "5.2"] },
    { "id": 3, "tasks": ["2.2", "2.3", "4.2"] },
    { "id": 4, "tasks": ["2.4", "2.5", "4.3", "4.4", "4.5"] },
    { "id": 5, "tasks": ["5.1", "5.3"] },
    { "id": 6, "tasks": ["7.1", "7.2"] },
    { "id": 7, "tasks": ["8.1", "8.2", "8.3", "9.1"] },
    { "id": 8, "tasks": ["9.2", "9.3", "9.4"] },
    { "id": 9, "tasks": ["10.1", "10.2", "10.3"] }
  ]
}
```
