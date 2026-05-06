package com.example.ui;

import java.util.ArrayList;
import java.util.List;

public class ExtensionLoader {
    private static List<ExtensionModule> loadedModules = new ArrayList<>();
    public static List<ExtensionModule> getLoadedModules() {
        return loadedModules;
    }
    public static void reload() {}
    public static String getExtensionPath() { return "extensions/"; }
}
