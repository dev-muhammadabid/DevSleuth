# Requirements Document

## Introduction

DevSleuth lets a signed-in user connect GitHub repositories and run hybrid (static + AI) code reviews on pull requests. Two areas are currently non-functional:

- The **Pull Requests** page never shows pull requests. The list endpoint reads only the local database (empty for a freshly connected repo) and never calls the existing-but-unused GitHub fetch method.
- The **Experiments** feature is unfinished: there is no way to define, trigger, or persist an experiment, the `ExperimentRunner` is never invoked, and `/api/experiments/results` always returns an empty list.

This spec makes both areas work end to end and adds the supporting features needed to make them useful. All data access remains scoped to the signed-in user's repository memberships, consistent with the existing access model.

## Requirements

### Requirement 1: List a repository's open pull requests from GitHub

**User Story:** As a signed-in user, I want to see the open pull requests for a repository I have connected, so that I can pick one to review.

#### Acceptance Criteria
1. WHEN a user selects a connected repository on the Pull Requests page THEN the system SHALL fetch that repository's open pull requests from GitHub and display them.
2. WHEN the system fetches pull requests THEN it SHALL upsert each pull request (number, title, author, source branch, target branch, head commit SHA, state) into the local database.
3. IF the selected repository is not one the requesting user is a member of THEN the system SHALL respond with HTTP 404 and SHALL NOT return any pull requests.
4. IF the GitHub API call fails or the access token is invalid THEN the system SHALL return a clear, actionable error and the UI SHALL display it without crashing.
5. WHEN a repository has no open pull requests THEN the UI SHALL show an empty state rather than an error.
6. WHILE pull requests are being fetched THE UI SHALL show a loading indicator.

### Requirement 2: View pull request details

**User Story:** As a user, I want to see meaningful details for each pull request, so that I can decide what to review.

#### Acceptance Criteria
1. WHEN pull requests are displayed THEN each row SHALL show the PR number, title, author, target branch, and state.
2. WHEN a pull request has one or more reviews THEN the row SHALL show the most recent review's status and final finding count.
3. WHEN a pull request has no reviews THEN the row SHALL indicate that it has not been analyzed yet.

### Requirement 3: Trigger analysis on a pull request

**User Story:** As a user, I want to start a review on a chosen pull request, so that I receive findings.

#### Acceptance Criteria
1. WHEN a user clicks Analyze on a pull request THEN the system SHALL create a QUEUED review and start asynchronous analysis.
2. IF the requesting user is not a member of the pull request's repository THEN the system SHALL respond with HTTP 404 and SHALL NOT start analysis.
3. WHILE a review is in progress THE UI SHALL reflect an in-progress state for that pull request.
4. WHEN a review has been created THEN the UI SHALL provide a link to the review detail page.
5. IF analysis cannot be started (e.g. no usable access token) THEN the system SHALL return a clear error and SHALL NOT leave a review stuck in QUEUED without an error message.

### Requirement 4: See review history for a pull request

**User Story:** As a user, I want to see prior reviews for a pull request, so that I can track progress and compare runs.

#### Acceptance Criteria
1. WHEN a user views a pull request's reviews THEN the system SHALL return that PR's reviews ordered from most to least recent, scoped to the user's membership.
2. WHEN reviews are listed THEN each SHALL show status, creation time, and final finding count.
3. WHEN two or more completed reviews exist for a pull request THEN the UI SHALL allow comparing two of them.

### Requirement 5: Define an experiment

**User Story:** As a researcher, I want to define an experiment with a dataset and ground truth, so that I can evaluate detection quality.

#### Acceptance Criteria
1. WHEN a user creates an experiment THEN the system SHALL persist its name, dataset (a set of file changes), and ground-truth entries (file path, line, category).
2. IF required fields are missing or malformed THEN the system SHALL reject the request with HTTP 400 and a validation message.
3. WHEN an experiment is created THEN it SHALL be associated with the creating user.
4. WHEN a user lists experiments THEN the system SHALL return only experiments they own.

### Requirement 6: Run an experiment across analysis modes

**User Story:** As a researcher, I want to run an experiment in STATIC_ONLY, AI_ONLY, and HYBRID modes, so that I can compare their effectiveness.

#### Acceptance Criteria
1. WHEN a user runs an experiment in a given mode THEN the system SHALL execute the analysis pipeline for that mode over the experiment's dataset.
2. WHEN a run completes THEN the system SHALL compute true positives, false positives, false negatives, precision, recall, F1, and elapsed time by comparing findings against ground truth.
3. WHEN a run completes THEN the system SHALL persist an experiment run and its metrics, linked to the experiment and mode.
4. WHILE a run is executing THE system SHALL record a status (e.g. RUNNING, COMPLETED, FAILED) that the UI can poll.
5. IF a run fails THEN the system SHALL persist a FAILED status with an error message rather than silently discarding the run.

### Requirement 7: Trigger and monitor experiment runs from the UI

**User Story:** As a user, I want to start experiment runs and watch their progress in the UI, so that I do not have to call the API manually.

#### Acceptance Criteria
1. WHEN a user opens the Experiments page THEN the UI SHALL let them start a run for a chosen mode.
2. WHILE a run is in progress THE UI SHALL show its status and update without a full page reload.
3. IF starting a run fails THEN the UI SHALL display the error message.

### Requirement 8: Compare modes and view results

**User Story:** As a user, I want to compare metrics across modes, so that I can judge which configuration performs best.

#### Acceptance Criteria
1. WHEN experiment results exist THEN the UI SHALL present precision, recall, F1, and latency per mode in a comparison view.
2. WHEN results include multiple modes THEN the UI SHALL visually compare them (e.g. bars per metric).
3. WHEN no results exist THEN the UI SHALL show an empty state that explains how to run an experiment.

### Requirement 9: Authorization, validation, and resilience

**User Story:** As the system owner, I want the new endpoints to be secure and robust, so that they do not weaken the application.

#### Acceptance Criteria
1. WHEN any new endpoint is called without an authenticated session THEN the system SHALL respond with HTTP 401.
2. WHEN any resource is accessed THEN the system SHALL scope it to the requesting user's repository membership or experiment ownership.
3. WHEN input is received at a trust boundary THEN the system SHALL validate it before use.
4. IF an external call (GitHub or AI provider) fails THEN the system SHALL degrade gracefully with a clear error and SHALL NOT crash the request thread.

### Requirement 10: Fix existing defects uncovered during this work

**User Story:** As a maintainer, I want the latent defects in these flows fixed, so that the features behave correctly.

#### Acceptance Criteria
1. WHEN the Pull Requests list endpoint is called THEN it SHALL return GitHub-sourced pull requests (fixing the current behavior of returning an empty local list).
2. WHEN the experiment runner logs metrics THEN it SHALL use valid SLF4J placeholders so that logging does not malfunction or drop values.

## Glossary

- **Connected repository:** a GitHub repository a user has synced and enabled for analysis; access is governed by repository membership.
- **Pull request (PR):** a GitHub change proposal identified by its number within a repository.
- **Review:** one analysis run over a PR, producing findings; has a status (QUEUED, in-progress states, COMPLETED, FAILED).
- **Finding:** a single issue reported by analysis (file, line, category, severity, source).
- **Analysis mode:** STATIC_ONLY (static analyzers only), AI_ONLY (LLM only), or HYBRID (both, merged).
- **Experiment:** a named, user-owned evaluation consisting of a dataset and ground truth.
- **Dataset:** the set of file changes fed to the pipeline during an experiment.
- **Ground truth:** the known-correct findings (file, line, category) used to score a run.
- **Experiment run:** one execution of an experiment in a specific mode, with a status and metrics.
- **Metrics:** TP/FP/FN counts plus precision, recall, F1, and elapsed time for a run.
- **Membership:** the relationship linking a user to a repository they can access.
