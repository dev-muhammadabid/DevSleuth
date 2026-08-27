package com.devsleuth.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts sensitive string columns at rest with AES-256-GCM.
 *
 * <p>Applied to columns holding secrets (e.g. the GitHub access token) via
 * {@code @Convert(converter = EncryptedStringConverter.class)}. The key comes from
 * {@code devsleuth.security.encryption-key}; any non-empty string works (it is
 * SHA-256-hashed to a 256-bit key), so operators don't have to produce exact key
 * lengths.
 *
 * <p>Migration-friendly: encrypted values are tagged with an {@code enc:} prefix. On
 * read, untagged values are returned as-is (legacy plaintext), so enabling encryption
 * doesn't break rows written before it. If no key is configured the converter is a
 * no-op (plaintext) and logs a warning — acceptable for local dev, not production.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);
    private static final String PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedStringConverter(@Value("${devsleuth.security.encryption-key:}") String rawKey) {
        this.key = (rawKey == null || rawKey.isBlank()) ? null : deriveKey(rawKey);
        if (this.key == null) {
            log.warn("devsleuth.security.encryption-key is not set — sensitive columns are stored in PLAINTEXT. "
                    + "Set it for any non-local environment.");
        }
    }

    static SecretKey deriveKey(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || key == null) return attribute;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || key == null) return dbData;
        if (!dbData.startsWith(PREFIX)) return dbData; // legacy plaintext, read through
        try {
            byte[] combined = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }
}
