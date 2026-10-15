import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * HistoryPanel — History tab UI.
 *
 * Layout (horizontal split):
 *   LEFT  — sortable expense table (Date | Category | Description | Amount)
 *            with grand total label below
 *   RIGHT — category breakdown panel with progress bars per category
 *
 * Both sides update via refresh() which is wired to DataStore's listener.
 * The table shows all-time expenses; the breakdown shows all-time category totals.
 */
public class HistoryPanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Table ──────────────────────────────────────────────────────────────────
    private JTable             table;
    private DefaultTableModel  tableModel;
    private JLabel             grandTotalLabel;

    // ── Category summary ───────────────────────────────────────────────────────
    private JPanel summaryPanel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public HistoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeaderBar(),  BorderLayout.NORTH);
        add(buildSplitPanel(), BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    // ── Header bar ─────────────────────────────────────────────────────────────

    private JPanel buildHeaderBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG);

        JLabel title = new JLabel("\uD83D\uDCCA  Expense History & Summary");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(title, BorderLayout.WEST);

        JButton clearBtn = UITheme.dangerButton("\uD83D\uDDD1  Clear All History");
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Clear ALL expense history? This can't be undone.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // Iterate in reverse to avoid index shifting during removal
                List<Expense> expenses = store.getExpenses();
                for (int i = expenses.size() - 1; i >= 0; i--) store.removeExpense(i);
            }
        });
        panel.add(clearBtn, BorderLayout.EAST);

        return panel;
    }

    // ── Split pane ─────────────────────────────────────────────────────────────

    private JSplitPane buildSplitPanel() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), buildSummaryPanel()
        );
        split.setDividerLocation(520);
        split.setBackground(UITheme.BG);
        split.setBorder(null);
        split.setDividerSize(8);
        return split;
    }

    // ── Expense table ──────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG);

        // Non-editable table model
        String[] cols = {"Date", "Category", "Description", "Amount (\u20B1)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(UITheme.BODY_FONT);
        table.setBackground(UITheme.CARD);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(UITheme.SYMBOL_BODY_FONT.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(UITheme.ACCENT);
        table.getTableHeader().setForeground(Color.BLACK);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        // Amount column — right-aligned, Arial for peso sign, alternating row color
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(UITheme.PESO_FONT);
                if (!isSelected) {
                    setForeground(UITheme.ACCENT);
                    setBackground(row % 2 == 0 ? UITheme.CARD : new Color(33, 33, 46));
                }
                return this;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.CARD);
        panel.add(scroll, BorderLayout.CENTER);

        grandTotalLabel = new JLabel("Grand Total: \u20B10.00");
        grandTotalLabel.setFont(UITheme.SYMBOL_HEADER_FONT);
        grandTotalLabel.setForeground(UITheme.ACCENT);
        grandTotalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(grandTotalLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ── Category breakdown panel ───────────────────────────────────────────────

    private JPanel buildSummaryPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);

        JLabel title = new JLabel("  Category Breakdown");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(UITheme.BG);

        JScrollPane scroll = new JScrollPane(summaryPanel);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.BG);
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        // Rebuild table rows
        tableModel.setRowCount(0);
        for (Expense e : store.getExpenses()) {
            tableModel.addRow(new Object[]{
                    e.getFormattedDate(),
                    e.getCategory(),
                    e.getDescription(),
                    String.format("\u20B1%.2f", e.getAmount())
            });
        }
        grandTotalLabel.setText(String.format("Grand Total: \u20B1%.2f", store.getTotalExpenses()));

        // Rebuild category breakdown cards
        summaryPanel.removeAll();
        summaryPanel.add(Box.createVerticalStrut(8));

        double total = store.getTotalExpenses();
        for (String cat : Expense.CATEGORIES) {
            double catTotal = store.getTotalByCategory(cat);
            if (catTotal == 0) continue;

            int pct = total > 0 ? (int)((catTotal / total) * 100) : 0;

            // Category card
            JPanel card = new JPanel(new BorderLayout(4, 2));
            card.setBackground(UITheme.CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UITheme.BORDER, 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

            JLabel catLabel = new JLabel(getCategoryIcon(cat) + " " + cat);
            catLabel.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
            catLabel.setForeground(UITheme.TEXT_PRIMARY);

            JLabel amtLabel = new JLabel(String.format("\u20B1%.2f (%d%%)", catTotal, pct));
            amtLabel.setFont(UITheme.PESO_FONT);
            amtLabel.setForeground(UITheme.ACCENT);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setBackground(UITheme.BG);
            bar.setForeground(getCategoryColor(cat));
            bar.setBorder(null);
            bar.setPreferredSize(new Dimension(0, 6));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setBackground(UITheme.CARD);
            topRow.add(catLabel, BorderLayout.WEST);
            topRow.add(amtLabel, BorderLayout.EAST);

            card.add(topRow, BorderLayout.NORTH);
            card.add(bar,    BorderLayout.SOUTH);

            summaryPanel.add(card);
            summaryPanel.add(Box.createVerticalStrut(6));
        }

        summaryPanel.revalidate();
        summaryPanel.repaint();
    }

    // ── Category metadata helpers ──────────────────────────────────────────────

    private String getCategoryIcon(String cat) {
        switch (cat) {
            case "Food":          return "\uD83C\uDF5C";
            case "Transport":     return "\uD83D\uDE0C";
            case "School":        return "\uD83D\uDCDA";
            case "Entertainment": return "\uD83C\uDFAE";
            case "Health":        return "\uD83D\uDC8A";
            case "Shopping":      return "\uD83D\uDECD";
            default:              return "\uD83D\uDCC1";
        }
    }

    private Color getCategoryColor(String cat) {
        switch (cat) {
            case "Food":          return new Color(255, 149,   0);
            case "Transport":     return new Color(  0, 122, 255);
            case "School":        return new Color( 52, 199,  89);
            case "Entertainment": return new Color(175,  82, 222);
            case "Health":        return new Color(255,  59,  48);
            case "Shopping":      return new Color(255, 204,   0);
            default:              return UITheme.ACCENT;
        }
    }
}