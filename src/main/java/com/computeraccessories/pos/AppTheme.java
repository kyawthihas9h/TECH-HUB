package com.computeraccessories.pos;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class AppTheme {
    static final Color NAVY = new Color(20, 35, 57);
    static final Color NAVY_LIGHT = new Color(29, 51, 79);
    static final Color TEAL = new Color(17, 153, 135);
    static final Color TEAL_DARK = new Color(13, 126, 112);
    static final Color PAGE = new Color(243, 246, 250);
    static final Color SURFACE = Color.WHITE;
    static final Color TEXT = new Color(36, 50, 68);
    static final Color MUTED = new Color(112, 127, 145);
    static final Color BORDER = new Color(222, 229, 237);
    static final Color ROW_ALT = new Color(248, 250, 252);

    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private AppTheme() {
    }

    static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the desktop look and feel.", e);
        }
        UIManager.put("defaultFont", BODY);
        UIManager.put("Panel.background", PAGE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Button.font", BODY_BOLD);
        UIManager.put("Label.font", BODY);
        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("Table.font", BODY);
        UIManager.put("TableHeader.font", BODY_BOLD);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", new Color(221, 241, 238));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.background", PAGE);
        UIManager.put("ToolTip.background", NAVY);
        UIManager.put("ToolTip.foreground", SURFACE);
    }

    static void stylePanel(JPanel panel, Color background) {
        panel.setOpaque(true);
        panel.setBackground(background);
    }

    static void styleLabel(JLabel label, Color foreground, float size, boolean bold) {
        label.setForeground(foreground);
        label.setFont(BODY.deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
    }

    static void styleButton(JButton button) {
        styleButton(button, SURFACE, TEXT, BORDER);
    }

    static void stylePrimaryButton(JButton button) {
        styleButton(button, TEAL, Color.WHITE, TEAL);
    }

    static void styleDarkButton(JButton button) {
        styleButton(button, NAVY_LIGHT, new Color(226, 235, 244), NAVY_LIGHT);
    }

    static void styleDangerButton(JButton button) {
        styleButton(button, new Color(255, 242, 242), new Color(172, 64, 72),
                new Color(248, 220, 222));
    }

    static void styleButton(JButton button, Color background, Color foreground, Color borderColor) {
        button.setFont(BODY_BOLD);
        button.setForeground(foreground);
        button.setBackground(background);
        button.putClientProperty("posThemeBackground", background);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        if (!(button.getUI() instanceof ThemeButtonUI)) {
            button.setUI(new ThemeButtonUI());
        }
        button.putClientProperty("posThemeBorder", borderColor);
        if (button.getClientProperty("posThemeHover") == null) {
            button.putClientProperty("posThemeHover", Boolean.TRUE);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    button.putClientProperty("posThemeHovered", Boolean.TRUE);
                    button.repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    button.putClientProperty("posThemeHovered", Boolean.FALSE);
                    button.repaint();
                }
            });
        }
    }

    private static final class ThemeButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int inset = 1;
                int width = button.getWidth() - inset * 2 - 1;
                int height = button.getHeight() - inset * 2 - 1;
                Color background = button.isEnabled()
                        ? (Color) button.getClientProperty("posThemeBackground")
                        : new Color(230, 234, 239);
                if (background == null) background = SURFACE;
                ButtonModel model = button.getModel();
                if (button.isEnabled() && model.isPressed() && model.isArmed()) {
                    background = background.darker();
                } else if (button.isEnabled()
                        && Boolean.TRUE.equals(button.getClientProperty("posThemeHovered"))) {
                    background = mix(background, Color.WHITE, 0.12f);
                }
                g.setColor(background);
                g.fillRoundRect(inset, inset, width, height, 12, 12);
                Color border = button.isEnabled()
                        ? (Color) button.getClientProperty("posThemeBorder") : BORDER;
                if (border != null) {
                    g.setColor(border);
                    g.setStroke(new BasicStroke(1f));
                    g.drawRoundRect(inset, inset, width, height, 12, 12);
                }
            } finally {
                g.dispose();
            }
            super.paint(graphics, component);
            if (buttonFocusPainted(button)) {
                Graphics2D focus = (Graphics2D) graphics.create();
                try {
                    focus.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    focus.setColor(TEAL);
                    focus.setStroke(new BasicStroke(1.5f));
                    focus.drawRoundRect(2, 2, button.getWidth() - 5, button.getHeight() - 5, 10, 10);
                } finally {
                    focus.dispose();
                }
            }
        }

        private boolean buttonFocusPainted(JButton button) {
            return button.isFocusOwner() && button.isEnabled();
        }

        private Color mix(Color first, Color second, float amount) {
            float remainder = 1f - amount;
            return new Color(
                    Math.round(first.getRed() * remainder + second.getRed() * amount),
                    Math.round(first.getGreen() * remainder + second.getGreen() * amount),
                    Math.round(first.getBlue() * remainder + second.getBlue() * amount));
        }
    }

    static void styleTextField(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(TEAL_DARK);
        field.setBorder(fieldBorder());
    }

    static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    static void styleTable(JTable table) {
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setSelectionBackground(new Color(221, 241, 238));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setRowHeight(36);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setFont(BODY_BOLD);
        header.setForeground(new Color(74, 91, 111));
        header.setBackground(new Color(247, 249, 252));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setReorderingAllowed(false);
    }

    static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }
}
