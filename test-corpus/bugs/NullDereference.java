package corpus.bugs;

import java.util.*;

/**
 * GROUND TRUTH:
 * - Vulnerability: Null Pointer Dereference
 * - Location: line 17
 * - Severity: MEDIUM
 * - Category: BUG
 * - Description: Map.get() may return null, used without null check
 */
public class NullDereference {

    public int getUserAge(Map<String, String> data) {
        String ageStr = data.get("age");
        return Integer.parseInt(ageStr.trim()); // VULNERABLE: ageStr may be null
    }
}
