import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SettingsPanel — Settings tab UI.
 *
 * Layout (top to bottom):
 *   - Appearance card:  theme selector + Apply button
 *   - Admin Access card: password field + Unlock button + status label
 *   - Admin Tools section (hidden until unlocked):
 *       header row with Lock Admin button
 *       2x2 grid of reset action cards
 *       info footer
 *
 * The admin section is toggled via refreshAdminVisibility() after
 * successful authentication or explicit lock. The password is checked
 * in plain text against ADMIN_PASSWORD — this is a lightweight local
 * guard, not a security-critical mechanism.
 */
public class SettingsPanel extends JPanel {

    // ── Admin auth ─────────────────────────────────────────────────────────────
    private static final String ADMIN_PASSWORD = "Admin_Tools";
    private boolean adminUnlocked = false;

    // ── Auth controls ──────────────────────────────────────────────────────────
    private JPanel         adminSection;
    private JLabel         lockStatusLabel;
    private JPasswordField passwordField;
    private JButton        unlockBtn;

    // ── Constructor ────────────────────────────────────────────────────────────

    public SettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(UITheme.BG);
        topSection.add(buildThemePanel());
        topSection.add(Box.createVerticalStrut(12));
        topSection.add(buildAuthPanel());

        add(topSection,       BorderLayout.NORTH);
        add(buildAdminPanel(),BorderLayout.CENTER);

        refreshAdminVisibility();
    }

    // ── Appearance card ────────────────────────────────────────────────────────

    private JPanel buildThemePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(UITheme.accentCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        JLabel title = new JLabel("\uD83C\uDFA8  Appearance");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        panel.add(title, gbc);

        // Description
        gbc.gridy = 1;
        JLabel desc = new JLabel("Change the app color theme. Applies instantly and saves with your data.");
        desc.setFont(UITheme.BODY_FONT);
        desc.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(desc, gbc);

        // Theme selector row
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        gbc.gridx = 0;
        panel.add(UITheme.label("Theme:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<UITheme.Theme> themeCombo = new JComboBox<>(UITheme.Theme.values());
        themeCombo.setSelectedItem(UITheme.getCurrentTheme());
        UITheme.styleCombo(themeCombo);
        panel.add(themeCombo, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton applyBtn = UITheme.accentButton("Apply");
        applyBtn.addActionListener(e -> {
            UITheme.Theme selected = (UITheme.Theme) themeCombo.getSelectedItem();
            if (selected == null) return;
            UITheme.applyTheme(selected);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) UITheme.applyThemeToWindow(window);
            DataStore.getInstance().forceRefresh();
            SaveManager.save();
        });
        panel.add(applyBtn, gbc);

        return panel;
    }

    // ── Admin access card ──────────────────────────────────────────────────────

    private JPanel buildAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(UITheme.borderCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        JLabel title = new JLabel("[ SETTINGS ]  Admin Access");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        panel.add(title, gbc);

        // Description
        gbc.gridy = 1;
        JLabel desc = new JLabel("Enter the admin password to unlock developer tools.");
        desc.setFont(UITheme.BODY_FONT);
        desc.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(desc, gbc);

        // Password row
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        gbc.gridx = 0;
        panel.add(UITheme.label("Password:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        passwordField.setFont(UITheme.BODY_FONT);
        passwordField.setBackground(UITheme.INPUT);
        passwordField.setForeground(UITheme.TEXT_PRIMARY);
        passwordField.setCaretColor(UITheme.TEXT_PRIMARY);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        passwordField.addActionListener(e -> attemptUnlock()); // Enter key submits
        panel.add(passwordField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        unlockBtn = UITheme.accentButton("Unlock");
        unlockBtn.addActionListener(e -> attemptUnlock());
        panel.add(unlockBtn, gbc);

        // Status label
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 3;
        lockStatusLabel = new JLabel("  Status: Locked");
        lockStatusLabel.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        lockStatusLabel.setForeground(UITheme.DANGER);
        panel.add(lockStatusLabel, gbc);

        return panel;
    }

    // ── Admin tools section ────────────────────────────────────────────────────

    private JPanel buildAdminPanel() {
        adminSection = new JPanel();
        adminSection.setLayout(new BoxLayout(adminSection, BoxLayout.Y_AXIS));
        adminSection.setBackground(UITheme.BG);

        // Header row — title + lock button
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(UITheme.BG);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel adminTitle = new JLabel("[ ADMIN ]  Developer Tools");
        adminTitle.setFont(UITheme.HEADER_FONT);
        adminTitle.setForeground(UITheme.WARNING);
        headerRow.add(adminTitle, BorderLayout.WEST);

        JButton lockBtn = UITheme.dangerButton("Lock Admin");
        lockBtn.addActionListener(e -> lockAdmin());
        headerRow.add(lockBtn, BorderLayout.EAST);

        adminSection.add(headerRow);
        adminSection.add(Box.createVerticalStrut(12));

        // 2x2 reset card grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBackground(UITheme.BG);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        grid.add(buildResetCard(
                "Reset Savings Progress",
                "Clears current savings amount and resets goal back to P1,000.00.",
                UITheme.WARNING, "Reset Savings",
                () -> confirmAndRun("Reset savings to zero and goal to P1,000.00?", () -> {
                    DataStore.getInstance().adminResetSavings();
                    SaveManager.save();
                    flash("Savings reset.");
                })
        ));

        grid.add(buildResetCard(
                "Reset XP & Level",
                "Wipes all XP and sets level back to 1 (Broke Boy). Savings amount is kept.",
                UITheme.WARNING, "Reset XP",
                () -> confirmAndRun("Reset all XP and level back to 1?", () -> {
                    DataStore.getInstance().adminResetXP();
                    SaveManager.save();
                    flash("XP reset.");
                })
        ));

        grid.add(buildResetCard(
                "Reset Savings + XP",
                "Full savings tab wipe \u2014 clears savings, goal, XP, and level all at once.",
                UITheme.DANGER, "Full Savings Reset",
                () -> confirmAndRun("Full reset: savings, goal, XP, and level will all be cleared. Sure?", () -> {
                    DataStore.getInstance().adminResetSavings();
                    DataStore.getInstance().adminResetXP();
                    SaveManager.save();
                    flash("Full savings reset done.");
                })
        ));

        grid.add(buildResetCard(
                "Reset Everything",
                "Nuclear option. Wipes all expenses, budget, savings, XP, and level.",
                UITheme.DANGER, "FULL APP RESET",
                () -> confirmAndRun(
                        "This will wipe ALL data \u2014 expenses, budget, savings, and XP. No undo. Really?",
                        () -> {
                            DataStore store = DataStore.getInstance();
                            store.adminResetExpenses();
                            store.adminResetBudget();
                            store.adminResetSavings();
                            store.adminResetXP();
                            SaveManager.save();
                            flash("Full app reset done. Fresh slate.");
                        }
                )
        ));

        adminSection.add(grid);
        adminSection.add(Box.createVerticalStrut(16));

        // Footer hint
        JLabel footer = new JLabel("  All resets auto-save immediately.");
        footer.setFont(UITheme.SMALL_FONT);
        footer.setForeground(UITheme.TEXT_SECONDARY);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        adminSection.add(footer);

        JScrollPane scroll = new JScrollPane(adminSection);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(new EmptyBorder(14, 0, 0, 0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildResetCard(String title, String desc, Color accentColor,
                                  String btnLabel, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accentColor, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        titleLbl.setForeground(accentColor);

        JLabel descLbl = new JLabel("<html><body style='width:180px'>" + desc + "</body></html>");
        descLbl.setFont(UITheme.SMALL_FONT);
        descLbl.setForeground(UITheme.TEXT_SECONDARY);

        JButton btn = accentColor.equals(UITheme.DANGER)
                ? UITheme.dangerButton(btnLabel)
                : UITheme.buildWarningButton(btnLabel);
        btn.addActionListener(e -> action.run());
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBackground(UITheme.CARD);
        top.add(titleLbl, BorderLayout.NORTH);
        top.add(descLbl,  BorderLayout.CENTER);

        card.add(top, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    // ── Auth logic ─────────────────────────────────────────────────────────────

    private void attemptUnlock() {
        String entered = new String(passwordField.getPassword());
        if (ADMIN_PASSWORD.equals(entered)) {
            adminUnlocked = true;
            passwordField.setText("");
            lockStatusLabel.setText("  Status: Unlocked");
            lockStatusLabel.setForeground(UITheme.SUCCESS);
            unlockBtn.setEnabled(false);
            passwordField.setEnabled(false);
            refreshAdminVisibility();
        } else {
            passwordField.setText("");
            lockStatusLabel.setText("  Status: Wrong password.");
            lockStatusLabel.setForeground(UITheme.DANGER);
            shakeComponent(passwordField);
        }
    }

    private void lockAdmin() {
        adminUnlocked = false;
        lockStatusLabel.setText("  Status: Locked");
        lockStatusLabel.setForeground(UITheme.DANGER);
        unlockBtn.setEnabled(true);
        passwordField.setEnabled(true);
        passwordField.setText("");
        refreshAdminVisibility();
    }

    private void refreshAdminVisibility() {
        adminSection.setVisible(adminUnlocked);
    }

    // ── Helper dialogs ─────────────────────────────────────────────────────────

    private void confirmAndRun(String message, Runnable action) {
        int result = JOptionPane.showConfirmDialog(
                this, message, "Confirm Admin Action",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) action.run();
    }

    private void flash(String message) {
        JOptionPane.showMessageDialog(this, message, "Admin", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Password field shake animation ─────────────────────────────────────────

    private void shakeComponent(JComponent comp) {
        Point    origin  = comp.getLocation();
        int[]    offsets = {-8, 8, -6, 6, -4, 4, -2, 2, 0};
        final int[] tick = {0};
        Timer shaker = new Timer(30, null);
        shaker.addActionListener(e -> {
            if (tick[0] < offsets.length) {
                comp.setLocation(origin.x + offsets[tick[0]], origin.y);
                tick[0]++;
            } else {
                comp.setLocation(origin);
                shaker.stop();
            }
        });
        shaker.start();
    }
}