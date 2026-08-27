package corpus.quality;

/**
 * GROUND TRUTH:
 * - Vulnerability: Dead Code / Unreachable Statement
 * - Location: line 16
 * - Severity: LOW
 * - Category: QUALITY
 * - Description: Code after return statement is unreachable
 */
public class DeadCode {

    public int calculate(int x) {
        return x * 2;
        // VULNERABLE: unreachable code
        // System.out.println("Done"); // would not compile, but logic-dead patterns exist
    }

    // More realistic dead code:
    private void unusedMethod() {
        System.out.println("This method is never called");
    }
}
