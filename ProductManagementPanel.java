package inventorymanagementsystem.ui;

import inventorymanagementsystem.dao.ProductDAO;
import inventorymanagementsystem.model.Product;
import inventorymanagementsystem.ui.component.ProductTableModel;
import inventorymanagementsystem.ui.component.StockLevelRenderer;
import inventorymanagementsystem.ui.component.TableActionsRenderer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import java.awt.Dimension; // ADDED PARA SA SAME SIZE NG BUTTON DIMENSION

/**
 * Recreates the "Product Management" screen from the mockup: search bar,
 * Add/Update/Delete actions, and a paginated product table with per-row
 * edit/delete icons.
 */
public class ProductManagementPanel extends JPanel {

    private final ProductDAO productDAO = new ProductDAO();
    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JLabel pageInfoLabel = new JLabel();
    private final JLabel pageNumbersLabel = new JLabel();

    /** When false, Add/Update/Delete and the per-row action icons are hidden - used to embed a read-only, search-only table on the Report Generation screen. */
    private final boolean showCrudActions;

    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalCount = 0;

    /** Full CRUD screen - used for the standalone "Product Management" nav item. */
    public ProductManagementPanel() {
        this(true);
    }

    public ProductManagementPanel(boolean showCrudActions) {
        this.showCrudActions = showCrudActions;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(6, 0, 0, 0));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildPagination(), BorderLayout.SOUTH);

        reload();
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setOpaque(false);

        UITheme.styleTextField(searchField);
        searchField.putClientProperty("JTextField.placeholderText", "Search products...");
        searchField.addActionListener(e -> {
            currentPage = 1;
            reload();
        });
        searchWrap.add(searchField, BorderLayout.CENTER);

        JButton refresh = new JButton("\u21BB REFRESH"); // <-- Capital REFRESH text
        UITheme.styleSecondaryButton(refresh);
        refresh.setToolTipText("Re-run the search and reload the latest data");
        refresh.addActionListener(e -> reload());
        searchWrap.add(refresh, BorderLayout.EAST);

        bar.add(searchWrap, BorderLayout.CENTER);

        if (showCrudActions) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);

            JButton add = new JButton("+ ADD PRODUCT");
            UITheme.stylePrimaryButton(add);
            add.addActionListener(e -> openAddDialog());

            JButton update = new JButton("\u270E UPDATE PRODUCT");
            UITheme.styleSecondaryButton(update);
            update.addActionListener(e -> openUpdateDialogForSelection());

            JButton delete = new JButton("\uD83D\uDDD1 DELETE PRODUCT");
            UITheme.styleDangerButton(delete);
            delete.addActionListener(e -> deleteSelection());


            // ADDED PARA DAMAY LAHAT NG BUTTON
            setEqualToolbarButtonSize(refresh, add, update, delete);

            actions.add(add);
            actions.add(update);
            actions.add(delete);

            bar.add(actions, BorderLayout.EAST);
        }

        return bar;
    }

     // ADDED PARA SA PAGSET NG SIZES
    private void setEqualToolbarButtonSize(JButton... buttons) {
    int maxWidth = 0;
    int maxHeight = 0;

    for (JButton button : buttons) {
        Dimension preferredSize = button.getPreferredSize();

        maxWidth = Math.max(maxWidth, preferredSize.width);
        maxHeight = Math.max(maxHeight, preferredSize.height);
    }

    // Add para extra horizontal space for longer text
    // mahaba si "UPDATE PRODUCT" eh
    maxWidth += 20;

    Dimension commonSize = new Dimension(maxWidth, maxHeight);

    for (JButton button : buttons) {
        button.setPreferredSize(commonSize);
        button.setMinimumSize(commonSize);
        button.setMaximumSize(commonSize);
    }
}

    private JComponent buildTable() {
        table.setRowHeight(34);
        table.setBackground(UITheme.BG_PANEL);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(new Color(0x22, 0x22, 0x22));
        table.setSelectionBackground(UITheme.BG_TABLE_ALT);
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(UITheme.BG_PANEL);
        header.setForeground(UITheme.ACCENT_GREEN);
        header.setFont(UITheme.FONT_BOLD);
        header.setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(showCrudActions ? 36 : 0);
        table.getColumnModel().getColumn(0).setPreferredWidth(showCrudActions ? 36 : 0);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        table.getColumnModel().getColumn(7).setCellRenderer(new StockLevelRenderer(20, 5));

        table.getColumnModel().getColumn(8).setMinWidth(0);
        table.getColumnModel().getColumn(8).setMaxWidth(showCrudActions ? 70 : 0);
        table.getColumnModel().getColumn(8).setPreferredWidth(showCrudActions ? 70 : 0);

        if (showCrudActions) {
            table.getColumnModel().getColumn(8).setCellRenderer(new TableActionsRenderer());

            // A JButton embedded as a table cell EDITOR needs two clicks in
            // Swing (the first click only starts editing / swaps the editor
            // component in; the button itself doesn't receive that click).
            // A plain MouseListener on the table fires on the very first
            // click instead, so it's used here for the pencil/trash icons.
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    int row = table.rowAtPoint(e.getPoint());
                    if (col != 8 || row < 0) {
                        return;
                    }
                    Rectangle cellRect = table.getCellRect(row, col, false);
                    boolean clickedLeftHalf = e.getX() < cellRect.x + cellRect.width / 2;
                    if (clickedLeftHalf) {
                        openUpdateDialogForRow(row);
                    } else {
                        deleteRow(row);
                    }
                }
            });
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);
        scroll.setBorder(UITheme.cardBorder());
        return scroll;
    }

    private JComponent buildPagination() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        pageInfoLabel.setForeground(UITheme.TEXT_MUTED);
        pageInfoLabel.setFont(UITheme.FONT_BODY);
        row.add(pageInfoLabel, BorderLayout.WEST);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        nav.setOpaque(false);

        JButton prev = linkButton("Prev");
        prev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                reload();
            }
        });

        pageNumbersLabel.setForeground(UITheme.TEXT_PRIMARY);
        pageNumbersLabel.setFont(UITheme.FONT_BOLD);

        JButton next = linkButton("Next");
        next.addActionListener(e -> {
            int maxPage = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
            if (currentPage < maxPage) {
                currentPage++;
                reload();
            }
        });

        nav.add(prev);
        nav.add(pageNumbersLabel);
        nav.add(next);
        row.add(nav, BorderLayout.EAST);
        return row;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(UITheme.TEXT_MUTED);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Re-queries the database for the current page + search term and refreshes the table. */
    public void reload() {
        String term = searchField.getText();
        List<Product> products = productDAO.getProducts(currentPage, pageSize, term);
        totalCount = productDAO.getTotalCount(term);
        tableModel.setRows(products);

        int from = totalCount == 0 ? 0 : (currentPage - 1) * pageSize + 1;
        int to = Math.min(currentPage * pageSize, totalCount);
        pageInfoLabel.setText("Showing " + from + "-" + to + " of " + totalCount + " products");

        int maxPage = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        pageNumbersLabel.setText(currentPage + " / " + maxPage);
    }

    private void openAddDialog() {
        ProductDialog dialog = new ProductDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            try {
                productDAO.insert(dialog.getResult());
                reload();
                JOptionPane.showMessageDialog(this, "Product added successfully.",
                        "Add Product", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showDbError("add the product", ex);
            }
        }
    }

    /**
     * Update uses the table's normal (single) row selection - click a row,
     * then click this button - rather than requiring a checkbox tick.
     * Checkboxes are reserved for bulk delete.
     */
    private void openUpdateDialogForSelection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Click a row to select it, then click Update Product.",
                    "Update Product", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openUpdateDialogForProduct(tableModel.getProductAt(row));
    }

    private void openUpdateDialogForRow(int row) {
        openUpdateDialogForProduct(tableModel.getProductAt(row));
    }

    private void openUpdateDialogForProduct(Product existing) {
        if (existing == null) {
            return;
        }
        ProductDialog dialog = new ProductDialog((Frame) SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            try {
                productDAO.update(dialog.getResult());
                reload();
                JOptionPane.showMessageDialog(this, "Product updated successfully.",
                        "Update Product", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showDbError("update the product", ex);
            }
        }
    }

    /**
     * Deletes whatever is checked (bulk delete); if nothing is checked,
     * falls back to whichever row is currently selected so a single click
     * + Delete still works without ticking a box first.
     */
    private void deleteSelection() {
        Set<Integer> checked = tableModel.getCheckedIds();
        List<Integer> ids;
        if (!checked.isEmpty()) {
            ids = new ArrayList<>(checked);
        } else {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        "Check one or more products, or click a row, then click Delete Product.",
                        "Delete Product", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ids = Collections.singletonList(tableModel.getProductAt(row).getIdno());
        }
        confirmAndDelete(ids);
    }

    private void deleteRow(int row) {
        confirmAndDelete(Collections.singletonList(tableModel.getProductAt(row).getIdno()));
    }

    private void confirmAndDelete(List<Integer> ids) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + ids.size() + " product(s)? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                productDAO.deleteMultiple(ids);
                tableModel.clearChecked();
                reload();
                JOptionPane.showMessageDialog(this,
                        ids.size() == 1 ? "Product deleted successfully." : ids.size() + " products deleted successfully.",
                        "Delete Product", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showDbError("delete the product(s)", ex);
            }
        }
    }

    /**
     * Shows the ACTUAL database error instead of failing silently. If you
     * see a foreign key message here, it means the product still has
     * rows in `inventory` referencing it - MySQL is refusing the delete
     * to protect that history.
     */
    private void showDbError(String action, Exception ex) {
        JOptionPane.showMessageDialog(this,
                "Could not " + action + ":\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
