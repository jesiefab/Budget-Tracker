import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SaveManager — persistence for the Student Budget Tracker.
 *
 * Save file : ~/budget_tracker_save.dat
 * Backup    : ~/budget_tracker_save.bak  (previous good save, updated on every save)
 *
 * Data-integrity behavior (audit fixes):
 *  - Versioned format: new saves start with a "VERSION=3" header line.
 *    Legacy saves (no header) still load and decode exactly as before.
 *  - Atomic save: the new state is written fully to a temp file in the same
 *    directory, something only then rotated into place. The live save file is
 *    only ever replaced by a complete temp file, so a crash mid-save can never
 *    leave a half-written .dat.
 *  - Backup rotation AFTER the write succeeds: the previous save becomes .bak
 *    first, then the new file moves in. The on-disk pair (save, backup) always
 *    contains at least one fully valid state.
 *  - Missing-save recovery: on startup, if .dat is absent (or fails to parse)
 *    but .bak exists, the app restores from .bak instead of silently starting
 *    empty.
 *  - Corrupt-file detection: a file with no recognizable record keys is
 *    treated as corrupt so the backup path is tried.
 *  - Lossless escaping: free text now escapes backslashes as well as pipes,
 *    so descriptions like "a\|b", "a|b" or a trailing backslash round-trip
 *    byte-identically. Legacy saves keep their old (pipe-only) escaping rules
 *    on read.
 *  - Levels are derived from XP on load (see DataStore.restoreXP), so the
 *    saved LEVEL value can never desync.
 *
 * Expense format (5 pipe-separated fields, new saves):
 *   EXPENSE_N=date|category|description|amount|needWant
 * Legacy expense format (4 fields) is still read; needWant defaults to "Need".
 */
public class SaveManager {

    // ── File locations ─────────────────────────────────────────────────────────

    private static final String SAVE_NAME   = "budget_tracker_save.dat";
    private static final String BACKUP_NAME = "budget_tracker_save.bak";

    /**
     * Directory holding the save files. Injectable so the test harness can
     * exercise save/load round trips without touching the user's real data.
     */
    private static Path saveDirectory = Paths.get(System.getProperty("user.home"));

    /** Redirects where save files live. Call before save()/load(). */
    public static void setSaveDirectory(Path dir) {
        saveDirectory = dir;
    }

    public static Path getSaveFile()   { return saveDirectory.resolve(SAVE_NAME); }
    public static Path getBackupFile() { return saveDirectory.resolve(BACKUP_NAME); }

    /** Legacy string path (used by the UI; kept for API compatibility). */
    public static String getSaveFilePath() { return getSaveFile().toString(); }

    // ── Save ───────────────────────────────────────────────────────────────────

    public static void save() {
        Path save = getSaveFile();
        Path tmp  = null;
        try {
            Path dir = save.getParent() != null ? save.getParent() : Paths.get(".");
            Files.createDirectories(dir);
            tmp = Files.createTempFile(dir, SAVE_NAME, ".tmp");

            try (BufferedWriter w = new BufferedWriter(new FileWriter(tmp.toFile()))) {
                w.write("VERSION=3");                                    w.newLine();
                w.write("BUDGET="          + DataStore.getInstance().getMonthlyBudget());             w.newLine();
                w.write("BUDGET_PERIOD="   + DataStore.getInstance().getBudgetPeriod().displayName);  w.newLine();
                w.write("NEEDS_PERCENT="   + DataStore.getInstance().getNeedsPercent());             w.newLine();
                w.write("TRACKING_MODE="   + DataStore.getInstance().getTrackingMode().displayName);  w.newLine();
                w.write("SAVINGS_GOAL="    + DataStore.getInstance().getSavingsGoal());              w.newLine();
                w.write("CURRENT_SAVINGS=" + DataStore.getInstance().getCurrentSavings());           w.newLine();
                w.write("TOTAL_XP="        + DataStore.getInstance().getTotalXP());                  w.newLine();
                w.write("LEVEL="           + DataStore.getInstance().getLevel());                    w.newLine();
                w.write("THEME="           + UITheme.getCurrentTheme().displayName);                 w.newLine();
                w.write("EXPENSE_COUNT="   + DataStore.getInstance().getExpenses().size());          w.newLine();

                List<Expense> expenses = DataStore.getInstance().getExpenses();
                for (int i = 0; i < expenses.size(); i++) {
                    Expense e = expenses.get(i);
                    w.write("EXPENSE_" + i + "="
                            + e.getDate()           + "|"
                            + e.getCategory()       + "|"
                            + escape(e.getDescription()) + "|"
                            + e.getAmount()         + "|"
                            + e.getNeedWant());
                    w.newLine();
                }

                List<DataStore.SavingsGoal> goals = DataStore.getInstance().getSavingsGoals();
                w.write("GOAL_COUNT=" + goals.size()); w.newLine();
                for (int i = 0; i < goals.size(); i++) {
                    DataStore.SavingsGoal g = goals.get(i);
                    w.write("GOAL_" + i + "=" + escape(g.getName())
                            + "|" + g.getTargetAmount() + "|" + g.isAchieved());
                    w.newLine();
                }
            } // BufferedWriter closed and flushed here

            // Write succeeded — rotate the previous save to .bak, then swap the
            // new file in as the live save. The pair on disk always holds at
            // least one complete, valid state.
            if (Files.exists(save)) moveReplace(save, getBackupFile());
            moveReplace(tmp, save);
            tmp = null;
        } catch (IOException ex) {
            System.err.println("[SaveManager] Failed to save: " + ex.getMessage());
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); }
                catch (IOException cleanupEx) { System.err.println("[SaveManager] Could not delete temp file: " + cleanupEx); }
            }
        }
    }

    /** Move src onto dst in one step, preferring a true atomic replace. */
    private static void moveReplace(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    public static void load() {
        if (tryLoad(getSaveFile())) return;
        if (tryLoad(getBackupFile())) {
            System.out.println("[SaveManager] Restored from backup.");
        }
    }

    /** Returns true only if the file exists and was fully recognized and applied. */
    private static boolean tryLoad(Path file) {
        if (!Files.exists(file)) return false;
        try {
            loadFromFile(file.toFile());
            return true;
        } catch (Exception ex) {
            System.err.println("[SaveManager] Could not load " + file + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Parses and applies one save file. Nothing is applied to the store until
     * the whole file parses, so a partially corrupt file cannot leave the app
     * in a half-restored state.
     */
    private static void loadFromFile(File file) throws IOException {
        DataStore store = DataStore.getInstance();

        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }

        // Legacy saves have no VERSION header and encode text with pipe-only
        // escaping; both must be honored or old files decode incorrectly.
        boolean legacy = lines.isEmpty() || !lines.get(0).startsWith("VERSION=");

        double  budget         = 5000.0;
        double  savingsGoal    = 1000.0;
        double  currentSavings = 0.0;
        int     totalXP        = 0;
        List<Expense>               expenses = new ArrayList<>();
        List<DataStore.SavingsGoal> goals    = new ArrayList<>();
        DataStore.BudgetPeriod    period      = null;
        Double                    needsPercent = null;
        DataStore.TrackingMode    mode        = null;
        UITheme.Theme             theme       = null;
        int recognized = 0;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq == -1) continue;

            String key   = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            try {
                switch (key) {
                    case "VERSION": recognized++; break; // header; future migrations read its value
                    case "BUDGET":          budget        = Double.parseDouble(value); recognized++; break;
                    case "BUDGET_PERIOD":   period        = DataStore.BudgetPeriod.fromString(value); recognized++; break;
                    case "NEEDS_PERCENT":   needsPercent  = Double.parseDouble(value); recognized++; break;
                    case "TRACKING_MODE":   mode          = DataStore.TrackingMode.fromString(value); recognized++; break;
                    case "SAVINGS_GOAL":    savingsGoal   = Double.parseDouble(value); recognized++; break;
                    case "CURRENT_SAVINGS": currentSavings = Double.parseDouble(value); recognized++; break;
                    case "TOTAL_XP":        totalXP       = Integer.parseInt(value); recognized++; break;
                    case "LEVEL":           Integer.parseInt(value); recognized++; break; // ignored; level is derived from XP
                    case "THEME":           theme         = UITheme.Theme.fromString(value); recognized++; break;
                    case "EXPENSE_COUNT": case "GOAL_COUNT": recognized++; break;
                    default:
                        if (key.startsWith("EXPENSE_")) {
                            Expense e = parseExpense(value, legacy);
                            if (e != null) expenses.add(e);
                            recognized++;
                        } else if (key.startsWith("GOAL_")) {
                            DataStore.SavingsGoal g = parseGoal(value, legacy);
                            if (g != null) goals.add(g);
                            recognized++;
                        }
                        break;
                }
            } catch (RuntimeException corrupt) {
                // Unparseable numeric/state fields mean a damaged or truncated
                // file: treat the whole file as corrupt so the caller falls
                // back to the backup (or fresh defaults) instead of silently
                // loading half-parsed values.
                throw new IOException("Corrupt record: " + key, corrupt);
            }
        }

        if (recognized == 0) throw new IOException("No recognizable records (corrupt file)");

        // Commit, mirroring the original apply order.
        if (period       != null) store.restoreBudgetPeriod(period);
        if (needsPercent != null) store.restoreNeedsPercent(needsPercent);
        if (mode         != null) store.restoreTrackingMode(mode);
        store.setMonthlyBudget(budget);
        store.setSavingsGoal(savingsGoal);
        store.restoreSavings(currentSavings);
        store.restoreXP(totalXP, 0); // level re-derived from XP inside restoreXP
        store.restoreExpenses(expenses);
        store.restoreSavingsGoals(goals);
        if (theme != null) UITheme.applyTheme(theme);
    }

    // ── Text encoding for record fields ────────────────────────────────────────

    /**
     * Escapes free text for the pipe-separated record format: backslashes
     * first, then pipes. Combined with {@link #splitEscaped(String, int)} this
     * makes descriptions containing "|", "\" or the sequence "\|" survive a
     * save/load round trip byte-for-byte.
     */
    static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * Splits a record value on unescaped '|'. A '\' escapes the next character,
     * so both \| and \\ sequences decode in place; neighboring fields are still
     * separated cleanly.
     */
    static String[] splitEscaped(String value, int maxParts) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {   // escape pair
                cur.append(value.charAt(i + 1));
                i++;
            } else if (c == '|') {
                if (parts.size() == maxParts - 1) {      // no match limit beyond maxParts
                    cur.append(c);
                } else {
                    parts.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    // ── Expense line parser ────────────────────────────────────────────────────

    /**
     * New format (5 fields): date|category|description|amount|needWant
     * Legacy format (4 fields): date|category|description|amount
     *   → needWant defaults to "Need" for backward compatibility.
     * Legacy files keep the pre-audit decoder (regex split + pipe-only unescape)
     * so old saves decode exactly as they always have.
     */
    private static Expense parseExpense(String value, boolean legacy) {
        String[] parts;
        String description;
        if (legacy) {
            parts = value.split("(?<!\\\\)\\|", 5);
            if (parts.length < 4) return null;
            description = parts[2].trim().replace("\\|", "|");
        } else {
            parts = splitEscaped(value, 5);
            if (parts.length < 5) return null;
            description = parts[2].trim();
        }

        try {
            LocalDate date     = LocalDate.parse(parts[0].trim());
            String    category = parts[1].trim();
            double    amount   = Double.parseDouble(parts[3].trim());
            String    needWant = parts.length >= 5 ? parts[4].trim() : "Need";
            return new Expense(description, amount, category, date, needWant);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad expense entry: " + value);
            return null;
        }
    }

    // ── Goal line parser ───────────────────────────────────────────────────────

    private static DataStore.SavingsGoal parseGoal(String value, boolean legacy) {
        String[] parts;
        String name;
        if (legacy) {
            parts = value.split("(?<!\\\\)\\|", 3);
            if (parts.length != 3) return null;
            name = parts[0].trim().replace("\\|", "|");
        } else {
            parts = splitEscaped(value, 3);
            if (parts.length != 3) return null;
            name = parts[0].trim();
        }

        try {
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
}
