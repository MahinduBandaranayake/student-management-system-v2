package com.example.util;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class LicenseManager {
    private static String cachedKey = null;
    private static final String LICENSE_FILE = "license.txt";

    /**
     * Reads the license key from a local license.txt file.
     * If the file doesn't exist, it returns "GUEST".
     */
    public static String getLicenseKey() {
        if (cachedKey != null) return cachedKey;

        try {
            File file = new File(LICENSE_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath());
                if (!lines.isEmpty()) {
                    cachedKey = lines.get(0).trim();
                    return cachedKey;
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading license file: " + e.getMessage());
        }

        return "GUEST"; // Default if no license is found
    }

    public static boolean isRegistered() {
        return !getLicenseKey().equals("GUEST");
    }
}
