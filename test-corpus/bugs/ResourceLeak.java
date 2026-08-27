package corpus.bugs;

import java.io.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: Resource Leak
 * - Location: lines 15-17
 * - Severity: MEDIUM
 * - Category: BUG
 * - Description: InputStream opened but never closed in exception path
 */
public class ResourceLeak {

    public String readConfig(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path); // VULNERABLE: not in try-with-resources
        byte[] data = fis.readAllBytes();
        return new String(data);
        // fis never closed if readAllBytes throws
    }
}
