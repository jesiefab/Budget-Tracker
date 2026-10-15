package edu.student.budgettracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Expense — immutable value object representing a single spending entry.
 *
 * <p>Fields:</p>
 * <dl>
 *   <dt>id</dt><dd>stable primary key (UUID). Replaces list-index identity so removals
 *       and filtered-indices mappings go away entirely.</dd>
 *   <dt>description</dt><dd>free-text label entered by the user.</dd>
 *   <dt>amount</dt><dd>peso value, stored as {@link BigDecimal} to eliminate
 *       floating-point rounding drift. Must be positive.</dd>
 *   <dt>category</dt><dd>one of the {@link Category} enum values.</dd>
 *   <dt>date</dt><dd>date the expense was created/recorded.</dd>
 *   <dt>needWant</dt><dd>{@link NeedWant} classification.</dd>
 * </dl>
 *
 * <p>Serialization contract (plain text, pipe-delimited):</p>
 * <pre>
 * EXPENSE_N=yyyy-MM-dd|Category|description|amount|needWant
 * </pre>
 * <p>Pipe characters inside {@code description} are escaped as {@code \|}
 * on save and unescaped on load. The {@code VERSION} header exists so that
 * future format changes can be detected and migrated.</p>
 */
public final class Expense {

    /** Stable identifier. Present from day one; used for removal and lookups. */
    private final UUID id;

    private final String    description;
    private final BigDecimal amount;
    private final Category  category;
    private final LocalDate date;
    private final NeedWant  needWant;

    /** Serialisation version of the value format (not the wire format). */
    private static final long serialVersionUID = 1L;

    /** Default category used when a saved record has no matching enum constant. */
    private static final Category DEFAULT_CATEGORY = Category.OTHER;

    // ── Constructors ────────────────────────────────────────────────────────────

    /** Normal constructor — date defaults to today; needWant defaults to Need. */
    public Expense(String description, BigDecimal amount, Category category) {
        this(description, amount, category, LocalDate.now(), NeedWant.NEED);
    }

    /**
     * Full constructor — used by the UI and services.
     *
     * @param description free-text label (not null or blank)
     * @param amount      positive peso value
     * @param category    one of the Category constants
     * @param date        recorded date (not null)
     * @param needWant    NEED or WANT
     */
    public Expense(String description, BigDecimal amount, Category category,
                   LocalDate date, NeedWant needWant) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
        if (needWant == null) {
            throw new IllegalArgumentException("NeedWant must not be null");
        }
        this.id         = UUID.randomUUID();
        this.description = description.trim();
        this.amount      = amount.stripTrailingZeros();
        this.category    = category;
        this.date        = date;
        this.needWant    = needWant;
    }

    /** Restore constructor — used by SaveManager when restoring saved expenses. */
    public Expense(UUID id, String description, BigDecimal amount, Category category,
                   LocalDate date, NeedWant needWant) {
        if (id == null) throw new IllegalArgumentException("ID must not be null");
        this.id          = id;
        this.description = (description == null) ? "" : description.trim();
        this.amount      = (amount == null) ? BigDecimal.ZERO : amount.stripTrailingZeros();
        this.category    = (category == null) ? DEFAULT_CATEGORY : category;
        this.date        = (date == null) ? LocalDate.now() : date;
        this.needWant    = (needWant == null) ? NeedWant.NEED : needWant;
    }

    // ── Accessors ───────────────────────────────────────────────────────────────

    public UUID getId()          { return id; }
    public String    getDescription()   { return description; }
    public BigDecimal getAmount()        { return amount; }
    public Category  getCategory()       { return category; }
    public LocalDate getDate()           { return date; }
    public String    getFormattedDate()  { return date.format(DateTimeFormatter.ISO_LOCAL_DATE); }
    public NeedWant  getNeedWant()       { return needWant; }

    // ── Display ──────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s - %s: \u20B1%s",
                getFormattedDate(), needWant, category, description, amount.toPlainString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense)) return false;
        Expense other = (Expense) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    /**
     * Serialisation helpers for the plain key=value text format.
     *
     * <p>{@code id} is omitted from the persisted record so that legacy saves
     * (which predate UUID) can be loaded and assigned a fresh id on load.</p>
     */
    String toDataRecord() {
        return String.format("%s|%s|%s|%s|%s",
                getDate(), getCategory(), escapePipes(description),
                amount.toPlainString(), getNeedWant().name());
    }

    /** Inverse of toDataRecord (id supplied by the loader). */
    static Expense fromDataRecord(UUID id, String record) {
        String[] parts = record.split("\\|", 5);
        if (parts.length != 5) return null;
        try {
            LocalDate date        = LocalDate.parse(parts[0].trim());
            Category   category   = Category.valueOf(parts[1].trim());
            String     description = parts[2].trim();
            BigDecimal amount      = new BigDecimal(parts[3].trim());
            NeedWant   needWant    = NeedWant.valueOf(parts[4].trim().toUpperCase());
            return new Expense(id, description, amount, category, date, needWant);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Escape a pipe that might otherwise split a data record. */
    public static String escapePipes(String s) {
        return s.replace("|", "\\|");
    }

    /** Unescape a pipe that was escaped by escapePipes. */
    public static String unescapePipes(String s) {
        return s.replace("\\|", "|");
    }

    /** Field order used in persisted lines (no id). */
    private static final class DateTimeFormatter {
        static final java.time.format.DateTimeFormatter ISO_LOCAL_DATE =
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
    }
}
