package inventorymanagementsystem.ui;

import inventorymanagementsystem.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;

/**
 * Single dialog reused for both "Add Product" and "Update Product" -
 * pass an existing Product to pre-fill it in edit mode, or null to add.
 */
public class ProductDialog extends JDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JTextField categoryField = new JTextField();
    private final JTextField brandField = new JTextField();
    private final JTextField priceField = new JTextField();
    private final JTextField stockField = new JTextField();

    private final Product editing;
    private Product result;
    private boolean saved = false;

    public ProductDialog(Frame owner, Product existing) {
        super(owner, existing == null ? "Add Product" : "Update Product", true);
        this.editing = existing;
        buildUI();
        if (existing != null) {
            populateFrom(existing);
        }
        setSize(420, 420);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        getContentPane().setBackground(UITheme.BG_APP);
        setLayout(new BorderLayout(0, 12));

         // Added Code (Para sa header nung ADD PRODUCT PANEL)
        JLabel header = new JLabel(editing == null ? "ADD PRODUCT" : "Update Product");
        header.setForeground(UITheme.TEXT_PRIMARY);
        header.setFont(UITheme.FONT_TITLE);
        header.setBorder(new EmptyBorder(20, 25, 10, 20));

        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 20, 10, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        addField(form, gc, 0, "Name", nameField);
        addField(form, gc, 1, "Description", descriptionField);
        addField(form, gc, 2, "Category", categoryField);
        addField(form, gc, 3, "Brand", brandField);
        addField(form, gc, 4, "Price", priceField);
        addField(form, gc, 5, "Stock", stockField);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        UITheme.styleSecondaryButton(cancel);
        // Added code (same size with add button)
        cancel.setPreferredSize(new Dimension(72, 35));
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton(editing == null ? "Add" : "Save");
        UITheme.stylePrimaryButton(save);
        save.addActionListener(e -> onSave());

        buttons.add(cancel);
        buttons.add(save);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addField(JPanel form, GridBagConstraints gc, int row, String label, JTextField field) {
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(UITheme.TEXT_MUTED);
        l.setFont(UITheme.FONT_BODY);
        form.add(l, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        UITheme.styleTextField(field);
        form.add(field, gc);
    }

    private void populateFrom(Product p) {
        nameField.setText(p.getName());
        descriptionField.setText(p.getDescription());
        categoryField.setText(p.getCategory());
        brandField.setText(p.getBrand());
        priceField.setText(p.getPrice() == null ? "" : p.getPrice().toString());
        stockField.setText(String.valueOf(p.getStock()));
    }

    private void onSave() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BigDecimal price;
        int stock;
        try {
            price = new BigDecimal(priceField.getText().trim());
            stock = Integer.parseInt(stockField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price and Stock must be numbers.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        result = editing == null ? new Product() : editing;
        result.setName(nameField.getText().trim());
        result.setDescription(descriptionField.getText().trim());
        result.setCategory(categoryField.getText().trim());
        result.setBrand(brandField.getText().trim());
        result.setPrice(price);
        result.setStock(stock);

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public Product getResult() {
        return result;
    }
}
