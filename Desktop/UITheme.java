import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * UITheme — centralized styling and component factory for the Budget Tracker.
 *
 * Responsibilities:
 *   - Defines the Theme enum (Dark / Light / Midnight Blue / Forest Green / Warm Sunset)
 *   - Holds live color fields (BG, CARD, ACCENT, etc.) updated on theme change
 *   - applyTheme()        — sets color fields + syncs UIManager
 *   - applyThemeToWindow()— walks the component tree and repaints all colors live
 *   - Component factories — label(), textField(), styleCombo(), accentButton(), etc.
 *   - Live borders        — accentBorder() / borderBorder() read theme colors at paint time
 *
 * All color-bearing components created via the factory methods will respond
 * correctly to theme switches when applyThemeToWindow() is called.
 */
public class UITheme {

    // ── Theme enum ─────────────────────────────────────────────────────────────

    public enum Theme {
        DARK         ("Dark"),
        LIGHT        ("Light"),
        MIDNIGHT_BLUE("Midnight Blue"),
        FOREST_GREEN ("Forest Green"),
        WARM_SUNSET  ("Warm Sunset");

        public final String displayName;
        Theme(String displayName) { this.displayName = displayName; }

        @Override public String toString() { return displayName; }

        public static Theme fromString(String s) {
            for (Theme t : values()) if (t.displayName.equals(s)) return t;
            return DARK;
        }
    }

    private static Theme currentTheme = Theme.DARK;
    public  static Theme getCurrentTheme() { return currentTheme; }

    // ── Live color fields ──────────────────────────────────────────────────────

    public static Color BG;
    public static Color HEADER_BG;
    public static Color CARD;
    public static Color INPUT;
    public static Color ACCENT;
    public static Color SUCCESS;
    public static Color DANGER;
    public static Color WARNING;
    public static Color TEXT_PRIMARY;
    public static Color TEXT_SECONDARY;
    public static Color BORDER;

    static { applyTheme(Theme.DARK); } // initialize with default theme

    // ── Theme color tables ─────────────────────────────────────────────────────

    public static void applyTheme(Theme theme) {
        currentTheme = theme;
        switch (theme) {
            case LIGHT:
                BG             = new Color(238, 238, 245);
                HEADER_BG      = new Color(225, 225, 236);
                CARD           = new Color(250, 250, 254);
                INPUT          = new Color(228, 228, 238);
                ACCENT         = new Color( 90, 130, 190);
                SUCCESS        = new Color( 75, 158, 105);
                DANGER         = new Color(188,  75,  75);
                WARNING        = new Color(180, 130,  40);
                TEXT_PRIMARY   = new Color( 40,  40,  55);
                TEXT_SECONDARY = new Color(110, 110, 130);
                BORDER         = new Color(208, 208, 220);
                break;
            case MIDNIGHT_BLUE:
                BG             = new Color( 18,  22,  48);
                HEADER_BG      = new Color( 12,  15,  35);
                CARD           = new Color( 25,  32,  65);
                INPUT          = new Color( 32,  42,  82);
                ACCENT         = new Color(110, 155, 210);
                SUCCESS        = new Color( 80, 175, 140);
                DANGER         = new Color(195, 100, 100);
                WARNING        = new Color(200, 165,  75);
                TEXT_PRIMARY   = new Color(200, 210, 235);
                TEXT_SECONDARY = new Color(120, 135, 170);
                BORDER         = new Color( 45,  58, 105);
                break;
            case FOREST_GREEN:
                BG             = new Color( 22,  32,  24);
                HEADER_BG      = new Color( 15,  22,  17);
                CARD           = new Color( 30,  44,  33);
                INPUT          = new Color( 38,  58,  42);
                ACCENT         = new Color( 95, 175, 120);
                SUCCESS        = new Color(120, 190, 145);
                DANGER         = new Color(190,  95,  90);
                WARNING        = new Color(190, 165,  75);
                TEXT_PRIMARY   = new Color(200, 225, 205);
                TEXT_SECONDARY = new Color(120, 158, 128);
                BORDER         = new Color( 50,  80,  56);
                break;
            case WARM_SUNSET:
                BG             = new Color( 32,  22,  18);
                HEADER_BG      = new Color( 22,  14,  10);
                CARD           = new Color( 46,  30,  22);
                INPUT          = new Color( 60,  38,  28);
                ACCENT         = new Color(205, 128,  80);
                SUCCESS        = new Color(100, 175, 125);
                DANGER         = new Color(195, 100,  95);
                WARNING        = new Color(200, 170,  80);
                TEXT_PRIMARY   = new Color(238, 220, 205);
                TEXT_SECONDARY = new Color(165, 128, 105);
                BORDER         = new Color( 88,  55,  38);
                break;
            default: // DARK
                BG             = new Color( 22,  22,  30);
                HEADER_BG      = new Color( 16,  16,  22);
                CARD           = new Color( 32,  32,  44);
                INPUT          = new Color( 44,  44,  60);
                ACCENT         = new Color(110, 168, 210);
                SUCCESS        = new Color( 85, 180, 140);
                DANGER         = new Color(200,  95,  95);
                WARNING        = new Color(205, 165,  75);
                TEXT_PRIMARY   = new Color(255, 255, 255);
                TEXT_SECONDARY = new Color(130, 130, 152);
                BORDER         = new Color( 58,  58,  78);
                break;
        }
        refreshUIManager();
    }

    // ── Live window repaint ────────────────────────────────────────────────────

    /** Walks every component in the window and remaps colors to the active theme. */
    public static void applyThemeToWindow(Window window) {
        refreshUIManager();
        repaintTree(window);
        window.revalidate();
        window.repaint();
    }

    private static void repaintTree(Component c) {
        // Remap background colors for layout containers
        if (c instanceof JPanel || c instanceof JScrollPane
                || c instanceof JSplitPane || c instanceof JTabbedPane) {
            Color bg = c.getBackground();
            if      (matchesAny(bg, ALL_HEADER_BG)) c.setBackground(HEADER_BG);
            else if (matchesAny(bg, ALL_BG))        c.setBackground(BG);
            else if (matchesAny(bg, ALL_CARD))       c.setBackground(CARD);
            else if (matchesAny(bg, ALL_INPUT))      c.setBackground(INPUT);

            if (c instanceof JScrollPane)
                ((JScrollPane) c).getViewport().setBackground(BG);
        }

        // Remap text input colors.
        // JPasswordField is checked first — it extends JTextField, so if JTextField
        // were checked first the JPasswordField branch would never be reached.
        if (c instanceof JPasswordField) {
            c.setBackground(INPUT);
            c.setForeground(TEXT_PRIMARY);
            ((JPasswordField) c).setCaretColor(TEXT_PRIMARY);
        } else if (c instanceof JTextField) {
            c.setBackground(INPUT);
            c.setForeground(TEXT_PRIMARY);
            ((JTextField) c).setCaretColor(TEXT_PRIMARY);
        }

        // Remap button colors by stored role
        if (c instanceof JButton) {
            Object role = ((JButton) c).getClientProperty("colorRole");
            if (role instanceof String) c.setBackground(roleColor((String) role));
        }

        // Remap label foreground colors (skip table cell renderers)
        if (c instanceof JLabel && !(c instanceof javax.swing.table.DefaultTableCellRenderer)) {
            Color fg = c.getForeground();
            if      (matchesAny(fg, ALL_TEXT_PRIMARY))   c.setForeground(TEXT_PRIMARY);
            else if (matchesAny(fg, ALL_TEXT_SECONDARY)) c.setForeground(TEXT_SECONDARY);
            else if (matchesAny(fg, ALL_ACCENT))         c.setForeground(ACCENT);
            else if (matchesAny(fg, ALL_SUCCESS))        c.setForeground(SUCCESS);
            else if (matchesAny(fg, ALL_DANGER))         c.setForeground(DANGER);
            else if (matchesAny(fg, ALL_WARNING))        c.setForeground(WARNING);
        }

        // Remap list colors
        if (c instanceof JList) {
            c.setBackground(CARD);
            c.setForeground(TEXT_PRIMARY);
        }

        // Recurse into children
        if (c instanceof Container)
            for (Component child : ((Container) c).getComponents()) repaintTree(child);
    }

    // ── Color match tables — all known values across all 5 themes ─────────────

    private static final Color[] ALL_BG = {
            new Color(22,22,30), new Color(238,238,245), new Color(18,22,48),
            new Color(22,32,24), new Color(32,22,18)
    };
    private static final Color[] ALL_HEADER_BG = {
            new Color(16,16,22), new Color(225,225,236), new Color(12,15,35),
            new Color(15,22,17), new Color(22,14,10)
    };
    private static final Color[] ALL_CARD = {
            new Color(32,32,44), new Color(250,250,254), new Color(25,32,65),
            new Color(30,44,33), new Color(46,30,22)
    };
    private static final Color[] ALL_INPUT = {
            new Color(44,44,60), new Color(228,228,238), new Color(32,42,82),
            new Color(38,58,42), new Color(60,38,28)
    };
    private static final Color[] ALL_TEXT_PRIMARY = {
            new Color(215,215,228), new Color(40,40,55),  new Color(200,210,235),
            new Color(200,225,205), new Color(238,220,205)
    };
    private static final Color[] ALL_TEXT_SECONDARY = {
            new Color(130,130,152), new Color(110,110,130), new Color(120,135,170),
            new Color(120,158,128), new Color(165,128,105)
    };
    private static final Color[] ALL_ACCENT = {
            new Color(110,168,210), new Color(90,130,190),  new Color(110,155,210),
            new Color(95,175,120),  new Color(205,128,80)
    };
    private static final Color[] ALL_SUCCESS = {
            new Color(85,180,140), new Color(75,158,105), new Color(80,175,140),
            new Color(120,190,145), new Color(100,175,125)
    };
    private static final Color[] ALL_DANGER = {
            new Color(200,95,95), new Color(188,75,75), new Color(195,100,100),
            new Color(190,95,90), new Color(195,100,95)
    };
    private static final Color[] ALL_WARNING = {
            new Color(205,165,75), new Color(180,130,40), new Color(200,165,75),
            new Color(190,165,75), new Color(200,170,80)
    };

    /**
     * Fuzzy color match — allows a tolerance of 15 across R+G+B combined.
     * Needed because theme switching may introduce minor rounding differences.
     */
    private static boolean matchesAny(Color c, Color[] list) {
        if (c == null) return false;
        for (Color ref : list)
            if (Math.abs(c.getRed()  - ref.getRed())
                    + Math.abs(c.getGreen()- ref.getGreen())
                    + Math.abs(c.getBlue() - ref.getBlue()) < 15) return true;
        return false;
    }

    // ── Fonts ──────────────────────────────────────────────────────────────────

    public static final Font HEADER_FONT        = new Font("Segoe UI Emoji", Font.BOLD,  15);
    public static final Font BODY_FONT          = new Font("Segoe UI Emoji", Font.PLAIN, 13);
    public static final Font SMALL_FONT         = new Font("Segoe UI Emoji", Font.PLAIN, 11);
    public static final Font SYMBOL_HEADER_FONT = new Font("Arial",          Font.BOLD,  15);
    public static final Font SYMBOL_BODY_FONT   = new Font("Arial",          Font.PLAIN, 13);
    public static final Font SYMBOL_SMALL_FONT  = new Font("Arial",          Font.PLAIN, 11);
    public static final Font PESO_FONT          = new Font("Arial",          Font.PLAIN, 13);
    public static final Font PESO_FONT_BOLD     = new Font("Arial",          Font.BOLD,  15);

    // ── Component factories ────────────────────────────────────────────────────

    public static JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(BODY_FONT);
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    /** Label using Arial (for peso signs and symbols that need font fallback). */
    public static JLabel SymbolLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(SYMBOL_BODY_FONT);
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    public static JTextField textField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(BODY_FONT);
        field.setBackground(INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        field.putClientProperty("placeholder", placeholder);
        return field;
    }

    // Fixed combo face colors — consistent across all themes to avoid L&F overrides
    public static final Color COMBO_DISPLAY_BG = new Color(55, 55, 75);
    public static final Color COMBO_DISPLAY_FG = new Color(18, 18, 24);

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(BODY_FONT);
        combo.setBackground(COMBO_DISPLAY_BG);
        combo.setForeground(COMBO_DISPLAY_FG);
        combo.setBorder(new LineBorder(BORDER, 1, true));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );
                lbl.setFont(BODY_FONT);

                if (index == -1) {
                    // Closed face of the combo — override system L&F hard
                    lbl.setBackground(COMBO_DISPLAY_BG);
                    lbl.setForeground(COMBO_DISPLAY_FG);
                } else if (isSelected) {
                    lbl.setBackground(ACCENT);
                    lbl.setForeground(currentTheme == Theme.LIGHT ? Color.WHITE : Color.BLACK);
                } else {
                    lbl.setBackground(INPUT);
                    lbl.setForeground(TEXT_PRIMARY);
                }
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                lbl.setOpaque(true);
                return lbl;
            }
        });

        combo.setOpaque(true);
        if (combo.getEditor() != null
                && combo.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
            editor.setBackground(COMBO_DISPLAY_BG);
            editor.setForeground(COMBO_DISPLAY_FG);
        }
    }

    // ── Button factories ───────────────────────────────────────────────────────

    public static JButton accentButton(String text)       { return buildButton(text, "ACCENT");  }
    public static JButton successButton(String text)      { return buildButton(text, "SUCCESS"); }
    public static JButton dangerButton(String text)       { return buildButton(text, "DANGER");  }
    public static JButton buildWarningButton(String text) { return buildButton(text, "WARNING"); }

    public static Color roleColor(String role) {
        switch (role) {
            case "SUCCESS": return SUCCESS;
            case "DANGER":  return DANGER;
            case "WARNING": return WARNING;
            default:        return ACCENT;
        }
    }

    private static JButton buildButton(String text, String colorRole) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getBackground();
                if      (getModel().isPressed())   g2.setColor(c.darker());
                else if (getModel().isRollover())  g2.setColor(c.brighter());
                else                               g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.putClientProperty("colorRole", colorRole);
        btn.setFont(BODY_FONT.deriveFont(Font.BOLD));
        btn.setForeground(Color.WHITE);
        btn.setBackground(roleColor(colorRole));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setFocusPainted(false);
        return btn;
    }

    // ── UIManager sync ─────────────────────────────────────────────────────────

    public static void applyDarkLookAndFeel() { refreshUIManager(); }

    public static void refreshUIManager() {
        UIManager.put("Panel.background",              BG);
        UIManager.put("OptionPane.background",         CARD);
        UIManager.put("OptionPane.messageForeground",  TEXT_PRIMARY);
        UIManager.put("Button.background",             ACCENT);
        UIManager.put("Button.foreground",             Color.WHITE);
        UIManager.put("Label.foreground",              TEXT_PRIMARY);
        UIManager.put("TextField.background",          INPUT);
        UIManager.put("TextField.foreground",          TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",     TEXT_PRIMARY);
        UIManager.put("PasswordField.background",      INPUT);
        UIManager.put("PasswordField.foreground",      TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background",           COMBO_DISPLAY_BG);
        UIManager.put("ComboBox.foreground",           COMBO_DISPLAY_FG);
        UIManager.put("ComboBox.selectionBackground",  ACCENT);
        UIManager.put("ComboBox.selectionForeground",  Color.WHITE);
        UIManager.put("ComboBox.buttonBackground",     INPUT);
        UIManager.put("ComboBox.buttonShadow",         BORDER);
        UIManager.put("ComboBox.buttonDarkShadow",     BORDER);
        UIManager.put("ComboBox.buttonHighlight",      CARD);
        UIManager.put("ScrollBar.background",          CARD);
        UIManager.put("ScrollBar.thumb",               BORDER);
        UIManager.put("TabbedPane.background",         BG);
        UIManager.put("TabbedPane.foreground",         Color.BLACK);
        UIManager.put("TabbedPane.selected",           CARD);
        UIManager.put("TabbedPane.contentAreaColor",   CARD);
        UIManager.put("SplitPane.background",          BG);
        UIManager.put("Table.background",              CARD);
        UIManager.put("Table.foreground",              TEXT_PRIMARY);
        UIManager.put("TableHeader.background",        ACCENT);
        UIManager.put("TableHeader.foreground",        Color.WHITE);
        UIManager.put("List.background",               CARD);
        UIManager.put("List.foreground",               TEXT_PRIMARY);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Oops", JOptionPane.ERROR_MESSAGE);
    }

    // ── Live borders ───────────────────────────────────────────────────────────
    // These read theme color fields at paint time so they never go stale
    // when the theme changes, even without being recreated.

    /** Shared factory — builds a rounded 1px border using the supplied color supplier. */
    private static Border buildLiveBorder(java.util.function.Supplier<Color> colorSupplier) {
        return new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colorSupplier.get());
                g2.drawRoundRect(x, y, w - 1, h - 1, 4, 4);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c)           { return new Insets(1,1,1,1); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1,1,1,1); return i; }
        };
    }

    public static Border accentBorder() { return buildLiveBorder(() -> ACCENT); }
    public static Border borderBorder() { return buildLiveBorder(() -> BORDER); }

    public static Border accentCardBorder() {
        return BorderFactory.createCompoundBorder(accentBorder(), new EmptyBorder(16, 18, 16, 18));
    }

    public static Border borderCardBorder() {
        return BorderFactory.createCompoundBorder(borderBorder(), new EmptyBorder(18, 18, 18, 18));
    }
}