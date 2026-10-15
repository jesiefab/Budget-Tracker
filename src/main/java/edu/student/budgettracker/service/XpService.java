package edu.student.budgettracker.service;

import edu.student.budgettracker.model.SavingsGoal;

import java.util.List;

/**
 * Pure XP/level service.
 *
 * <p>Level is <em>derived</em> from total XP — there is no separate
 * {@code LEVEL} column or setter in this class. This fixes the desync bug
 * the audit flagged: saving the XP but never recalculating level, or
 * restoring both from a save file where they disagreed.</p>
 *
 * <p>XP per level: 100 XP for level 1, 200 for level 2, … (100 × level).</p>
 */
public final class XpService {

    public static final int MAX_LEVEL = 10;

    private static final int[] XP_TIERS = new int[MAX_LEVEL + 1];
    static {
        for (int i = 1; i <= MAX_LEVEL; i++) {
            XP_TIERS[i] = 100 * i;
        }
    }

    private XpService() {}

    /** Total XP required to reach {@code level}.
     *
     * <p>Level 1 requires 0 XP (starting rank). Level 2 requires 100 XP,
     * level 3 requires 200 XP, and so on: 100 XP for each level above 1.</p>
     */
    public static int xpForLevel(int level) {
        if (level <= 1) return 0;
        return level >= XP_TIERS.length ? XP_TIERS[XP_TIERS.length - 1] : XP_TIERS[level - 1];
    }

    /** XP earned toward the current level (0 if level 10).
     *
     * <p>For level N, the tier is 100*(N-1). Current level XP is
     * totalXP minus the tier threshold for the current level.</p>
     */
    public static int currentLevelXP(int totalXP) {
        if (totalXP <= 0) return 0;
        int level = levelFromXP(totalXP);
        int tier = xpForLevel(level);
        return Math.max(0, totalXP - tier);
    }

    /** Current level from total XP (1..MAX_LEVEL). */
    public static int levelFromXP(int totalXP) {
        if (totalXP <= 0) return 1;
        int level = 1;
        while (level < MAX_LEVEL && totalXP >= xpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /** Title index (1-based), matching the legacy TITLES array. */
    public static int titleIndex(int level) {
        return Math.min(level, MAX_LEVEL);
    }

    /** Next level title string for the badge. */
    public static String nextTitle(int level) {
        return level < MAX_LEVEL ? XpService.displayTitles()[level + 1] : null;
    }

    /** Titles for the rank table (index 0 unused). */
    private static final String[] TITLES = {
            "", "Broke Boy", "Budget Apprentice", "Penny Pincher", "Frugal Warrior",
            "Savings Knight", "Money Mage", "Budget Royalty", "Wealth Sage",
            "Diamond Saver", "Financial Legend"
    };

    public static String[] displayTitles() { return TITLES; }

    /** XP remaining to next level. */
    public static int xpToNextLevel(int totalXP, int level) {
        int tier = xpForLevel(level + 1);
        return Math.max(0, tier - totalXP);
    }
}
