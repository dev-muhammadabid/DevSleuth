package corpus.security;

import java.io.*;
import java.nio.file.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: Path Traversal
 * - Location: line 16
 * - Severity: HIGH
 * - Category: SECURITY
 * - Description: User input used directly in file path without sanitization
 */
public class PathTraversal {

    public String readFile(String userFilename) throws IOException {
        Path path = Paths.get("/app/data/" + userFilename); // VULNERABLE: ../../etc/passwd
        return Files.readString(path);
    }
}
