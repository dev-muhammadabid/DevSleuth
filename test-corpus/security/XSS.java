package corpus.security;

import jakarta.servlet.http.*;
import java.io.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: Cross-Site Scripting (Reflected XSS)
 * - Location: line 17
 * - Severity: HIGH
 * - Category: SECURITY
 * - Description: User input reflected in response without encoding
 */
public class XSS {

    public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        resp.getWriter().println("<h1>Hello " + name + "</h1>"); // VULNERABLE
    }
}
