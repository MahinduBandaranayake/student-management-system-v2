package com.example.ui;

import javafx.scene.Parent;

public interface ExtensionModule {
    String getMenuLabel();
    String getTooltip();
    String getInternalName();
    Parent getView();
    default String getTargetFeature() { return null; }
}
