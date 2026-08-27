package corpus.security;

import java.sql.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: Hardcoded credentials
 * - Location: lines 14-15
 * - Severity: HIGH
 * - Category: SECURITY
 * - Description: Database password stored in source code
 */
public class HardcodedPassword {

    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "SuperSecret123!"; // VULNERABLE

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/mydb", DB_USER, DB_PASSWORD);
    }
}
