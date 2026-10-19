package com.computeraccessories.pos;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class AdminPanel extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final JLabel salesValue = new JLabel("0");
    private final JLabel stockValue = new JLabel("0");
    private final JPanel dashboardCharts = new JPanel(new GridLayout(1, 2, 10, 10));
    private final DataPanel inventory;
    private final DataPanel suppliers;
    private final DataPanel vip;
    private final DataPanel purchases;
    private final DataPanel sales;
    private final CashiersPanel cashiers;
    private final SettingsPanel settings;
    private int extraCharts;

    AdminPanel(User user) {
        super(new BorderLayout());
        AppTheme.stylePanel(this, AppTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        AppTheme.stylePanel(content, AppTheme.PAGE);
        inventory = new DataPanel("Inventory items", "products",
                "SELECT id, barcode, item_name, photo, quantity, size, price FROM products ORDER BY id DESC",
                new String[]{"barcode", "item_name", "photo", "quantity", "size", "price"},
                new String[]{"Barcode", "Item name", "Photo path or URL", "Quantity", "Size", "Price"},
                this::refreshDashboard);
        suppliers = new DataPanel("Suppliers", "suppliers",
                "SELECT id, name, phone, address FROM suppliers ORDER BY name",
                new String[]{"name", "phone", "address"}, new String[]{"Supplier name", "Phone", "Address"});
        vip = new DataPanel("VIP members", "vip_members",
                "SELECT id, name, phone, points, created_at FROM vip_members ORDER BY id DESC",
                new String[]{"name", "phone", "points"}, new String[]{"Name", "Phone", "Points"});
        purchases = new DataPanel("Purchases (supplier and product links use their database IDs)",
                "purchases", "SELECT p.id, p.product_id, p.supplier_id, p.source_type, "
                + "p.quantity, p.unit_cost, pr.item_name, s.name AS supplier_name, p.purchased_at FROM purchases p "
                + "JOIN products pr ON pr.id=p.product_id JOIN suppliers s ON s.id=p.supplier_id "
                + "ORDER BY p.id DESC",
                new String[]{"product_id", "supplier_id", "source_type", "quantity", "unit_cost"},
                new String[]{"Product ID", "Supplier ID", "Source (SHOP or FACTORY)", "Quantity", "Unit cost"},
                () -> {
                    inventory.refresh();
                    refreshDashboard();
                });
        sales = new DataPanel("Sale records", "sales",
                "SELECT s.id, COALESCE(GROUP_CONCAT(CONCAT(pr.item_name, ' x', si.quantity) "
                + "ORDER BY si.id SEPARATOR ', '), '(no items)') AS itemName, "
                + "u.display_name AS cashier_name, s.paid_at AS date_time, s.total, "
                + "s.payment_method, s.payment_provider "
                + "FROM sales s JOIN users u ON u.id=s.cashier_id "
                + "LEFT JOIN sale_items si ON si.sale_id=s.id LEFT JOIN products pr ON pr.id=si.product_id "
                + "GROUP BY s.id ORDER BY s.paid_at DESC",
                new String[]{}, new String[]{});
        cashiers = new CashiersPanel();
        settings = new SettingsPanel();
        content.add(dashboard(), "Dashboard");
        content.add(sales, "Sale Record");
        content.add(inventorySection(), "Inventory");
        content.add(suppliers, "SupplierList");
        content.add(vip, "VIP List");
        content.add(cashiers, "Cashiers");
        content.add(settings, "Setting");
        add(sidebar(user), BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout(0, 0));
        AppTheme.stylePanel(workspace, AppTheme.PAGE);
        workspace.add(header(user), BorderLayout.NORTH);
        workspace.add(content, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        refreshDashboard();
    }

    private JPanel header(User user) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        AppTheme.stylePanel(header, AppTheme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 26, 14, 24)));
        AppTheme.styleLabel(pageTitle, AppTheme.TEXT, 20f, true);
        header.add(pageTitle, BorderLayout.WEST);
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        AppTheme.stylePanel(identity, AppTheme.SURFACE);
        JLabel name = new JLabel(user.displayName());
        AppTheme.styleLabel(name, AppTheme.TEXT, 12f, true);
        JLabel role = new JLabel("Administrator");
        AppTheme.styleLabel(role, AppTheme.MUTED, 10f, false);
        identity.add(name);
        identity.add(role);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        AppTheme.stylePanel(buttons, AppTheme.SURFACE);
        buttons.add(identity);
        JButton export = new JButton("Export all to Excel");
        AppTheme.styleButton(export);
        export.addActionListener(e -> ExcelExporter.export(this));
        JButton logout = new JButton("Log out");
        AppTheme.styleButton(logout);
        logout.addActionListener(e -> { disposeWindow(); new LoginFrame().setVisible(true); });
        buttons.add(export); buttons.add(logout);
        header.add(buttons, BorderLayout.EAST);
        return header;
    }

    private JPanel sidebar(User user) {
        JPanel sidebar = new JPanel(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(220, 0));
        AppTheme.stylePanel(sidebar, AppTheme.NAVY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 15, 16, 15));
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        AppTheme.stylePanel(brand, AppTheme.NAVY);
        JLabel mark = new JLabel(" TH ");
        mark.setOpaque(true);
        mark.setBackground(AppTheme.TEAL);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel brandName = new JLabel(" TECHHUB");
        AppTheme.styleLabel(brandName, Color.WHITE, 15f, true);
        JLabel subtitle = new JLabel(" COMPUTER ACCESSORIES");
        AppTheme.styleLabel(subtitle, new Color(161, 180, 202), 9f, true);
        brand.add(mark);
        brand.add(Box.createVerticalStrut(14));
        brand.add(brandName);
        brand.add(Box.createVerticalStrut(4));
        brand.add(subtitle);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        AppTheme.stylePanel(navigation, AppTheme.NAVY);
        navigation.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        String[][] pages = {
                {"Dashboard", "Overview"},
                {"Sale Record", "Sales"},
                {"Inventory", "Inventory"},
                {"SupplierList", "Suppliers"},
                {"VIP List", "VIP members"},
                {"Cashiers", "Cashiers"},
                {"Setting", "Settings"}
        };
        for (String[] page : pages) {
            JButton button = new JButton(page[1]);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            AppTheme.styleDarkButton(button);
            button.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 10));
            button.addActionListener(e -> showPage(page[0], page[1]));
            navigationButtons.add(button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(5));
        }
        sidebar.add(navigation, BorderLayout.CENTER);

        JPanel account = new JPanel(new BorderLayout(9, 0));
        AppTheme.stylePanel(account, AppTheme.NAVY_LIGHT);
        account.setBorder(BorderFactory.createEmptyBorder(11, 10, 11, 8));
        JLabel initials = new JLabel(initials(user.displayName()), SwingConstants.CENTER);
        initials.setOpaque(true);
        initials.setBackground(AppTheme.TEAL);
        initials.setForeground(Color.WHITE);
        initials.setFont(new Font("Segoe UI", Font.BOLD, 12));
        initials.setPreferredSize(new Dimension(36, 36));
        JLabel accountName = new JLabel("<html>" + escapeHtml(user.displayName())
                + "<br><span style='color:#A1B4CA'>Administrator</span></html>");
        AppTheme.styleLabel(accountName, Color.WHITE, 11f, true);
        account.add(initials, BorderLayout.WEST);
        account.add(accountName, BorderLayout.CENTER);
        sidebar.add(account, BorderLayout.SOUTH);
        showPage("Dashboard", "Overview");
        return sidebar;
    }

    private void showPage(String name, String title) {
        cards.show(content, name);
        pageTitle.setText(title);
        for (int i = 0; i < navigationButtons.size(); i++) {
            JButton button = navigationButtons.get(i);
            if (button.getText().equals(title)) {
                AppTheme.styleButton(button, AppTheme.TEAL, Color.WHITE, AppTheme.TEAL);
                button.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 10));
            } else {
                AppTheme.styleDarkButton(button);
                button.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 10));
            }
        }
        if ("Dashboard".equals(name)) refreshDashboard();
        if ("Sale Record".equals(name)) sales.refresh();
        if ("Inventory".equals(name)) { inventory.refresh(); purchases.refresh(); }
        if ("SupplierList".equals(name)) suppliers.refresh();
        if ("VIP List".equals(name)) vip.refresh();
        if ("Cashiers".equals(name)) cashiers.refresh();
        if ("Setting".equals(name)) settings.load();
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return "U";
        return words.length == 1 ? words[0].substring(0, 1).toUpperCase()
                : (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel dashboard() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(panel, AppTheme.PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        JPanel summary = new JPanel(new GridLayout(1, 2, 8, 8));
        AppTheme.stylePanel(summary, AppTheme.PAGE);
        summary.add(metric("Today's sales", salesValue));
        summary.add(metric("Items in stock", stockValue));
        JButton addChart = new JButton("Add chart");
        AppTheme.stylePrimaryButton(addChart);
        addChart.addActionListener(e -> {
            extraCharts++;
            dashboardCharts.add(paymentChart());
            dashboardCharts.setLayout(new GridLayout(0, 2, 10, 10));
            dashboardCharts.revalidate();
            dashboardCharts.repaint();
        });
        JPanel top = new JPanel(new BorderLayout());
        AppTheme.stylePanel(top, AppTheme.PAGE);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        top.add(summary, BorderLayout.CENTER);
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        AppTheme.stylePanel(action, AppTheme.PAGE);
        action.add(addChart);
        top.add(action, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        dashboardCharts.add(salesChart());
        dashboardCharts.add(inventoryChart());
        AppTheme.stylePanel(dashboardCharts, AppTheme.PAGE);
        JScrollPane chartsScroll = new JScrollPane(dashboardCharts);
        chartsScroll.setBorder(BorderFactory.createEmptyBorder());
        chartsScroll.getViewport().setBackground(AppTheme.PAGE);
        panel.add(chartsScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel metric(String title, JLabel value) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        AppTheme.stylePanel(panel, AppTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 17, 12, 17)));
        JLabel caption = new JLabel(title.toUpperCase());
        AppTheme.styleLabel(caption, AppTheme.MUTED, 10f, true);
        panel.add(caption);
        AppTheme.styleLabel(value, AppTheme.TEXT, 26f, true);
        panel.add(value);
        return panel;
    }

    private JComponent salesChart() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT DATE(paid_at) day, SUM(total) total FROM sales "
                        + "WHERE paid_at >= CURRENT_DATE - INTERVAL 6 DAY GROUP BY DATE(paid_at) ORDER BY day");
             ResultSet result = query.executeQuery()) {
            while (result.next()) {
                labels.add(result.getString("day"));
                values.add(result.getBigDecimal("total").doubleValue());
            }
        } catch (Exception e) {
            showError(e);
        }
        return new DashboardChart("Sales (daily)", ChartType.LINE, labels, values);
    }

    private JComponent inventoryChart() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT item_name, quantity FROM products ORDER BY item_name LIMIT 20");
             ResultSet result = query.executeQuery()) {
            while (result.next()) {
                labels.add(result.getString("item_name"));
                values.add((double) result.getInt("quantity"));
            }
        } catch (Exception e) {
            showError(e);
        }
        return new DashboardChart("Inventory quantity", ChartType.BAR, labels, values);
    }

    private JComponent paymentChart() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT payment_method, COUNT(*) count FROM sales GROUP BY payment_method");
             ResultSet result = query.executeQuery()) {
            while (result.next()) {
                labels.add(result.getString("payment_method"));
                values.add((double) result.getInt("count"));
            }
        } catch (Exception e) {
            showError(e);
        }
        return new DashboardChart("Sales by payment method", ChartType.DOUGHNUT, labels, values);
    }

    private void refreshDashboard() {
        if (salesValue == null) return;
        try (Connection connection = Db.connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE((SELECT SUM(total) FROM sales WHERE DATE(paid_at)=CURRENT_DATE),0) today_sales, "
                             + "COALESCE((SELECT SUM(quantity) FROM products),0) stock");
             ResultSet result = query.executeQuery()) {
            result.next();
            salesValue.setText(result.getBigDecimal("today_sales").toPlainString());
            stockValue.setText(Long.toString(result.getLong("stock")));
        } catch (Exception e) {
            showError(e);
        }
        dashboardCharts.removeAll();
        dashboardCharts.add(salesChart());
        dashboardCharts.add(inventoryChart());
        for (int i = 0; i < extraCharts; i++) {
            dashboardCharts.add(paymentChart());
        }
        dashboardCharts.setLayout(new GridLayout(0, 2, 10, 10));
        dashboardCharts.revalidate();
        dashboardCharts.repaint();
    }

    private JPanel inventorySection() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory", inventory);
        tabs.addTab("Purchase List", purchases);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private void showError(Exception e) {
        String message = e.getMessage() == null ? e.toString() : e.getMessage();
        JOptionPane.showMessageDialog(this, message, "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private enum ChartType {
        LINE, BAR, DOUGHNUT
    }

    private static final class DashboardChart extends JPanel {
        private static final Color[] COLORS = {
                new Color(45, 116, 181), AppTheme.TEAL,
                new Color(235, 156, 52), new Color(181, 83, 92),
                new Color(126, 95, 168), new Color(61, 158, 177)
        };
        private final String title;
        private final ChartType type;
        private final List<String> labels;
        private final List<Double> values;

        DashboardChart(String title, ChartType type, List<String> labels, List<Double> values) {
            this.title = title;
            this.type = type;
            this.labels = List.copyOf(labels);
            this.values = List.copyOf(values);
            setOpaque(true);
            setBackground(AppTheme.SURFACE);
            setPreferredSize(new Dimension(420, 280));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(AppTheme.TEXT);
                g.setFont(getFont().deriveFont(Font.BOLD, 15f));
                g.drawString(title, 16, 24);
                if (values.isEmpty()) {
                    g.setFont(getFont());
                    g.drawString("No data available", 20, 55);
                } else if (type == ChartType.DOUGHNUT) {
                    paintDoughnut(g);
                } else {
                    paintAxes(g);
                    if (type == ChartType.LINE) paintLine(g);
                    else paintBars(g);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintAxes(Graphics2D g) {
            int left = 48;
            int top = 46;
            int right = getWidth() - 18;
            int bottom = getHeight() - 50;
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            if (max <= 0) max = 1;
            g.setFont(getFont().deriveFont(10f));
            g.setColor(new Color(190, 190, 190));
            for (int tick = 0; tick <= 4; tick++) {
                int y = bottom - tick * (bottom - top) / 4;
                g.drawLine(left, y, right, y);
                g.setColor(AppTheme.TEXT);
                g.drawString(String.format("%.0f", max * tick / 4), 8, y + 4);
                g.setColor(new Color(190, 190, 190));
            }
            g.setColor(AppTheme.MUTED);
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
        }

        private void paintBars(Graphics2D g) {
            int left = 48;
            int top = 46;
            int right = getWidth() - 18;
            int bottom = getHeight() - 50;
            double max = Math.max(1, values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            int slot = Math.max(1, (right - left) / values.size());
            int barWidth = Math.max(3, Math.min(34, slot * 2 / 3));
            g.setFont(getFont().deriveFont(9f));
            for (int i = 0; i < values.size(); i++) {
                int x = left + slot * i + (slot - barWidth) / 2;
                int barHeight = (int) Math.round((bottom - top) * values.get(i) / max);
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRoundRect(x, bottom - barHeight, barWidth, barHeight, 5, 5);
                g.setColor(AppTheme.TEXT);
                String label = abbreviate(labels.get(i), Math.max(4, slot / 7));
                g.drawString(label, x - 4, bottom + 14);
            }
        }

        private void paintLine(Graphics2D g) {
            int left = 48;
            int top = 46;
            int right = getWidth() - 18;
            int bottom = getHeight() - 50;
            double max = Math.max(1, values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            int denominator = Math.max(1, values.size() - 1);
            int previousX = 0;
            int previousY = 0;
            g.setFont(getFont().deriveFont(9f));
            for (int i = 0; i < values.size(); i++) {
                int x = left + (right - left) * i / denominator;
                int y = bottom - (int) Math.round((bottom - top) * values.get(i) / max);
                if (i > 0) {
                    g.setColor(COLORS[0]);
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(previousX, previousY, x, y);
                }
                g.setColor(COLORS[0]);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(AppTheme.TEXT);
                g.drawString(abbreviate(labels.get(i), 12), x - 18, bottom + 14);
                previousX = x;
                previousY = y;
            }
        }

        private void paintDoughnut(Graphics2D g) {
            int diameter = Math.min(150, getHeight() - 70);
            int x = 24;
            int y = 42;
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            int start = 0;
            if (sum > 0) {
                for (int i = 0; i < values.size(); i++) {
                    int extent = (int) Math.round(values.get(i) * 360 / sum);
                    g.setColor(COLORS[i % COLORS.length]);
                    g.fillArc(x, y, diameter, diameter, start, extent);
                    start += extent;
                }
                int hole = diameter / 2;
                g.setColor(getBackground());
                g.fillOval(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole);
            }
            int legendX = x + diameter + 18;
            int legendY = y + 26;
            g.setFont(getFont().deriveFont(11f));
            for (int i = 0; i < labels.size(); i++) {
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRect(legendX, legendY + i * 24 - 10, 12, 12);
                g.setColor(AppTheme.TEXT);
                g.drawString(labels.get(i) + " (" + values.get(i).intValue() + ")",
                        legendX + 19, legendY + i * 24);
            }
        }

        private String abbreviate(String text, int maximum) {
            if (maximum < 2 || text.length() <= maximum) return text;
            return text.substring(0, maximum - 1) + "\u2026";
        }
    }

    private void disposeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) window.dispose();
    }

    private static final class CashiersPanel extends JPanel {
        private final JTable grid = new JTable();
        private final javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel();

        CashiersPanel() {
            super(new BorderLayout(8, 8));
            AppTheme.stylePanel(this, AppTheme.PAGE);
            setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
            grid.setModel(model);
            AppTheme.styleTable(grid);
            JLabel heading = new JLabel("Cashier accounts");
            AppTheme.styleLabel(heading, AppTheme.TEXT, 17f, true);
            heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
            add(heading, BorderLayout.NORTH);
            JScrollPane tableScroll = new JScrollPane(grid);
            tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
            tableScroll.getViewport().setBackground(AppTheme.SURFACE);
            add(tableScroll, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            AppTheme.stylePanel(actions, AppTheme.PAGE);
            actions.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
            JButton add = new JButton("Add cashier");
            JButton toggle = new JButton("Activate / deactivate selected");
            JButton refresh = new JButton("Refresh");
            AppTheme.stylePrimaryButton(add);
            AppTheme.styleButton(toggle);
            AppTheme.styleButton(refresh);
            actions.add(add); actions.add(toggle); actions.add(refresh);
            add(actions, BorderLayout.SOUTH);
            add.addActionListener(e -> addCashier());
            toggle.addActionListener(e -> toggleActive());
            refresh.addActionListener(e -> refresh());
            refresh();
        }

        void refresh() {
            try (Connection connection = Db.connect(); PreparedStatement query = connection.prepareStatement(
                    "SELECT id, username, display_name, is_active, created_at FROM users "
                            + "WHERE role='CASHIER' ORDER BY id DESC"); ResultSet result = query.executeQuery()) {
                var metadata = result.getMetaData();
                String[] columns = new String[metadata.getColumnCount()];
                for (int i = 0; i < columns.length; i++) columns[i] = metadata.getColumnLabel(i + 1);
                model.setDataVector(new Object[0][columns.length], columns);
                while (result.next()) {
                    Object[] row = new Object[columns.length];
                    for (int i = 0; i < row.length; i++) row[i] = result.getObject(i + 1);
                    model.addRow(row);
                }
            } catch (Exception e) {
                error(e);
            }
        }

        private void addCashier() {
            JTextField username = new JTextField();
            JTextField displayName = new JTextField();
            JPasswordField password = new JPasswordField();
            Object[] form = {"Username", username, "Display name", displayName,
                    "Password (at least 8 characters)", password};
            if (JOptionPane.showConfirmDialog(this, form, "Add cashier",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            char[] secret = password.getPassword();
            if (username.getText().isBlank() || displayName.getText().isBlank() || secret.length < 8) {
                JOptionPane.showMessageDialog(this, "Enter a username, display name, and password of at least 8 characters.");
                return;
            }
            try (Connection connection = Db.connect(); PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (username, display_name, password_hash, role) VALUES (?, ?, ?, 'CASHIER')")) {
                insert.setString(1, username.getText().trim());
                insert.setString(2, displayName.getText().trim());
                insert.setString(3, org.mindrot.jbcrypt.BCrypt.hashpw(new String(secret),
                        org.mindrot.jbcrypt.BCrypt.gensalt(12)));
                insert.executeUpdate();
                refresh();
            } catch (Exception e) {
                error(e);
            }
        }

        private void toggleActive() {
            int row = grid.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a cashier first.");
                return;
            }
            int modelRow = grid.convertRowIndexToModel(row);
            Object id = model.getValueAt(modelRow, 0);
            boolean active = Boolean.parseBoolean(String.valueOf(model.getValueAt(modelRow, 3)));
            try (Connection connection = Db.connect(); PreparedStatement update = connection.prepareStatement(
                    "UPDATE users SET is_active=? WHERE id=? AND role='CASHIER'")) {
                update.setBoolean(1, !active);
                update.setObject(2, id);
                update.executeUpdate();
                refresh();
            } catch (Exception e) {
                error(e);
            }
        }

        private void error(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cashier error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class SettingsPanel extends JPanel {
        private final JTextField taxRate = new JTextField(12);
        private final JTextField[] qrPaths = {new JTextField(34), new JTextField(34), new JTextField(34)};
        private final String[] keys = {"kbz_pay_qr", "wave_pay_qr", "aya_pay_qr"};

        SettingsPanel() {
            super(new BorderLayout());
            AppTheme.stylePanel(this, AppTheme.PAGE);
            setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
            JPanel form = new JPanel(new GridBagLayout());
            AppTheme.stylePanel(form, AppTheme.SURFACE);
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(20, 22, 20, 22)));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8, 6, 8, 6);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0; c.gridy = 0;
            JLabel taxLabel = new JLabel("Tax rate (%)");
            AppTheme.styleLabel(taxLabel, AppTheme.TEXT, 13f, true);
            form.add(taxLabel, c);
            AppTheme.styleTextField(taxRate);
            c.gridx = 1; form.add(taxRate, c);
            String[] names = {"KBZ Pay QR", "Wave Pay QR", "AYA Pay QR"};
            for (int i = 0; i < names.length; i++) {
                final int index = i;
                c.gridx = 0; c.gridy++;
                JLabel providerLabel = new JLabel(names[i]);
                AppTheme.styleLabel(providerLabel, AppTheme.TEXT, 13f, true);
                form.add(providerLabel, c);
                JPanel pathRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                AppTheme.stylePanel(pathRow, AppTheme.SURFACE);
                AppTheme.styleTextField(qrPaths[i]);
                pathRow.add(qrPaths[i]);
                JButton browse = new JButton("Choose local image");
                AppTheme.styleButton(browse);
                browse.addActionListener(e -> chooseQr(index));
                pathRow.add(browse);
                c.gridx = 1; form.add(pathRow, c);
            }
            JButton save = new JButton("Save settings");
            AppTheme.stylePrimaryButton(save);
            c.gridx = 1; c.gridy++; form.add(save, c);
            save.addActionListener(e -> save());
            add(form, BorderLayout.NORTH);
            load();
        }

        void load() {
            try (Connection connection = Db.connect()) {
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT setting_value FROM app_settings WHERE setting_key='tax_rate'");
                     ResultSet result = query.executeQuery()) {
                    if (result.next()) taxRate.setText(result.getString(1));
                }
                for (int i = 0; i < keys.length; i++) {
                    try (PreparedStatement query = connection.prepareStatement(
                            "SELECT setting_value FROM app_settings WHERE setting_key=?")) {
                        query.setString(1, keys[i]);
                        try (ResultSet result = query.executeQuery()) {
                            qrPaths[i].setText(result.next() ? result.getString(1) : "");
                        }
                    }
                }
            } catch (Exception e) {
                error(e);
            }
        }

        private void chooseQr(int index) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Image files", "png", "jpg", "jpeg", "gif", "bmp"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                qrPaths[index].setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }

        private void save() {
            try {
                BigDecimal tax = new BigDecimal(taxRate.getText().trim());
                if (tax.signum() < 0 || tax.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException("Tax rate must be between 0 and 100.");
                }
                try (Connection connection = Db.connect()) {
                    upsert(connection, "tax_rate", tax.stripTrailingZeros().toPlainString());
                    for (int i = 0; i < keys.length; i++) upsert(connection, keys[i], qrPaths[i].getText().trim());
                }
                JOptionPane.showMessageDialog(this, "Settings saved.");
            } catch (Exception e) {
                error(e);
            }
        }

        private void upsert(Connection connection, String key, String value) throws Exception {
            try (PreparedStatement update = connection.prepareStatement(
                    "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)")) {
                update.setString(1, key);
                update.setString(2, value);
                update.executeUpdate();
            }
        }

        private void error(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Settings error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
