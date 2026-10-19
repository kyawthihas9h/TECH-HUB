package com.computeraccessories.pos;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppTheme.install();
                Db.initialize();
                Db.bootstrapAdmin();
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage() == null ? e.toString() : e.getMessage(),
                        "POS startup failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
