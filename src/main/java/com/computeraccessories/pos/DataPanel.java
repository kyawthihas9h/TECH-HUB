package com.computeraccessories.pos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class DataPanel extends JPanel {
    private final String table;
    private final String selectSql;
    private final String[] columns;
    private final String[] labels;
    private final JTable grid = new JTable();
    private final DefaultTableModel model = new DefaultTableModel();
    private final Runnable changed;

    DataPanel(String title, String table, String selectSql, String[] columns, String[] labels) {
        this(title, table, selectSql, columns, labels, () -> { });
    }

    DataPanel(String title, String table, String selectSql, String[] columns, String[] labels, Runnable changed) {
        super(new BorderLayout(8, 8));
        this.table = table;
        this.selectSql = selectSql;
        this.columns = columns;
        this.labels = labels;
        this.changed = changed;
        AppTheme.stylePanel(this, AppTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        grid.setModel(model);
        grid.setAutoCreateRowSorter(true);
        AppTheme.styleTable(grid);
        JLabel heading = new JLabel(title);
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
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit selected");
        JButton delete = new JButton("Delete selected");
        JButton refresh = new JButton("Refresh");
        AppTheme.stylePrimaryButton(add);
        AppTheme.styleButton(edit);
        AppTheme.styleDangerButton(delete);
        AppTheme.styleButton(refresh);
        actions.add(add); actions.add(edit); actions.add(delete); actions.add(refresh);
        add(actions, BorderLayout.SOUTH);
        boolean readOnly = columns.length == 0;
        add.setEnabled(!readOnly);
        edit.setEnabled(!readOnly);
        delete.setEnabled(!readOnly);
        add.addActionListener(e -> editRow(false));
        edit.addActionListener(e -> editRow(true));
        delete.addActionListener(e -> deleteRow());
        refresh.addActionListener(e -> refresh());
        refresh();
    }

    void refresh() {
        try (Connection connection = Db.connect();
             PreparedStatement query = connection.prepareStatement(selectSql);
             ResultSet result = query.executeQuery()) {
            var metadata = result.getMetaData();
            String[] headers = new String[metadata.getColumnCount()];
            for (int i = 0; i < headers.length; i++) headers[i] = metadata.getColumnLabel(i + 1);
            model.setDataVector(new Object[0][headers.length], headers);
            while (result.next()) {
                Object[] row = new Object[headers.length];
                for (int i = 0; i < row.length; i++) row[i] = result.getObject(i + 1);
                model.addRow(row);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void editRow(boolean editing) {
        int viewRow = grid.getSelectedRow();
        if (editing && viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        Object[] oldValues = null;
        if (editing) {
            int row = grid.convertRowIndexToModel(viewRow);
            oldValues = new Object[columns.length];
            for (int i = 0; i < oldValues.length; i++) oldValues[i] = model.getValueAt(row, i + 1);
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        AppTheme.stylePanel(form, AppTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        List<JTextField> fields = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            JTextField input = new JTextField(oldValues == null || oldValues[i] == null
                    ? "" : String.valueOf(oldValues[i]), 24);
            AppTheme.styleTextField(input);
            form.add(new JLabel(labels[i]));
            if ("photo".equals(columns[i])) {
                JPanel photoPicker = new JPanel(new BorderLayout(4, 0));
                photoPicker.add(input, BorderLayout.CENTER);
                JButton browse = new JButton("Browse");
                browse.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        input.setText(file.getAbsolutePath());
                    }
                });
                photoPicker.add(browse, BorderLayout.EAST);
                form.add(photoPicker);
            } else {
                form.add(input);
            }
            fields.add(input);
        }
        int result = JOptionPane.showConfirmDialog(this, form,
                editing ? "Edit record" : "Add record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try (Connection connection = Db.connect()) {
            boolean purchase = "purchases".equals(table);
            if (purchase) connection.setAutoCommit(false);
            try {
                Object oldProductId = null;
                int oldQuantity = 0;
                if (purchase && editing) {
                    int row = grid.convertRowIndexToModel(viewRow);
                    oldProductId = model.getValueAt(row, 1);
                    oldQuantity = ((Number) model.getValueAt(row, 4)).intValue();
                }
            String sql;
            if (editing) {
                String assignments = String.join("=?,", columns) + "=?";
                sql = "UPDATE " + table + " SET " + assignments + " WHERE id=?";
            } else {
                sql = "INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES ("
                        + String.join(",", java.util.Collections.nCopies(columns.length, "?")) + ")";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < fields.size(); i++) {
                    String value = fields.get(i).getText().trim();
                    statement.setObject(i + 1, value.isEmpty() ? null : value);
                }
                if (editing) statement.setObject(fields.size() + 1,
                        model.getValueAt(grid.convertRowIndexToModel(viewRow), 0));
                statement.executeUpdate();
            }
            if (purchase) {
                if (editing) adjustStock(connection, oldProductId, -oldQuantity);
                adjustStock(connection, fields.get(0).getText().trim(),
                        Integer.parseInt(fields.get(3).getText().trim()));
                connection.commit();
            }
            refresh();
            changed.run();
            } catch (Exception e) {
                if ("purchases".equals(table)) connection.rollback();
                throw e;
            } finally {
                if ("purchases".equals(table)) connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteRow() {
        int viewRow = grid.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete the selected record?", "Confirm delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection connection = Db.connect()) {
            boolean purchase = "purchases".equals(table);
            if (purchase) connection.setAutoCommit(false);
            try {
                int modelRow = grid.convertRowIndexToModel(viewRow);
                Object id = model.getValueAt(modelRow, 0);
                Object productId = purchase ? model.getValueAt(modelRow, 1) : null;
                int quantity = purchase ? ((Number) model.getValueAt(modelRow, 4)).intValue() : 0;
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE id=?")) {
                    delete.setObject(1, id);
                    delete.executeUpdate();
                }
                if (purchase) {
                    adjustStock(connection, productId, -quantity);
                    connection.commit();
                }
            } catch (Exception e) {
                if (purchase) connection.rollback();
                throw e;
            } finally {
                if (purchase) connection.setAutoCommit(true);
            }
            refresh();
            changed.run();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void adjustStock(Connection connection, Object productId, int delta) throws Exception {
        if (delta == 0) return;
        String sql = delta > 0
                ? "UPDATE products SET quantity=quantity+? WHERE id=?"
                : "UPDATE products SET quantity=quantity-? WHERE id=? AND quantity>=?";
        try (PreparedStatement update = connection.prepareStatement(sql)) {
            update.setInt(1, Math.abs(delta));
            update.setObject(2, productId);
            if (delta < 0) update.setInt(3, Math.abs(delta));
            if (update.executeUpdate() != 1) {
                throw new IllegalStateException("Purchase change would make inventory stock negative or product is missing.");
            }
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
