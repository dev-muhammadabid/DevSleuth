package corpus.performance;

import java.sql.*;
import java.util.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: N+1 Query Problem
 * - Location: lines 18-20
 * - Severity: MEDIUM
 * - Category: PERFORMANCE
 * - Description: Executing a query inside a loop causes O(n) database calls
 */
public class BadQuery {

    public List<String> getUserEmails(Connection conn, List<Integer> userIds) throws SQLException {
        List<String> emails = new ArrayList<>();
        for (int id : userIds) {
            // VULNERABLE: query per iteration instead of batch
            PreparedStatement ps = conn.prepareStatement("SELECT email FROM users WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) emails.add(rs.getString(1));
        }
        return emails;
    }
}
