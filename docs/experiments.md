# Experiments

## Purpose

Compare analysis approaches on the same dataset to measure accuracy and performance.

## Modes

| Mode | Description |
|------|-------------|
| STATIC_ONLY | Only static analyzers (Semgrep, SpotBugs, Checkstyle) |
| AI_ONLY | Only LLM-based analysis |
| HYBRID | Static + AI + deduplication + correlation (default) |

## Metrics

| Metric | Formula |
|--------|---------|
| Precision | TP / (TP + FP) |
| Recall | TP / (TP + FN) |
| F1 | 2 * P * R / (P + R) |
| Analysis time | wall-clock milliseconds |

## Ground Truth

Test corpus at `test-corpus/` with documented vulnerabilities:
- security/ (SQL injection, XSS, hardcoded password, path traversal)
- bugs/ (null dereference, resource leak)
- performance/ (N+1 query)
- quality/ (dead code)

Each file has a `GROUND_TRUTH` Javadoc comment specifying expected vulnerability, line, severity, and category.

## Matching

A finding matches ground truth if:
- Same file (suffix match)
- Same category
- Line within 3 lines of expected

## Running Experiments

Trigger via API with mode parameter:
```
POST /api/repositories/{id}/pull-requests/{number}/analyze?mode=STATIC_ONLY
POST /api/repositories/{id}/pull-requests/{number}/analyze?mode=AI_ONLY
POST /api/repositories/{id}/pull-requests/{number}/analyze?mode=HYBRID
```

Results stored in `experiment_metrics` table and visible on `/experiments` dashboard.
