# Static Analysis Engine

## Architecture

```
StaticAnalyzer (interface)
       |
  +----+----+----------+
  |         |          |
Semgrep  SpotBugs  Checkstyle
```

Each adapter: writes temp files → runs CLI → parses output → returns `List<RawFinding>`.

## Analyzers

### Semgrep (primary)
- Fast, pattern-based, 30+ languages
- Config: `--config auto` (community rules)
- Output: JSON → `SemgrepParser`
- Severity mapping: ERROR→HIGH, WARNING→MEDIUM, INFO→LOW

### SpotBugs
- Java bytecode analysis
- Output: XML → `SpotBugsParser`
- Priority mapping: 1→HIGH, 2→MEDIUM, 3→LOW
- Category mapping: SECURITY→SECURITY, PERFORMANCE→PERFORMANCE, else→BUG

### Checkstyle
- Java style/quality checks
- Config: Google checks (`/google_checks.xml`)
- Output: XML → `CheckstyleParser`
- All findings categorized as QUALITY

## StaticAnalysisEngine

Collects all `StaticAnalyzer` beans via Spring DI. Runs each available analyzer, skips unavailable ones. Returns aggregated `List<RawFinding>`.

## Process Isolation

All analyzers execute via `ProcessIsolation`:
- Fixed executable, no shell
- Cleared environment
- Timeout enforcement
- Temp working directory (cleaned after use)
