package com.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SupabaseConnection {
    private static final String URL = "jdbc:postgresql://db.egnnleprgpkpoultnkwa.supabase.co:5432/postgres";
    private static final String USER = "postgres.egnnleprgpkpoultnkwa";
    private static final String PASS = "Admin12345#";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
