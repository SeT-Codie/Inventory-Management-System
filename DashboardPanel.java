package inventorymanagementsystem.ui;

import inventorymanagementsystem.dao.InventoryDAO;
import inventorymanagementsystem.dao.ProductDAO;
import inventorymanagementsystem.dao.SupplierDAO;
import inventorymanagementsystem.model.Inventory;
import inventorymanagementsystem.ui.component.BarChartPanel;
import inventorymanagementsystem.ui.component.GaugePanel;
import inventorymanagementsystem.ui.component.SupplierTableModel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recreates the "Report Generation" screen: summary cards, a top
 * categories chart, a stock health gauge, recent activity, a read-only
 * (search-only, no Add/Update/Delete) product table reused from
 * {@link ProductManagementPanel}, and a supplier directory with its own
 * "Add Supplier" action.
 */
public class DashboardPanel extends JPanel {

    private final ProductDAO productDAO = new ProductDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final SupplierTableModel supplierTableModel = new SupplierTableModel();
    private final JTable supplierTable = new JTable(supplierTableModel);

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel heading = new JLabel("Report Generation");
        heading.setForeground(UITheme.TEXT_PRIMARY);
        heading.setFont(UITheme.FONT_TITLE.deriveFont(20f));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(heading, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(buildCardsRow());
        center.add(Box.createVerticalStrut(16));
        center.add(buildOperationsHeader());
        center.add(Box.createVerticalStrut(10));

        ProductManagementPanel productTable = new ProductManagementPanel(false);
        productTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(productTable);

        center.add(Box.createVerticalStrut(20));
        center.add(buildSupplierSection());

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JComponent buildCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        row.setPreferredSize(new Dimension(0, 170));

        row.add(buildInventoryValueCard());
        row.add(buildTopCategoriesCard());
        row.add(buildStockHealthCard());
        row.add(buildRecentTransactionsCard());
        return row;
    }

    private JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(UITheme.BG_PANEL);
        p.setBorder(UITheme.cardBorder());
        JLabel t = new JLabel(title);
        t.setForeground(UITheme.TEXT_MUTED);
        t.setFont(UITheme.FONT_BOLD);
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    private JPanel buildInventoryValueCard() {
        JPanel p = card("Total Inventory Value");
        BigDecimal value = productDAO.getTotalInventoryValue();
        JLabel amount = new JLabel(formatCompactCurrency(value));
        amount.setForeground(UITheme.ACCENT_GREEN);
        amount.setFont(UITheme.FONT_TITLE.deriveFont(26f));
        p.add(amount, BorderLayout.CENTER);
        return p;
    }

    private String formatCompactCurrency(BigDecimal value) {
        double v = value.doubleValue();
        if (v >= 1_000_000) {
            return String.format(Locale.US, "$%.1fM", v / 1_000_000.0);
        }
        if (v >= 1_000) {
            return String.format(Locale.US, "$%.1fK", v / 1_000.0);
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(v);
    }

    private JPanel buildTopCategoriesCard() {
        JPanel p = card("Top Selling Categories");
        Map<String, Integer> data = inventoryDAO.getTopSellingCategories(5);
        BarChartPanel chart = new BarChartPanel();
        chart.setData(data);
        p.add(chart, BorderLayout.CENTER);

        int totalUnits = inventoryDAO.getTotalUnitsSold();
        JLabel footer = new JLabel("Total Units Sold: " + totalUnits);
        footer.setForeground(UITheme.TEXT_MUTED);
        footer.setFont(UITheme.FONT_BODY.deriveFont(11f));
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildStockHealthCard() {
        JPanel p = card("Stock Health");
        double pct = productDAO.getStockHealthPercentage(20);
        GaugePanel gauge = new GaugePanel(pct);
        p.add(gauge, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRecentTransactionsCard() {
        JPanel p = card("Recent Transactions");
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        List<Inventory> recent = inventoryDAO.getRecentTransactions(4);
        SimpleDateFormat df = new SimpleDateFormat("MMM d");
        if (recent.isEmpty()) {
            JLabel empty = new JLabel("No transactions yet");
            empty.setForeground(UITheme.TEXT_MUTED);
            empty.setFont(UITheme.FONT_BODY);
            list.add(empty);
        } else {
            for (Inventory inv : recent) {
                String sign = inv.getQuantity() >= 0 ? "+" : "";
                String text = (inv.getProductName() == null ? "Product #" + inv.getProductId() : inv.getProductName())
                        + "  " + sign + inv.getQuantity()
                        + (inv.getTransactionDate() != null ? "  \u00B7 " + df.format(inv.getTransactionDate()) : "");
                JLabel row = new JLabel(text);
                row.setForeground(UITheme.TEXT_PRIMARY);
                row.setFont(UITheme.FONT_BODY.deriveFont(11f));
                row.setBorder(new EmptyBorder(3, 0, 3, 0));
                list.add(row);
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildOperationsHeader() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = UITheme.sectionTitle("Advanced Data Operations");
        row.add(label, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton importExcel = new JButton("Import Excel");
        JButton importPdf = new JButton("Import PDF");
        JButton generateReport = new JButton("Generate Detailed Report");
        UITheme.styleSecondaryButton(importExcel);
        UITheme.styleSecondaryButton(importPdf);
        // Add code (same size din with other buttons)
        importExcel.setPreferredSize(new Dimension(110, 35));// <-- eto
        importPdf.setPreferredSize(new Dimension(90, 35));// <-- eto
        UITheme.stylePrimaryButton(generateReport);

        // TODO: wire these up to Apache POI (Excel) / PDFBox or iText (PDF)
        // once those libraries are added to the NetBeans project's Libraries node.
        importExcel.addActionListener(e -> notImplementedYet("Excel import"));
        importPdf.addActionListener(e -> notImplementedYet("PDF import"));
        generateReport.addActionListener(e -> notImplementedYet("Report generation"));

        actions.add(importExcel);
        actions.add(importPdf);
        actions.add(generateReport);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JComponent buildSupplierSection() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UITheme.sectionTitle("Supplier directory"), BorderLayout.WEST);

        JButton addSupplier = new JButton("+ ADD SUPPLIER");
        UITheme.stylePrimaryButton(addSupplier);
        addSupplier.addActionListener(e -> openAddSupplierDialog());
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addWrap.setOpaque(false);
        addWrap.add(addSupplier);
        header.add(addWrap, BorderLayout.EAST);
        wrap.add(header, BorderLayout.NORTH);

        supplierTable.setRowHeight(30);
        supplierTable.setBackground(UITheme.BG_PANEL);
        supplierTable.setForeground(UITheme.TEXT_PRIMARY);
        supplierTable.setGridColor(new Color(0x22, 0x22, 0x22));
        supplierTable.setSelectionBackground(UITheme.BG_TABLE_ALT);
        supplierTable.setSelectionForeground(UITheme.TEXT_PRIMARY);
        supplierTable.setShowVerticalLines(false);
        supplierTable.setFillsViewportHeight(false);

        JTableHeader sh = supplierTable.getTableHeader();
        sh.setBackground(UITheme.BG_PANEL);
        sh.setForeground(UITheme.ACCENT_GREEN);
        sh.setFont(UITheme.FONT_BOLD);
        sh.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(supplierTable);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);
        scroll.setBorder(UITheme.cardBorder());
        scroll.setPreferredSize(new Dimension(0, 220));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        wrap.add(scroll, BorderLayout.CENTER);

        reloadSuppliers();
        return wrap;
    }

    private void reloadSuppliers() {
        supplierTableModel.setRows(supplierDAO.getAll());
    }

    private void openAddSupplierDialog() {
        SupplierDialog dialog = new SupplierDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            try {
                supplierDAO.insert(dialog.getResult());
                reloadSuppliers();
                JOptionPane.showMessageDialog(this, "Supplier added successfully.",
                        "Add Supplier", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not add the supplier:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void notImplementedYet(String feature) {
        JOptionPane.showMessageDialog(this,
                feature + " is a placeholder - hook it up to Apache POI / PDFBox here.",
                feature, JOptionPane.INFORMATION_MESSAGE);
    }
}
