import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Expense — immutable value object representing a single spending entry.
 *
 * Fields:
 *   description  — free-text label entered by the user
 *   amount       — peso value (must be > 0)
 *   category     — one of the CATEGORIES constants
 *   date         — date the expense was created (defaults to today)
 *   needWant     — "Need" or "Want" classification
 *
 * The restore constructor is used exclusively by SaveManager when
 * restoring saved expenses with their original dates and tags.
 */
public class Expense {

    // ── Category list (shared with UI dropdowns) ───────────────────────────────
    public static final String[] CATEGORIES = {
            "Food", "Transport", "School", "Entertainment", "Health", "Shopping", "Other"
    };

    // ── Need / Want options (shared with UI) ───────────────────────────────────
    public static final String[] NEED_WANT_OPTIONS = { "Need", "Want" };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final String    description;
    private final double    amount;
    private final String    category;
    private final LocalDate date;
    private final String    needWant;   // "Need" or "Want"

    // ── Constructors ───────────────────────────────────────────────────────────

    /** Normal constructor — date defaults to today. */
    public Expense(String description, double amount, String category, String needWant) {
        this(description, amount, category, LocalDate.now(), needWant);
    }

    /** Restore constructor — used by SaveManager to preserve original date and tag. */
    public Expense(String description, double amount, String category,
                   LocalDate date, String needWant) {
        this.description = description;
        this.amount      = amount;
        this.category    = category;
        this.date        = date;
        this.needWant    = (needWant != null && needWant.equals("Want")) ? "Want" : "Need";
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String    getDescription()   { return description; }
    public double    getAmount()        { return amount; }
    public String    getCategory()      { return category; }
    public LocalDate getDate()          { return date; }
    public String    getFormattedDate() { return date.format(DATE_FMT); }
    public String    getNeedWant()      { return needWant; }

    // ── Display ────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s - %s: \u20B1%.2f",
                getFormattedDate(), needWant, category, description, amount);
    }
}