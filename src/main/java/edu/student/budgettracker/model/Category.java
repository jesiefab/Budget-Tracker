package edu.student.budgettracker.model;

/**
 * Fixed catalogue of expense categories.
 *
 * <p>Each category carries an icon and a default swatch colour so no code
 * outside this file needs to know the mapping — the duplicated {@code switch}
 * in {@code HistoryPanel} and the old {@code BudgetPanel} both disappear.</p>
 */
public enum Category {
    FOOD("Food", "\uD83C\uDF5C", 0xFF9500),
    TRANSPORT("Transport", "\uD83D\uDE8C", 0x007AFF),
    SCHOOL("School", "\uD83D\uDCDA", 0x34C759),
    ENTERTAINMENT("Entertainment", "\uD83C\uDFAE", 0xAF52DE),
    HEALTH("Health", "\uD83D\uDC8A", 0xFF3B30),
    SHOPPING("Shopping", "\uD83D\uDECD", 0xFFCC00),
    OTHER("Other", "\uD83D\uDCC1", 0x828298);

    private final String displayName;
    private final String icon;
    private final int      swatch;

    Category(String displayName, String icon, int swatch) {
        this.displayName = displayName;
        this.icon        = icon;
        this.swatch      = swatch;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon()        { return icon; }
    public int      getSwatch()    { return swatch; }

    public static Category fromString(String s) {
        if (s == null) return OTHER;
        for (Category c : values()) {
            if (c.displayName.equals(s)) return c;
        }
        return OTHER;
    }

    public static Category[] valuesOrdered() {
        return values();
    }
}
