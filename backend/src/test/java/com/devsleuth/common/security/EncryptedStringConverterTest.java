package com.devsleuth.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter enabled = new EncryptedStringConverter("test-key");
    private final EncryptedStringConverter disabled = new EncryptedStringConverter("");

    @Test
    void roundTripsAndTagsCiphertext() {
        String plain = "ghp_exampleToken1234567890";
        String stored = enabled.convertToDatabaseColumn(plain);

        assertNotNull(stored);
        assertTrue(stored.startsWith("enc:"), "ciphertext must be tagged");
        assertFalse(stored.contains(plain), "plaintext must not appear in stored value");
        assertEquals(plain, enabled.convertToEntityAttribute(stored));
    }

    @Test
    void usesRandomIvSoSameInputDiffers() {
        String plain = "same-value";
        assertNotEquals(
                enabled.convertToDatabaseColumn(plain),
                enabled.convertToDatabaseColumn(plain),
                "GCM IV must be random per encryption");
    }

    @Test
    void readsLegacyPlaintextUntouched() {
        // A value written before encryption was enabled has no prefix.
        assertEquals("legacy-plaintext", enabled.convertToEntityAttribute("legacy-plaintext"));
    }

    @Test
    void handlesNulls() {
        assertNull(enabled.convertToDatabaseColumn(null));
        assertNull(enabled.convertToEntityAttribute(null));
    }

    @Test
    void disabledConverterIsPassThrough() {
        assertEquals("secret", disabled.convertToDatabaseColumn("secret"));
        assertEquals("secret", disabled.convertToEntityAttribute("secret"));
    }
}
