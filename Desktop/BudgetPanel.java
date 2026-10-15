import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * BudgetPanel — Budget tab UI.
 * New layout:
 *   TOP    — Budget Settings card (period | amount | needs % | Set Budget)
 *   CENTER — horizontal split:
 *       LEFT  (fixed 280px wide)
 *           - Pie chart panel (draws category spending as a donut chart)
 *           - Basic / Advanced mode toggle buttons below the chart
 *       RIGHT (fills rest)
 *           - Shared header labels (budget display + prorated info)
 *           - CardLayout switcher: BASIC view | ADVANCED view
 * The pie chart shows all-time category spending slices.
 * It updates via the same refresh() listener as the stat cards.
 *
 * <p>Project: Student Budget Tracker v3.0</p>
 * <p>Developed as an academic project</p>
 *
 * @author  John Erwin
 * @role    Budget tab UI, donut chart, Basic/Advanced mode, stat cards
 */
public class BudgetPanel extends JPanel {

    // ── Card keys ──────────────────────────────────────────────────────────────
    private static final String CARD_BASIC    = "BASIC";
    private static final String CARD_ADVANCED = "ADVANCED";

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField                        budgetField;
    private JTextField                        needsPercentField;
    private JComboBox<DataStore.BudgetPeriod> periodBox;
    private JLabel                            periodInfoLabel;
    private JLabel                            needsPercentLabel;
    private JButton                           basicBtn;
    private JButton                           advancedBtn;

    // ── Shared header display ──────────────────────────────────────────────────
    private JLabel budgetDisplayLabel;
    private JLabel proratedInfoLabel;

    // ── CardLayout container ───────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     cardPanel;

    // ── Basic mode components ──────────────────────────────────────────────────
    private JProgressBar basicProgressBar;
    private JLabel       basicStatusLabel;
    private JLabel       basicShouldveLabel;
    private JLabel       basicActuallyLabel;
    private JLabel       basicBufferLabel;
    private JLabel       basicSafeLabel;

    // ── Advanced mode — needs ──────────────────────────────────────────────────
    private JProgressBar needsProgressBar;
    private JLabel       needsStatusLabel;
    private JLabel       needsShouldveLabel;
    private JLabel       needsActuallyLabel;
    private JLabel       needsBufferLabel;
    private JLabel       needsSafeLabel;

    // ── Advanced mode — wants ──────────────────────────────────────────────────
    private JProgressBar wantsProgressBar;
    private JLabel       wantsStatusLabel;
    private JLabel       wantsShouldveLabel;
    private JLabel       wantsActuallyLabel;
    private JLabel       wantsBufferLabel;
    private JLabel       wantsSafeLabel;

    // ── Advanced total bar ─────────────────────────────────────────────────────
    private JProgressBar advTotalProgressBar;
    private JLabel       advTotalStatusLabel;

    // ── Pie chart panel (custom-drawn) ─────────────────────────────────────────
    private PieChartPanel pieChart;

    // ── Constructor ────────────────────────────────────────────────────────────

    public BudgetPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildSettingsCard(), BorderLayout.NORTH);
        add(buildMainArea(),     BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    // ── Budget settings card ───────────────────────────────────────────────────

    private JPanel buildSettingsCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(UITheme.accentCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("\uD83D\uDCB0  Budget Settings");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.weightx = 0;
        panel.add(title, gbc);

        // Row 1 — period | budget amount + set button
        gbc.gridwidth = 1; gbc.gridy = 1;

        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Budget Period:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        periodBox = new JComboBox<>(DataStore.BudgetPeriod.values());
        periodBox.setSelectedItem(store.getBudgetPeriod());
        UITheme.styleCombo(periodBox);
        panel.add(periodBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.SymbolLabel("Budget Amount (\u20B1):"), gbc);

        JPanel budgetInputPanel = new JPanel(new BorderLayout(6, 0));
        budgetInputPanel.setBackground(UITheme.CARD);
        budgetField = UITheme.textField("0.00");
        JButton setBtn = UITheme.accentButton("Set Budget");
        setBtn.addActionListener(e -> applyBudget());
        budgetInputPanel.add(budgetField, BorderLayout.CENTER);
        budgetInputPanel.add(setBtn,      BorderLayout.EAST);

        gbc.gridx = 3; gbc.weightx = 1.0;
        panel.add(budgetInputPanel, gbc);

        // Row 2 — needs % (advanced only)
        gbc.gridy = 2;

        gbc.gridx = 0; gbc.weightx = 0;
        needsPercentLabel = UITheme.label("Needs % (rest = Wants):");
        panel.add(needsPercentLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        needsPercentField = UITheme.textField("50");
        panel.add(needsPercentField, gbc);

        // Row 3 — period hint
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 4; gbc.weightx = 1.0;
        periodInfoLabel = new JLabel(" ");
        periodInfoLabel.setFont(UITheme.SMALL_FONT);
        periodInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(periodInfoLabel, gbc);

        periodBox.addActionListener(e -> updatePeriodHint());
        updatePeriodHint();

        return panel;
    }

    // ── Main area: LEFT pie chart | RIGHT stats ────────────────────────────────

    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(UITheme.BG);

        panel.add(buildChartSide(), BorderLayout.WEST);
        panel.add(buildStatsSide(), BorderLayout.CENTER);

        return panel;
    }

    // ── LEFT: Pie chart + mode toggle ─────────────────────────────────────────

    private JPanel buildChartSide() {
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBackground(UITheme.BG);
        side.setPreferredSize(new Dimension(280, 0));

        // Pie chart
        pieChart = new PieChartPanel();
        pieChart.setPreferredSize(new Dimension(280, 260));
        side.add(pieChart, BorderLayout.CENTER);

        // Basic / Advanced toggle buttons — below the chart
        JPanel toggleRow = new JPanel(new GridLayout(1, 2, 8, 0));
        toggleRow.setBackground(UITheme.BG);

        basicBtn    = UITheme.accentButton("Basic");
        advancedBtn = UITheme.accentButton("Advanced");
        basicBtn.addActionListener(e    -> switchMode(DataStore.TrackingMode.BASIC));
        advancedBtn.addActionListener(e -> switchMode(DataStore.TrackingMode.ADVANCED));
        toggleRow.add(basicBtn);
        toggleRow.add(advancedBtn);
        side.add(toggleRow, BorderLayout.SOUTH);

        return side;
    }

    // ── RIGHT: Shared header + CardLayout stats ────────────────────────────────

    private JPanel buildStatsSide() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(UITheme.BG);

        JPanel headerArea = new JPanel();
        headerArea.setLayout(new BoxLayout(headerArea, BoxLayout.Y_AXIS));
        headerArea.setBackground(UITheme.BG);
        headerArea.setBorder(new EmptyBorder(8, 0, 12, 0));

        budgetDisplayLabel = new JLabel(" ");
        budgetDisplayLabel.setFont(UITheme.PESO_FONT_BOLD);
        budgetDisplayLabel.setForeground(UITheme.TEXT_PRIMARY);
        budgetDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerArea.add(budgetDisplayLabel);

        headerArea.add(Box.createVerticalStrut(4));

        proratedInfoLabel = new JLabel(" ");
        proratedInfoLabel.setFont(UITheme.SMALL_FONT);
        proratedInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        proratedInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerArea.add(proratedInfoLabel);

        panel.add(headerArea, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG);
        cardPanel.add(buildBasicView(),    CARD_BASIC);
        cardPanel.add(buildAdvancedView(), CARD_ADVANCED);
        panel.add(cardPanel, BorderLayout.CENTER);

        return panel;
    }

    // ── Basic view ─────────────────────────────────────────────────────────────

    private JPanel buildBasicView() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UITheme.BG);

        JPanel progressPanel = new JPanel(new BorderLayout(0, 4));
        progressPanel.setBackground(UITheme.BG);

        basicProgressBar = new JProgressBar(0, 100);
        basicProgressBar.setStringPainted(true);
        basicProgressBar.setFont(UITheme.BODY_FONT);
        basicProgressBar.setPreferredSize(new Dimension(400, 26));
        basicProgressBar.setBackground(UITheme.CARD);
        basicProgressBar.setForeground(UITheme.SUCCESS);
        basicProgressBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        progressPanel.add(basicProgressBar, BorderLayout.CENTER);

        basicStatusLabel = new JLabel(" ");
        basicStatusLabel.setFont(UITheme.BODY_FONT);
        basicStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        basicStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressPanel.add(basicStatusLabel, BorderLayout.SOUTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(UITheme.BG);

        basicShouldveLabel = buildStatCard("Should've Spent", UITheme.ACCENT);
        basicActuallyLabel = buildStatCard("Actually Spent",  UITheme.DANGER);
        basicBufferLabel   = buildStatCard("Today's Buffer",  UITheme.SUCCESS);
        basicSafeLabel     = buildStatCard("Safe to Spend",   UITheme.WARNING);

        statsRow.add(buildStatWrapper(basicShouldveLabel));
        statsRow.add(buildStatWrapper(basicActuallyLabel));
        statsRow.add(buildStatWrapper(basicBufferLabel));
        statsRow.add(buildStatWrapper(basicSafeLabel));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setBackground(UITheme.BG);
        north.add(progressPanel, BorderLayout.NORTH);
        north.add(statsRow,      BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        return panel;
    }

    // ── Advanced view ──────────────────────────────────────────────────────────

    private JPanel buildAdvancedView() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG);

        JPanel totalProgressPanel = new JPanel(new BorderLayout(0, 4));
        totalProgressPanel.setBackground(UITheme.BG);

        advTotalProgressBar = new JProgressBar(0, 100);
        advTotalProgressBar.setStringPainted(true);
        advTotalProgressBar.setFont(UITheme.BODY_FONT);
        advTotalProgressBar.setPreferredSize(new Dimension(400, 26));
        advTotalProgressBar.setBackground(UITheme.CARD);
        advTotalProgressBar.setForeground(UITheme.SUCCESS);
        advTotalProgressBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        totalProgressPanel.add(advTotalProgressBar, BorderLayout.CENTER);

        advTotalStatusLabel = new JLabel(" ");
        advTotalStatusLabel.setFont(UITheme.SMALL_FONT);
        advTotalStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        advTotalStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalProgressPanel.add(advTotalStatusLabel, BorderLayout.SOUTH);

        JPanel splitRow = new JPanel(new GridLayout(1, 2, 12, 0));
        splitRow.setBackground(UITheme.BG);
        splitRow.add(buildPoolCard(true));
        splitRow.add(buildPoolCard(false));

        panel.add(totalProgressPanel, BorderLayout.NORTH);
        panel.add(splitRow,           BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPoolCard(boolean isNeeds) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(isNeeds ? UITheme.ACCENT : UITheme.WARNING, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel headerArea = new JPanel(new BorderLayout(0, 2));
        headerArea.setBackground(UITheme.CARD);

        JLabel sectionLabel = new JLabel(isNeeds ? "\uD83C\uDFE0  NEEDS" : "\uD83C\uDF89  WANTS");
        sectionLabel.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        sectionLabel.setForeground(isNeeds ? UITheme.ACCENT : UITheme.WARNING);
        headerArea.add(sectionLabel, BorderLayout.NORTH);

        JLabel hint = new JLabel(isNeeds
                ? "Food, Transport, School, Health"
                : "Entertainment, Shopping, Other");
        hint.setFont(UITheme.SMALL_FONT);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        headerArea.add(hint, BorderLayout.SOUTH);

        card.add(headerArea, BorderLayout.NORTH);

        JPanel barArea = new JPanel(new BorderLayout(0, 4));
        barArea.setBackground(UITheme.CARD);
        barArea.setBorder(new EmptyBorder(8, 0, 8, 0));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setFont(UITheme.SMALL_FONT);
        bar.setPreferredSize(new Dimension(0, 22));
        bar.setBackground(UITheme.BG);
        bar.setForeground(UITheme.SUCCESS);
        bar.setBorder(new LineBorder(UITheme.BORDER, 1));
        barArea.add(bar, BorderLayout.CENTER);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(UITheme.SMALL_FONT);
        statusLbl.setForeground(UITheme.TEXT_SECONDARY);
        barArea.add(statusLbl, BorderLayout.SOUTH);

        card.add(barArea, BorderLayout.CENTER);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 6, 0));
        statsRow.setBackground(UITheme.CARD);

        JLabel shouldveLbl = buildStatCard("Should've Spent", UITheme.ACCENT);
        JLabel actuallyLbl = buildStatCard("Actually Spent",  UITheme.DANGER);
        JLabel bufferLbl   = buildStatCard("Today's Buffer",  UITheme.SUCCESS);
        JLabel safeLbl     = buildStatCard("Safe to Spend",   UITheme.WARNING);

        statsRow.add(buildStatWrapper(shouldveLbl));
        statsRow.add(buildStatWrapper(actuallyLbl));
        statsRow.add(buildStatWrapper(bufferLbl));
        statsRow.add(buildStatWrapper(safeLbl));

        card.add(statsRow, BorderLayout.SOUTH);

        if (isNeeds) {
            needsProgressBar   = bar;       needsStatusLabel   = statusLbl;
            needsShouldveLabel = shouldveLbl; needsActuallyLabel = actuallyLbl;
            needsBufferLabel   = bufferLbl; needsSafeLabel     = safeLbl;
        } else {
            wantsProgressBar   = bar;       wantsStatusLabel   = statusLbl;
            wantsShouldveLabel = shouldveLbl; wantsActuallyLabel = actuallyLbl;
            wantsBufferLabel   = bufferLbl; wantsSafeLabel     = safeLbl;
        }

        return card;
    }

    // ── Stat card helpers ──────────────────────────────────────────────────────

    private JLabel buildStatCard(String title, Color color) {
        JLabel lbl = new JLabel(statHtml(title, "\u20B10.00"));
        lbl.setForeground(color);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel buildStatWrapper(JLabel label) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        wrapper.add(label, BorderLayout.CENTER);
        return wrapper;
    }

    private String statHtml(String title, String value) {
        return String.format(
                "<html><center>"
                        + "<span style='font-family:Arial;font-size:10px;'>%s</span>"
                        + "<br><b style='font-family:Arial;font-size:14px;'>%s</b>"
                        + "</center></html>",
                title, value
        );
    }

    // ── Mode switch ────────────────────────────────────────────────────────────

    private void switchMode(DataStore.TrackingMode mode) {
        store.setTrackingMode(mode);
    }

    private void applyModeUI(DataStore.TrackingMode mode) {
        boolean isAdvanced = mode == DataStore.TrackingMode.ADVANCED;
        needsPercentLabel.setVisible(isAdvanced);
        needsPercentField.setVisible(isAdvanced);
        basicBtn.setBackground(isAdvanced ? UITheme.BORDER : UITheme.ACCENT);
        advancedBtn.setBackground(isAdvanced ? UITheme.ACCENT : UITheme.BORDER);
        cardLayout.show(cardPanel, isAdvanced ? CARD_ADVANCED : CARD_BASIC);
    }

    // ── Period hint ────────────────────────────────────────────────────────────

    private void updatePeriodHint() {
        DataStore.BudgetPeriod p = (DataStore.BudgetPeriod) periodBox.getSelectedItem();
        if (p == null) return;
        periodInfoLabel.setText(String.format(
                "Day %d of %d in current %s  \u2014  budget resets to 0 on period change",
                p.daysElapsed(), p.days, p.displayName.toLowerCase()
        ));
    }

    // ── Budget apply action ────────────────────────────────────────────────────

    private void applyBudget() {
        try {
            double budget = Double.parseDouble(budgetField.getText().trim());
            if (budget <= 0) throw new NumberFormatException();

            // Capture needs % NOW — before any store call fires notifyListeners()
            // and refresh() overwrites the field with the old stored value.
            double needsPct = 50.0; // default, only used in Advanced mode
            if (store.getTrackingMode() == DataStore.TrackingMode.ADVANCED) {
                needsPct = Double.parseDouble(needsPercentField.getText().trim());
                if (needsPct < 0 || needsPct > 100) throw new NumberFormatException();
            }

            DataStore.BudgetPeriod selected = (DataStore.BudgetPeriod) periodBox.getSelectedItem();
            if (selected != store.getBudgetPeriod()) store.setBudgetPeriod(selected);
            store.setMonthlyBudget(budget);

            if (store.getTrackingMode() == DataStore.TrackingMode.ADVANCED) {
                store.setNeedsPercent(needsPct);
            }
        } catch (NumberFormatException ex) {
            UITheme.showError(this, store.getTrackingMode() == DataStore.TrackingMode.ADVANCED
                    ? "Enter a valid budget amount and needs % (0-100)."
                    : "Enter a valid budget amount.");
        }
    }

    // ── Progress bar color + status helper ────────────────────────────────────

    private void updateBarColor(JProgressBar bar, JLabel statusLbl, int percent, String poolName) {
        if (percent >= 100) {
            bar.setForeground(UITheme.DANGER);
            statusLbl.setText("\u26A0 Exceeded prorated " + poolName + " budget.");
            statusLbl.setForeground(UITheme.DANGER);
        } else if (percent >= 80) {
            bar.setForeground(UITheme.WARNING);
            statusLbl.setText("\u26A1 Getting close to the " + poolName + " prorated limit.");
            statusLbl.setForeground(UITheme.WARNING);
        } else {
            bar.setForeground(UITheme.SUCCESS);
            statusLbl.setText("\u2705 On track with " + poolName + " budget.");
            statusLbl.setForeground(UITheme.SUCCESS);
        }
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        DataStore.TrackingMode mode   = store.getTrackingMode();
        DataStore.BudgetPeriod period = store.getBudgetPeriod();
        double budget   = store.getMonthlyBudget();
        double prorated = store.getProratedBudget();
        double spent    = store.getTotalExpensesForCurrentPeriod();
        int    elapsed  = period.daysElapsed();

        applyModeUI(mode);

        periodBox.setSelectedItem(period);
        budgetField.setText(String.format("%.2f", budget));
        needsPercentField.setText(String.format("%.0f", store.getNeedsPercent()));

        if (mode == DataStore.TrackingMode.ADVANCED) {
            budgetDisplayLabel.setText(String.format(
                    "%s Budget: \u20B1%.2f  (Day %d of %d)  \u2014  Needs: %.0f%%  /  Wants: %.0f%%",
                    period.displayName, budget, elapsed, period.days,
                    store.getNeedsPercent(), store.getWantsPercent()
            ));
        } else {
            budgetDisplayLabel.setText(String.format(
                    "%s Budget: \u20B1%.2f  (Day %d of %d)",
                    period.displayName, budget, elapsed, period.days
            ));
        }

        double dailyRate = period.days > 0 ? budget / period.days : 0;
        proratedInfoLabel.setText(String.format(
                "Daily rate: \u20B1%.2f  \u2022  Prorated budget for today: \u20B1%.2f",
                dailyRate, prorated
        ));

        // Basic
        int basicPct = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        basicProgressBar.setValue(basicPct);
        basicProgressBar.setString(basicPct + "% of prorated budget used");
        updateBarColor(basicProgressBar, basicStatusLabel, basicPct, period.displayName.toLowerCase());
        basicShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", prorated)));
        basicActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", spent)));
        basicBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(store.getRemainingProrated(), 0))));
        basicSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getDailyAllowance())));

        // Advanced total
        int advTotalPct = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        advTotalProgressBar.setValue(advTotalPct);
        advTotalProgressBar.setString(advTotalPct + "% of total prorated budget used");
        updateBarColor(advTotalProgressBar, advTotalStatusLabel, advTotalPct,
                "total " + period.displayName.toLowerCase());

        // Needs
        double needsProrated = store.getNeedsProratedBudget();
        double needsSpent    = store.getNeedsSpentThisPeriod();
        double needsRemain   = store.getNeedsRemainingProrated();
        int    needsPct      = needsProrated > 0 ? (int) Math.min((needsSpent / needsProrated) * 100, 100) : 0;
        needsProgressBar.setValue(needsPct);
        needsProgressBar.setString(needsPct + "% of needs prorated budget used");
        updateBarColor(needsProgressBar, needsStatusLabel, needsPct, "needs");
        needsShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", needsProrated)));
        needsActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", needsSpent)));
        needsBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(needsRemain, 0))));
        needsSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getNeedsDailyAllowance())));

        // Wants
        double wantsProrated = store.getWantsProratedBudget();
        double wantsSpent    = store.getWantsSpentThisPeriod();
        double wantsRemain   = store.getWantsRemainingProrated();
        int    wantsPct      = wantsProrated > 0 ? (int) Math.min((wantsSpent / wantsProrated) * 100, 100) : 0;
        wantsProgressBar.setValue(wantsPct);
        wantsProgressBar.setString(wantsPct + "% of wants prorated budget used");
        updateBarColor(wantsProgressBar, wantsStatusLabel, wantsPct, "wants");
        wantsShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", wantsProrated)));
        wantsActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", wantsSpent)));
        wantsBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(wantsRemain, 0))));
        wantsSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getWantsDailyAllowance())));

        updatePeriodHint();

        // Update pie chart with category totals
        if (pieChart != null) {
            double[] values = new double[Expense.CATEGORIES.length];
            for (int i = 0; i < Expense.CATEGORIES.length; i++) {
                values[i] = store.getTotalByCategory(Expense.CATEGORIES[i]);
            }
            pieChart.setData(Expense.CATEGORIES, values);
        }
    }

    // ── Pie Chart Panel ────────────────────────────────────────────────────────

    /**
     * Custom-painted donut chart.
     * Shows spending per category as proportional arc slices.
     * Renders a legend below the donut.
     * Shows "No expenses yet" when all values are zero.
     */
    private static class PieChartPanel extends JPanel {

        private String[] labels = new String[0];
        private double[] values = new double[0];

        // Fixed palette aligned to HistoryPanel category colors
        private static final Color[] SLICE_COLORS = {
                new Color(255, 149,   0),  // Food
                new Color(  0, 122, 255),  // Transport
                new Color( 52, 199,  89),  // School
                new Color(175,  82, 222),  // Entertainment
                new Color(255,  59,  48),  // Health
                new Color(255, 204,   0),  // Shopping
                new Color(130, 130, 152),  // Other
        };

        public PieChartPanel() {
            setOpaque(false);
            setBackground(UITheme.BG);
        }

        public void setData(String[] labels, double[] values) {
            this.labels = labels;
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background card
            g2.setColor(UITheme.CARD);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(UITheme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // Title
            g2.setFont(UITheme.HEADER_FONT);
            g2.setColor(UITheme.TEXT_PRIMARY);
            String chartTitle = "Spending Breakdown";
            FontMetrics titleFm = g2.getFontMetrics();
            g2.drawString(chartTitle, (w - titleFm.stringWidth(chartTitle)) / 2, 22);

            double total = 0;
            for (double v : values) total += v;

            // Legend row height: ~14px per entry, 2 columns, reserve space at bottom
            int legendRows   = (int) Math.ceil(labels.length / 2.0);
            int legendHeight = legendRows * 16 + 8;
            int chartAreaH   = h - 30 - legendHeight - 10;

            int diameter  = Math.min(w - 32, chartAreaH);
            if (diameter < 20) { g2.dispose(); return; }

            int arcX = (w - diameter) / 2;
            int arcY = 30;
            int hole  = (int)(diameter * 0.42); // donut hole size

            if (total <= 0) {
                // Empty state — draw a gray ring
                g2.setColor(UITheme.BORDER);
                g2.setStroke(new BasicStroke(diameter * 0.3f));
                int cx = arcX + diameter / 2;
                int cy = arcY + diameter / 2;
                g2.drawOval(arcX + (int)(diameter * 0.15), arcY + (int)(diameter * 0.15),
                        (int)(diameter * 0.7), (int)(diameter * 0.7));
                g2.setFont(UITheme.SMALL_FONT);
                g2.setColor(UITheme.TEXT_SECONDARY);
                String msg = "No expenses yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + fm.getAscent() / 2);
                g2.dispose();
                return;
            }

            // Draw slices
            double startAngle = -90.0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                double sweep = (values[i] / total) * 360.0;
                g2.setColor(SLICE_COLORS[i % SLICE_COLORS.length]);
                g2.fill(new Arc2D.Double(arcX, arcY, diameter, diameter,
                        startAngle, sweep, Arc2D.PIE));
                startAngle += sweep;
            }

            // Donut hole
            g2.setColor(UITheme.CARD);
            int holeX = arcX + (diameter - hole) / 2;
            int holeY = arcY + (diameter - hole) / 2;
            g2.fillOval(holeX, holeY, hole, hole);

            // Center text — total amount
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(UITheme.TEXT_PRIMARY);
            String totalStr = String.format("\u20B1%.0f", total);
            FontMetrics fm = g2.getFontMetrics();
            int cx = arcX + diameter / 2;
            int cy = arcY + diameter / 2;
            g2.drawString(totalStr, cx - fm.stringWidth(totalStr) / 2, cy + fm.getAscent() / 2);

            // Legend — 2 column grid below the chart
            int legendY  = arcY + diameter + 14;
            int colWidth = w / 2;
            g2.setFont(UITheme.SMALL_FONT);
            FontMetrics lfm = g2.getFontMetrics();

            int col = 0, row = 0;
            for (int i = 0; i < labels.length; i++) {
                if (values[i] <= 0) { col++; if (col >= 2) { col = 0; row++; } continue; }
                int lx = col * colWidth + 10;
                int ly = legendY + row * 16;
                g2.setColor(SLICE_COLORS[i % SLICE_COLORS.length]);
                g2.fillRoundRect(lx, ly - 8, 10, 10, 3, 3);
                g2.setColor(UITheme.TEXT_SECONDARY);
                String entry = labels[i] + " " + (int)((values[i] / total) * 100) + "%";
                g2.drawString(entry, lx + 13, ly);
                col++;
                if (col >= 2) { col = 0; row++; }
            }

            g2.dispose();
        }
    }
}