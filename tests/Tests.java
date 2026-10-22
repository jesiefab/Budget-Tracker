/**
 * Verification harness — shipped-code checks only (Desktop/*.java).
 *
 * The twin suite over the retired refactored core (src/main/java) was removed
 * together with that tree (tasks.md T4): it never ran on a packaged artifact,
 * and its semantics (XP curve, proration math, money discipline, escaping) are
 * either locked on shipped code by DesktopTests or re-landed tests-first in the
 * new app/ stack (T10/T11). The 24 -> 12 check-count change is by design, not a
 * weakening of any shipped-code assertion.
 *
 * Run with tests/run-tests.sh or tests/run-tests.bat.
 * Exit code 0 = all green; 1 = any failure.
 */
public class Tests {

    public static void main(String[] args) {
        System.out.println("=== Budget Tracker verification harness ===\n");

        DesktopTests.runAll();
        if (DesktopTests.failed > 0) {
            System.out.println("\n(Shipped-code checks failed - see [FAIL] lines above.)");
        }

        System.out.println("\n==============================");
        System.out.println("PASSED: " + DesktopTests.passed + "  FAILED: " + DesktopTests.failed);
        System.out.println("==============================");

        if (DesktopTests.failed > 0) {
            System.exit(1);
        }
    }
}
