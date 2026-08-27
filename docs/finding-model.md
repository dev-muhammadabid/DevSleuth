# Finding Model

The central contract of DevSleuth. All analyzers (static and AI) produce findings normalized to this format.

## RawFinding (in-memory)

```
source: STATIC | AI
category: BUG | SECURITY | PERFORMANCE | QUALITY
severity: CRITICAL | HIGH | MEDIUM | LOW | INFO
confidence: 0-100
title: short description
description: explanation of why it matters
recommendation: how to fix
filePath: relative path in repository
lineStart: line number
lineEnd: line number
```

## Finding (persisted entity)

Same fields as RawFinding plus:
- `id`: UUID
- `review_id`: FK to the review that produced it
- `fingerprint`: SHA-256 hash for dedup (file:line:category:normalizedTitle)

## Pipeline

1. **Normalization**: `FindingNormalizer` converts `RawFinding` → `Finding` entity
2. **Severity Policy**: `SeverityEngine` adjusts severity based on documented rules
3. **Deduplication**: `HybridEngine` merges duplicates by fingerprint + semantic similarity
4. **Correlation**: Cross-source findings on nearby lines get confidence boost
5. **Ranking**: Sorted by severity (critical first), then confidence descending

## Fingerprint

```
SHA-256(filePath + ":" + lineStart + ":" + category + ":" + normalizedTitle)
```

Truncated to 16 hex chars. Used for:
- Dedup within a review
- Identifying recurring findings across reviews (comparison feature)

## Severity Policy

| Condition | Floor |
|-----------|-------|
| SECURITY + exploitable keyword | HIGH |
| SECURITY general | MEDIUM |
| BUG + null/leak/deadlock keyword | MEDIUM |
| PERFORMANCE (AI source, above MEDIUM) | capped at MEDIUM |
| QUALITY | capped at LOW |
| AI confidence < 60% | demote one level |
| STATIC source | confidence forced to 100% |
