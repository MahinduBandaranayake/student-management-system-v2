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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
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
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GradeRange, String> nameCol = new TableColumn<>("Grade");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("gradeName"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            e.getRowValue().setGradeName(e.getNewValue());
            updateGrade(e.getRowValue().getId(), "grade_name", e.getNewValue());
        });

        TableColumn<GradeRange, Double> minCol = new TableColumn<>("Min Score");
        minCol.setCellValueFactory(new PropertyValueFactory<>("minScore"));
        minCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        minCol.setOnEditCommit(e -> {
            e.getRowValue().setMinScore(e.getNewValue());
            updateGrade(e.getRowValue().getId(), "min_score", e.getNewValue());
        });

        TableColumn<GradeRange, Double> maxCol = new TableColumn<>("Max Score");
        maxCol.setCellValueFactory(new PropertyValueFactory<>("maxScore"));
        maxCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        maxCol.setOnEditCommit(e -> {
            e.getRowValue().setMaxScore(e.getNewValue());
            updateGrade(e.getRowValue().getId(), "max_score", e.getNewValue());
        });

        TableColumn<GradeRange, Double> gpaCol = new TableColumn<>("GPA Points");
        gpaCol.setCellValueFactory(new PropertyValueFactory<>("gpaPoints"));
        gpaCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        gpaCol.setOnEditCommit(e -> {
            e.getRowValue().setGpaPoints(e.getNewValue());
            updateGrade(e.getRowValue().getId(), "gpa_points", e.getNewValue());
        });

        table.getColumns().addAll(nameCol, minCol, maxCol, gpaCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        refreshTable(table);

        root.getChildren().addAll(header, table);
        return root;
    }

    private void updateGrade(int id, String column, Object value) {
        String sql = "UPDATE grade_ranges SET " + column + " = ? WHERE id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (value instanceof String) {
                pstmt.setString(1, (String) value);
            } else if (value instanceof Double) {
                pstmt.setDouble(1, (Double) value);
            }
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            
            // Notification mechanism could be triggered here
            if (DashboardView.getInstance() != null) {
                DashboardView.getInstance().notifyUpdate("Grades scale updated: " + column);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        public SimpleIntegerProperty idProperty() { return id; }
        
        public String getGradeName() { return gradeName.get(); }
        public void setGradeName(String name) { this.gradeName.set(name); }
        public SimpleStringProperty gradeNameProperty() { return gradeName; }
        
        public double getMinScore() { return minScore.get(); }
        public void setMinScore(double min) { this.minScore.set(min); }
        public SimpleDoubleProperty minScoreProperty() { return minScore; }
        
        public double getMaxScore() { return maxScore.get(); }
        public void setMaxScore(double max) { this.maxScore.set(max); }
        public SimpleDoubleProperty maxScoreProperty() { return maxScore; }
        
        public double getGpaPoints() { return gpaPoints.get(); }
        public void setGpaPoints(double gpa) { this.gpaPoints.set(gpa); }
        public SimpleDoubleProperty gpaPointsProperty() { return gpaPoints; }
    }
}