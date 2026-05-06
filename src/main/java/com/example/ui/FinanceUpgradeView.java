package com.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FinanceUpgradeView {
    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getChildren().add(new Label("Finance Upgrade Module"));
        return root;
    }
}
