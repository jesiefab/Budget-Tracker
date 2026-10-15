package edu.student.budgettracker.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A named savings goal with a target amount and an achieved flag.
 *
 * <p>Every goal has a stable {@link #id} (UUID) so that the UI can reference
 * it directly — no list-index removal, no fragile {@code filteredIndices}
 * mapping.</p>
 *
 * <p>The target amount keeps exactly the scale it was constructed with
 * ({@code 1500.00} stays {@code 1500.00}). It is never re-scaled, so the
 * record written by {@link #toString()} parses back through
 * {@link #fromString(String)} byte-for-byte.</p>
 */
public final class SavingsGoal {

    private final UUID id;
    private final String name;
    private final BigDecimal targetAmount;
    private       boolean achieved;

    public SavingsGoal(String name, BigDecimal targetAmount) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Goal name must not be blank");
        }
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException("Target amount must be positive");
        }
        this.id            = UUID.randomUUID();
        this.name          = name.trim();
        // Keep the caller's scale (no stripTrailingZeros) so saved goal
        // records round-trip exactly instead of losing ".00".
        this.targetAmount  = targetAmount;
        this.achieved      = false;
    }

    /** Restore constructor used by the persistence layer. */
    public SavingsGoal(UUID id, String name, BigDecimal targetAmount, boolean achieved) {
        if (id == null) throw new IllegalArgumentException("ID must not be null");
        this.id            = id;
        this.name          = (name == null) ? "Unnamed" : name.trim();
        this.targetAmount  = (targetAmount == null) ? BigDecimal.ZERO : targetAmount;
        this.achieved      = achieved;
    }

    public UUID getId()            { return id; }
    public String  getName()       { return name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public boolean isAchieved()    { return achieved; }
    public void setAchieved(boolean achieved) { this.achieved = achieved; }

    /** Progress percentage (0..100). */
    public int progress(double currentSavings) {
        if (targetAmount.signum() == 0) return 0;
        return (int) Math.min((currentSavings / targetAmount.doubleValue()) * 100, 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavingsGoal)) return false;
        return id.equals(((SavingsGoal) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return String.format("%s|%s|%s|%s", id, name, targetAmount.toPlainString(), achieved);
    }

    public static SavingsGoal fromString(String s) {
        if (s == null) return null;
        String[] parts = s.split("\\|", 4);
        if (parts.length != 4) return null;
        try {
            UUID id = UUID.fromString(parts[0].trim());
            String name = parts[1].trim();
            BigDecimal target = new BigDecimal(parts[2].trim());
            boolean achieved = Boolean.parseBoolean(parts[3].trim());
            return new SavingsGoal(id, name, target, achieved);
        } catch (Exception ex) {
            return null;
        }
    }
}
