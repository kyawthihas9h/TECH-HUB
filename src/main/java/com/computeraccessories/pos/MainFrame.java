package com.computeraccessories.pos;

import javax.swing.*;
import java.awt.*;

final class MainFrame extends JFrame {
    MainFrame(User user) {
        super("TechHub Computer Accessories POS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppTheme.PAGE);
        if (user.isAdmin()) {
            add(new AdminPanel(user));
        } else {
            add(new PosPanel(user));
        }
    }
}
