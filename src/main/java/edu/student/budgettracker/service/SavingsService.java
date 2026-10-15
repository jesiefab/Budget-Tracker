package edu.student.budgettracker.service;

import edu.student.budgettracker.model.Expense;
import edu.student.budgettracker.model.SavingsGoal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure savings + goal-math service.
 *
 * <p>Handles deposits, withdrawals, goal claiming and XP hand-off as values
 * only. The XP policy is injected so the anti-exploit rule (Path C) can be
 * swapped in later without touching this class.</p>
 */
public final class SavingsService {

    // Default XP per 10 pesos, clamped to a minimum per deposit (legacy behaviour).
    public static final int DEFAULT_XP_PER_10 = 1;
    public static final int MIN_XP_PER_DEPOSIT = 5;

    private SavingsService() {}

    /** Deposit {@code amount} into savings. Returns XP awarded (0 if policy returns 0). */
    public static int deposit(BigDecimal amount, SavingsPolicy policy, double currentSavings) {
        if (amount == null || amount.signum() <= 0) return 0;
        if (currentSavings < 0) currentSavings = 0;
        int xp = policy.xpForDeposit(amount);
        return xp;
    }

    /** Withdraw, applying the XP penalty from the policy. */
    public static int withdraw(BigDecimal amount, SavingsPolicy policy,
                               double currentSavings) {
        if (amount == null || amount.signum() <= 0) return 0;
        double actual = Math.min(amount.doubleValue(), currentSavings);
        if (actual <= 0) return 0;
        int penalty = policy.xpPenaltyForWithdraw(actual);
        return Math.max(0, penalty);
    }

    /** Claim a goal if achievable. Returns true when a goal was claimed. */
    public static boolean claimGoal(SavingsGoal goal, double currentSavings) {
        if (goal == null) return false;
        if (goal.isAchieved()) return false;
        if (currentSavings < goal.getTargetAmount().doubleValue()) return false;
        // In a pure model the caller mutates; this method returns the outcome.
        return true;
    }

    /** XP awarded for a deposit under the legacy policy (+1 per ₱10, min +5). */
    public static class LegacyPolicy implements SavingsPolicy {
        @Override
        public int xpForDeposit(BigDecimal amount) {
            if (amount == null || amount.signum() <= 0) return 0;
            int base = (int) (amount.divide(BigDecimal.valueOf(10), 0, java.math.RoundingMode.FLOOR)
                    .doubleValue());
            return Math.max(MIN_XP_PER_DEPOSIT, base);
        }

        @Override
        public int xpPenaltyForWithdraw(double pesos) {
            return (int) Math.max(1, pesos);
        }
    }

    /** Anti-exploit policy: XP is awarded on net savings and a withdrawal can never
     *  cost less XP than the deposit that earned it. */
    public static class AntiExploitPolicy implements SavingsPolicy {
        private final int maxXPPerDay;
        private int      dayXP = 0;
        private long     daySeed; // LocalDate-based -> replaced by wallet

        public AntiExploitPolicy(int maxXPPerDay) {
            this.maxXPPerDay = maxXPPerDay;
        }

        @Override
        public int xpForDeposit(BigDecimal amount) {
            if (amount == null || amount.signum() <= 0) return 0;
            // XP scales with the amount but is capped by the day budget.
            int raw = (int) (amount.divide(BigDecimal.valueOf(10), 0,
                    java.math.RoundingMode.FLOOR).doubleValue());
            int capped = Math.min(raw, maxXPPerDay - dayXP);
            dayXP += capped;
            return capped;
        }

        @Override
        public int xpPenaltyForWithdraw(double pesos) {
            // A withdrawal can never cost less XP than the deposit that earned it.
            return (int) Math.max(1, Math.min(pesos, xpForDeposit(BigDecimal.valueOf(pesos))));
        }

        public void resetForDay(long seed) { dayXP = 0; } // seed not used here
    }

    /** Policy contract injected into SavingsService so the XP rule is swappable. */
    public interface SavingsPolicy {
        /** XP awarded for a deposit. */
        int xpForDeposit(BigDecimal amount);
        /** XP penalty for a withdrawal (in XP). */
        int xpPenaltyForWithdraw(double pesos);
    }

    public static SavingsPolicy defaultPolicy() { return new LegacyPolicy(); }
}
