import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ExpensePanel — Expenses tab UI.
 *
 * Layout (top to bottom):
 *   - Search / filter bar: description text filter + category dropdown + "+ Add Expense" button
 *   - Filtered expense list: scrollable, one entry per matching expense
 *   - Bottom bar: running total of *filtered* entries + Remove Selected button
 *
 * "Add Expense" opens a modal overlay JDialog with:
 *   Description | Amount | Category | Need / Want toggle | Add button
 *
 * The Need/Want field is persisted (via Expense.needWant) and shown
 * in both the list and the History table.
 */
public class ExpensePanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Search / filter controls ───────────────────────────────────────────────
    private JTextField        searchField;
    private JComboBox<String> filterCategoryBox;

    // ── List display ───────────────────────────────────────────────────────────
    private JList<String>            expenseList;
    private DefaultListModel<String> listModel;

    // ── Tracks which DataStore index each visible row maps to ──────────────────
    private final List<Integer> filteredIndices = new ArrayList<>();

    // ── Summary ────────────────────────────────────────────────────────────────
    private JLabel totalLabel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public ExpensePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildSearchBar(),  BorderLayout.NORTH);
        add(buildListPanel(),  BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);

        store.addListener(this::refresh);
        refresh();
    }

    // ── Search + filter bar ────────────────────────────────────────────────────

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.gridy  = 0;

        // Section label
        JLabel title = new JLabel("🔍  Search Expenses");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridwidth = 5; gbc.weightx = 0;
        panel.add(title, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;

        // Description search
        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Description:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        searchField = UITheme.textField("Search by description...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        panel.add(searchField, gbc);

        // Category filter
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.label("Category:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.4;
        String[] catOptions = buildCategoryOptions();
        filterCategoryBox = new JComboBox<>(catOptions);
        UITheme.styleCombo(filterCategoryBox);
        filterCategoryBox.addActionListener(e -> refresh());
        panel.add(filterCategoryBox, gbc);

        // Add Expense button — opens the dialog
        gbc.gridx = 4; gbc.weightx = 0;
        JButton addBtn = UITheme.accentButton("＋ Add Expense");
        addBtn.addActionListener(e -> openAddDialog());
        panel.add(addBtn, gbc);

        return panel;
    }

    // ── Expense list ───────────────────────────────────────────────────────────

    private JPanel buildListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG);

        JLabel lbl = new JLabel("📋  Recent Expenses");
        lbl.setFont(UITheme.HEADER_FONT);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        listModel   = new DefaultListModel<>();
        expenseList = new JList<>(listModel);
        expenseList.setFont(UITheme.PESO_FONT);
        expenseList.setBackground(UITheme.CARD);
        expenseList.setForeground(UITheme.TEXT_PRIMARY);
        expenseList.setSelectionBackground(UITheme.ACCENT);
        expenseList.setSelectionForeground(Color.WHITE);
        expenseList.setFixedCellHeight(36);
        expenseList.setBorder(new EmptyBorder(4, 8, 4, 8));

        // Custom cell renderer — color the [Need]/[Want] tag
        expenseList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                lbl.setFont(UITheme.PESO_FONT);
                if (!isSelected) {
                    lbl.setBackground(index % 2 == 0 ? UITheme.CARD : new Color(
                            UITheme.CARD.getRed()   + 6,
                            UITheme.CARD.getGreen() + 6,
                            UITheme.CARD.getBlue()  + 8));
                    lbl.setForeground(UITheme.TEXT_PRIMARY);
                }
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(expenseList);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.CARD);
        panel.add(scroll, BorderLayout.CENTER);

        JButton removeBtn = UITheme.dangerButton("🗑  Remove Selected");
        removeBtn.addActionListener(e -> removeSelected());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setBackground(UITheme.BG);
        btnRow.add(removeBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // ── Bottom bar ─────────────────────────────────────────────────────────────

    private JPanel buildBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(UITheme.BG);

        totalLabel = new JLabel();
        totalLabel.setFont(UITheme.PESO_FONT_BOLD);
        totalLabel.setForeground(UITheme.ACCENT);
        panel.add(totalLabel);

        return panel;
    }

    // ── Add Expense dialog ─────────────────────────────────────────────────────

    /**
     * Opens a modal dialog for entering a new expense.
     * Fields: Description | Amount | Category | Need / Want toggle
     */
    private void openAddDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Add New Expense", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 340);
        dialog.setLocationRelativeTo(owner);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Title
        JLabel title = new JLabel("➕  Add New Expense");
        title.setFont(UITheme.SYMBOL_HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        title.setBorder(new EmptyBorder(0, 0, 14, 0));
        content.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 6, 7, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Description
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(UITheme.label("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField descField = UITheme.textField("e.g. Lunch at canteen");
        form.add(descField, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(UITheme.SymbolLabel("Amount (₱):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField amountField = UITheme.textField("0.00");
        form.add(amountField, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(UITheme.label("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> categoryBox = new JComboBox<>(Expense.CATEGORIES);
        UITheme.styleCombo(categoryBox);
        form.add(categoryBox, gbc);

        // Need / Want toggle
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(UITheme.label("Type:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel togglePanel = buildNeedWantToggle();
        togglePanel.setBackground(UITheme.CARD);
        form.add(togglePanel, gbc);

        content.add(form, BorderLayout.CENTER);

        // Buttons row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(UITheme.BG);
        btnRow.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton cancelBtn = UITheme.buildWarningButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = UITheme.accentButton("Add Expense");
        confirmBtn.addActionListener(e -> {
            String desc     = descField.getText().trim();
            String amtText  = amountField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            String needWant = getSelectedNeedWant(togglePanel);

            if (desc.isEmpty()) {
                UITheme.showError(dialog, "Description can't be empty.");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amtText);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                UITheme.showError(dialog, "Enter a valid amount greater than 0.");
                return;
            }
            store.addExpense(new Expense(desc, amount, category, needWant));
            dialog.dispose();
        });

        // Allow Enter key on the confirm button
        dialog.getRootPane().setDefaultButton(confirmBtn);

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);
        content.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /**
     * Builds the two-button Need / Want toggle panel.
     * The active button is highlighted with ACCENT; the other uses BORDER color.
     * A client property "selectedNeedWant" on the panel stores the current value.
     */
    private JPanel buildNeedWantToggle() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.putClientProperty("selectedNeedWant", "Need"); // default

        JButton needBtn = buildToggleBtn("✅ Need", true);
        JButton wantBtn = buildToggleBtn("🛍 Want", false);

        needBtn.addActionListener(e -> {
            panel.putClientProperty("selectedNeedWant", "Need");
            needBtn.setBackground(UITheme.ACCENT);
            needBtn.setForeground(Color.WHITE);
            wantBtn.setBackground(UITheme.BORDER);
            wantBtn.setForeground(UITheme.TEXT_SECONDARY);
        });

        wantBtn.addActionListener(e -> {
            panel.putClientProperty("selectedNeedWant", "Want");
            wantBtn.setBackground(UITheme.WARNING);
            wantBtn.setForeground(Color.WHITE);
            needBtn.setBackground(UITheme.BORDER);
            needBtn.setForeground(UITheme.TEXT_SECONDARY);
        });

        panel.add(needBtn);
        panel.add(wantBtn);
        return panel;
    }

    /** Reads the selected Need/Want value from the toggle panel's client property. */
    private String getSelectedNeedWant(JPanel togglePanel) {
        Object val = togglePanel.getClientProperty("selectedNeedWant");
        return (val instanceof String) ? (String) val : "Need";
    }

    private JButton buildToggleBtn(String label, boolean activeDefault) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getBackground();
                if      (getModel().isPressed())  g2.setColor(c.darker());
                else if (getModel().isRollover()) g2.setColor(c.brighter());
                else                              g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        btn.setBackground(activeDefault ? UITheme.ACCENT : UITheme.BORDER);
        btn.setForeground(activeDefault ? Color.WHITE    : UITheme.TEXT_SECONDARY);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    // ── Remove selected ────────────────────────────────────────────────────────

    private void removeSelected() {
        int listIndex = expenseList.getSelectedIndex();
        if (listIndex == -1) {
            UITheme.showError(this, "Select an expense to remove first.");
            return;
        }
        int storeIndex = filteredIndices.get(listIndex);
        int confirm = JOptionPane.showConfirmDialog(
                this, "Remove this expense?", "Confirm", JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) store.removeExpense(storeIndex);
    }

    // ── Refresh / filter ───────────────────────────────────────────────────────

    private void refresh() {
        String searchText = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String catFilter  = filterCategoryBox != null
                ? (String) filterCategoryBox.getSelectedItem() : "All Categories";

        listModel.clear();
        filteredIndices.clear();

        double filteredTotal = 0;
        List<Expense> expenses = store.getExpenses();

        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);

            boolean matchesText = searchText.isEmpty()
                    || e.getDescription().toLowerCase().contains(searchText);
            boolean matchesCat  = "All Categories".equals(catFilter)
                    || e.getCategory().equals(catFilter);

            if (matchesText && matchesCat) {
                // Tag badge prefix for Need/Want
                String tag = "Need".equals(e.getNeedWant()) ? "[Need]" : "[Want]";
                listModel.addElement(String.format("  %s  %s", tag, e.toString()));
                filteredIndices.add(i);
                filteredTotal += e.getAmount();
            }
        }

        String suffix = filteredIndices.size() < expenses.size()
                ? String.format(" (%d of %d shown)", filteredIndices.size(), expenses.size()) : "";
        totalLabel.setText(String.format("Total: \u20B1%.2f%s", filteredTotal, suffix));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String[] buildCategoryOptions() {
        String[] cats = Expense.CATEGORIES;
        String[] options = new String[cats.length + 1];
        options[0] = "All Categories";
        System.arraycopy(cats, 0, options, 1, cats.length);
        return options;
    }
}