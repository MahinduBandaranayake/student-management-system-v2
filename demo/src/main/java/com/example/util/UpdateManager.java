package com.example.util;

import com.example.data.SupabaseConnection;
import com.example.ui.DashboardView;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.http.HttpResponse;

public class UpdateManager {
    public static final String CURRENT_VERSION = "1.2.0"; // Current App Version

    /**
     * Connects to Supabase to check if this specific license has an update available.
     */
    public static void checkForUpdates() {
        String key = LicenseManager.getLicenseKey();
        if (key.equals("GUEST")) return;

        new Thread(() -> {
            try {
                // Query Supabase for the version assigned to this license
                String query = "license_key=eq." + key;
                HttpResponse<String> response = SupabaseConnection.get("licenses", query);

                if (response.statusCode() == 200) {
                    JSONArray results = new JSONArray(response.body());
                    if (results.length() > 0) {
                        JSONObject licenseData = results.getJSONObject(0);
                        String remoteVersion = licenseData.optString("current_version", "1.0.0");
                        boolean isCloudEnabled = licenseData.optBoolean("cloud_sync_enabled", true);
                        boolean isActive = licenseData.optBoolean("is_active", true);

                        if (!isActive) {
                            // Logic for disabled licenses can go here
                            return;
                        }

                        // If the version in Supabase is higher than current, notify user
                        if (isNewer(remoteVersion, CURRENT_VERSION)) {
                            Platform.runLater(() -> {
                                if (DashboardView.getInstance() != null) {
                                    String msg = "A new software update (v" + remoteVersion + ") is available for your system!\n\n" +
                                                 "Contact your administrator to download the latest JAR installer.";
                                    DashboardView.getInstance().notifyUpdate(msg);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Update check failed: " + e.getMessage());
            }
        }).start();
    }

    private static boolean isNewer(String remote, String current) {
        try {
            String[] rParts = remote.split("\\.");
            String[] cParts = current.split("\\.");
            for (int i = 0; i < Math.min(rParts.length, cParts.length); i++) {
                int r = Integer.parseInt(rParts[i]);
                int c = Integer.parseInt(cParts[i]);
                if (r > c) return true;
                if (r < c) return false;
            }
            return rParts.length > cParts.length;
        } catch (Exception e) {
            return false;
        }
    }
}
