package corpus.security;

import java.sql.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: SQL Injection
 * - Location: line 18
 * - Severity: CRITICAL
 * - Category: SECURITY
 * - Description: User input directly concatenated into SQL query
 */
public class SQLInjection {

    public ResultSet findUser(Connection conn, String userInput) throws SQLException {
        // VULNERABLE: string concatenation with user input
        String query = "SELECT * FROM users WHERE name = '" + userInput + "'";
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }
}
