package com.computeraccessories.pos;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

final class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);

    LoginFrame() {
        super("TechHub Computer Accessories POS | Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(900, 570);
        setLocationRelativeTo(null);
        JPanel page = new JPanel(new BorderLayout());
        AppTheme.stylePanel(page, AppTheme.SURFACE);
        page.add(new BrandPanel(), BorderLayout.WEST);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(70, 56, 52, 56));
        AppTheme.stylePanel(form, AppTheme.SURFACE);
        JLabel eyebrow = new JLabel("WELCOME BACK");
        AppTheme.styleLabel(eyebrow, AppTheme.TEAL_DARK, 12f, true);
        JLabel title = new JLabel("Sign in to your account");
        AppTheme.styleLabel(title, AppTheme.TEXT, 25f, true);
        JLabel subtitle = new JLabel("Use your staff account to continue.");
        AppTheme.styleLabel(subtitle, AppTheme.MUTED, 14f, false);
        form.add(eyebrow);
        form.add(Box.createVerticalStrut(12));
        form.add(title);
        form.add(Box.createVerticalStrut(8));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(34));
        JLabel usernameLabel = new JLabel("Username");
        AppTheme.styleLabel(usernameLabel, AppTheme.TEXT, 13f, true);
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        AppTheme.styleTextField(username);
        JLabel passwordLabel = new JLabel("Password");
        AppTheme.styleLabel(passwordLabel, AppTheme.TEXT, 13f, true);
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        AppTheme.styleTextField(password);
        form.add(usernameLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(username);
        form.add(Box.createVerticalStrut(20));
        form.add(passwordLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(password);
        form.add(Box.createVerticalStrut(27));
        JButton signIn = new JButton("Sign in");
        AppTheme.stylePrimaryButton(signIn);
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        form.add(signIn);
        form.add(Box.createVerticalGlue());
        JLabel footer = new JLabel("Secure staff access  |  Admin and cashier");
        AppTheme.styleLabel(footer, AppTheme.MUTED, 11f, false);
        form.add(footer);
        page.add(form, BorderLayout.CENTER);
        signIn.addActionListener(e -> signIn());
        getRootPane().setDefaultButton(signIn);
        add(page);
    }

    private void signIn() {
        try (Connection connection = Db.connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT id, username, display_name, password_hash, role, is_active "
                             + "FROM users WHERE username = ?")) {
            query.setString(1, username.getText().trim());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next() || !result.getBoolean("is_active")
                        || !BCrypt.checkpw(new String(password.getPassword()), result.getString("password_hash"))) {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.",
                            "Sign in failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                User user = new User(result.getLong("id"), result.getString("username"),
                        result.getString("display_name"), result.getString("role"));
                dispose();
                new MainFrame(user).setVisible(true);
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.toString() : e.getMessage();
            JOptionPane.showMessageDialog(this, message, "Sign in error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class BrandPanel extends JPanel {
        BrandPanel() {
            setPreferredSize(new Dimension(330, 0));
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(42, 36, 36, 30));
            setOpaque(false);

            JPanel brand = new JPanel();
            brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
            brand.setOpaque(false);
            JLabel mark = new JLabel("TH");
            mark.setOpaque(true);
            mark.setBackground(AppTheme.TEAL);
            mark.setForeground(Color.WHITE);
            mark.setFont(new Font("Segoe UI", Font.BOLD, 20));
            mark.setHorizontalAlignment(SwingConstants.CENTER);
            mark.setPreferredSize(new Dimension(54, 48));
            mark.setMinimumSize(new Dimension(54, 48));
            mark.setMaximumSize(new Dimension(54, 48));
            JLabel name = new JLabel("TECHHUB");
            AppTheme.styleLabel(name, Color.WHITE, 14f, true);
            JLabel subtitle = new JLabel("COMPUTER ACCESSORIES");
            AppTheme.styleLabel(subtitle, new Color(165, 183, 202), 10f, true);
            brand.add(mark);
            brand.add(Box.createVerticalStrut(18));
            brand.add(name);
            brand.add(Box.createVerticalStrut(4));
            brand.add(subtitle);
            add(brand, BorderLayout.NORTH);

            JPanel statement = new JPanel();
            statement.setOpaque(false);
            statement.setLayout(new BoxLayout(statement, BoxLayout.Y_AXIS));
            JLabel headline = new JLabel("<html>Everything your<br>shop needs, in<br>one place.</html>");
            AppTheme.styleLabel(headline, Color.WHITE, 27f, true);
            JLabel detail = new JLabel("<html>Track stock, manage sales<br>and keep your business moving.</html>");
            AppTheme.styleLabel(detail, new Color(190, 205, 221), 13f, false);
            statement.add(headline);
            statement.add(Box.createVerticalStrut(18));
            statement.add(detail);
            add(statement, BorderLayout.CENTER);
            JLabel secure = new JLabel("TECHHUB COMPUTER ACCESSORIES POS");
            AppTheme.styleLabel(secure, new Color(153, 173, 194), 10f, true);
            add(secure, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, AppTheme.NAVY_LIGHT,
                        getWidth(), getHeight(), AppTheme.NAVY));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(36, 69, 91, 100));
                g.fillOval(getWidth() - 100, getHeight() - 210, 190, 190);
                g.setColor(new Color(26, 157, 139, 45));
                g.fillOval(getWidth() - 76, getHeight() - 186, 142, 142);
            } finally {
                g.dispose();
            }
        }
    }
}
