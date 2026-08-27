package com.devsleuth.review.service;

import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.finding.entity.Finding;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class DeduplicationService {

    /**
     * Deduplicates findings by fingerprint.
     * If two findings share the same fingerprint (same file + line + title), merge them:
     * - Keep the highest severity
     * - Mark source as HYBRID
     * - Keep the higher confidence
     */
    public List<Finding> deduplicate(List<Finding> findings) {
        Map<String, Finding> seen = new LinkedHashMap<>();

        for (Finding f : findings) {
            String fp = computeFingerprint(f);
            f.setFingerprint(fp);

            if (seen.containsKey(fp)) {
                Finding existing = seen.get(fp);
                // Merge: keep higher severity
                if (f.getSeverity().ordinal() < existing.getSeverity().ordinal()) {
                    existing.setSeverity(f.getSeverity());
                }
                // Merge: keep higher confidence
                if (f.getConfidence() > existing.getConfidence()) {
                    existing.setConfidence(f.getConfidence());
                }
                existing.setSource(FindingSource.HYBRID);
            } else {
                seen.put(fp, f);
            }
        }
        return new ArrayList<>(seen.values());
    }

    private String computeFingerprint(Finding f) {
        String raw = f.getFilePath() + ":" + f.getLineStart() + ":" + f.getTitle();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // ponytail: SHA-256 is always available in standard JVMs
            throw new RuntimeException(e);
        }
    }
}
