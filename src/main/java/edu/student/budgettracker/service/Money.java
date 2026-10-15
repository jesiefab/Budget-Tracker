package edu.student.budgettracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers that keep calculations in {@link BigDecimal} rather than
 * {@code double} (which produces the rounding drift the audit flagged).
 *
 * <p>All methods are deterministic and the constants are expressed in
 * centavos-scale to make the arithmetic exact.</p>
 */
public final class Money {

    private static final BigDecimal CENTAVOS_SCALE = BigDecimal.valueOf(100);
    public static final BigDecimal ZERO            = BigDecimal.ZERO;
    public static final BigDecimal ONE             = BigDecimal.ONE;

    private Money() {}

    /** Return the amount unchanged (trimmed of trailing zeros). */
    public static BigDecimal from(String s) {
        if (s == null || s.isBlank()) return ZERO;
        return new BigDecimal(s).stripTrailingZeros();
    }

    public static BigDecimal from(double d) {
        return BigDecimal.valueOf(d).stripTrailingZeros();
    }

    /** Add two amounts. */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b == null ? ZERO : b);
    }

    /** Subtract. */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b == null ? ZERO : b);
    }

    /** Multiply by an integer count. */
    public static BigDecimal multiply(BigDecimal a, int n) {
        return a.multiply(ONE.multiply(BigDecimal.valueOf(n)));
    }

    /** Divide, rounding to 2 decimal places in centavos. */
    public static BigDecimal divide(BigDecimal a, BigDecimal b, int scale) {
        if (b == null || b.signum() == 0) return ZERO;
        return a.divide(b, scale, RoundingMode.HALF_UP);
    }

    /** Return the amount truncated to whole centavos (no fractional cents).
     *
     * <p>{@code 66.67} -> {@code 6667} centavos.</p>
     */
    public static BigDecimal truncateToCents(BigDecimal a) {
        return a.multiply(CENTAVOS_SCALE).setScale(0, RoundingMode.DOWN);
    }

    /** Format for display, preserving the exact value, not a rounded double. */
    public static String format(BigDecimal amount) {
        if (amount == null) return "\u20B10.00";
        return "\u20B1" + amount.toPlainString();
    }

    /** Return the amount as whole centavos (safe for comparison). */
    public static long asCents(BigDecimal amount) {
        return amount.multiply(CENTAVOS_SCALE).longValueExact();
    }

    /** Compare two amounts exactly. */
    public static int compare(BigDecimal a, BigDecimal b) {
        return a.compareTo(b == null ? ZERO : b);
    }

    public static boolean isPositive(BigDecimal a) {
        return a != null && a.signum() > 0;
    }
}
