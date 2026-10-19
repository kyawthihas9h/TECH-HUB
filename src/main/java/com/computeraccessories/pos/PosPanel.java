package com.computeraccessories.pos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

final class PosPanel extends JPanel {
    private final User cashier;
    private final JTextField barcode = new JTextField(24);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Product ID", "Barcode", "Item name", "Qty", "Unit price", "Line total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable cartTable = new JTable(model);
    private final Map<Long, CartLine> cart = new LinkedHashMap<>();
    private final JLabel subtotalLabel = new JLabel("0.00");
    private final JLabel taxLabel = new JLabel("0.00");
    private final JLabel totalLabel = new JLabel("0.00");

    PosPanel(User cashier) {
        super(new BorderLayout(10, 10));
        this.cashier = cashier;
        AppTheme.stylePanel(this, AppTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(header(), BorderLayout.NORTH);
        JPanel scan = new JPanel(new BorderLayout(12, 0));
        AppTheme.stylePanel(scan, AppTheme.SURFACE);
        scan.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 18, 15, 18)));
        JPanel scanInput = new JPanel(new BorderLayout(10, 0));
        AppTheme.stylePanel(scanInput, AppTheme.SURFACE);
        JLabel barcodeLabel = new JLabel("Scan barcode");
        AppTheme.styleLabel(barcodeLabel, AppTheme.TEXT, 13f, true);
        scanInput.add(barcodeLabel, BorderLayout.WEST);
        AppTheme.styleTextField(barcode);
        barcode.setToolTipText("Scan a barcode or type the code and press Enter");
        scanInput.add(barcode, BorderLayout.CENTER);
        scan.add(scanInput, BorderLayout.CENTER);
        JPanel scanActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        AppTheme.stylePanel(scanActions, AppTheme.SURFACE);
        JButton scanButton = new JButton("Scan / Add");
        AppTheme.stylePrimaryButton(scanButton);
        scanActions.add(scanButton);
        JButton remove = new JButton("Remove selected");
        AppTheme.styleButton(remove);
        scanActions.add(remove);
        scan.add(scanActions, BorderLayout.EAST);
        cartTable.setRowHeight(25);
        AppTheme.styleTable(cartTable);
        JPanel center = new JPanel(new BorderLayout(6, 6));
        AppTheme.stylePanel(center, AppTheme.PAGE);
        center.setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 18));
        center.add(scan, BorderLayout.NORTH);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        cartScroll.getViewport().setBackground(AppTheme.SURFACE);
        center.add(cartScroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(checkoutPanel(), BorderLayout.EAST);
        scanButton.addActionListener(e -> addBarcode());
        barcode.addActionListener(e -> addBarcode());
        remove.addActionListener(e -> removeSelected());
        barcode.requestFocusInWindow();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        AppTheme.stylePanel(header, AppTheme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(17, 24, 17, 24));
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        AppTheme.stylePanel(identity, AppTheme.NAVY);
        JLabel title = new JLabel("POINT OF SALE");
        AppTheme.styleLabel(title, new Color(172, 193, 213), 10f, true);
        JLabel user = new JLabel(cashier.displayName());
        AppTheme.styleLabel(user, Color.WHITE, 17f, true);
        identity.add(title);
        identity.add(Box.createVerticalStrut(3));
        identity.add(user);
        header.add(identity, BorderLayout.WEST);
        JButton logout = new JButton("Log out");
        AppTheme.styleDarkButton(logout);
        logout.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) window.dispose();
            new LoginFrame().setVisible(true);
        });
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel checkoutPanel() {
        JPanel panel = new JPanel();
        AppTheme.stylePanel(panel, AppTheme.SURFACE);
        panel.setPreferredSize(new Dimension(295, 240));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(24, 18, 24, 20)));
        JLabel title = new JLabel("ORDER SUMMARY");
        AppTheme.styleLabel(title, AppTheme.MUTED, 11f, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(22));
        summaryRow(panel, "Subtotal", subtotalLabel);
        summaryRow(panel, "Tax", taxLabel);
        panel.add(Box.createVerticalStrut(10));
        JSeparator separator = new JSeparator();
        separator.setForeground(AppTheme.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(separator);
        panel.add(Box.createVerticalStrut(14));
        summaryRow(panel, "Total", totalLabel);
        AppTheme.styleLabel(totalLabel, AppTheme.TEAL_DARK, 24f, true);
        panel.add(Box.createVerticalStrut(16));
        JButton cashPay = new JButton("Pay by cash");
        AppTheme.stylePrimaryButton(cashPay);
        cashPay.setAlignmentX(Component.LEFT_ALIGNMENT);
        cashPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.add(cashPay);
        panel.add(Box.createVerticalStrut(8));
        JButton mobilePay = new JButton("Mobile banking");
        AppTheme.styleButton(mobilePay);
        mobilePay.setAlignmentX(Component.LEFT_ALIGNMENT);
        mobilePay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.add(mobilePay);
        cashPay.addActionListener(e -> checkout("CASH"));
        mobilePay.addActionListener(e -> checkout("MOBILE_BANKING"));
        return panel;
    }

    private void summaryRow(JPanel panel, String name, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        AppTheme.stylePanel(row, AppTheme.SURFACE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(240, 30));
        JLabel label = new JLabel(name);
        AppTheme.styleLabel(label, AppTheme.MUTED, 13f, false);
        row.add(label, BorderLayout.WEST);
        AppTheme.styleLabel(value, AppTheme.TEXT, 13f, true);
        row.add(value, BorderLayout.EAST);
        panel.add(row);
    }

    private void addBarcode() {
        String code = barcode.getText().trim();
        if (code.isEmpty()) return;
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT id, barcode, item_name, quantity, price FROM products WHERE barcode=?")) {
            query.setString(1, code);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    JOptionPane.showMessageDialog(this, "No product found for barcode: " + code);
                    return;
                }
                long id = result.getLong("id");
                CartLine existing = cart.get(id);
                int inCart = existing == null ? 0 : existing.quantity;
                if (result.getInt("quantity") <= inCart) {
                    JOptionPane.showMessageDialog(this, "There is not enough stock for this item.");
                    return;
                }
                if (existing == null) {
                    cart.put(id, new CartLine(id, result.getString("barcode"),
                            result.getString("item_name"), 1, result.getBigDecimal("price")));
                } else {
                    existing.quantity++;
                }
                barcode.setText("");
                refreshCart();
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void removeSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a cart item first.");
            return;
        }
        long id = ((Number) model.getValueAt(cartTable.convertRowIndexToModel(row), 0)).longValue();
        CartLine line = cart.get(id);
        if (line.quantity > 1) line.quantity--;
        else cart.remove(id);
        refreshCart();
    }

    private void refreshCart() {
        model.setRowCount(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartLine line : cart.values()) {
            BigDecimal lineTotal = line.price.multiply(BigDecimal.valueOf(line.quantity));
            subtotal = subtotal.add(lineTotal);
            model.addRow(new Object[]{line.id, line.barcode, line.name, line.quantity,
                    line.price, lineTotal.setScale(2, RoundingMode.HALF_UP)});
        }
        try {
            BigDecimal rate = readTaxRate();
            BigDecimal tax = subtotal.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            subtotalLabel.setText(subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
            taxLabel.setText(tax.toPlainString());
            totalLabel.setText(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP).toPlainString());
        } catch (Exception e) {
            error(e);
        }
    }

    private BigDecimal readTaxRate() throws Exception {
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key='tax_rate'");
             ResultSet result = query.executeQuery()) {
            if (!result.next()) throw new IllegalStateException("Tax rate setting is missing from the database.");
            return new BigDecimal(result.getString(1));
        }
    }

    private void checkout(String method) {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Scan at least one item before checkout.");
            return;
        }
        BigDecimal checkoutTaxRate;
        try {
            checkoutTaxRate = readTaxRate();
        } catch (Exception e) {
            error(e);
            return;
        }
        String paymentProvider = null;
        BigDecimal amountReceived = null;
        if ("MOBILE_BANKING".equals(method)) {
            String[] providers = {"KBZ Pay", "Wave Pay", "AYA Pay"};
            paymentProvider = (String) JOptionPane.showInputDialog(this, "Select mobile bank",
                    "Mobile banking", JOptionPane.QUESTION_MESSAGE, null, providers, providers[0]);
            if (paymentProvider == null) return;
            if (!showQr(paymentProvider)) return;
            int confirmed = JOptionPane.showConfirmDialog(this,
                    "Confirm that " + paymentProvider + " payment was received.",
                    "Confirm mobile payment", JOptionPane.YES_NO_OPTION);
            if (confirmed != JOptionPane.YES_OPTION) return;
        } else {
            BigDecimal total = currentSubtotal().multiply(
                    BigDecimal.ONE.add(checkoutTaxRate.divide(new BigDecimal("100"))))
                    .setScale(2, RoundingMode.HALF_UP);
            String received = JOptionPane.showInputDialog(this,
                    "Amount received (total " + total.toPlainString() + ")");
            if (received == null) return;
            try {
                amountReceived = new BigDecimal(received.trim());
                if (amountReceived.compareTo(total) < 0) {
                    JOptionPane.showMessageDialog(this, "Received amount is less than the total.");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter a valid amount.");
                return;
            }
        }
        try (Connection connection = Db.connect()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal subtotal = BigDecimal.ZERO;
                for (CartLine line : cart.values()) {
                    subtotal = subtotal.add(line.price.multiply(BigDecimal.valueOf(line.quantity)));
                }
                BigDecimal tax = subtotal.multiply(checkoutTaxRate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
                long saleId;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO sales (cashier_id, subtotal, tax_rate, tax_amount, total, payment_method, "
                                + "payment_provider) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, cashier.id());
                    insert.setBigDecimal(2, subtotal.setScale(2, RoundingMode.HALF_UP));
                    insert.setBigDecimal(3, checkoutTaxRate);
                    insert.setBigDecimal(4, tax);
                    insert.setBigDecimal(5, total);
                    insert.setString(6, method);
                    insert.setString(7, paymentProvider);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new IllegalStateException("Database did not return the sale ID.");
                        saleId = keys.getLong(1);
                    }
                }
                for (CartLine line : cart.values()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE products SET quantity=quantity-? WHERE id=? AND quantity>=?");
                         PreparedStatement insert = connection.prepareStatement(
                                 "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)")) {
                        update.setInt(1, line.quantity);
                        update.setLong(2, line.id);
                        update.setInt(3, line.quantity);
                        if (update.executeUpdate() != 1) {
                            throw new IllegalStateException("Stock changed during checkout for " + line.name
                                    + ". Refresh and try again.");
                        }
                        insert.setLong(1, saleId);
                        insert.setLong(2, line.id);
                        insert.setInt(3, line.quantity);
                        insert.setBigDecimal(4, line.price);
                        insert.executeUpdate();
                    }
                }
                connection.commit();
                JOptionPane.showMessageDialog(this, "Sale #" + saleId + " completed.\n"
                        + "Payment: " + (paymentProvider == null ? "Cash" : paymentProvider)
                        + "\nTotal: " + total.toPlainString()
                        + (amountReceived == null ? "" : "\nChange: "
                        + amountReceived.subtract(total).setScale(2, RoundingMode.HALF_UP).toPlainString()),
                        "Sale complete", JOptionPane.INFORMATION_MESSAGE);
                cart.clear();
                refreshCart();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private boolean showQr(String provider) {
        String key = switch (provider) {
            case "KBZ Pay" -> "kbz_pay_qr";
            case "Wave Pay" -> "wave_pay_qr";
            default -> "aya_pay_qr";
        };
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key=?")) {
            query.setString(1, key);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next() || result.getString(1).isBlank()) {
                    JOptionPane.showMessageDialog(this, "No QR code has been configured for " + provider + ".");
                    return false;
                }
                String path = result.getString(1);
                ImageIcon icon = new ImageIcon(path);
                if (icon.getIconWidth() <= 0) {
                    JOptionPane.showMessageDialog(this, "Could not open QR image:\n" + path);
                    return false;
                }
                Image scaled = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                JLabel image = new JLabel(new ImageIcon(scaled));
                JOptionPane.showMessageDialog(this, image,
                        provider + " QR - confirm payment before completing", JOptionPane.PLAIN_MESSAGE);
                return true;
            }
        } catch (Exception e) {
            error(e);
            return false;
        }
    }

    private BigDecimal currentSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartLine line : cart.values()) {
            subtotal = subtotal.add(line.price.multiply(BigDecimal.valueOf(line.quantity)));
        }
        return subtotal;
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "POS error", JOptionPane.ERROR_MESSAGE);
    }

    private static final class CartLine {
        final long id;
        final String barcode;
        final String name;
        int quantity;
        final BigDecimal price;

        CartLine(long id, String barcode, String name, int quantity, BigDecimal price) {
            this.id = id;
            this.barcode = barcode;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
