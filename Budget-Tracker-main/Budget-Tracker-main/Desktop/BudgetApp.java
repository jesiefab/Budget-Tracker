import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * BudgetApp — root application window (JFrame).
 *
 * Builds the top-level layout:
 *   - Custom nav bar: logo | app title | rounded tab buttons | gear icon (Settings)
 *   - CardLayout center: swaps panels when nav buttons are clicked
 *
 * The old JTabbedPane is replaced by a hand-rolled nav bar so we get
 * full control over styling (rounded buttons, header integration, gear icon).
 * Save-on-close is still handled by the WindowListener.
 */
public class BudgetApp extends JFrame {

    // ── Panel card keys ────────────────────────────────────────────────────────
    private static final String CARD_EXPENSES = "EXPENSES";
    private static final String CARD_BUDGET   = "BUDGET";
    private static final String CARD_SAVINGS  = "SAVINGS";
    private static final String CARD_HISTORY  = "HISTORY";
    private static final String CARD_SETTINGS = "SETTINGS";

    // ── Nav state ──────────────────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private String     activeCard = CARD_EXPENSES;

    // Nav buttons (kept as fields so we can toggle their active style)
    private JButton expensesBtn;
    private JButton budgetBtn;
    private JButton savingsBtn;
    private JButton historyBtn;
    private JButton gearBtn;

    // ── Constructor ────────────────────────────────────────────────────────────

    public BudgetApp() {
        setTitle("Budget Tracker");
        setMinimumSize(new Dimension(900, 650));
        setPreferredSize(new Dimension(1050, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);
        setLayout(new BorderLayout());

        add(buildNavBar(),   BorderLayout.NORTH);
        add(buildCards(),    BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SaveManager.save();
                dispose();
                System.exit(0);
            }
        });

        pack();
    }

    // ── Nav bar ────────────────────────────────────────────────────────────────

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.HEADER_BG);
        bar.setBorder(new EmptyBorder(10, 20, 10, 20));

        // LEFT — logo + app title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setBackground(UITheme.HEADER_BG);

        JLabel logo = new JLabel("📚");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));

        JLabel title = new JLabel("Budget Tracker");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 20));
        title.setForeground(UITheme.ACCENT);

        brand.add(logo);
        brand.add(title);

        // CENTER — tab nav buttons
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        navButtons.setBackground(UITheme.HEADER_BG);

        expensesBtn = buildNavBtn("💸 Expenses", CARD_EXPENSES);
        budgetBtn   = buildNavBtn("💰 Budget",   CARD_BUDGET);
        savingsBtn  = buildNavBtn("🎯 Savings",  CARD_SAVINGS);
        historyBtn  = buildNavBtn("📊 History",  CARD_HISTORY);

        navButtons.add(expensesBtn);
        navButtons.add(budgetBtn);
        navButtons.add(savingsBtn);
        navButtons.add(historyBtn);

        // RIGHT — gear icon for Settings
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(UITheme.HEADER_BG);

        gearBtn = buildGearBtn();
        rightPanel.add(gearBtn);

        bar.add(brand,      BorderLayout.WEST);
        bar.add(navButtons, BorderLayout.CENTER);
        bar.add(rightPanel, BorderLayout.EAST);

        // Highlight the default active tab
        setActiveNav(CARD_EXPENSES);

        return bar;
    }

    /**
     * Builds a rounded nav button that swaps to the given card when clicked
     * and updates the active highlight.
     */
    private JButton buildNavBtn(String label, String cardKey) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getBackground();
                if      (getModel().isPressed())  g2.setColor(bg.darker());
                else if (getModel().isRollover()) g2.setColor(bg.brighter());
                else                              g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.HEADER_BG);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.addActionListener(e -> navigateTo(cardKey));
        return btn;
    }

    /** Builds the ⚙ gear button — same rounded style, sits on the far right. */
    private JButton buildGearBtn() {
        JButton btn = new JButton("⚙️") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getBackground();
                if      (getModel().isPressed())  g2.setColor(bg.darker());
                else if (getModel().isRollover()) g2.setColor(bg.brighter());
                else                              g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.HEADER_BG);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setToolTipText("Settings");
        btn.addActionListener(e -> navigateTo(CARD_SETTINGS));
        return btn;
    }

    // ── Card panel ─────────────────────────────────────────────────────────────

    private JPanel buildCards() {
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG);

        cardPanel.add(new ExpensePanel(), CARD_EXPENSES);
        cardPanel.add(new BudgetPanel(),  CARD_BUDGET);
        cardPanel.add(new SavingsPanel(), CARD_SAVINGS);
        cardPanel.add(new HistoryPanel(), CARD_HISTORY);
        cardPanel.add(new SettingsPanel(),CARD_SETTINGS);

        return cardPanel;
    }

    // ── Navigation logic ───────────────────────────────────────────────────────

    private void navigateTo(String cardKey) {
        activeCard = cardKey;
        cardLayout.show(cardPanel, cardKey);
        setActiveNav(cardKey);
    }

    /**
     * Updates button visual state: active tab gets ACCENT background + white text,
     * all others revert to the muted HEADER_BG style.
     */
    private void setActiveNav(String cardKey) {
        JButton[] navBtns = {expensesBtn, budgetBtn, savingsBtn, historyBtn};
        String[]  keys    = {CARD_EXPENSES, CARD_BUDGET, CARD_SAVINGS, CARD_HISTORY};

        for (int i = 0; i < navBtns.length; i++) {
            boolean active = keys[i].equals(cardKey);
            navBtns[i].setBackground(active ? UITheme.ACCENT    : UITheme.HEADER_BG);
            navBtns[i].setForeground(active ? Color.WHITE        : UITheme.TEXT_SECONDARY);
        }

        // Gear button highlights when Settings is active
        boolean gearActive = CARD_SETTINGS.equals(cardKey);
        gearBtn.setBackground(gearActive ? UITheme.ACCENT : UITheme.HEADER_BG);
        gearBtn.setForeground(gearActive ? Color.WHITE    : UITheme.TEXT_SECONDARY);
    }
}