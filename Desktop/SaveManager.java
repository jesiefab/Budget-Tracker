import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SaveManager — handles persistence for the Student Budget Tracker.
 *
 * Save file : ~/budget_tracker_save.dat
 * Backup    : ~/budget_tracker_save.bak  (rotated before every save)
 *
 * Expense format (5 pipe-separated fields):
 *   EXPENSE_N=date|category|description|amount|needWant
 *
 * Pipe characters inside description are escaped as \| on save
 * and unescaped on load.  The needWant field is new — old save files
 * (4-field format) default to "Need" on load for backward compatibility.
 */
public class SaveManager {

    // ── File paths ─────────────────────────────────────────────────────────────

    private static final String SAVE_FILE   = System.getProperty("user.home")
            + File.separator + "budget_tracker_save.dat";

    private static final String BACKUP_FILE = System.getProperty("user.home")
            + File.separator + "budget_tracker_save.bak";

    // ── Save ───────────────────────────────────────────────────────────────────

    public static void save() {
        DataStore     store    = DataStore.getInstance();
        List<Expense> expenses = store.getExpenses();

        rotateBackup();

        try (BufferedWriter w = new BufferedWriter(new FileWriter(SAVE_FILE))) {
            w.write("BUDGET="          + store.getMonthlyBudget());             w.newLine();
            w.write("BUDGET_PERIOD="   + store.getBudgetPeriod().displayName);  w.newLine();
            w.write("NEEDS_PERCENT="   + store.getNeedsPercent());              w.newLine();
            w.write("TRACKING_MODE="   + store.getTrackingMode().displayName);  w.newLine();
            w.write("SAVINGS_GOAL="    + store.getSavingsGoal());               w.newLine();
            w.write("CURRENT_SAVINGS=" + store.getCurrentSavings());            w.newLine();
            w.write("TOTAL_XP="        + store.getTotalXP());                   w.newLine();
            w.write("LEVEL="           + store.getLevel());                     w.newLine();
            w.write("THEME="           + UITheme.getCurrentTheme().displayName); w.newLine();
            w.write("EXPENSE_COUNT="   + expenses.size());                      w.newLine();

            for (int i = 0; i < expenses.size(); i++) {
                Expense e        = expenses.get(i);
                String  safeDesc = e.getDescription().replace("|", "\\|");
                // 5-field format: date|category|description|amount|needWant
                w.write("EXPENSE_" + i + "="
                        + e.getDate()       + "|"
                        + e.getCategory()   + "|"
                        + safeDesc          + "|"
                        + e.getAmount()     + "|"
                        + e.getNeedWant()
                );
                w.newLine();
            }

            // Savings goals
            List<DataStore.SavingsGoal> goals = store.getSavingsGoals();
            w.write("GOAL_COUNT=" + goals.size()); w.newLine();
            for (int i = 0; i < goals.size(); i++) {
                DataStore.SavingsGoal g    = goals.get(i);
                String                safe = g.getName().replace("|", "\\|");
                w.write("GOAL_" + i + "=" + safe + "|" + g.getTargetAmount() + "|" + g.isAchieved());
                w.newLine();
            }

        } catch (IOException ex) {
            System.err.println("[SaveManager] Failed to save: " + ex.getMessage());
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    public static void load() {
        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) return;

        try {
            loadFromFile(saveFile);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Save file corrupted, trying backup...");
            File backup = new File(BACKUP_FILE);
            if (backup.exists()) {
                try {
                    loadFromFile(backup);
                    System.out.println("[SaveManager] Restored from backup.");
                } catch (Exception bex) {
                    System.err.println("[SaveManager] Backup also failed. Starting fresh.");
                }
            }
        }
    }

    private static void loadFromFile(File file) throws IOException {
        DataStore     store          = DataStore.getInstance();
        double        budget         = 5000.0;
        double        savingsGoal    = 1000.0;
        double        currentSavings = 0.0;
        int           totalXP        = 0;
        int           level          = 1;
        List<Expense> expenses       = new ArrayList<>();
        List<DataStore.SavingsGoal> goals = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int eq = line.indexOf('=');
                if (eq == -1) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                switch (key) {
                    case "BUDGET":          budget         = Double.parseDouble(value);                          break;
                    case "BUDGET_PERIOD":   store.restoreBudgetPeriod(DataStore.BudgetPeriod.fromString(value)); break;
                    case "NEEDS_PERCENT":   store.restoreNeedsPercent(Double.parseDouble(value));                break;
                    case "TRACKING_MODE":   store.restoreTrackingMode(DataStore.TrackingMode.fromString(value)); break;
                    case "SAVINGS_GOAL":    savingsGoal    = Double.parseDouble(value);                          break;
                    case "CURRENT_SAVINGS": currentSavings = Double.parseDouble(value);                          break;
                    case "TOTAL_XP":        totalXP        = Integer.parseInt(value);                            break;
                    case "LEVEL":           level          = Integer.parseInt(value);                            break;
                    case "THEME":           UITheme.applyTheme(UITheme.Theme.fromString(value));                 break;
                    case "EXPENSE_COUNT":   /* informational */ break;
                    case "GOAL_COUNT":      /* informational */ break;
                    default:
                        if (key.startsWith("EXPENSE_")) {
                            Expense e = parseExpense(value);
                            if (e != null) expenses.add(e);
                        } else if (key.startsWith("GOAL_")) {
                            DataStore.SavingsGoal g = parseGoal(value);
                            if (g != null) goals.add(g);
                        }
                        break;
                }
            }
        }

        store.setMonthlyBudget(budget);
        store.setSavingsGoal(savingsGoal);
        store.restoreSavings(currentSavings);
        store.restoreXP(totalXP, level);
        store.restoreExpenses(expenses);
        store.restoreSavingsGoals(goals);
    }

    // ── Expense line parser ────────────────────────────────────────────────────

    /**
     * Parses an expense line.
     *
     * New format (5 fields): date|category|description|amount|needWant
     * Old format (4 fields): date|category|description|amount
     *   → needWant defaults to "Need" for backward compatibility
     */
    private static Expense parseExpense(String value) {
        // Split on un-escaped pipes, keep up to 5 parts
        String[] parts = value.split("(?<!\\\\)\\|", 5);
        if (parts.length < 4) return null;

        try {
            LocalDate date        = LocalDate.parse(parts[0].trim());
            String    category    = parts[1].trim();
            String    description = parts[2].trim().replace("\\|", "|");
            double    amount      = Double.parseDouble(parts[3].trim());
            String    needWant    = parts.length >= 5 ? parts[4].trim() : "Need";
            return new Expense(description, amount, category, date, needWant);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad expense entry: " + value);
            return null;
        }
    }

    // ── Goal line parser ───────────────────────────────────────────────────────

    private static DataStore.SavingsGoal parseGoal(String value) {
        String[] parts = value.split("(?<!\\\\)\\|", 3);
        if (parts.length != 3) return null;

        try {
            String  name     = parts[0].trim().replace("\\|", "|");
            double  target   = Double.parseDouble(parts[1].trim());
            boolean achieved = Boolean.parseBoolean(parts[2].trim());
            DataStore.SavingsGoal goal = new DataStore.SavingsGoal(name, target);
            goal.setAchieved(achieved);
            return goal;
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad goal entry: " + value);
            return null;
        }
    }

    // ── Backup rotation ────────────────────────────────────────────────────────

    private static void rotateBackup() {
        File current = new File(SAVE_FILE);
        File backup  = new File(BACKUP_FILE);
        if (current.exists()) {
            if (backup.exists() && !backup.delete()) {
                System.err.println("[SaveManager] Warning: could not delete old backup file.");
            }
            if (!current.renameTo(backup)) {
                System.err.println("[SaveManager] Warning: could not rotate save to backup.");
            }
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    public static String getSaveFilePath() { return SAVE_FILE; }
}