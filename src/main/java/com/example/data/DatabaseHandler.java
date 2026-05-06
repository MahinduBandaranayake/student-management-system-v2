package com.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:database.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void saveSetting(String key, String value) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
            stmt.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('" + key + "', '" + value + "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logCloudSync(String status, String message) {
        System.out.println("Cloud Sync [" + status + "]: " + message);
    }
}
