package com.example.ui;

import com.example.data.DatabaseHandler;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.*;

public class GradesView {
    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #ffffff;");

        Label header = new Label("Academic Grade Scales (GPA)");
        header.getStyleClass().add("header-label");

        TableView<GradeRange> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GradeRange, String> nameCol = new TableColumn<>("Grade");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("gradeName"));
        TableColumn<GradeRange, Double> minCol = new TableColumn<>("Min Score");
        minCol.setCellValueFactory(new PropertyValueFactory<>("minScore"));
        TableColumn<GradeRange, Double> maxCol = new TableColumn<>("Max Score");
        maxCol.setCellValueFactory(new PropertyValueFactory<>("maxScore"));
        TableColumn<GradeRange, Double> gpaCol = new TableColumn<>("GPA Points");
        gpaCol.setCellValueFactory(new PropertyValueFactory<>("gpaPoints"));

        table.getColumns().addAll(nameCol, minCol, maxCol, gpaCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        refreshTable(table);

        root.getChildren().addAll(header, table);
        return root;
    }

    private void refreshTable(TableView<GradeRange> table) {
        ObservableList<GradeRange> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM grade_ranges ORDER BY min_score DESC")) {
            while (rs.next()) {
                list.add(new GradeRange(
                    rs.getInt("id"),
                    rs.getString("grade_name"),
                    rs.getDouble("min_score"),
                    rs.getDouble("max_score"),
                    rs.getDouble("gpa_points")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        table.setItems(list);
    }

    public static class GradeRange {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty gradeName;
        private final SimpleDoubleProperty minScore;
        private final SimpleDoubleProperty maxScore;
        private final SimpleDoubleProperty gpaPoints;

        public GradeRange(int id, String name, double min, double max, double gpa) {
            this.id = new SimpleIntegerProperty(id);
            this.gradeName = new SimpleStringProperty(name);
            this.minScore = new SimpleDoubleProperty(min);
            this.maxScore = new SimpleDoubleProperty(max);
            this.gpaPoints = new SimpleDoubleProperty(gpa);
        }

        public int getId() { return id.get(); }
        public String getGradeName() { return gradeName.get(); }
        public double getMinScore() { return minScore.get(); }
        public double getMaxScore() { return maxScore.get(); }
        public double getGpaPoints() { return gpaPoints.get(); }
    }
}
