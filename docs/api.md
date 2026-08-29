# DevSleuth API Documentation

Base URL: `http://localhost:8080`

All endpoints require session authentication unless noted. Unauthenticated requests return `401`.

---

## Authentication

### GET /api/auth/github

Returns the GitHub OAuth authorization URL. Frontend redirects the user here.

**Auth required:** No

**Response 200:**
```json
{ "url": "https://github.com/login/oauth/authorize?client_id=...&scope=read:user,user:email,repo" }
```

---

### GET /api/auth/github/callback?code={code}

Exchanges GitHub OAuth code for access token, creates/updates user, sets session.

**Auth required:** No

**Query params:** `code` (required) — OAuth authorization code from GitHub

**Response 200:**
```json
{ "id": "uuid", "username": "octocat", "email": "octocat@github.com", "avatarUrl": "https://..." }
```

**Errors:** `401` — invalid or expired code

---

### GET /api/auth/me

Returns the currently authenticated user.

**Response 200:**
```json
{ "id": "uuid", "username": "octocat", "email": "octocat@github.com", "avatarUrl": "https://..." }
```

**Errors:** `401` — not authenticated

---

### POST /api/auth/logout

Invalidates the current session.

**Response 200:** empty

---

## Repositories

### GET /api/repositories

Lists GitHub repositories for the authenticated user. Syncs from GitHub on each call.

**Response 200:**
```json
[
  {
    "id": "uuid",
    "githubRepositoryId": 123456,
    "owner": "octocat",
    "name": "my-project",
    "fullName": "octocat/my-project",
    "defaultBranch": "main",
    "language": "Java",
    "connected": false
  }
]
```

---

### POST /api/repositories/{id}/connect

Marks a repository for analysis.

**Response 200:**
```json
{ "id": "uuid", "fullName": "octocat/my-project", "connected": true, "..." : "..." }
```

**Errors:** `404` — repository not found

---

## Pull Requests

### GET /api/repositories/{repoId}/pull-requests

Lists pull requests for a connected repository.

**Response 200:**
```json
[
  {
    "id": "uuid",
    "number": 142,
    "title": "Add payment API",
    "author": "octocat",
    "sourceBranch": "feature/payments",
    "targetBranch": "main",
    "commitSha": "abc1234",
    "status": "OPEN"
  }
]
```

---

### POST /api/repositories/{repoId}/pull-requests/{number}/analyze

Triggers a code review for a pull request.

**Query params:** `mode` (optional) — `HYBRID` (default), `STATIC_ONLY`, or `AI_ONLY`

**Request body:** none

**Response 200:**
```json
{ "reviewId": "uuid", "status": "QUEUED" }
```

**Errors:** `401` — not authenticated, `404` — repository not found

---

## Reviews

### GET /api/reviews/{id}

Returns review details. Poll this while status is not COMPLETED/FAILED.

**Response 200:**
```json
{
  "id": "uuid",
  "pullRequestId": "uuid",
  "commitSha": "abc1234",
  "status": "COMPLETED",
  "startedAt": "2024-01-15T10:00:00Z",
  "completedAt": "2024-01-15T10:00:38Z",
  "durationMs": 38200,
  "staticFindingCount": 5,
  "aiFindingCount": 3,
  "finalFindingCount": 6,
  "errorMessage": null,
  "createdAt": "2024-01-15T09:59:58Z"
}
```

**Status values:** `QUEUED`, `FETCHING`, `STATIC_ANALYSIS`, `AI_ANALYSIS`, `NORMALIZING`, `DEDUPLICATING`, `COMPLETED`, `FAILED`

**Errors:** `404` — review not found

---

### GET /api/reviews/{id}/findings

Returns all findings for a review.

**Response 200:**
```json
[
  {
    "id": "uuid",
    "source": "HYBRID",
    "category": "SECURITY",
    "severity": "HIGH",
    "confidence": 94,
    "title": "Potential SQL injection",
    "description": "User-controlled input is concatenated into SQL query",
    "recommendation": "Use parameterized queries",
    "filePath": "src/UserRepository.java",
    "lineStart": 42,
    "lineEnd": 42
  }
]
```

---

### GET /api/reviews/{baseId}/compare/{compareId}

Compares two reviews by fingerprint. Shows new, resolved, and remaining findings.

**Response 200:**
```json
{
  "baseReviewId": "uuid",
  "compareReviewId": "uuid",
  "newFindings": [...],
  "resolvedFindings": [...],
  "remainingFindings": [...]
}
```

---

## Findings

### GET /api/findings/{id}

Returns a single finding by ID.

**Response 200:** same shape as items in review findings list

**Errors:** `404` — finding not found

---

## Dashboard

### GET /api/dashboard/summary

Returns aggregate stats and recent reviews.

**Response 200:**
```json
{
  "totalReviews": 127,
  "totalFindings": 342,
  "highRiskFindings": 18,
  "recentReviews": [
    {
      "reviewId": "uuid",
      "prNumber": 142,
      "prTitle": "Add payment API",
      "repoFullName": "octocat/my-project",
      "status": "COMPLETED",
      "findingCount": 3,
      "createdAt": "2024-01-15T09:59:58Z"
    }
  ]
}
```

---

## Webhooks

### POST /api/webhooks/github

**Auth required:** No (verified via HMAC signature)

Receives GitHub webhook events. Returns 200 immediately; processing is async.

**Headers:**
- `X-GitHub-Event` — event type (e.g. `pull_request`)
- `X-Hub-Signature-256` — HMAC-SHA256 signature

**Supported events:** `pull_request` (actions: `opened`, `synchronize`)

**Response 200:** empty

**Errors:** `401` — invalid signature, `429` — rate limited

---

## Experiments

### GET /api/experiments/results

Returns the authenticated user's experiment metrics (scoped to the caller; requires an active session).

**Response 200:**
```json
[
  {
    "runId": "uuid",
    "truePositives": 7,
    "falsePositives": 2,
    "falseNegatives": 1,
    "precisionScore": 0.778,
    "recallScore": 0.875,
    "f1Score": 0.824,
    "analysisTimeMs": 12500
  }
]
```

---

## Common Error Responses

```json
{ "error": "message", "status": 401, "timestamp": "2024-01-15T10:00:00Z" }
```

| Status | Meaning |
|--------|---------|
| 401 | Not authenticated or invalid credentials |
| 404 | Resource not found |
| 429 | Rate limit exceeded |
| 500 | Internal server error |
