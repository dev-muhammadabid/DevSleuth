# Test Corpus Ground Truth

Evaluation dataset for DevSleuth analyzer accuracy measurement.

## Security

| File | Vulnerability | Line | Severity | Category |
|------|--------------|------|----------|----------|
| SQLInjection.java | SQL Injection via string concat | 18 | CRITICAL | SECURITY |
| XSS.java | Reflected XSS, unescaped output | 17 | HIGH | SECURITY |
| HardcodedPassword.java | Hardcoded DB credentials | 14-15 | HIGH | SECURITY |
| PathTraversal.java | User input in file path | 16 | HIGH | SECURITY |

## Bugs

| File | Vulnerability | Line | Severity | Category |
|------|--------------|------|----------|----------|
| NullDereference.java | NPE from unchecked Map.get() | 17 | MEDIUM | BUG |
| ResourceLeak.java | InputStream not closed | 15-17 | MEDIUM | BUG |

## Performance

| File | Vulnerability | Line | Severity | Category |
|------|--------------|------|----------|----------|
| BadQuery.java | N+1 query in loop | 18-20 | MEDIUM | PERFORMANCE |

## Quality

| File | Vulnerability | Line | Severity | Category |
|------|--------------|------|----------|----------|
| DeadCode.java | Unreachable/unused code | 16,21 | LOW | QUALITY |

## Usage

Run each file through the static analyzers and AI engine.
Compare detected findings against this ground truth to measure:
- True positives (correctly detected)
- False negatives (missed)
- False positives (incorrectly flagged)
- Severity accuracy
