import java.nio.file.*;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Verification harness for the SHIPPED desktop code (Desktop/*.java).
 *
 * Lives in the default package so plain class names resolve to the compiled
 * Desktop classes, not the src/ twin (which is imported by name in Tests.java).
 * The twin's tested fixes are ported here line-for-line where they apply, plus
 * checks for the shipped save/load behavior itself.
 */
public class DesktopTests {

    static int passed = 0;
    static int failed = 0;

    static void runAll() {
        System.out.println("\n--- Shipped code (Desktop/*.java) ---");

        check("Proration uses real month/year lengths",      DesktopTests::testDaysInPeriodRealLengths);
        check("XP-derived level (saved LEVEL ignored)",      DesktopTests::testXPDerivedLevel);
        check("Level derived matches awardXP entire path",   DesktopTests::testLevelConsistency);
        check("Deposit/withdraw XP invariant",               DesktopTests::testAwardWithdrawXP);
        check("Save/load round trip incl. tricky escapes",   DesktopTests::testSaveLoadRoundTripEscapes);
        check("Atomic save leaves no temp files",            DesktopTests::testNoTempLeftovers);
        check("Backup = previous save (rotated after write)",DesktopTests::testBackupRotationAfterWrite);
        check("Corrupt save falls back to .bak",             DesktopTests::testCorruptSaveFallsBackToBak);
        check("Missing save falls back to .bak",             DesktopTests::testMissingSaveFallsBackToBak);
        check("Both corrupt -> fresh start, no crash",       DesktopTests::testBothCorruptStartsFresh);
        check("Legacy v3 save loads (needWant default Need)",DesktopTests::testLegacyLoad);
        check("Theme restored from save file",               DesktopTests::testThemeRestore);

        System.out.println("Shipped subtotal: PASS " + passed + "  FAIL " + failed);
    }

    /** One [PASS]/[FAIL] line per test; an exception counts as that test failing. */
    private interface Check { boolean run() throws Exception; }

    private static void check(String name, Check c) {
        boolean ok;
        try {
            ok = c.run();
        } catch (Exception e) {
            System.out.println("[FAIL] " + name + " (exception: " + e + ")");
            failed++;
            return;
        }
        if (ok) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private static DataStore store() { return DataStore.getInstance(); }

    private static void resetStore() {
        DataStore s = store();
        s.adminResetExpenses();
        s.adminResetSavings();
        s.adminResetXP();
        s.adminResetBudget();
    }

    private static Path newTestDir() throws Exception {
        Path dir = Files.createTempDirectory("bt-desktop-tests"); // fresh unique dir each call
        SaveManager.setSaveDirectory(dir);
        return dir;
    }

    private static void rmDir(Path dir) throws Exception {
        if (dir == null || !Files.exists(dir)) return;
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (Exception ignored) {}
            });
        }
    }

    private static String readFile(Path p) throws Exception {
        return new String(Files.readAllBytes(p), "UTF-8");
    }

    /** Parses the first EXPENSE_N=value line with the given index. */
    private static String lineContaining(String content, String needle) {
        for (String line : content.split("\n"))
            if (line.startsWith(needle)) return line;
        return null;
    }

    private static boolean sameExpense(DataStore s, int index, String desc, double amt, String cat, String tag) {
        List<Expense> xs = s.getExpenses();
        if (index >= xs.size()) return false;
        Expense e = xs.get(index);
        return e.getDescription().equals(desc)
                && e.getAmount() == amt
                && e.getCategory().equals(cat)
                && e.getNeedWant().equals(tag);
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    private static boolean testDaysInPeriodRealLengths() {
        boolean ok = true;
        LocalDate now = LocalDate.now();

        ok &= DataStore.BudgetPeriod.WEEKLY.daysInPeriod() == 7;
        ok &= DataStore.BudgetPeriod.MONTHLY.daysInPeriod() == now.lengthOfMonth();
        ok &= DataStore.BudgetPeriod.YEARLY.daysInPeriod()  == now.lengthOfYear();

        // Proration math must compute against the real period length, not a
        // hardcoded 30.
        resetStore();
        DataStore s = store();
        s.restoreBudgetPeriod(DataStore.BudgetPeriod.MONTHLY);
        s.setMonthlyBudget(3100.0);
        int len     = now.lengthOfMonth();
        int elapsed = now.getDayOfMonth();
        double proratedExpected = (3100.0 / len) * elapsed;
        ok &= Math.abs(s.getProratedBudget() - proratedExpected) < 1e-6;

        // Daily allowance with no expenses: remaining / days left in period.
        int daysLeft = len - elapsed;
        double allowExpected = daysLeft <= 0 ? 0 : Math.max(3100.0 / daysLeft, 0);
        ok &= Math.abs(s.getDailyAllowance() - allowExpected) < 1e-6;
        return ok;
    }

    private static boolean testXPDerivedLevel() {
        boolean ok = true;
        DataStore s = store();

        // Shipped quadratic curve: level starts at 100·1, 100·2, … cumulative XP
        resetStore(); s.restoreXP(600, 7); ok &= s.getLevel() == 4;   // stale LEVEL=7 must be ignored
        resetStore(); s.restoreXP(300, 9); ok &= s.getLevel() == 3;   // exact level-3 boundary
        resetStore(); s.restoreXP(299, 9); ok &= s.getLevel() == 2;
        resetStore(); s.restoreXP(100, 1); ok &= s.getLevel() == 2;
        resetStore(); s.restoreXP(99,  1); ok &= s.getLevel() == 1;
        resetStore(); s.restoreXP(0,  10); ok &= s.getLevel() == 1;
        resetStore(); s.restoreXP(4500, 2); ok &= s.getLevel() == 10; // capped at final rank
        resetStore(); s.restoreXP(9999, 2); ok &= s.getLevel() == 10; // stays capped
        return ok;
    }

    private static boolean testLevelConsistency() {
        resetStore();
        DataStore s = store();
        s.awardXP(250);                       // should level up to exactly 2
        boolean ok = s.getLevel() == 2;
        ok &= s.getLevel() == DataStore.levelFromXP(s.getTotalXP());
        return ok;
    }

    private static boolean testAwardWithdrawXP() {
        boolean ok = true;
        resetStore();
        DataStore s = store();
        s.addSavings(1000);                    // +100 XP (amount/10, min 5)
        ok &= s.getTotalXP() == 100;
        ok &= s.getCurrentSavings() == 1000;
        ok &= s.getLevel() == 2;

        s.withdrawSavings(50);                 // penalty = max(1, 50) = 50 XP
        ok &= s.getCurrentSavings() == 950;
        ok &= s.getTotalXP() == 50;
        ok &= s.getLevel() == 1;               // level-down when XP drops below threshold
        return ok;
    }

    private static boolean testSaveLoadRoundTripEscapes() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();

        try {
            // Tricky descriptions: literal pipe, literal backslash, pair, trailing
            // backslash, plain, empty. Older escaping corrupted or dropped these.
            String[] descs = { "a|b", "a\\b|c", "x\\", "a\\|b", "plain desc", "" };
            LocalDate d = LocalDate.of(2026, 1, 15);
            for (int i = 0; i < descs.length; i++) {
                s.addExpense(new Expense(descs[i], 10.0 + i, "Food", d, i % 2 == 0 ? "Need" : "Want"));
            }
            s.addSavingsGoal("goal | with pipe", 500.0);

            SaveManager.save();
            String content = readFile(SaveManager.getSaveFile());

            boolean ok = Files.exists(SaveManager.getSaveFile());
            ok &= content.startsWith("VERSION=3");
            ok &= !Files.exists(SaveManager.getBackupFile());  // first save: no previous state to back up

            // Wipe the store, then prove load() genuinely restores it.
            s.restoreExpenses(Collections.<Expense>emptyList());
            s.adminResetSavings();
            SaveManager.load();

            ok &= s.getExpenses().size() == descs.length;
            for (int i = 0; i < descs.length && ok; i++) {
                ok = sameExpense(s, i, descs[i], 10.0 + i, "Food",
                                 i % 2 == 0 ? "Need" : "Want")
                     && s.getExpenses().get(i).getDate().equals(d);
            }
            ok &= s.getSavingsGoals().size() == 1;
            ok &= s.getSavingsGoals().get(0).getName().equals("goal | with pipe");
            return ok;
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testNoTempLeftovers() throws Exception {
        resetStore();
        Path dir = newTestDir();
        try {
            DataStore s = store();
            s.addExpense(new Expense("temp check", 5.0, "Food", LocalDate.now(), "Need"));
            SaveManager.save();
            SaveManager.save();
            try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                return files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp"));
            }
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testBackupRotationAfterWrite() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();
        try {
            s.addExpense(new Expense("v1", 1.0, "Food", LocalDate.now(), "Need"));
            SaveManager.save();
            boolean ok = !Files.exists(SaveManager.getBackupFile());  // no previous version yet

            s.addExpense(new Expense("v2", 2.0, "Food", LocalDate.now(), "Need"));
            SaveManager.save();

            String dat = readFile(SaveManager.getSaveFile());
            String bak = readFile(SaveManager.getBackupFile());
            ok &= dat.contains("EXPENSE_1");
            ok &= bak.contains("EXPENSE_0") && !bak.contains("EXPENSE_1"); // bak = previous version only
            return ok;
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testCorruptSaveFallsBackToBak() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();
        try {
            s.addExpense(new Expense("fallback test", 77.5, "Food", LocalDate.of(2026, 2, 2), "Need"));
            SaveManager.save();   // v1 -> will end up in .bak after next save
            s.addExpense(new Expense("newest", 1.0, "Food", LocalDate.of(2026, 2, 3), "Want"));
            SaveManager.save();   // v2 live, v1 in .bak

            // Corrupt the live file two ways: garbage-only and unparseable number.
            Files.write(SaveManager.getSaveFile(),
                    "COMPLETE GARBAGE".getBytes());
            SaveManager.load();
            boolean ok = s.getExpenses().size() == 1;   // from .bak
            ok &= sameExpense(s, 0, "fallback test", 77.5, "Food", "Need");

            Files.write(SaveManager.getSaveFile(),
                    "BUDGET=not_a_number\nTOTAL_XP=zzz\n".getBytes());
            s.adminResetExpenses();
            SaveManager.load();
            ok &= s.getExpenses().size() == 1;          // still recovered from .bak
            ok &= sameExpense(s, 0, "fallback test", 77.5, "Food", "Need");
            return ok;
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testMissingSaveFallsBackToBak() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();
        try {
            s.addExpense(new Expense("recover me", 55.0, "Food", LocalDate.of(2026, 3, 3), "Need"));
            SaveManager.save();
            s.addExpense(new Expense("second", 2.0, "Food", LocalDate.of(2026, 3, 4), "Want"));
            SaveManager.save();

            // THE fix: the .dat disappears (e.g. half-finished save between the
            // old rotate and write). Loader must fall back to .bak, not silently
            // start empty.
            Files.delete(SaveManager.getSaveFile());
            s.adminResetExpenses();
            SaveManager.load();
            boolean ok = s.getExpenses().size() == 1;
            ok &= sameExpense(s, 0, "recover me", 55.0, "Food", "Need");
            return ok;
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testBothCorruptStartsFresh() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();
        try {
            Files.write(SaveManager.getSaveFile(),   "%%%\n@@@\n".getBytes());
            Files.write(SaveManager.getBackupFile(), "%%%\n@@@\n".getBytes());
            SaveManager.load();
            return s.getExpenses().isEmpty()
                    && s.getMonthlyBudget() == 5000.0; // clean defaults, no crash
        } finally {
            rmDir(dir);
        }
    }

    /**
     * Legacy save = no VERSION header. Covers 4-field v2 expenses (needWant
     * defaults to Need), 5-field v3 expenses, escaped goal names, and the
     * XP-derived level (LEVEL line is validated but ignored).
     */
    private static boolean testLegacyLoad() throws Exception {
        resetStore();
        DataStore s = store();
        Path dir = newTestDir();
        try {
            String legacy = ""
                    + "BUDGET=4321.5\n"
                    + "BUDGET_PERIOD=Weekly\n"
                    + "NEEDS_PERCENT=60.0\n"
                    + "TRACKING_MODE=Advanced\n"
                    + "SAVINGS_GOAL=800.0\n"
                    + "CURRENT_SAVINGS=123.45\n"
                    + "TOTAL_XP=350\n"
                    + "LEVEL=7\n"                      // stale; must be ignored (derived 4)
                    + "THEME=Dark\n"
                    + "EXPENSE_COUNT=2\n"
                    + "EXPENSE_0=2026-05-01|Food|legacy 4-field|42.5\n"
                    + "EXPENSE_1=2026-05-02|Transport|legacy 5-field|9.99|Want\n"
                    + "GOAL_COUNT=1\n"
                    + "GOAL_0=goal\\|pipe|650.5|true\n";
            Files.write(SaveManager.getSaveFile(), legacy.getBytes());

            SaveManager.load();
            boolean ok = Math.abs(s.getMonthlyBudget() - 4321.5) < 1e-9;
            ok &= s.getBudgetPeriod() == DataStore.BudgetPeriod.WEEKLY;
            ok &= Math.abs(s.getNeedsPercent() - 60.0) < 1e-9;
            ok &= s.getTrackingMode() == DataStore.TrackingMode.ADVANCED;
            ok &= Math.abs(s.getCurrentSavings() - 123.45) < 1e-9;
            ok &= s.getTotalXP() == 350;
            ok &= s.getLevel() == 3;                            // derived (100/300/600 thresholds); LEVEL=7 ignored
            ok &= s.getExpenses().size() == 2;
            ok &= sameExpense(s, 0, "legacy 4-field", 42.5, "Food", "Need");     // defaulted
            ok &= sameExpense(s, 1, "legacy 5-field", 9.99, "Transport", "Want"); // explicit
            ok &= s.getSavingsGoals().size() == 1;
            ok &= s.getSavingsGoals().get(0).getName().equals("goal|pipe");       // unescaped
            ok &= s.getSavingsGoals().get(0).isAchieved();
            return ok;
        } finally {
            rmDir(dir);
        }
    }

    private static boolean testThemeRestore() throws Exception {
        resetStore();
        Path dir = newTestDir();
        try {
            boolean ok = true;
            Files.write(SaveManager.getSaveFile(),
                    ("BUDGET=100\nTHEME=Light\n").getBytes());
            SaveManager.load();
            ok &= UITheme.getCurrentTheme() == UITheme.Theme.LIGHT;

            // Restore the default so later tests / app state is untouched.
            Files.write(SaveManager.getSaveFile(),
                    ("BUDGET=100\nTHEME=Dark\n").getBytes());
            SaveManager.load();
            ok &= UITheme.getCurrentTheme() == UITheme.Theme.DARK;
            return ok;
        } finally {
            rmDir(dir);
        }
    }
}
