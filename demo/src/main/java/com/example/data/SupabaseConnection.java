package com.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles the direct PostgreSQL connection to the Supabase Cloud Database.
 * This acts as the Cloud Backup/Router as per the Offline-First Sync
 * Architecture.
 */
public class SupabaseConnection {

    // Supabase Project Reference: egnnleprgpkpoultnkwa
    // REST API URL: https://egnnleprgpkpoultnkwa.supabase.co
    // Publishable API Key: sb_publishable_h5n2y5ZuyoxUrZ69HRDrlw_-ZATCCvt

    // JDBC Connection details provided by the user
    // IMPORTANT: Replace [YOUR-PASSWORD] with your actual Supabase database
    // password
    private static final String DB_PASSWORD = "mpv4$ciGs123";
    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?stringtype=unspecified";
    private static final String DB_USER = "postgres.egnnleprgpkpoultnkwa";
    
    // REST API Details for lightweight checks
    private static final String REST_URL = "https://egnnleprgpkpoultnkwa.supabase.co/rest/v1/";
    private static final String API_KEY = "sb_publishable_h5n2y5ZuyoxUrZ69HRDrlw_-ZATCCvt";

    private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    /**
     * Gets a connection to the Supabase cloud database.
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Ensure the PostgreSQL driver is loaded
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Make sure it's in your pom.xml.");
            e.printStackTrace();
        }

        // Connect to Supabase
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Test the connection to the cloud database.
     */
    public static void testConnection() {
        if ("[YOUR-PASSWORD]".equals(DB_PASSWORD)) {
            System.err.println("[Cloud DB] Warning: Password has not been set! Please edit SupabaseConnection.java");
            return;
        }

        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("[Cloud DB] Successfully connected to Supabase PostgreSQL Database!");
            }
        } catch (SQLException e) {
            System.err.println("[Cloud DB] Connection failed!");
            e.printStackTrace();
        }
    }

    /**
     * Performs a lightweight REST GET request to Supabase.
     * Useful for license checks and versioning without opening a full JDBC connection.
     */
    public static java.net.http.HttpResponse<String> get(String table, String query) throws Exception {
        String url = REST_URL + table + "?" + query;
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("apikey", API_KEY)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }
}
