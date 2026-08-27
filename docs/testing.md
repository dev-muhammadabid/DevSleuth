# DevSleuth Testing Strategy

## Unit Tests

Test pure logic in isolation (no Spring context, no DB):

| Class | Tests |
|-------|-------|
| HybridEngine | Exact dedup, semantic dedup, correlation, ranking, empty input |
| SeverityEngine | Security floors, quality caps, confidence demotion, static=100% |
| AiResponseParser | Valid JSON, markdown-wrapped, garbage, null, empty |
| FindingNormalizer | RawFinding→Entity mapping, empty input |

Run: `mvn test -Dtest="HybridEngineTest,SeverityEngineTest,AiResponseParserTest,FindingNormalizerTest"`

## Test Corpus

Located at `test-corpus/` — 8 vulnerable Java files with documented ground truth.

Categories:
- security/ (SQL injection, XSS, hardcoded password, path traversal)
- bugs/ (null dereference, resource leak)
- performance/ (N+1 query)
- quality/ (dead code)

Each file has `GROUND_TRUTH` Javadoc specifying expected vulnerability, line, severity, category.

## Experiment-Based Testing

Use `ExperimentRunner` to run corpus through each mode and measure precision/recall/F1.
Results persisted and viewable on `/experiments` dashboard.

## Integration Tests (planned)

- Controller → Service → Repository → H2/Testcontainers
- Webhook payload processing end-to-end
- GitHub API mocking via MockRestServiceServer

## E2E Tests (planned)

- Full flow: webhook → review → findings → dashboard
- Cypress or Playwright against running frontend + backend
