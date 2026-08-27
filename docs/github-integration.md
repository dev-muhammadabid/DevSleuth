# GitHub Integration

## Authentication

DevSleuth uses GitHub OAuth (user login) and optionally GitHub App (webhook + installation tokens).

### OAuth Flow
1. Frontend calls `GET /api/auth/github` → gets authorization URL
2. User redirected to GitHub → authorizes → redirected to `/auth/callback?code=...`
3. Backend exchanges code for access token via GitHub Token API
4. User profile fetched, stored in `users` table with access token
5. Session established

### Permissions Requested
- `read:user` — user profile
- `user:email` — email address
- `repo` — repository access (read contents, PRs, write comments)

## Webhook Integration

### Setup
1. Register GitHub App in Developer Settings
2. Set webhook URL: `https://<host>/api/webhooks/github`
3. Generate webhook secret, store in `GITHUB_WEBHOOK_SECRET`
4. Subscribe to `pull_request` events

### Verification
HMAC-SHA256 signature verified on every request before processing.

### Supported Events
- `pull_request` action `opened` — new PR, triggers auto-review
- `pull_request` action `synchronize` — new commits pushed, triggers re-review

## GitHub API Usage

### GitHubService
Low-level REST client (`api.github.com`). Handles auth headers, JSON parsing.

### GitHubRepositoryService
- `listUserRepositories(token)` — fetches user's repos

### GitHubPullRequestService
- `listOpenPullRequests(owner, repo, token)`
- `getPullRequest(owner, repo, number, token)`
- `getPullRequestDiff(owner, repo, number, token)`
- `getPullRequestFiles(owner, repo, number, token)`

### Rate Limits
GitHub API: 5000 req/hour per installation token. Monitor via response headers.
