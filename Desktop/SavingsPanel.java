import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * SavingsPanel — Savings tab UI.
 *
 * Layout (matches wireframe):
 *   NORTH  — XP bar panel, full width, spans both columns
 *   CENTER — horizontal split (left | right):
 *       LEFT  (scrollable, ~40% width)
 *           stacked info cards: Set Goal, Add/Withdraw, Savings Progress, Level Badge
 *       RIGHT (fills rest)
 *           Add Goal form at the top
 *           Scrollable goal cards list below
 */
public class SavingsPanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();
    private int lastLevel = 1;

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField goalField;
    private JTextField depositField;
    private JTextField withdrawField;
    private JTextField goalNameField;
    private JTextField goalTargetField;

    // ── Progress display ───────────────────────────────────────────────────────
    private JProgressBar savingsBar;
    private JLabel       goalDisplayLabel;
    private JLabel       motivationLabel;
    private JLabel       savedLabel;
    private JLabel       remainingLabel;

    // ── Level badge ────────────────────────────────────────────────────────────
    private JPanel levelBadge;
    private JLabel levelLabel;
    private JLabel titleLabel;
    private JLabel xpInfoLabel;

    // ── XP bar (full-width, top) ───────────────────────────────────────────────
    private JProgressBar xpBar;

    // ── Goals list panel (rebuilt on refresh) ─────────────────────────────────
    private JPanel goalsListPanel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public SavingsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // XP bar spans full width at the very top
        add(buildXPPanel(),   BorderLayout.NORTH);

        // Two-column body fills the rest
        add(buildBodySplit(), BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    // ── NORTH: full-width XP bar ───────────────────────────────────────────────

    private JPanel buildXPPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(UITheme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel xpTitle = new JLabel("\u26A1  Experience Points");
        xpTitle.setFont(UITheme.HEADER_FONT);
        xpTitle.setForeground(UITheme.WARNING);
        panel.add(xpTitle, BorderLayout.NORTH);

        xpBar = new JProgressBar(0, 100);
        xpBar.setStringPainted(true);
        xpBar.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        xpBar.setBackground(UITheme.BG);
        xpBar.setForeground(UITheme.WARNING);
        xpBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(xpBar, BorderLayout.CENTER);

        JLabel hint = new JLabel("  +1 XP per \u20B110 saved  \u2022  Minimum +5 XP per deposit");
        hint.setFont(UITheme.SYMBOL_SMALL_FONT);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    // ── CENTER: left scrollable column | right goals column ───────────────────

    private JSplitPane buildBodySplit() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildLeftColumn(),
                buildRightColumn()
        );
        split.setDividerLocation(360);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setBackground(UITheme.BG);
        split.setResizeWeight(0.38);
        return split;
    }

    // ── LEFT: scrollable info islands ─────────────────────────────────────────

    private JScrollPane buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(UITheme.BG);
        col.setBorder(new EmptyBorder(0, 0, 0, 6));

        JPanel setGoalCard  = buildSetGoalCard();
        JPanel depositCard  = buildDepositCard();
        JPanel progressCard = buildProgressCard();
        levelBadge          = buildLevelBadge();

        for (JPanel card : new JPanel[]{setGoalCard, depositCard, progressCard, levelBadge}) {
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(card);
            col.add(Box.createVerticalStrut(10));
        }
        col.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(col);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    // ── RIGHT: Add Goal form + goal list ──────────────────────────────────────

    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setBackground(UITheme.BG);
        col.setBorder(new EmptyBorder(0, 6, 0, 0));

        JPanel northWrapper = new JPanel(new BorderLayout(0, 8));
        northWrapper.setBackground(UITheme.BG);

        JLabel sectionTitle = new JLabel("\uD83C\uDFAF  Savings Goals");
        sectionTitle.setFont(UITheme.HEADER_FONT);
        sectionTitle.setForeground(UITheme.TEXT_PRIMARY);
        northWrapper.add(sectionTitle,      BorderLayout.NORTH);
        northWrapper.add(buildAddGoalForm(), BorderLayout.CENTER);

        col.add(northWrapper, BorderLayout.NORTH);

        goalsListPanel = new JPanel();
        goalsListPanel.setLayout(new BoxLayout(goalsListPanel, BoxLayout.Y_AXIS));
        goalsListPanel.setBackground(UITheme.BG);

        JScrollPane goalsScroll = new JScrollPane(goalsListPanel);
        goalsScroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        goalsScroll.getViewport().setBackground(UITheme.BG);
        goalsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        col.add(goalsScroll, BorderLayout.CENTER);

        return col;
    }

    // ── Info card builders ─────────────────────────────────────────────────────

    private JPanel buildSetGoalCard() {
        JPanel card = buildCard(UITheme.ACCENT);
        GridBagConstraints gbc = cardGbc();

        JLabel t = new JLabel("\uD83C\uDFAF  Set Savings Goal");
        t.setFont(UITheme.HEADER_FONT);
        t.setForeground(UITheme.ACCENT);
        gbc.gridwidth = 2;
        card.add(t, gbc);

        gbc.gridwidth = 1; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridx = 0; gbc.weightx = 0;
        card.add(UITheme.SymbolLabel("Goal Amount (\u20B1):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        goalField = UITheme.textField("1000.00");
        card.add(goalField, gbc);

        gbc.gridx = 0; gbc.gridwidth = 2;
        JButton setGoalBtn = UITheme.accentButton("Set Goal");
        setGoalBtn.addActionListener(e -> applyGoal());
        card.add(setGoalBtn, gbc);

        return card;
    }

    private JPanel buildDepositCard() {
        JPanel card = buildCard(UITheme.SUCCESS);
        GridBagConstraints gbc = cardGbc();

        JLabel t = new JLabel("\uD83D\uDCB5  Add / Withdraw Savings");
        t.setFont(UITheme.HEADER_FONT);
        t.setForeground(UITheme.SUCCESS);
        gbc.gridwidth = 2;
        card.add(t, gbc);

        gbc.gridwidth = 1; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridx = 0; gbc.weightx = 0;
        card.add(UITheme.SymbolLabel("Amount to Save (\u20B1):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        depositField = UITheme.textField("0.00");
        card.add(depositField, gbc);

        gbc.gridx = 0; gbc.gridwidth = 2;
        JButton depositBtn = UITheme.successButton("Add Savings  +XP");
        depositBtn.addActionListener(e -> deposit());
        card.add(depositBtn, gbc);

        gbc.gridwidth = 2;
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER);
        card.add(sep, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.weightx = 0;
        card.add(UITheme.SymbolLabel("Amount to Withdraw (\u20B1):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        withdrawField = UITheme.textField("0.00");
        card.add(withdrawField, gbc);

        gbc.gridx = 0; gbc.gridwidth = 2;
        JButton withdrawBtn = UITheme.dangerButton("Withdraw  \u22121 XP");
        withdrawBtn.addActionListener(e -> withdraw());
        card.add(withdrawBtn, gbc);

        return card;
    }

    private JPanel buildProgressCard() {
        JPanel card = buildCard(UITheme.BORDER);
        GridBagConstraints gbc = cardGbc();

        JLabel t = new JLabel("\uD83D\uDCC8  Savings Progress");
        t.setFont(UITheme.HEADER_FONT);
        t.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridwidth = 2;
        card.add(t, gbc);

        gbc.gridy = GridBagConstraints.RELATIVE;
        goalDisplayLabel = new JLabel("Goal: \u20B11,000.00");
        goalDisplayLabel.setFont(UITheme.PESO_FONT);
        goalDisplayLabel.setForeground(UITheme.TEXT_SECONDARY);
        card.add(goalDisplayLabel, gbc);

        savingsBar = new JProgressBar(0, 100);
        savingsBar.setStringPainted(true);
        savingsBar.setFont(UITheme.BODY_FONT);
        savingsBar.setPreferredSize(new Dimension(300, 26));
        savingsBar.setBackground(UITheme.BG);
        savingsBar.setForeground(UITheme.SUCCESS);
        savingsBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        card.add(savingsBar, gbc);

        motivationLabel = new JLabel(" ");
        motivationLabel.setFont(UITheme.BODY_FONT);
        motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        card.add(motivationLabel, gbc);

        JPanel statsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        statsRow.setBackground(UITheme.CARD);
        savedLabel     = statLabel(UITheme.SUCCESS, "Saved");
        remainingLabel = statLabel(UITheme.WARNING, "Still Needed");
        statsRow.add(savedLabel);
        statsRow.add(remainingLabel);
        card.add(statsRow, gbc);

        return card;
    }

    private JPanel buildLevelBadge() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx  = 0;
        gbc.gridy  = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.NONE;

        JLabel badgeTitle = new JLabel("\uD83C\uDFC6  Saver Rank");
        badgeTitle.setFont(UITheme.HEADER_FONT);
        badgeTitle.setForeground(UITheme.ACCENT);
        card.add(badgeTitle, gbc);

        levelLabel = new JLabel("LEVEL 1");
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        levelLabel.setForeground(Color.WHITE);
        card.add(levelLabel, gbc);

        titleLabel = new JLabel(DataStore.TITLES[1]);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UITheme.WARNING);
        card.add(titleLabel, gbc);

        JSeparator sepLine = new JSeparator();
        sepLine.setForeground(UITheme.BORDER);
        sepLine.setPreferredSize(new Dimension(200, 1));
        card.add(sepLine, gbc);

        xpInfoLabel = new JLabel("0 / 100 XP to next level");
        xpInfoLabel.setFont(UITheme.SMALL_FONT);
        xpInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        card.add(xpInfoLabel, gbc);

        int nextIdx = Math.min(2, DataStore.TITLES.length - 1);
        JLabel nextTitle = new JLabel("Next: " + DataStore.TITLES[nextIdx]);
        nextTitle.setFont(UITheme.SMALL_FONT);
        nextTitle.setForeground(UITheme.TEXT_SECONDARY);
        card.add(nextTitle, gbc);

        return card;
    }

    // ── Add Goal form ──────────────────────────────────────────────────────────

    private JPanel buildAddGoalForm() {
        JPanel addForm = new JPanel(new GridBagLayout());
        addForm.setBackground(UITheme.CARD);
        addForm.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.weightx = 0;
        addForm.add(UITheme.label("Goal Name:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        goalNameField = UITheme.textField("e.g. New Phone");
        addForm.add(goalNameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        addForm.add(UITheme.SymbolLabel("Target (\u20B1):"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        goalTargetField = UITheme.textField("0.00");
        addForm.add(goalTargetField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        JButton addGoalBtn = UITheme.accentButton("Add Goal");
        addGoalBtn.addActionListener(e -> addGoal());
        addForm.add(addGoalBtn, gbc);

        return addForm;
    }

    // ── Goal card builder ──────────────────────────────────────────────────────

    private JPanel buildGoalCard(DataStore.SavingsGoal goal, int index) {
        double  saved     = store.getCurrentSavings();
        double  target    = goal.getTargetAmount();
        int     pct       = target > 0 ? (int) Math.min((saved / target) * 100, 100) : 0;
        boolean canAfford = saved >= target;
        boolean achieved  = goal.isAchieved();

        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(achieved ? UITheme.SUCCESS : (canAfford ? UITheme.WARNING : UITheme.BORDER), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(UITheme.CARD);

        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setBackground(UITheme.CARD);

        JLabel nameLbl = new JLabel(goal.getName());
        nameLbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        nameLbl.setForeground(achieved ? UITheme.SUCCESS : UITheme.TEXT_PRIMARY);
        nameRow.add(nameLbl, BorderLayout.WEST);

        JLabel targetLbl = new JLabel(String.format("Target: \u20B1%.2f", target));
        targetLbl.setFont(UITheme.PESO_FONT);
        targetLbl.setForeground(UITheme.TEXT_SECONDARY);
        nameRow.add(targetLbl, BorderLayout.EAST);

        leftPanel.add(nameRow, BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(achieved ? 100 : pct);
        bar.setStringPainted(true);
        bar.setFont(UITheme.SMALL_FONT);
        bar.setBackground(UITheme.BG);
        bar.setForeground(achieved ? UITheme.SUCCESS : (canAfford ? UITheme.WARNING : UITheme.ACCENT));
        bar.setBorder(new LineBorder(UITheme.BORDER, 1));
        bar.setPreferredSize(new Dimension(0, 22));
        if (achieved) {
            bar.setString("\u2705 Goal Achieved!");
        } else {
            bar.setString(String.format("%d%%  —  \u20B1%.2f saved of \u20B1%.2f",
                    pct, Math.min(saved, target), target));
        }
        leftPanel.add(bar, BorderLayout.CENTER);
        card.add(leftPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(UITheme.CARD);

        if (achieved) {
            JLabel achievedLbl = new JLabel("\u2705 Achieved");
            achievedLbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
            achievedLbl.setForeground(UITheme.SUCCESS);
            achievedLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnPanel.add(achievedLbl);
            btnPanel.add(Box.createVerticalStrut(6));
        } else if (canAfford) {
            JButton claimBtn = UITheme.successButton("\u2714 Claim");
            claimBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            claimBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        String.format("Claim \"%s\"? This will deduct \u20B1%.2f from your savings.",
                                goal.getName(), target),
                        "Confirm Claim", JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) store.claimSavingsGoal(index);
            });
            btnPanel.add(claimBtn);
            btnPanel.add(Box.createVerticalStrut(6));
        }

        JButton removeBtn = UITheme.dangerButton("\uD83D\uDDD1 Remove");
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove \"" + goal.getName() + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) store.removeSavingsGoal(index);
        });
        btnPanel.add(removeBtn);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private void applyGoal() {
        try {
            double goal = Double.parseDouble(goalField.getText().trim());
            if (goal <= 0) throw new NumberFormatException();
            store.setSavingsGoal(goal);
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid savings goal.");
        }
    }

    private void deposit() {
        try {
            double amount = Double.parseDouble(depositField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
            int prevLevel = store.getLevel();
            int xpEarned  = store.calcXPForAmount(amount);
            store.addSavings(amount);
            int newLevel = store.getLevel();
            depositField.setText("");
            showXPToast(xpEarned, newLevel > prevLevel, newLevel);
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid amount to save.");
        }
    }

    private void withdraw() {
        try {
            double amount = Double.parseDouble(withdrawField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
            if (amount > store.getCurrentSavings()) {
                UITheme.showError(this, String.format(
                        "You only have \u20B1%.2f saved. Can't withdraw more than that.",
                        store.getCurrentSavings()));
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Withdraw \u20B1%.2f from savings? This will deduct XP.", amount),
                    "Confirm Withdrawal", JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
            int prevLevel = store.getLevel();
            store.withdrawSavings(amount);
            int newLevel = store.getLevel();
            withdrawField.setText("");
            if (newLevel < prevLevel) showWithdrawToast(true, prevLevel, newLevel);
            else showWithdrawToast(false, prevLevel, newLevel);
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid amount to withdraw.");
        }
    }

    private void addGoal() {
        String name       = goalNameField.getText().trim();
        String targetText = goalTargetField.getText().trim();
        if (name.isEmpty()) { UITheme.showError(this, "Enter a name for the goal."); return; }
        try {
            double target = Double.parseDouble(targetText);
            if (target <= 0) throw new NumberFormatException();
            store.addSavingsGoal(name, target);
            goalNameField.setText("");
            goalTargetField.setText("");
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid target amount.");
        }
    }

    // ── Toast helpers ──────────────────────────────────────────────────────────

    private void showXPToast(int xpEarned, boolean leveledUp, int newLevel) {
        JWindow toast   = new JWindow(SwingUtilities.getWindowAncestor(this));
        JPanel  content = new JPanel(new java.awt.FlowLayout());
        content.setBackground(leveledUp ? UITheme.WARNING : new Color(40, 40, 55));
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(leveledUp ? UITheme.WARNING : UITheme.ACCENT, 2, true),
                new EmptyBorder(10, 18, 10, 18)
        ));
        String msg = leveledUp
                ? "\u2B06 LEVEL UP! Now Level " + newLevel + " \u2014 " + store.getCurrentTitle()
                : "\u26A1 +" + xpEarned + " XP earned!";
        JLabel lbl = new JLabel(msg);
        lbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        lbl.setForeground(leveledUp ? UITheme.BG : UITheme.TEXT_PRIMARY);
        content.add(lbl);
        showToast(toast, content);
    }

    private void showWithdrawToast(boolean leveledDown, int prevLevel, int newLevel) {
        JWindow toast   = new JWindow(SwingUtilities.getWindowAncestor(this));
        JPanel  content = new JPanel(new java.awt.FlowLayout());
        content.setBackground(leveledDown ? UITheme.DANGER : new Color(40, 40, 55));
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(leveledDown ? UITheme.DANGER : UITheme.BORDER, 2, true),
                new EmptyBorder(10, 18, 10, 18)
        ));
        String msg = leveledDown
                ? "\u2B07 LEVEL DOWN! Back to Level " + newLevel + " \u2014 " + store.getCurrentTitle()
                : "\uD83D\uDCE4 Withdrawal recorded. XP deducted.";
        JLabel lbl = new JLabel(msg);
        lbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        content.add(lbl);
        showToast(toast, content);
    }

    private void showToast(JWindow toast, JPanel content) {
        toast.add(content);
        toast.pack();
        try {
            Point loc = getLocationOnScreen();
            toast.setLocation(loc.x + (getWidth() - toast.getWidth()) / 2, loc.y + 10);
        } catch (Exception ex) {
            toast.setLocationRelativeTo(null);
        }
        toast.setVisible(true);
        Timer timer = new Timer(2500, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        double goal   = store.getSavingsGoal();
        double saved  = store.getCurrentSavings();
        double needed = Math.max(goal - saved, 0);
        int    pct    = goal > 0 ? (int) Math.min((saved / goal) * 100, 100) : 0;

        goalDisplayLabel.setText(String.format("Goal: \u20B1%.2f", goal));
        goalField.setText(String.format("%.2f", goal));
        savingsBar.setValue(pct);
        savingsBar.setString(pct + "% of goal reached");

        if (pct >= 100) {
            motivationLabel.setText("\uD83C\uDF89 Goal reached! You actually did it. Nice.");
            motivationLabel.setForeground(UITheme.SUCCESS);
        } else if (pct >= 50) {
            motivationLabel.setText("\uD83D\uDCAA Halfway there! Keep going.");
            motivationLabel.setForeground(UITheme.ACCENT);
        } else if (pct > 0) {
            motivationLabel.setText("\uD83D\uDE80 Every peso counts. Don't stop.");
            motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        } else {
            motivationLabel.setText("Set a goal and start saving!");
            motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        }

        savedLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>Saved</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B1%.2f</b></center></html>", saved));
        remainingLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>Still Needed</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B1%.2f</b></center></html>", needed));

        int level     = store.getLevel();
        int currentXP = store.getCurrentLevelXP();
        int neededXP  = store.getXPForNextLevel();
        int xpPct     = neededXP > 0 ? (int)((currentXP / (double) neededXP) * 100) : 100;

        levelLabel.setText("LEVEL " + level);
        titleLabel.setText(store.getCurrentTitle());
        xpBar.setValue(Math.min(xpPct, 100));
        xpBar.setString(currentXP + " / " + neededXP + " XP");
        xpInfoLabel.setText(currentXP + " / " + neededXP + " XP  \u2022  Total: " + store.getTotalXP() + " XP");

        if (level != lastLevel) {
            lastLevel = level;
            animateLevelUp();
        }

        goalsListPanel.removeAll();
        List<DataStore.SavingsGoal> goals = store.getSavingsGoals();
        if (goals.isEmpty()) {
            JLabel emptyLbl = new JLabel("  No savings goals yet. Add one above!");
            emptyLbl.setFont(UITheme.SMALL_FONT);
            emptyLbl.setForeground(UITheme.TEXT_SECONDARY);
            emptyLbl.setBorder(new EmptyBorder(12, 8, 0, 0));
            goalsListPanel.add(emptyLbl);
        } else {
            for (int i = 0; i < goals.size(); i++) {
                goalsListPanel.add(buildGoalCard(goals.get(i), i));
                goalsListPanel.add(Box.createVerticalStrut(8));
            }
        }
        goalsListPanel.revalidate();
        goalsListPanel.repaint();
    }

    // ── Level-up flash animation ───────────────────────────────────────────────

    private void animateLevelUp() {
        final int[] count = {0};
        Timer flashTimer = new Timer(120, null);
        flashTimer.addActionListener(e -> {
            boolean on = count[0] % 2 == 0;
            levelBadge.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(on ? UITheme.WARNING : UITheme.ACCENT, on ? 3 : 1, true),
                    new EmptyBorder(16, 16, 16, 16)
            ));
            levelLabel.setForeground(on ? UITheme.WARNING : Color.WHITE);
            count[0]++;
            if (count[0] >= 8) {
                flashTimer.stop();
                levelBadge.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UITheme.ACCENT, 1, true),
                        new EmptyBorder(16, 16, 16, 16)
                ));
                levelLabel.setForeground(Color.WHITE);
            }
        });
        flashTimer.start();
    }

    // ── Card / layout helpers ──────────────────────────────────────────────────

    private JPanel buildCard(Color borderColor) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));
        return card;
    }

    private GridBagConstraints cardGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx   = 0;
        gbc.gridy   = GridBagConstraints.RELATIVE;
        return gbc;
    }

    private JLabel statLabel(Color color, String sub) {
        JLabel lbl = new JLabel(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>%s</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B10.00</b></center></html>",
                sub
        ));
        lbl.setForeground(color);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        lbl.setOpaque(true);
        lbl.setBackground(UITheme.CARD);
        return lbl;
    }
}