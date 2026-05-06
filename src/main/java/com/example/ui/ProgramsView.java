package com.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ProgramsView {
    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getChildren().add(new Label("Academic Programs"));
        return root;
    }
}
