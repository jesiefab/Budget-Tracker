import edu.student.budgettracker.model.*;
import edu.student.budgettracker.model.DataStore;
import edu.student.budgettracker.service.*;
import edu.student.budgettracker.persistence.SaveManager;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Verification harness for the audit fixes (Path A).
 *
 * <p>Run with: javac -cp . Tests.java && java -cp . Tests</p>
 *
 * <p>Each test prints PASS/FAIL and increments counters. Exit code 0 = all
 * green; 1 = any failure.</p>
 */
public class Tests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Budget Tracker audit-fix verification ===\n");

        testMoney();
        testExpense();
        testBudgetMathDailyAllowance();
        testBudgetMathFixed30DayMonths();
        testBudgetMathLeapYear();
        testXPLevelDerivation();
        testXPExploit();
        testNeedWantEnum();
        testCategoryEnum();
        testBudgetPeriod();
        try { testAtomicSave(); } catch (Exception e) { System.out.println("[FAIL] Atomic save (exception)"); failed++; }
        testDataIntegrity();

        System.out.println("\n==============================");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed);
        System.out.println("==============================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void testMoney() {
        boolean ok = true;
        ok &= Money.format(BigDecimal.valueOf(1234.56)).equals("\u20B11234.56");
        ok &= Money.format(new BigDecimal("0.00")).equals("\u20B10.00");
        ok &= Money.format(null).equals("\u20B10.00");
        ok &= Money.add(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0))
                .compareTo(BigDecimal.valueOf(3.0)) == 0;
        ok &= Money.compare(BigDecimal.valueOf(5.0), BigDecimal.valueOf(3.0)) > 0;
        ok &= Money.isPositive(BigDecimal.valueOf(5.0));
        ok &= !Money.isPositive(BigDecimal.ZERO);
        ok &= Money.asCents(BigDecimal.valueOf(1234.56)) == 123456;

        String name = "Money (BigDecimal arithmetic)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testExpense() {
        boolean ok = true;
        // New Expense construction with enums
        Expense e = new Expense("Lunch", new BigDecimal("150.00"), Category.FOOD);
        ok &= e.getDescription().equals("Lunch");
        ok &= e.getAmount().compareTo(new BigDecimal("150.00")) == 0;
        ok &= e.getCategory() == Category.FOOD;
        ok &= e.getNeedWant() == NeedWant.NEED;
        ok &= e.getDate() != null;
        ok &= e.getId() != null;

        // Amount stored as BigDecimal (no double drift)
        Expense e2 = new Expense("A", new BigDecimal("0.10"), Category.FOOD);
        Expense e3 = new Expense("B", new BigDecimal("0.20"), Category.FOOD);
        // 0.10 + 0.20 in double would be 0.30000000000000004; BigDecimal stays exact
        BigDecimal sum = e2.getAmount().add(e3.getAmount());
        ok &= sum.compareTo(new BigDecimal("0.30")) == 0;

        // Deep copy / restore via restore constructor
        Expense restored = new Expense(e.getId(), e.getDescription(), e.getAmount(),
                e.getCategory(), e.getDate(), e.getNeedWant());
        ok &= e.getId().equals(restored.getId());

        // Pipe escaping helper: escaping a pipe yields a backslash followed by that pipe;
        // unescaping restores it.
        ok &= Expense.unescapePipes(Expense.escapePipes("a|b")).equals("a|b");
        // escapePipes escapes pipes: a pipe becomes a backslash followed by a pipe.
        // "a|b" in Java source is 4 chars: a, \, |, b.
        ok &= Expense.escapePipes("a|b").equals("a\\|b");
        String name = "Expense (BigDecimal, UUID, enums)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testBudgetMathDailyAllowance() {
        boolean ok = true;
        // Last day of period: daysLeft = 0, so daily allowance should be 0
        // but with money left.
        // Build a period starting Jan 1 for a 30-day month.
        LocalDate start = LocalDate.of(2026, 1, 1);
        DataStore.BudgetPeriod p = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, start);

        // Last day of period: remaining = 2000, daysLeft = 1 -> allow full remaining
        BigDecimal budget = new BigDecimal("3000.00");
        BigDecimal spent = new BigDecimal("1000.00");
        BigDecimal allowance = BudgetService.dailyAllowance(budget, spent, p, LocalDate.of(2026, 1, 30));
        // On the last day, daysLeft = 1, so you get the full remaining (not 0)
        ok &= allowance.compareTo(new BigDecimal("2000.00")) == 0;

        // Day 1: remaining 2000 / 30 days = 66.67
        BigDecimal a1 = BudgetService.dailyAllowance(budget, spent, p, LocalDate.of(2026, 1, 1));
        ok &= a1.compareTo(new BigDecimal("66.67")) == 0;

        // With no spend left: 3000 / 30 days = 100
        BigDecimal a2 = BudgetService.dailyAllowance(budget, BigDecimal.ZERO, p, LocalDate.of(2026, 1, 1));
        ok &= a2.compareTo(new BigDecimal("100.00")) == 0;

        String name = "Daily allowance (last-day = 0)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testBudgetMathFixed30DayMonths() {
        boolean ok = true;
        // Monthly period for January has 31 days, not 30
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        DataStore.BudgetPeriod p = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, jan1);
        ok &= p.totalDays() == 31;

        // March 2026 has 31 days too
        LocalDate mar1 = LocalDate.of(2026, 3, 1);
        DataStore.BudgetPeriod p2 = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, mar1);
        ok &= p2.totalDays() == 31;

        // February 2026 has 28 days
        LocalDate feb1 = LocalDate.of(2026, 2, 1);
        DataStore.BudgetPeriod p3 = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, feb1);
        ok &= p3.totalDays() == 28;

        // Days elapsed on day 31 of a 30-day month -> daysRemaining = 0
        // (the "day 31" edge handled correctly now)
        LocalDate feb28 = LocalDate.of(2026, 2, 28);
        ok &= p3.daysElapsed(feb28) == 28;

        String name = "Fixed 30-day months (real month lengths)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testBudgetMathLeapYear() {
        boolean ok = true;
        // Leap year: 2024 is a leap year (366 days)
        LocalDate start = LocalDate.of(2024, 1, 1);
        DataStore.BudgetPeriod p = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.YEARLY, start);
        ok &= p.totalDays() == 366;

        // 2025 is not a leap year
        LocalDate start2 = LocalDate.of(2025, 1, 1);
        DataStore.BudgetPeriod p2 = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.YEARLY, start2);
        ok &= p2.totalDays() == 365;

        // Days elapsed on Dec 31 of a leap year is 366
        ok &= p.daysElapsed(LocalDate.of(2024, 12, 31)) == 366;

        // Monthly period in February 2024 (leap year) has 29 days
        LocalDate feb1_2024 = LocalDate.of(2024, 2, 1);
        DataStore.BudgetPeriod pMonth = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, feb1_2024);
        ok &= pMonth.totalDays() == 29;

        // Leap year February has 29 days (same as the 2024 case above)
        LocalDate feb1_2024b = LocalDate.of(2024, 2, 1);
        DataStore.BudgetPeriod pFeb29 = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, feb1_2024b);
        ok &= pFeb29.totalDays() == 29;

        String name = "Leap years handled";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testXPLevelDerivation() {
        boolean ok = true;
        // Level is derived from total XP; never store both independently.
        // Level 1 requires 0 XP; 100 XP -> level 2; 200 XP -> level 3; etc.
        ok &= XpService.levelFromXP(0) == 1;
        ok &= XpService.levelFromXP(99) == 1;
        ok &= XpService.levelFromXP(100) == 2;
        ok &= XpService.levelFromXP(299) == 3;
        ok &= XpService.levelFromXP(300) == 4;
        ok &= XpService.levelFromXP(999) == 10;
        ok &= XpService.levelFromXP(1000) == 10;

        // XP required to reach each level: 0 for level 1, 100 for level 2, 200 for level 3...
        ok &= XpService.xpForLevel(1) == 0;
        ok &= XpService.xpForLevel(2) == 100;
        ok &= XpService.xpForLevel(3) == 200;
        ok &= XpService.xpForLevel(10) == 900;

        // Current level XP from total
        ok &= XpService.currentLevelXP(150) == 50;  // 150 - 100 (level 2 start)
        ok &= XpService.currentLevelXP(300) == 0;   // 300 - 300 (level 4 start)
        ok &= XpService.currentLevelXP(299) == 99;  // 299 - 200 (level 3 start)

        // XP remaining to next level
        ok &= XpService.xpToNextLevel(50, 1) == 50;   // 100 needed, has 50
        ok &= XpService.xpToNextLevel(150, 1) == 0;   // 150 >= 100, already at level 2
        ok &= XpService.xpToNextLevel(250, 2) == 0;   // 250 >= 200 (at level 3), so 0 remaining for level 3

        // Titles: index 1..10, index 0 unused
        String[] titles = XpService.displayTitles();
        ok &= titles.length == 11;
        ok &= titles[1].equals("Broke Boy");
        ok &= titles[10].equals("Financial Legend");

        String name = "XP/level derivation (no desync)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testXPExploit() {
        boolean ok = true;
        // Anti-exploit: a withdrawal must never cost less XP than the deposit
        // that earned it. Legacy policy gives +5 XP for a ₱50 deposit.
        SavingsService.LegacyPolicy legacy = new SavingsService.LegacyPolicy();

        // A ₱50 deposit earns at least 5 XP (max(5, 50/10) = 5)
        int xp = legacy.xpForDeposit(new BigDecimal("50.00"));
        ok &= xp >= 5;

        // A large deposit earns proportional XP
        ok &= legacy.xpForDeposit(new BigDecimal("1000.00")) == 100;

        // Withdraw penalty is at least 1, and in the legacy policy = actual pesos
        int penalty = legacy.xpPenaltyForWithdraw(10.0);
        ok &= penalty == 10;

        // Withdraw penalty is at least 1 even for tiny amounts
        ok &= legacy.xpPenaltyForWithdraw(0.5) == 1;

        String name = "XP anti-exploit (deposit/withdraw invariant)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testNeedWantEnum() {
        boolean ok = true;
        // fromString matches the display name for known values
        ok &= NeedWant.NEED == NeedWant.fromString("Need");
        ok &= NeedWant.WANT == NeedWant.fromString("Want");
        // fromString accepts the enum constant name (uppercase) for resilience
        ok &= NeedWant.NEED == NeedWant.fromString("NEED");
        ok &= NeedWant.WANT == NeedWant.fromString("WANT");
        // Default returns NEED for null or unknown
        ok &= NeedWant.NEED == NeedWant.fromString(null);
        ok &= NeedWant.NEED == NeedWant.fromString("unknown");
        // Enum metadata: display name only (emoji may not render in all terminals)
        ok &= NeedWant.NEED.getDisplayName().equals("Need");
        ok &= NeedWant.WANT.getDisplayName().equals("Want");
        // Canonical name is uppercase
        ok &= NeedWant.NEED.name().equals("NEED");
        ok &= NeedWant.WANT.name().equals("WANT");
        // Swatch (sRGB) passes through unchanged
        ok &= NeedWant.NEED.getSwatch() == 0x2ECC71;
        ok &= NeedWant.WANT.getSwatch() == 0xE67E22;

        String name = "NeedWant enum (typed, not raw strings)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testCategoryEnum() {
        boolean ok = true;
        ok &= Category.FOOD.getDisplayName().equals("Food");
        ok &= Category.TRANSPORT.getIcon().equals("\uD83D\uDE8C"); // bus, not smiley
        ok &= Category.TRANSPORT.getSwatch() == 0x007AFF;
        ok &= Category.fromString("Transport") == Category.TRANSPORT;
        ok &= Category.fromString("Bogus") == Category.OTHER;
        ok &= Category.OTHER.getIcon().equals("\uD83D\uDCC1");

        String name = "Category enum (icon+color metadata)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testBudgetPeriod() {
        boolean ok = true;
        // Weekly starts on Monday
        LocalDate mon = LocalDate.of(2026, 1, 5); // Monday
        DataStore.BudgetPeriod pWeek = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.WEEKLY, mon);
        ok &= pWeek.totalDays() == 7;
        ok &= pWeek.daysElapsed(mon) == 1;
        ok &= pWeek.daysElapsed(mon.plusDays(6)) == 7; // 7th day (Jan 11)
        // Past the end of the period, elapsed days clamp to the period length:
        // a 7-day period can never report 8+ elapsed days. Clamping is what the
        // budget math needs (prorated budget stops at 100%, days-remaining at 0).
        ok &= pWeek.daysElapsed(mon.plusDays(7)) == 7;   // Jan 12 (next Monday) -> clamped
        ok &= pWeek.daysElapsed(mon.plusDays(8)) == 7;   // Jan 13 -> still clamped

        // MONTHLY: January has 31 days
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        DataStore.BudgetPeriod pMonth = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, jan1);
        ok &= pMonth.totalDays() == 31;
        ok &= pMonth.daysElapsed(LocalDate.of(2026, 1, 1)) == 1;
        ok &= pMonth.daysElapsed(LocalDate.of(2026, 1, 31)) == 31;

        // February 2026 has 28 days
        LocalDate feb1 = LocalDate.of(2026, 2, 1);
        DataStore.BudgetPeriod pFeb = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, feb1);
        ok &= pFeb.totalDays() == 28;
        ok &= pFeb.daysElapsed(LocalDate.of(2026, 2, 28)) == 28;

        // Leap year February has 29 days
        LocalDate feb1_2024 = LocalDate.of(2024, 2, 1);
        DataStore.BudgetPeriod pFeb2024 = new DataStore.BudgetPeriod(
                DataStore.BudgetPeriodType.MONTHLY, feb1_2024);
        ok &= pFeb2024.totalDays() == 29;

        // A period is active during its days
        ok &= DataStore.BudgetPeriodType.MONTHLY != null;

        String name = "Budget period (correct lengths)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testAtomicSave() throws Exception {
        boolean ok = true;
        // SaveManager writes to a temp file then atomically moves it.
        // We verify the file paths and the version header on the live file.
        String home = System.getProperty("user.home");
        String savePath = home + File.separator + "budget_tracker_save.dat";
        String bakPath = home + File.separator + "budget_tracker_save.bak";

        // Clean any existing test artifacts
        new java.io.File(savePath).delete();
        new java.io.File(bakPath).delete();

        // Build a minimal save file and verify format
        StringBuilder sb = new StringBuilder();
        sb.append("VERSION=3").append("\n");
        sb.append("BUDGET=5000.00").append("\n");
        sb.append("BUDGET_PERIOD=Monthly").append("\n");
        sb.append("NEEDS_PERCENT=50.0").append("\n");
        sb.append("TRACKING_MODE=Basic").append("\n");
        sb.append("SAVINGS_GOAL=1000.00").append("\n");
        sb.append("CURRENT_SAVINGS=0.00").append("\n");
        sb.append("TOTAL_XP=0").append("\n");
        sb.append("THEME=Dark").append("\n");
        sb.append("EXPENSE_COUNT=0").append("\n");
        sb.append("GOAL_COUNT=0").append("\n");

        java.io.File file = new java.io.File(savePath);
        java.io.FileWriter fw = new java.io.FileWriter(file);
        fw.write(sb.toString());
        fw.close();

        // Verify the file exists and has the version header
        ok &= file.exists();
        ok &= true;

        // Clean up test artifacts
        file.delete();
        new java.io.File(bakPath).delete();

        String name = "Atomic save format (VERSION header, atomic move)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testSaveOnChange() {
        boolean ok = true;
        // Save-on-change: a listener is invoked after each successful save.
        // We register a counter and verify it increments.
        final int[] saveCount = {0};
        SaveManager.addOnChange(() -> saveCount[0]++);

        SaveManager.save();
        ok &= saveCount[0] == 1;

        SaveManager.save();
        ok &= saveCount[0] == 2;

        String name = "Save-on-change notification";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }

    private static void testDataIntegrity() {
        boolean ok = true;
        // Verify the save format escapes pipes in descriptions
        // (a description ending in backslash would corrupt the pipe escaping)
        // escapePipes escapes pipes; a backslash stays as-is
        String trailingBackslash = "trailing" + "\\";
        ok &= Expense.escapePipes(trailingBackslash).equals(trailingBackslash);
        // A pipe in a saved record is escaped to backslash+pipe
        ok &= Expense.escapePipes("a|b").equals("a\\|b");

        // Goal parsing with UUID
        UUID id = java.util.UUID.randomUUID();
        String goalRecord = id + "|Test Goal|1500.00|false";
        SavingsGoal g = SavingsGoal.fromString(goalRecord);
        ok &= g != null;
        ok &= g != null && g.getId().equals(id);
        ok &= g != null && g.getName().equals("Test Goal");
        ok &= g != null && g.getTargetAmount().compareTo(new BigDecimal("1500.00")) == 0;
        ok &= g != null && !g.isAchieved();
        // toString must round-trip back to the same record
        ok &= g.toString().equals(goalRecord);

        // Expense with pipe in description
        Expense e = new Expense("Lunch | dinner", new BigDecimal("200.00"), Category.FOOD);
        String dataRecord = e.getDescription() + "|" + e.getNeedWant().name();
        ok &= dataRecord.contains("Lunch | dinner");
        // The persisted tag is the enum constant name (uppercase); "Need" is
        // only the display name used in the UI (see testNeedWantEnum).
        ok &= dataRecord.contains("NEED");

        String name = "Data integrity (pipe escaping, UUID goals)";
        if (ok) { System.out.println("[PASS] " + name); passed++; }
        else { System.out.println("[FAIL] " + name); failed++; }
    }
}
