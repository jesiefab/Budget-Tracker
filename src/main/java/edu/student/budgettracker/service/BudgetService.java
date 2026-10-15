package edu.student.budgettracker.service;

import edu.student.budgettracker.model.Category;
import edu.student.budgettracker.model.DataStore;
import edu.student.budgettracker.model.Expense;
import edu.student.budgettracker.model.NeedWant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure budget-math service. No Swing, no I/O, no UI imports — safe to unit test
 * and share by any future interface (Path D) or the Android port.
 *
 * <p>All dates are driven by an explicit period object and a "now" date so
 * calls can be driven deterministically in tests. The period is the
 * {@link DataStore.BudgetPeriod} type.</p>
 */
public final class BudgetService {

    // 7-day weeks start on Monday; weekday values 1..7 from java.time.
    private static final int WEEK_STARTS_ON_MONDAY = 1;

    private BudgetService() {}

    // ── Period helpers ───────────────────────────────────────────────────────────

    /** Days in a non-leap year. */
    public static final int YEAR_DAYS = 365;

    /** Days in a leap year. */
    public static final int LEAP_YEAR_DAYS = 366;

    /**
     * Number of days in the period starting at {@code start}, as of {@code now}.
     * WEEKLY counts full weeks; MONTHLY counts days in the month;
     * YEARLY counts 365 or 366 (leap-safe).
     */
    public static int periodTotalDays(DataStore.BudgetPeriod period, LocalDate now) {
        if (period == null) return 30;
        return period.totalDays();
    }

    /**
     * Days elapsed in the period so far (1-based, never 0 for the current day).
     * If {@code now} is before the period start, returns 0.
     */
    public static int daysElapsed(DataStore.BudgetPeriod period, LocalDate now) {
        if (period == null || now == null) return 0;
        if (now.isBefore(period.start)) return 0;
        return period.daysElapsed(now);
    }

    /**
     * Days remaining in the period at {@code now}.
     * Never negative — a full period at/after its end returns 0.
     */
    public static int daysRemaining(DataStore.BudgetPeriod period, LocalDate now) {
        int elapsed = daysElapsed(period, now);
        return Math.max(periodTotalDays(period, now) - elapsed, 0);
    }

    /**
     * True when {@code now} is inside the period (inclusive of start).
     */
    public static boolean isActive(DataStore.BudgetPeriod period, LocalDate now) {
        return period != null && now != null
                && !now.isBefore(period.start)
                && now.isBefore(period.start.plusDays(period.totalDays()));
    }

    // ── Top-level budget numbers ─────────────────────────────────────────────────

    /** Prorated budget for the current day of a period. */
    public static BigDecimal proratedBudget(BigDecimal totalBudget, DataStore.BudgetPeriod period,
                                            LocalDate now) {
        if (totalBudget == null || totalBudget.signum() <= 0) return Money.ZERO;
        int elapsed = daysElapsed(period, now);
        return Money.divide(totalBudget, BigDecimal.valueOf(period.totalDays()), 2)
                     .multiply(BigDecimal.valueOf(elapsed));
    }

    /** Remaining prorated budget at {@code now}. */
    public static BigDecimal remainingProrated(BigDecimal budgeted, BigDecimal spent,
                                               DataStore.BudgetPeriod period, LocalDate now) {
        return Money.subtract(proratedBudget(budgeted, period, now), spent);
    }

    /** Safe-to-spend today (never negative). Returns amount in pesos (2 dp). */
    public static BigDecimal dailyAllowance(BigDecimal budgeted, BigDecimal spent,
                                            DataStore.BudgetPeriod period, LocalDate now) {
        int daysLeft = daysRemaining(period, now);
        if (daysLeft <= 0) return Money.ZERO;
        BigDecimal remaining = Money.subtract(budgeted, spent);
        return Money.divide(remaining, BigDecimal.valueOf(daysLeft), 2);
    }

    // ── Needs / wants pools (Advanced mode) ─────────────────────────────────────

    /** Split of the total budget by the needs percentage (0..100). */
    public static BigDecimal needsBudget(BigDecimal totalBudget, double needsPercent) {
        if (totalBudget == null || totalBudget.signum() <= 0) return Money.ZERO;
        return Money.divide(totalBudget, BigDecimal.valueOf(100), 2)
                     .multiply(BigDecimal.valueOf(needsPercent));
    }

    public static BigDecimal wantsBudget(BigDecimal totalBudget, double needsPercent) {
        if (totalBudget == null || totalBudget.signum() <= 0) return Money.ZERO;
        return Money.divide(totalBudget, BigDecimal.valueOf(100), 2)
                     .multiply(BigDecimal.valueOf(100 - needsPercent));
    }

    /** Prorated needs budget for the current day. */
    public static BigDecimal needsProratedBudget(BigDecimal totalBudget,
                                                 double needsPercent, DataStore.BudgetPeriod period,
                                                 LocalDate now) {
        return proratedBudget(needsBudget(totalBudget, needsPercent), period, now);
    }

    /** Prorated wants budget for the current day. */
    public static BigDecimal wantsProratedBudget(BigDecimal totalBudget,
                                                 double needsPercent, DataStore.BudgetPeriod period,
                                                 LocalDate now) {
        return proratedBudget(wantsBudget(totalBudget, needsPercent), period, now);
    }

    /** Spent on Needs in the period. */
    public static BigDecimal needsSpent(List<Expense> expenses, LocalDate start, LocalDate end) {
        return spentByType(expenses, start, end, NeedWant.NEED);
    }

    /** Spent on Wants in the period. */
    public static BigDecimal wantsSpent(List<Expense> expenses, LocalDate start, LocalDate end) {
        return spentByType(expenses, start, end, NeedWant.WANT);
    }

    /** Spent on a given category across the period (all-time). */
    public static BigDecimal spentByCategory(List<Expense> expenses, LocalDate start, LocalDate end,
                                             Category category) {
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .filter(e -> e.getCategory() == category)
                .map(Expense::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    /** Total spent in the period. */
    public static BigDecimal totalSpent(List<Expense> expenses, LocalDate start, LocalDate end) {
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .map(Expense::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    private static BigDecimal spentByType(List<Expense> expenses, LocalDate start, LocalDate end,
                                          NeedWant want) {
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .filter(e -> e.getNeedWant() == want)
                .map(Expense::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    // ── Per-category subtotals for a breakdown card list ────────────────────────

    /** Map of category -> spent amount for all-time. */
    public static Map<Category, BigDecimal> categoryTotals(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.reducing(Money.ZERO, Expense::getAmount, Money::add)));
    }
}
