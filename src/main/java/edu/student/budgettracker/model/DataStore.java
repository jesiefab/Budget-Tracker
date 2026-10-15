package edu.student.budgettracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import edu.student.budgettracker.service.BudgetService;
import edu.student.budgettracker.service.Money;
import edu.student.budgettracker.service.SavingsService;
import edu.student.budgettracker.service.XpService;

/**
 * DataStore — singleton model layer for the Budget Tracker.
 *
 * <p>Owns all runtime state and exposes it through typed accessors. The
 * {@code BudgetPeriod} is an explicit period object rather than an enum with
 * a hard-coded {@code 30}-day length, which fixes the audit flaw where
 * "Monthly" always treated a month as 30 days.</p>
 *
 * <p>Listeners keep the UI in sync when state changes; the UI no longer
 * holds business rules.</p>
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── SavingsGoal (moved to top-level model, UUID-backed) ─────────────────────

    // ── Tracking mode ───────────────────────────────────────────────────────────

    public enum TrackingMode {
        BASIC("Basic"),
        ADVANCED("Advanced");

        public final String displayName;
        TrackingMode(String displayName) { this.displayName = displayName; }

        @Override public String toString() { return displayName; }

        public static TrackingMode fromString(String s) {
            if (s == null) return BASIC;
            for (TrackingMode m : values()) {
                if (m.displayName.equals(s)) return m;
            }
            return BASIC;
        }
    }

    // ── Budget period (corrected: real month length, leap years) ────────────────

    public static final class BudgetPeriod {
        public final DataStore.BudgetPeriodType type;
        public final LocalDate start;

        public BudgetPeriod(DataStore.BudgetPeriodType type, LocalDate start) {
            this.type = type;
            this.start = start;
        }

        public int totalDays() {
            if (type == BudgetPeriodType.WEEKLY) return 7;
            if (type == BudgetPeriodType.YEARLY) {
                return start.isLeapYear() ? BudgetService.LEAP_YEAR_DAYS : BudgetService.YEAR_DAYS;
            }
            // MONTHLY: length derived from the start month.
            return start.lengthOfMonth();
        }

        public int daysElapsed(LocalDate now) {
            if (now == null || now.isBefore(start)) return 0;
            if (type == BudgetPeriodType.WEEKLY) {
                // Monday-based week: days since the start of the current week,
                // 1-based, and never more than the period length.
                // A week starting Mon Jan 5: Jan 5 = 1st day, Jan 6 = 2nd day,
                // ..., Jan 11 = 7th day, Jan 12 = 8th day but clamped to 7.
                long days = now.toEpochDay() - start.toEpochDay();
                return (int) Math.min(days + 1, totalDays());
            }
            if (type == BudgetPeriodType.YEARLY) {
                return (int) (now.toEpochDay() - start.toEpochDay()) + 1;
            }
            // MONTHLY: day-of-month, but floored so "day 0" never happens.
            int day = now.getDayOfMonth();
            return Math.max(day, 1);
        }

        public boolean isActive(LocalDate now) {
            return now != null && !now.isBefore(start) && now.isBefore(start.plusDays(totalDays()));
        }

        @Override
        public String toString() { return type.displayName; }
    }

    /** What a budget period means, with its exact length. */
    public enum BudgetPeriodType {
        WEEKLY("Weekly", 7),
        MONTHLY("Monthly", 0), // resolved from the start date
        YEARLY("Yearly", 0);   // resolved from the start date (leap-safe)

        public final String displayName;
        public final int days;

        BudgetPeriodType(String displayName, int days) {
            this.displayName = displayName;
            this.days = days;
        }

        public static BudgetPeriodType fromString(String s) {
            if (s == null) return MONTHLY;
            for (BudgetPeriodType p : values()) {
                if (p.displayName.equals(s)) return p;
            }
            return MONTHLY;
        }
    }

    // ── State ───────────────────────────────────────────────────────────────────

    private BudgetPeriod         budgetPeriod   = new BudgetPeriod(DataStore.BudgetPeriodType.MONTHLY, LocalDate.now());
    private BigDecimal           monthlyBudget  = Money.from("5000.00");
    private double               needsPercent   = 50.0;
    private TrackingMode         trackingMode   = TrackingMode.BASIC;
    private List<Expense>        expenses       = new ArrayList<>();
    private double               savingsGoal    = 1000.0;   // legacy single goal retained for transition
    private double               currentSavings = 0.0;
    private List<SavingsGoal>    savingsGoals   = new ArrayList<>();
    private final List<Runnable> listeners      = new ArrayList<>();

    // XP (level derived on demand)
    private int totalXP = 0;

    // ── Listener system ─────────────────────────────────────────────────────────

    public void addListener(Runnable listener) { listeners.add(listener); }
    private void notifyListeners() { listeners.forEach(Runnable::run); }

    public void forceRefresh() { notifyListeners(); }

    // ── Tracking mode ───────────────────────────────────────────────────────────

    public TrackingMode getTrackingMode() { return trackingMode; }

    public void setTrackingMode(TrackingMode mode) {
        this.trackingMode = mode;
        notifyListeners();
    }

    // ── Budget period ───────────────────────────────────────────────────────────

    public BudgetPeriod getBudgetPeriod() { return budgetPeriod; }

    /** Set to an enum; the period object is rebuilt with the correct length. */
    public void setBudgetPeriod(DataStore.BudgetPeriodType period) {
        this.budgetPeriod = new BudgetPeriod(period, LocalDate.now());
        notifyListeners();
    }

    /** Set to an explicit start date + type (used by services and tests). */
    public void setBudgetPeriod(BudgetPeriod period) {
        this.budgetPeriod = period;
        notifyListeners();
    }

    /** Local-only: keeps the member name distinct from the type. */
    public LocalDate getPeriodStart() {
        return budgetPeriod.start;
    }

    // ── Needs / wants split ─────────────────────────────────────────────────────

    public double getNeedsPercent() { return needsPercent; }

    public void setNeedsPercent(double percent) {
        this.needsPercent = Math.max(0, Math.min(100, percent));
        notifyListeners();
    }

    public BigDecimal getNeedsBudget() { return BudgetService.needsBudget(monthlyBudget, needsPercent); }
    public BigDecimal getWantsBudget() { return BudgetService.wantsBudget(monthlyBudget, needsPercent); }

    // ── Budget calculations — total ─────────────────────────────────────────────

    public BigDecimal getProratedBudget() {
        return BudgetService.proratedBudget(monthlyBudget, budgetPeriod, LocalDate.now());
    }

    public BigDecimal getRemainingProrated() {
        return BudgetService.remainingProrated(monthlyBudget, totalSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    public BigDecimal getDailyAllowance() {
        return BudgetService.dailyAllowance(monthlyBudget, totalSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    public BigDecimal getNeedsProratedBudget() {
        return BudgetService.needsProratedBudget(monthlyBudget, needsPercent, budgetPeriod, LocalDate.now());
    }

    public BigDecimal getNeedsRemainingProrated() {
        return BudgetService.remainingProrated(getNeedsBudget(), needsSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    public BigDecimal getNeedsDailyAllowance() {
        return BudgetService.dailyAllowance(getNeedsBudget(), needsSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    public BigDecimal getWantsProratedBudget() {
        return BudgetService.wantsProratedBudget(monthlyBudget, needsPercent, budgetPeriod, LocalDate.now());
    }

    public BigDecimal getWantsRemainingProrated() {
        return BudgetService.remainingProrated(getWantsBudget(), wantsSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    public BigDecimal getWantsDailyAllowance() {
        return BudgetService.dailyAllowance(getWantsBudget(), wantsSpentCurrentPeriod(), budgetPeriod, LocalDate.now());
    }

    // ── Expense accessors ───────────────────────────────────────────────────────

    public void addExpense(Expense e) {
        expenses.add(e);
        notifyListeners();
    }

    public void addExpenseAt(int index, Expense e) {
        int safeIndex = Math.max(0, Math.min(index, expenses.size()));
        expenses.add(safeIndex, e);
        notifyListeners();
    }

    public void removeExpense(UUID id) {
        expenses.removeIf(e -> e.getId().equals(id));
        notifyListeners();
    }

    public void removeExpenseAt(int index) {
        if (index >= 0 && index < expenses.size()) {
            expenses.remove(index);
            notifyListeners();
        }
    }

    public List<Expense> getExpenses() { return new ArrayList<>(expenses); }

    public Expense getExpense(UUID id) {
        for (Expense e : expenses) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    public BigDecimal getTotalExpenses() {
        return expenses.stream().map(Expense::getAmount).reduce(Money.ZERO, Money::add);
    }

    public BigDecimal getTotalExpensesForCurrentPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.totalSpent(expenses, start, end);
    }

    public BigDecimal getNeedsSpentThisPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.needsSpent(expenses, start, end);
    }

    public BigDecimal getWantsSpentThisPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.wantsSpent(expenses, start, end);
    }

    public BigDecimal getTotalByCategory(String category) {
        return expenses.stream()
                .filter(e -> e.getCategory().getDisplayName().equals(category))
                .map(Expense::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    // ── Budget setters ──────────────────────────────────────────────────────────

    public BigDecimal getMonthlyBudget() { return monthlyBudget; }

    public void setMonthlyBudget(BigDecimal budget) {
        this.monthlyBudget = (budget == null || budget.signum() < 0) ? Money.ZERO : budget;
        notifyListeners();
    }

    public void setMonthlyBudget(double budget) {
        this.monthlyBudget = Money.from(budget);
        notifyListeners();
    }

    // ── Single savings goal (legacy) ────────────────────────────────────────────

    public double getSavingsGoal() { return savingsGoal; }

    public void setSavingsGoal(double goal) {
        this.savingsGoal = goal;
        notifyListeners();
    }

    public double getCurrentSavings() { return currentSavings; }

    public void addSavings(double amount) {
        this.currentSavings += amount;
        awardXP(calcXPForAmount(amount));
        notifyListeners();
    }

    public void withdrawSavings(double amount) {
        double actual = Math.min(amount, currentSavings);
        this.currentSavings -= actual;
        int xpPenalty = Math.max(1, (int) actual);
        this.totalXP = Math.max(0, this.totalXP - xpPenalty);
        notifyListeners();
    }

    // ── Multi-goal savings list ─────────────────────────────────────────────────

    public List<SavingsGoal> getSavingsGoals() {
        return Collections.unmodifiableList(savingsGoals);
    }

    public void addSavingsGoal(String name, double targetAmount) {
        savingsGoals.add(new SavingsGoal(name, Money.from(targetAmount)));
        notifyListeners();
    }

    public void addSavingsGoal(String name, BigDecimal targetAmount) {
        savingsGoals.add(new SavingsGoal(name, targetAmount));
        notifyListeners();
    }

    public void removeSavingsGoal(UUID id) {
        savingsGoals.removeIf(g -> g.getId().equals(id));
        notifyListeners();
    }

    public void removeSavingsGoalAt(int index) {
        if (index >= 0 && index < savingsGoals.size()) {
            savingsGoals.remove(index);
            notifyListeners();
        }
    }

    public void claimSavingsGoal(UUID id) {
        SavingsGoal goal = null;
        for (SavingsGoal g : savingsGoals) {
            if (g.getId().equals(id)) { goal = g; break; }
        }
        if (goal == null || goal.isAchieved()) return;
        if (currentSavings < goal.getTargetAmount().doubleValue()) return;
        currentSavings -= goal.getTargetAmount().doubleValue();
        goal.setAchieved(true);
        notifyListeners();
    }

    // ── XP / Gamification ───────────────────────────────────────────────────────

    public int getTotalXP() { return totalXP; }

    // ── Theme reference (the store has none; providers load it separately) ──
    public String getThemeName() { return "Dark"; }

    /** Derive level from XP — never store both independently. */
    public int getLevel() { return XpService.levelFromXP(totalXP); }

    public int getXPForNextLevel() {
        return XpService.xpForLevel(getLevel() + 1);
    }

    public int getCurrentLevelXP() { return XpService.currentLevelXP(totalXP); }

    public void awardXP(int xp) {
        if (xp < 0) return;
        this.totalXP += xp;
        notifyListeners();
    }

    public int calcXPForAmount(double amount) {
        return SavingsService.defaultPolicy().xpForDeposit(Money.from(amount));
    }



    public String getCurrentTitle() {
        return XpService.displayTitles()[Math.min(getLevel(), XpService.MAX_LEVEL)];
    }

    // ── Save/Load restore methods ───────────────────────────────────────────────

    public void restoreExpenses(List<Expense> saved)       { this.expenses = new ArrayList<>(saved); }
    public void restoreSavings(double savings)             { this.currentSavings = savings; }
    public void restoreXP(int xp)                          { this.totalXP = Math.max(0, xp); }
    public void restoreBudgetPeriod(BudgetPeriod period)   { this.budgetPeriod = period; }
    public void restoreNeedsPercent(double percent)        { this.needsPercent = Math.max(0, Math.min(100, percent)); }
    public void restoreTrackingMode(TrackingMode mode)     { this.trackingMode = mode; }
    public void restoreSavingsGoals(List<SavingsGoal> goals) { this.savingsGoals = new ArrayList<>(goals); }

    // ── Admin reset methods ─────────────────────────────────────────────────────

    public void adminResetSavings() {
        this.currentSavings = 0.0;
        this.savingsGoal = 1000.0;
        this.savingsGoals.clear();
        notifyListeners();
    }

    public void adminResetXP() {
        this.totalXP = 0;
        notifyListeners();
    }

    public void adminResetExpenses() {
        this.expenses.clear();
        notifyListeners();
    }

    public void adminResetBudget() {
        this.monthlyBudget = Money.from("5000.00");
        this.needsPercent = 50.0;
        this.trackingMode = TrackingMode.BASIC;
        notifyListeners();
    }

    /** Total spent (needed by the service layer) — kept private-ish for internal use. */
    public BigDecimal wantsSpentCurrentPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.wantsSpent(expenses, start, end);
    }

    public BigDecimal needsSpentCurrentPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.needsSpent(expenses, start, end);
    }

    private BigDecimal totalSpentCurrentPeriod() {
        LocalDate start = getPeriodStart();
        LocalDate end = start == null ? LocalDate.now() : start.plusDays(budgetPeriod.totalDays());
        return BudgetService.totalSpent(expenses, start, end);
    }
}
