package com.devsleuth.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotenvEnvironmentPostProcessorTest {

    @Test
    void parsesKeyValuesAndSkipsNoise() {
        Map<String, Object> result = DotenvEnvironmentPostProcessor.parseLines(List.of(
                "# a comment",
                "",
                "GITHUB_CLIENT_ID=abc123",
                "export GITHUB_CLIENT_SECRET=shh",
                "QUOTED=\"with spaces\"",
                "SINGLE='single'",
                "  SPACED = value ",
                "NOT_A_PAIR",
                "=missingkey"
        ));

        assertEquals("abc123", result.get("GITHUB_CLIENT_ID"));
        assertEquals("shh", result.get("GITHUB_CLIENT_SECRET"), "export prefix should be stripped");
        assertEquals("with spaces", result.get("QUOTED"), "surrounding double quotes stripped");
        assertEquals("single", result.get("SINGLE"), "surrounding single quotes stripped");
        assertEquals("value", result.get("SPACED"), "key/value trimmed");
        assertFalse(result.containsKey("NOT_A_PAIR"), "lines without '=' ignored");
        assertEquals(5, result.size(), "comment, blank, and empty-key lines excluded");
    }

    @Test
    void preservesEqualsInsideValue() {
        Map<String, Object> result = DotenvEnvironmentPostProcessor.parseLines(List.of(
                "DATABASE_URL=jdbc:postgresql://localhost:5432/db?opt=1"
        ));
        assertEquals("jdbc:postgresql://localhost:5432/db?opt=1", result.get("DATABASE_URL"));
    }
}
