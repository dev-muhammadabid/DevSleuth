package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.AiResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser();

    @Test
    void parsesValidJson() {
        String json = """
                {
                  "findings": [
                    {
                      "category": "SECURITY",
                      "severity": "HIGH",
                      "confidence": 0.94,
                      "title": "SQL Injection",
                      "description": "User input concatenated",
                      "recommendation": "Use prepared statements",
                      "filePath": "src/Repo.java",
                      "lineStart": 42,
                      "lineEnd": 42
                    }
                  ]
                }
                """;

        Optional<AiResponse> result = parser.parse(json);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().findings().size());
        assertEquals("SQL Injection", result.get().findings().get(0).title());
        assertEquals(0.94, result.get().findings().get(0).confidence());
    }

    @Test
    void parsesMarkdownWrappedJson() {
        String wrapped = """
                ```json
                {"findings": []}
                ```
                """;

        Optional<AiResponse> result = parser.parse(wrapped);
        assertTrue(result.isPresent());
        assertEquals(0, result.get().findings().size());
    }

    @Test
    void returnsEmptyForGarbage() {
        assertEquals(Optional.empty(), parser.parse("not json at all"));
    }

    @Test
    void returnsEmptyForNull() {
        assertEquals(Optional.empty(), parser.parse(null));
    }

    @Test
    void returnsEmptyForBlank() {
        assertEquals(Optional.empty(), parser.parse("   "));
    }

    @Test
    void parsesEmptyFindings() {
        Optional<AiResponse> result = parser.parse("{\"findings\": []}");
        assertTrue(result.isPresent());
        assertEquals(0, result.get().findings().size());
    }
}
