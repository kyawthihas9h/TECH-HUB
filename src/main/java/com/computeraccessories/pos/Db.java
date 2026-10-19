package com.computeraccessories.pos;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

final class Db {
    private static final String DEFAULT_DATABASE = "computer_accessories_pos";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/" + DEFAULT_DATABASE + "?serverTimezone=UTC&useSSL=false";

    private Db() {
    }

    static Connection connect() throws Exception {
        return DriverManager.getConnection(databaseUrl(), databaseUser(), databasePassword());
    }

    static void initialize() throws Exception {
        createDatabaseIfMissing();
        String schema;
        try (var stream = Db.class.getResourceAsStream("/schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Database schema resource /schema.sql was not found.");
            }
            schema = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
        }
    }

    private static void createDatabaseIfMissing() throws Exception {
        String database = databaseName(databaseUrl());
        String serverUrl = serverUrl(databaseUrl());
        String sql = "CREATE DATABASE IF NOT EXISTS `" + database
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection connection = DriverManager.getConnection(serverUrl, databaseUser(), databasePassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static String databaseUser() {
        return System.getenv().getOrDefault("POS_DB_USER", "root");
    }

    private static String databasePassword() {
        String password = System.getenv("POS_DB_PASSWORD");
        if (password == null) {
            throw new IllegalStateException("Run the application using run.bat to securely enter the MySQL password.");
        }
        return password;
    }

    private static String databaseUrl() {
        String configuredUrl = System.getenv().getOrDefault("POS_DB_URL", DEFAULT_URL);
        int schemeEnd = configuredUrl.indexOf("://");
        if (!configuredUrl.startsWith("jdbc:mysql://") || schemeEnd < 0) {
            throw new IllegalArgumentException("POS_DB_URL must be a MySQL JDBC URL starting with jdbc:mysql://.");
        }
        int authorityStart = schemeEnd + 3;
        int pathStart = configuredUrl.indexOf('/', authorityStart);
        int queryStart = configuredUrl.indexOf('?', authorityStart);
        if (pathStart < 0 || (queryStart >= 0 && pathStart > queryStart)
                || pathStart + 1 == configuredUrl.length()
                || (queryStart >= 0 && pathStart + 1 == queryStart)) {
            String database = quotedDatabaseName(DEFAULT_DATABASE);
            int insertionPoint = queryStart < 0 ? configuredUrl.length() : queryStart;
            return configuredUrl.substring(0, insertionPoint) + "/" + database
                    + configuredUrl.substring(insertionPoint);
        }
        databaseName(configuredUrl);
        return configuredUrl;
    }

    private static String databaseName(String url) {
        int authorityStart = url.indexOf("://") + 3;
        int pathStart = url.indexOf('/', authorityStart);
        int queryStart = url.indexOf('?', authorityStart);
        if (pathStart < 0 || (queryStart >= 0 && pathStart > queryStart)) return DEFAULT_DATABASE;
        int pathEnd = queryStart < 0 ? url.length() : queryStart;
        String database = url.substring(pathStart + 1, pathEnd);
        if (database.isEmpty()) return DEFAULT_DATABASE;
        return quotedDatabaseName(database);
    }

    private static String quotedDatabaseName(String database) {
        if (!database.matches("[A-Za-z0-9_$]{1,64}")) {
            throw new IllegalArgumentException(
                    "The database name in POS_DB_URL must contain only letters, numbers, underscores, or '$'.");
        }
        return database;
    }

    private static String serverUrl(String url) {
        int authorityStart = url.indexOf("://") + 3;
        int pathStart = url.indexOf('/', authorityStart);
        int queryStart = url.indexOf('?', authorityStart);
        int authorityEnd = pathStart < 0 || (queryStart >= 0 && pathStart > queryStart)
                ? (queryStart < 0 ? url.length() : queryStart)
                : pathStart;
        String query = queryStart < 0 ? "" : url.substring(queryStart);
        return url.substring(0, authorityEnd) + "/" + query;
    }

    static void bootstrapAdmin() throws Exception {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            result.next();
            if (result.getLong(1) != 0) return;
        }
        JTextField username = new JTextField("admin", 20);
        JPasswordField password = new JPasswordField(20);
        JPasswordField confirmation = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Admin username"));
        form.add(username);
        form.add(new JLabel("Password (at least 8 characters)"));
        form.add(password);
        form.add(new JLabel("Confirm password"));
        form.add(confirmation);
        int choice = JOptionPane.showConfirmDialog(null, form, "Create first admin account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            throw new IllegalStateException("First-admin setup was canceled. Start the application again to set it up.");
        }
        char[] secret = password.getPassword();
        char[] confirmedSecret = confirmation.getPassword();
        if (username.getText().isBlank() || secret.length < 8 || !Arrays.equals(secret, confirmedSecret)) {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirmedSecret, '\0');
            throw new IllegalArgumentException("Enter a username, a password of at least 8 characters, "
                    + "and matching password confirmation. Restart to try again.");
        }
        String passwordValue = new String(secret);
        Arrays.fill(secret, '\0');
        Arrays.fill(confirmedSecret, '\0');
        try (Connection connection = connect();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO users (username, display_name, password_hash, role) VALUES (?, ?, ?, 'ADMIN')")) {
            insert.setString(1, username.getText().trim());
            insert.setString(2, username.getText().trim());
            insert.setString(3, BCrypt.hashpw(passwordValue, BCrypt.gensalt(12)));
            insert.executeUpdate();
        }
    }
}
