package com.devsleuth.review.service;

import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.finding.entity.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Hybrid engine: combines static + AI findings via normalize → deduplicate → correlate → rank.
 *
 * Deduplication uses:
 * 1. Exact fingerprint match (same file + line + title)
 * 2. Semantic similarity (same file + nearby lines + same category + similar title keywords)
 *
 * Correlation:
 * - When both static and AI flag the same region, boost confidence and mark HYBRID.
 *
 * Ranking:
 * - Severity first (CRITICAL > HIGH > MEDIUM > LOW > INFO), then confidence descending.
 */
@Service
public class HybridEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridEngine.class);

    private static final int LINE_PROXIMITY = 5;
    private static final int CORRELATION_CONFIDENCE_BOOST = 10;
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.5;

    public List<Finding> process(List<Finding> findings) {
        // Step 1: Assign fingerprints
        for (Finding f : findings) {
            f.setFingerprint(computeFingerprint(f));
        }

        // Step 2: Deduplicate (exact + semantic)
        List<Finding> deduplicated = deduplicate(findings);

        // Step 3: Correlate cross-source
        correlate(deduplicated);

        // Step 4: Rank
        deduplicated.sort(Comparator
                .comparingInt((Finding f) -> f.getSeverity().ordinal())
                .thenComparingInt(f -> -f.getConfidence()));

        log.info("Hybrid engine: {} input → {} final", findings.size(), deduplicated.size());
        return deduplicated;
    }

    // -- Fingerprint --

    /**
     * Stable fingerprint: hash(repository context is implicit via review, file + line + category + normalized title).
     * This identifies recurring findings across reviews of the same PR.
     */
    public static String computeFingerprint(Finding f) {
        String normalizedTitle = normalizeTitle(f.getTitle());
        String raw = f.getFilePath() + ":" + f.getLineStart() + ":" + f.getCategory() + ":" + normalizedTitle;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // -- Deduplication --

    private List<Finding> deduplicate(List<Finding> findings) {
        List<Finding> result = new ArrayList<>();

        for (Finding candidate : findings) {
            Finding match = findMatch(result, candidate);
            if (match != null) {
                merge(match, candidate);
            } else {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Finds a matching finding using:
     * 1. Exact fingerprint match
     * 2. Semantic match: same file + nearby line + same category + similar title
     */
    private Finding findMatch(List<Finding> existing, Finding candidate) {
        for (Finding e : existing) {
            // Exact fingerprint
            if (e.getFingerprint().equals(candidate.getFingerprint())) {
                return e;
            }
            // Semantic similarity
            if (isSemanticallyDuplicate(e, candidate)) {
                return e;
            }
        }
        return null;
    }

    private boolean isSemanticallyDuplicate(Finding a, Finding b) {
        // Must be same file
        if (!a.getFilePath().equals(b.getFilePath())) return false;
        // Must be same category
        if (a.getCategory() != b.getCategory()) return false;
        // Must be nearby lines
        if (!isNearby(a, b)) return false;
        // Title must be semantically similar
        return titleSimilarity(a.getTitle(), b.getTitle()) >= TITLE_SIMILARITY_THRESHOLD;
    }

    /**
     * Simple token-overlap similarity (Jaccard on words).
     * ponytail: upgrade to embedding similarity if false-negative rate is too high.
     */
    private double titleSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0;

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        String normalized = normalizeTitle(text);
        return new HashSet<>(Arrays.asList(normalized.split("\\s+")));
    }

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // -- Correlation --

    private void correlate(List<Finding> findings) {
        Map<String, List<Finding>> byFile = new LinkedHashMap<>();
        for (Finding f : findings) {
            byFile.computeIfAbsent(f.getFilePath(), k -> new ArrayList<>()).add(f);
        }

        for (List<Finding> fileFindings : byFile.values()) {
            for (int i = 0; i < fileFindings.size(); i++) {
                for (int j = i + 1; j < fileFindings.size(); j++) {
                    Finding a = fileFindings.get(i);
                    Finding b = fileFindings.get(j);

                    if (a.getSource() == b.getSource()) continue;
                    if (a.getSource() == FindingSource.HYBRID || b.getSource() == FindingSource.HYBRID) continue;
                    if (!isNearby(a, b)) continue;

                    a.setConfidence(Math.min(100, a.getConfidence() + CORRELATION_CONFIDENCE_BOOST));
                    b.setConfidence(Math.min(100, b.getConfidence() + CORRELATION_CONFIDENCE_BOOST));
                    log.debug("Correlated: {}:{} '{}' ↔ '{}'",
                            a.getFilePath(), a.getLineStart(), a.getTitle(), b.getTitle());
                }
            }
        }
    }

    // -- Helpers --

    private boolean isNearby(Finding a, Finding b) {
        if (a.getLineStart() == null || b.getLineStart() == null) return false;
        return Math.abs(a.getLineStart() - b.getLineStart()) <= LINE_PROXIMITY;
    }

    private void merge(Finding existing, Finding incoming) {
        if (incoming.getSeverity().ordinal() < existing.getSeverity().ordinal()) {
            existing.setSeverity(incoming.getSeverity());
        }
        if (incoming.getConfidence() > existing.getConfidence()) {
            existing.setConfidence(incoming.getConfidence());
        }
        if (existing.getSource() != incoming.getSource()) {
            existing.setSource(FindingSource.HYBRID);
        }
        if (incoming.getRecommendation() != null
                && !incoming.getRecommendation().equals(existing.getRecommendation())) {
            existing.setRecommendation(existing.getRecommendation() + " | " + incoming.getRecommendation());
        }
    }
}
