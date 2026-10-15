package edu.student.budgettracker.model;

/**
 * Classification of an expense as essential (Need) or discretionary (Want).
 *
 * <p>Used instead of raw string comparisons like {@code "Need".equals(...)}
 * throughout the code base. The enum also carries metadata that removes the
 * duplicated {@code switch} statements that existed in {@code HistoryPanel}
 * and the old {@code BudgetPanel} category-icons/colors.</p>
 */
public enum NeedWant {
    NEED("Need", "✅ Need", "\u2713", 0x2ECC71),
    WANT("Want", "🛍 Want", "\uD83D\uDCA1", 0xE67E22);

    private final String displayName;
    private final String emojiLabel;
    private final String iconHeatmap;   // flat unicode for dark/light themes
    private final int    swatch;        // sRGB int for glyph rendering

    NeedWant(String displayName, String emojiLabel, String iconHeatmap, int swatch) {
        this.displayName = displayName;
        this.emojiLabel  = emojiLabel;
        this.iconHeatmap = iconHeatmap;
        this.swatch      = swatch;
    }

    public String getDisplayName() { return displayName; }
    public String getEmojiLabel()  { return emojiLabel; }
    public String getIcon()        { return iconHeatmap; }
    public int getSwatch()         { return swatch; }

    public static NeedWant fromString(String s) {
        if (s == null) return NEED;
        for (NeedWant n : values()) {
            if (n.displayName.equals(s)) return n;
        }
        // Also accept the enum constant name (uppercase) for resilience.
        for (NeedWant n : values()) {
            if (n.name().equalsIgnoreCase(s)) return n;
        }
        return NEED; // default for legacy data
    }
}
