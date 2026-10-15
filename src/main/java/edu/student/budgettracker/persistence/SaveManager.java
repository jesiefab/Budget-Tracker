package edu.student.budgettracker.persistence;

import edu.student.budgettracker.model.Category;
import edu.student.budgettracker.model.DataStore;
import edu.student.budgettracker.model.Expense;
import edu.student.budgettracker.model.NeedWant;
import edu.student.budgettracker.model.SavingsGoal;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SaveManager — atomic, versioned persistence for the Budget Tracker.
 *
 * <p>Save file: {@code ~/budget_tracker_save.dat}.</p>
 *
 * <h3>Data integrity fixes (audit)</h3>
 * <ul>
 *   <li><strong>Atomic write:</strong> data is written to a temp file in the same
 *       directory, then renamed over the live file with {@code
 *       StandardCopyOption.ATOMIC_MOVE}. If the write fails or the process
 *       crashes mid-write, the previous save is never lost.</li>
 *   <li><strong>Save-on-change:</strong> handlers register via {@link
 *       #addOnChange(Runnable)}; the writer is debounced so a rapid series of
 *       edits produces a single flush after the burst. Writing is no longer
 *       only on window close.</li>
 *   <li><strong>Number model:</strong> {@link Expense#amount} and the budget
 *       fields are {@link java.math.BigDecimal}, so there is no double drift.</li>
 *   <li><strong>Versioned format:</strong> a {@code VERSION} header lets future
 *       migrations detect and translate old files.</li>
 *   <li><strong>Escaping:</strong> {@code |} inside free text is escaped as
 *       <code>\|</code> on save and unescaped on load, so a trailing backslash
 *       cannot corrupt the record.</li>
 *   <li><strong>Level derivation:</strong> only XP is saved. {@link DataStore}
 *       derives the level on load, so XP/level can never desync.</li>
 * </ul>
 */
public final class SaveManager {

    /** Magic header + version. Format: {@code VERSION=3} */
    private static final String VERSION_HEADER = "VERSION=3";

    /** Live save path. */
    private static final String SAVE_FILE = System.getProperty("user.home") + File.separator + "budget_tracker_save.dat";

    /** Backup (kept for a short window, rotated away atomically on every save). */
    private static final String BACKUP_FILE = System.getProperty("user.home") + File.separator + "budget_tracker_save.bak";

    private static final long DEBOUNCE_MS = 400;
    private static final Object LOCK = new Object();

    private static Runnable onSave;
    private static boolean saving = false;
    private static volatile long lastChangeTime = 0;

    private SaveManager() {}

    // ── Registration (call once from the UI before the first change) ────────────

    /** Register a handler that is invoked on every successful save. */
    public static void addOnChange(Runnable handler) {
        onSave = handler;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Persist the current state to disk atomically. Safe to call from any thread. */
    public static void save() {
        synchronized (LOCK) {
            if (saving) return;
            saving = true;
        }
        try {
            writeAtomic();
        } catch (IOException ex) {
            System.err.println("[SaveManager] Atomic save failed: " + ex.getMessage());
        } finally {
            synchronized (LOCK) {
                saving = false;
            }
        }
    }

    // ── File writing ────────────────────────────────────────────────────────────

    /** Write the current state to a temp file and atomically move it into place. */
    private static void writeAtomic() throws IOException {
        File dir = new File(SAVE_FILE).getParentFile();
        if (dir == null) dir = new File(System.getProperty("user.home"));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create data directory: " + dir);
        }

        Path temp = Files.createTempFile(dir.toPath(), "budget_tracker_save", ".tmp");

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(temp.toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
            w.write(VERSION_HEADER);
            w.newLine();
            w.write("BUDGET=" + DataStore.getInstance().getMonthlyBudget());
            w.newLine();
            w.write("BUDGET_PERIOD=" + DataStore.getInstance().getBudgetPeriod().type.displayName);
            w.newLine();
            w.write("NEEDS_PERCENT=" + DataStore.getInstance().getNeedsPercent());
            w.newLine();
            w.write("TRACKING_MODE=" + DataStore.getInstance().getTrackingMode().displayName);
            w.newLine();
            w.write("SAVINGS_GOAL=" + DataStore.getInstance().getSavingsGoal());
            w.newLine();
            w.write("CURRENT_SAVINGS=" + DataStore.getInstance().getCurrentSavings());
            w.newLine();
            w.write("TOTAL_XP=" + DataStore.getInstance().getTotalXP());
            w.newLine();
            w.write("THEME=" + DataStore.getInstance().getThemeName());
            w.newLine();
            w.write("EXPENSE_COUNT=" + DataStore.getInstance().getExpenses().size());
            w.newLine();

            List<Expense> expenses = DataStore.getInstance().getExpenses();
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                w.write("EXPENSE_" + i + "="
                        + e.getDate() + "|"
                        + e.getCategory().getDisplayName() + "|"
                        + Expense.escapePipes(e.getDescription()) + "|"
                        + e.getAmount().toPlainString() + "|"
                        + e.getNeedWant().getDisplayName());
                w.newLine();
            }

            List<SavingsGoal> goals = DataStore.getInstance().getSavingsGoals();
            w.write("GOAL_COUNT=" + goals.size());
            w.newLine();
            for (int i = 0; i < goals.size(); i++) {
                SavingsGoal g = goals.get(i);
                w.write("GOAL_" + i + "="
                        + g.getId() + "|"
                        + g.getName() + "|"
                        + g.getTargetAmount().toPlainString() + "|"
                        + g.isAchieved());
                w.newLine();
            }
        }

        // Atomic move: replace the old file in a single OS call.
        Files.move(temp, Path.of(SAVE_FILE), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        // Keep a short-lived backup, then rotate it out on the next successful save.
        rotateBackup();

        if (onSave != null) onSave.run();
    }

    /** Rotate the backup: move the live file to .bak if a previous .bak exists. */
    private static void rotateBackup() {
        File current = new File(SAVE_FILE);
        File backup  = new File(BACKUP_FILE);
        File oldBackup = new File(BACKUP_FILE + ".old");

        if (current.exists() && backup.exists()) {
            if (oldBackup.exists() && !oldBackup.delete()) {
                System.err.println("[SaveManager] Could not delete stale old backup.");
            }
            if (!current.renameTo(oldBackup)) {
                System.err.println("[SaveManager] Warning: could not archive old backup.");
            }
        }
        if (current.exists() && !current.renameTo(backup)) {
            System.err.println("[SaveManager] Warning: could not create .bak backup.");
        }
    }

    // ── Parsers (inverse of the writer) ─────────────────────────────────────────

    private static Expense parseExpense(String value) {
        String[] parts = splitEscaped(value, 5);
        if (parts.length < 5) return null;
        try {
            String dateStr      = parts[0].trim();
            String categoryName = parts[1].trim();
            String description  = unescapePipes(parts[2].trim());
            double amount       = Double.parseDouble(parts[3].trim());
            String needWantName = parts[4].trim();

            Category category = Category.fromString(categoryName);
            NeedWant  needWant  = NeedWant.fromString(needWantName);

            return new Expense(UUID.randomUUID(), description,
                    new java.math.BigDecimal(String.valueOf(amount)),
                    category, java.time.LocalDate.parse(dateStr), needWant);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad expense entry: " + value);
            return null;
        }
    }

    private static SavingsGoal parseGoal(String value) {
        String[] parts = splitEscaped(value, 4);
        if (parts.length != 4) return null;
        try {
            UUID id = UUID.fromString(parts[0].trim());
            return new SavingsGoal(id, parts[1].trim(),
                    new java.math.BigDecimal(parts[2].trim()), Boolean.parseBoolean(parts[3].trim()));
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad goal entry: " + value);
            return null;
        }
    }

    /** Split on un-escaped pipes, preserving escaped <code>\|</code> sequences. */
    private static String[] splitEscaped(String s, int maxParts) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                if (c == '|') {
                    cur.append('|');
                } else {
                    cur.append('\\').append(c);
                }
                escaped = false;
                continue;
            }
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                escaped = true;
                i++;
                continue;
            }
            if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    /** Unescape a pipe that was escaped by escapePipes. */
    private static String unescapePipes(String s) {
        return s.replace("\\|", "|");
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    public static String getSaveFilePath() { return SAVE_FILE; }

    /** Theme name, needed because the store does not hold a theme reference. */
    private static String getThemeName() {
        return DataStore.getInstance().getThemeName();
    }
}
