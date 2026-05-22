import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

/**
 * DataStore — singleton model layer for the Budget Tracker.
 *
 * Owns all runtime state:
 *   - Expense list
 *   - Budget amount + period
 *   - Needs/wants split percentage
 *   - Tracking mode (BASIC or ADVANCED)
 *   - Single savings goal + current savings (legacy)
 *   - Multi-goal savings list (SavingsGoal)
 *   - XP / level / gamification
 *
 * Panels register Runnable listeners via addListener(); every mutating
 * method calls notifyListeners() so the UI stays in sync automatically.
 *
 * SavingsGoal behavior:
 *   - All goals share the same currentSavings pool
 *   - Progress = currentSavings / goal.targetAmount (capped at 100%)
 *   - When claimed, targetAmount is subtracted from currentSavings
 *     and the goal is marked achieved
 */
public class DataStore {

    // ── Singleton ──────────────────────────────────────────────────────────────

    private static DataStore instance;

    private DataStore() {}

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    // ── SavingsGoal inner class ────────────────────────────────────────────────

    public static class SavingsGoal {
        private String  name;
        private double  targetAmount;
        private boolean achieved;

        public SavingsGoal(String name, double targetAmount) {
            this.name         = name;
            this.targetAmount = targetAmount;
            this.achieved     = false;
        }

        public String  getName()         { return name; }
        public double  getTargetAmount() { return targetAmount; }
        public boolean isAchieved()      { return achieved; }
        public void    setAchieved(boolean achieved) { this.achieved = achieved; }

        @Override
        public String toString() {
            return name + "|" + targetAmount + "|" + achieved;
        }

        public static SavingsGoal fromString(String s) {
            String[] parts = s.split("\\|", 3);
            if (parts.length != 3) return null;
            try {
                SavingsGoal goal = new SavingsGoal(
                        parts[0].trim(),
                        Double.parseDouble(parts[1].trim())
                );
                goal.achieved = Boolean.parseBoolean(parts[2].trim());
                return goal;
            } catch (Exception e) {
                return null;
            }
        }
    }

    // ── Tracking mode enum ────────────────────────────────────────────────────

    public enum TrackingMode {
        BASIC("Basic"),
        ADVANCED("Advanced");

        public final String displayName;
        TrackingMode(String displayName) { this.displayName = displayName; }

        @Override public String toString() { return displayName; }

        public static TrackingMode fromString(String s) {
            for (TrackingMode m : values())
                if (m.displayName.equals(s)) return m;
            return BASIC;
        }
    }

    // ── Budget Period enum ─────────────────────────────────────────────────────

    public enum BudgetPeriod {
        WEEKLY ("Weekly",  7),
        MONTHLY("Monthly", 30),
        YEARLY ("Yearly",  365);

        public final String displayName;
        public final int    days;

        BudgetPeriod(String displayName, int days) {
            this.displayName = displayName;
            this.days        = days;
        }

        @Override
        public String toString() { return displayName; }

        public static BudgetPeriod fromString(String s) {
            for (BudgetPeriod p : values())
                if (p.displayName.equals(s)) return p;
            return MONTHLY;
        }

        /**
         * Returns how many days have elapsed so far in the current period.
         * WEEKLY : Mon=1 … Sun=7
         * MONTHLY: day-of-month (1-31)
         * YEARLY : day-of-year  (1-365/366)
         */
        public int daysElapsed() {
            LocalDate now = LocalDate.now();
            switch (this) {
                case WEEKLY:  return now.getDayOfWeek().getValue();
                case YEARLY:  return now.getDayOfYear();
                default:      return now.getDayOfMonth(); // MONTHLY
            }
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private BudgetPeriod         budgetPeriod   = BudgetPeriod.MONTHLY;
    private double               monthlyBudget  = 5000.0;
    private double               needsPercent   = 50.0;
    private TrackingMode         trackingMode   = TrackingMode.BASIC;
    private List<Expense>        expenses       = new ArrayList<>();
    private double               savingsGoal    = 1000.0;
    private double               currentSavings = 0.0;
    private List<SavingsGoal>    savingsGoals   = new ArrayList<>();
    private final List<Runnable> listeners      = new ArrayList<>();

    // XP / gamification
    private int totalXP = 0;
    private int level   = 1;

    // ── Listener system ────────────────────────────────────────────────────────

    public void addListener(Runnable listener) { listeners.add(listener); }

    private void notifyListeners() { listeners.forEach(Runnable::run); }

    /** Forces all panels to re-render — used after a theme change. */
    public void forceRefresh() { notifyListeners(); }

    // ── Tracking mode ──────────────────────────────────────────────────────────

    public TrackingMode getTrackingMode() { return trackingMode; }

    public void setTrackingMode(TrackingMode mode) {
        this.trackingMode = mode;
        notifyListeners();
    }

    // ── Budget period ──────────────────────────────────────────────────────────

    public BudgetPeriod getBudgetPeriod() { return budgetPeriod; }

    public void setBudgetPeriod(BudgetPeriod period) {
        this.budgetPeriod  = period;
        this.monthlyBudget = 0.0;
        notifyListeners();
    }

    public LocalDate getPeriodStart() {
        LocalDate now = LocalDate.now();
        switch (budgetPeriod) {
            case WEEKLY:  return now.minusDays(now.getDayOfWeek().getValue() - 1);
            case YEARLY:  return LocalDate.of(now.getYear(), 1, 1);
            default:      return LocalDate.of(now.getYear(), now.getMonth(), 1);
        }
    }

    // ── Needs / wants split ────────────────────────────────────────────────────

    public double getNeedsPercent() { return needsPercent; }
    public double getWantsPercent() { return 100.0 - needsPercent; }

    public void setNeedsPercent(double percent) {
        this.needsPercent = Math.max(0, Math.min(100, percent));
        notifyListeners();
    }

    public double getNeedsBudget() { return monthlyBudget * (needsPercent / 100.0); }
    public double getWantsBudget() { return monthlyBudget * ((100.0 - needsPercent) / 100.0); }

    // ── Budget calculations — total ────────────────────────────────────────────

    public double getProratedBudget() {
        if (monthlyBudget <= 0) return 0;
        int elapsed = Math.max(1, budgetPeriod.daysElapsed());
        return (monthlyBudget / budgetPeriod.days) * elapsed;
    }

    public double getRemainingProrated() {
        return getProratedBudget() - getTotalExpensesForCurrentPeriod();
    }

    public double getDailyAllowance() {
        int daysLeft = budgetPeriod.days - budgetPeriod.daysElapsed();
        if (daysLeft <= 0) return 0;
        double remaining = monthlyBudget - getTotalExpensesForCurrentPeriod();
        return Math.max(remaining / daysLeft, 0);
    }

    // ── Budget calculations — needs ────────────────────────────────────────────

    public double getNeedsProratedBudget() {
        if (getNeedsBudget() <= 0) return 0;
        int elapsed = Math.max(1, budgetPeriod.daysElapsed());
        return (getNeedsBudget() / budgetPeriod.days) * elapsed;
    }

    public double getNeedsRemainingProrated() {
        return getNeedsProratedBudget() - getNeedsSpentThisPeriod();
    }

    public double getNeedsDailyAllowance() {
        int daysLeft = budgetPeriod.days - budgetPeriod.daysElapsed();
        if (daysLeft <= 0) return 0;
        double remaining = getNeedsBudget() - getNeedsSpentThisPeriod();
        return Math.max(remaining / daysLeft, 0);
    }

    // ── Budget calculations — wants ────────────────────────────────────────────

    public double getWantsProratedBudget() {
        if (getWantsBudget() <= 0) return 0;
        int elapsed = Math.max(1, budgetPeriod.daysElapsed());
        return (getWantsBudget() / budgetPeriod.days) * elapsed;
    }

    public double getWantsRemainingProrated() {
        return getWantsProratedBudget() - getWantsSpentThisPeriod();
    }

    public double getWantsDailyAllowance() {
        int daysLeft = budgetPeriod.days - budgetPeriod.daysElapsed();
        if (daysLeft <= 0) return 0;
        double remaining = getWantsBudget() - getWantsSpentThisPeriod();
        return Math.max(remaining / daysLeft, 0);
    }

    // ── Expense accessors ──────────────────────────────────────────────────────

    public void addExpense(Expense e) {
        expenses.add(e);
        notifyListeners();
    }

    /** Inserts an expense at a specific index — used by ExpensePanel when editing a row. */
    public void addExpenseAt(int index, Expense e) {
        int safeIndex = Math.max(0, Math.min(index, expenses.size()));
        expenses.add(safeIndex, e);
        notifyListeners();
    }

    public void removeExpense(int index) {
        if (index >= 0 && index < expenses.size()) {
            expenses.remove(index);
            notifyListeners();
        }
    }

    public List<Expense> getExpenses() { return new ArrayList<>(expenses); }

    public double getTotalExpenses() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    public double getTotalExpensesForCurrentPeriod() {
        LocalDate periodStart = getPeriodStart();
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(periodStart))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getNeedsSpentThisPeriod() {
        LocalDate periodStart = getPeriodStart();
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(periodStart) && "Need".equals(e.getNeedWant()))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getWantsSpentThisPeriod() {
        LocalDate periodStart = getPeriodStart();
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(periodStart) && "Want".equals(e.getNeedWant()))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getTotalByCategory(String category) {
        return expenses.stream()
                .filter(e -> e.getCategory().equals(category))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    // ── Budget setters ─────────────────────────────────────────────────────────

    public double getMonthlyBudget() { return monthlyBudget; }

    public void setMonthlyBudget(double budget) {
        this.monthlyBudget = budget;
        notifyListeners();
    }

    // ── Single savings goal (legacy) ───────────────────────────────────────────

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

    /**
     * Withdraws an amount from savings.
     * Deducts 1 XP per peso withdrawn, minimum 1 XP deducted total.
     * Savings floor is 0 — cannot go negative.
     */
    public void withdrawSavings(double amount) {
        double actual = Math.min(amount, currentSavings); // can't go below 0
        this.currentSavings -= actual;
        int xpPenalty = Math.max(1, (int) actual);
        this.totalXP  = Math.max(0, this.totalXP - xpPenalty);
        // Recalculate level downward if XP dropped below current level threshold
        while (level > 1 && totalXP < xpAtLevelStart()) level--;
        notifyListeners();
    }

    // ── Multi-goal savings list ────────────────────────────────────────────────

    /** Returns an unmodifiable view of the savings goals list. */
    public List<SavingsGoal> getSavingsGoals() {
        return Collections.unmodifiableList(savingsGoals);
    }

    public void addSavingsGoal(String name, double targetAmount) {
        savingsGoals.add(new SavingsGoal(name, targetAmount));
        notifyListeners();
    }

    public void removeSavingsGoal(int index) {
        if (index >= 0 && index < savingsGoals.size()) {
            savingsGoals.remove(index);
            notifyListeners();
        }
    }

    /**
     * Claims a savings goal — subtracts its target from currentSavings
     * and marks it as achieved. Does nothing if already achieved or
     * if savings are insufficient.
     */
    public void claimSavingsGoal(int index) {
        if (index < 0 || index >= savingsGoals.size()) return;
        SavingsGoal goal = savingsGoals.get(index);
        if (goal.isAchieved()) return;
        if (currentSavings < goal.getTargetAmount()) return;
        currentSavings -= goal.getTargetAmount();
        goal.setAchieved(true);
        notifyListeners();
    }

    // ── XP / Gamification ─────────────────────────────────────────────────────

    public int getTotalXP()         { return totalXP; }
    public int getLevel()           { return level; }
    public int getXPForNextLevel()  { return 100 * level; }
    public int getCurrentLevelXP() { return totalXP - xpAtLevelStart(); }

    private int xpAtLevelStart() {
        int sum = 0;
        for (int i = 1; i < level; i++) sum += 100 * i;
        return sum;
    }

    public void awardXP(int xp) {
        totalXP += xp;
        while (getCurrentLevelXP() >= getXPForNextLevel()) level++;
        notifyListeners();
    }

    public int calcXPForAmount(double amount) {
        return Math.max(5, (int)(amount / 10));
    }

    public static final String[] TITLES = {
            "",
            "Broke Boy",
            "Budget Apprentice",
            "Penny Pincher",
            "Frugal Warrior",
            "Savings Knight",
            "Money Mage",
            "Budget Royalty",
            "Wealth Sage",
            "Diamond Saver",
            "Financial Legend"
    };

    public String getCurrentTitle() {
        return TITLES[Math.min(level, TITLES.length - 1)];
    }

    // ── Save/Load restore methods ──────────────────────────────────────────────

    public void restoreExpenses(List<Expense> saved)      { this.expenses = new ArrayList<>(saved); }
    public void restoreSavings(double savings)            { this.currentSavings = savings; }
    public void restoreXP(int xp, int lvl)               { this.totalXP = xp; this.level = lvl; }
    public void restoreBudgetPeriod(BudgetPeriod period)  { this.budgetPeriod = period; }
    public void restoreNeedsPercent(double percent)       { this.needsPercent = percent; }
    public void restoreTrackingMode(TrackingMode mode)    { this.trackingMode = mode; }
    public void restoreSavingsGoals(List<SavingsGoal> goals) { this.savingsGoals = new ArrayList<>(goals); }

    // ── Admin reset methods ────────────────────────────────────────────────────

    public void adminResetSavings() {
        this.currentSavings = 0.0;
        this.savingsGoal    = 1000.0;
        this.savingsGoals.clear();
        notifyListeners();
    }

    public void adminResetXP() {
        this.totalXP = 0;
        this.level   = 1;
        notifyListeners();
    }

    public void adminResetExpenses() {
        this.expenses.clear();
        notifyListeners();
    }

    public void adminResetBudget() {
        this.monthlyBudget = 5000.0;
        this.needsPercent  = 50.0;
        this.trackingMode  = TrackingMode.BASIC;
        notifyListeners();
    }
}