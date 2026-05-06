package com.example.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudMigrationService {

    public static void syncSchemaDynamically() {
        System.out.println("Starting Dynamic Schema Synchronization...");
        try (Connection sqliteConn = DatabaseHandler.getConnection();
             Connection pgConn = SupabaseConnection.getConnection()) {
            
            DatabaseMetaData dbm = sqliteConn.getMetaData();
            try (ResultSet tables = dbm.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName.startsWith("sqlite_")) continue;
                    
                    // Create table in Postgres if it doesn't exist
                    ensureTableExists(pgConn, sqliteConn, tableName);
                    
                    // Add missing columns
                    syncColumns(pgConn, sqliteConn, tableName);
                }
            }
            System.out.println("Schema Sync Completed Successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ensureTableExists(Connection pgConn, Connection sqliteConn, String tableName) throws SQLException {
        try (Statement st = pgConn.createStatement()) {
            // Very basic table creation if missing
            st.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (id SERIAL PRIMARY KEY)");
        } catch (SQLException e) {
            // Table might exist but without SERIAL id, or named differently
        }
    }

    private static void syncColumns(Connection pgConn, Connection sqliteConn, String tableName) throws SQLException {
        List<String> sqliteCols = new ArrayList<>();
        try (Statement st = sqliteConn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                
                // Map SQLite types to Postgres
                String pgType = mapType(type);
                
                try (Statement st2 = pgConn.createStatement()) {
                    st2.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS \"" + name + "\" " + pgType);
                } catch (SQLException e) {
                    // Column might already exist
                }
            }
        }
    }

    private static String mapType(String sqliteType) {
        sqliteType = sqliteType.toUpperCase();
        if (sqliteType.contains("INT")) return "INTEGER";
        if (sqliteType.contains("TEXT") || sqliteType.contains("CHAR")) return "TEXT";
        if (sqliteType.contains("REAL") || sqliteType.contains("DOUBLE") || sqliteType.contains("FLOAT")) return "DOUBLE PRECISION";
        if (sqliteType.contains("BLOB")) return "BYTEA";
        return "TEXT";
    }
    
    public static boolean migrateAllData() {
        // Correct order of insertion due to Foreign Keys
        List<String> tables = Arrays.asList(
            "grade_ranges", "settings", "users", "programs", "program_batches", 
            "students", "lecturers", "events", "courses", "enrollments",
            "sessions", "attendance", "marks", "submissions", 
            "materials", "student_documents", "payments"
        );
        
        // First, dynamically update Postgres schema to match SQLite
        syncSchemaDynamically();
        
        try (Connection sqliteConn = DatabaseHandler.getConnection();
             Connection pgConn = SupabaseConnection.getConnection()) {
             
             for (String table : tables) {
                 migrateTable(table, sqliteConn, pgConn);
             }
             
             // Sync sequences for SERIAL primary keys in Postgres
             for (String table : tables) {
                 try (Statement st = pgConn.createStatement()) {
                     st.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE(MAX(id), 1)) FROM " + table);
                 } catch (Exception ignored) {
                     // Some tables like settings might not have 'id'
                 }
             }
             
             DatabaseHandler.logCloudSync("SUCCESS", "Data successfully transferred to Supabase cloud.");
             return true;
             
        } catch (SQLException e) {
            e.printStackTrace();
            DatabaseHandler.logCloudSync("FAILED", "Error: " + e.getMessage());
            return false;
        }
    }
    
    private static void migrateTable(String tableName, Connection sqliteConn, Connection pgConn) throws SQLException {
        try (Statement st = sqliteConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            
            StringBuilder cols = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            StringBuilder updatePart = new StringBuilder();
            
            for (int i=1; i<=colCount; i++) {
                String colName = meta.getColumnName(i);
                cols.append("\"").append(colName).append("\"").append(i < colCount ? "," : "");
                placeholders.append("?").append(i < colCount ? "," : "");
                
                // Exclude conflict targets from the update list
                boolean isConflictCol = colName.equalsIgnoreCase("id") || 
                                       colName.equalsIgnoreCase("key") || 
                                       colName.equalsIgnoreCase("grade_name") ||
                                       (tableName.equals("attendance") && (colName.equalsIgnoreCase("student_id") || colName.equalsIgnoreCase("session_id")));
                
                if (!isConflictCol) {
                    if (updatePart.length() > 0) updatePart.append(", ");
                    updatePart.append("\"").append(colName).append("\" = EXCLUDED.\"").append(colName).append("\"");
                }
            }
            
            String conflictTarget = "id";
            if (tableName.equals("settings")) conflictTarget = "\"key\"";
            else if (tableName.equals("grade_ranges")) conflictTarget = "grade_name";
            else if (tableName.equals("attendance")) conflictTarget = "student_id, session_id";

            String insertSql = "INSERT INTO " + tableName + " (" + cols.toString() + ") VALUES (" + placeholders.toString() + ") " +
                               "ON CONFLICT (" + conflictTarget + ") ";
            
            if (updatePart.length() > 0) {
                insertSql += "DO UPDATE SET " + updatePart.toString();
            } else {
                insertSql += "DO NOTHING";
            }
            
            try (PreparedStatement ps = pgConn.prepareStatement(insertSql)) {
                int count = 0;
                int found = 0;
                while (rs.next()) {
                    found++;
                    try {
                        for (int i=1; i<=colCount; i++) {
                            ps.setObject(i, rs.getObject(i));
                        }
                        ps.executeUpdate();
                        count++;
                    } catch (SQLException ex) {
                        System.err.println("  [Row Error] Table " + tableName + ": " + ex.getMessage());
                    }
                }
                System.out.println("Synced " + count + " rows for table: " + tableName + " (Total in SQLite: " + found + ")");
            }
        } catch (SQLException e) {
             System.err.println("Skipping or Error in table " + tableName + ": " + e.getMessage());
             if (e.getNextException() != null) {
                 System.err.println("Cause: " + e.getNextException().getMessage());
             }
        }
    }
}
