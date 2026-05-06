package com.example.ui;

import com.example.data.DatabaseHandler;
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

public class EventsView {
    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #ffffff;");

        Label header = new Label("Campus Events Calendar");
        header.getStyleClass().add("header-label");

        TableView<Event> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Event, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Event, String> titleCol = new TableColumn<>("Event Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Event, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Event, String> managerCol = new TableColumn<>("Manager");
        managerCol.setCellValueFactory(new PropertyValueFactory<>("manager"));

        table.getColumns().addAll(dateCol, titleCol, descCol, managerCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        refreshTable(table);

        root.getChildren().addAll(header, table);
        return root;
    }

    private void refreshTable(TableView<Event> table) {
        ObservableList<Event> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM events ORDER BY event_date DESC")) {
            while (rs.next()) {
                list.add(new Event(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("event_date"),
                    rs.getString("description"),
                    rs.getString("manager_name")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        table.setItems(list);
    }

    public static class Event {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty title;
        private final SimpleStringProperty date;
        private final SimpleStringProperty description;
        private final SimpleStringProperty manager;

        public Event(int id, String t, String d, String desc, String m) {
            this.id = new SimpleIntegerProperty(id);
            this.title = new SimpleStringProperty(t);
            this.date = new SimpleStringProperty(d);
            this.description = new SimpleStringProperty(desc);
            this.manager = new SimpleStringProperty(m);
        }

        public int getId() { return id.get(); }
        public String getTitle() { return title.get(); }
        public String getDate() { return date.get(); }
        public String getDescription() { return description.get(); }
        public String getManager() { return manager.get(); }
    }
}
