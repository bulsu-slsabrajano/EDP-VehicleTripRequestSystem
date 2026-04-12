package com.driver.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AssignmentPanel extends VBox {

    private TableView<Object[]> assignmentTable;
    private ObservableList<Object[]> tableData;

    // Detail labels
    private Label vehicleIdValue    = detailLbl();
    private Label plateNumberValue  = detailLbl();
    private Label typeValue         = detailLbl();
    private Label dateAssignedValue = detailLbl();
    private Label statusValue       = detailLbl();

    public AssignmentPanel() {
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(20));
        setSpacing(15);
        buildUI();
    }

    public void loadData() { loadFromDb(); }

    private void buildUI() {
        Label title = new Label("Assignments");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");

        // ── Assignments table section ─────────────────────────────────────────
        VBox tableSection = new VBox(10);
        tableSection.setStyle(
            "-fx-border-color:#C8D2E6;-fx-border-width:1px;-fx-border-radius:6px;" +
            "-fx-background-color:white;-fx-background-radius:6px;-fx-padding:16px;");

        Label sectionTitle = new Label("Vehicle Assignments");
        sectionTitle.getStyleClass().add("title-small");

        assignmentTable = FxUtil.buildTable(
            "Vehicle ID", "Plate No.", "Type", "Date Assigned", "Assignment Status");
        FxUtil.applyStatusRenderer(assignmentTable, 4);
        tableData = FxUtil.tableData(assignmentTable);
        assignmentTable.setPrefHeight(220);

        assignmentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> {
                if (row == null) return;
                vehicleIdValue.setText(row[0].toString());
                plateNumberValue.setText(row[1].toString());
                typeValue.setText(row[2].toString());
                dateAssignedValue.setText(row[3].toString());
                String s = row[4].toString();
                statusValue.setText(s);
                statusValue.setStyle("Active".equalsIgnoreCase(s)
                    ? "-fx-text-fill:#228B22;-fx-font-weight:bold;"
                    : "-fx-text-fill:#B40000;-fx-font-weight:bold;");
            });

        tableSection.getChildren().addAll(sectionTitle, FxUtil.tableScroll(assignmentTable));
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        // ── Detail card ───────────────────────────────────────────────────────
        VBox detailCard = new VBox(10);
        detailCard.setStyle(
            "-fx-border-color:#C8D2E6;-fx-border-width:1px;-fx-border-radius:6px;" +
            "-fx-background-color:white;-fx-background-radius:6px;-fx-padding:16px;");

        Label detailTitle = new Label("Assignment Details");
        detailTitle.getStyleClass().add("title-small");

        GridPane grid = FxUtil.formGrid();
        int y = 0;
        FxUtil.addInfoRow(grid, "Vehicle ID:",     vehicleIdValue,    y++);
        FxUtil.addInfoRow(grid, "Plate Number:",   plateNumberValue,  y++);
        FxUtil.addInfoRow(grid, "Vehicle Type:",   typeValue,         y++);
        FxUtil.addInfoRow(grid, "Date Assigned:",  dateAssignedValue, y++);
        FxUtil.addInfoRow(grid, "Status:",         statusValue,       y);

        detailCard.getChildren().addAll(detailTitle, grid);

        getChildren().addAll(title, tableSection, detailCard);
    }

    private void loadFromDb() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT v.vehicle_id, v.plate_number, v.vehicle_type," +
                "       va.date_assigned, va.assignment_status " +
                "FROM Vehicle_Assignment va " +
                "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id " +
                "WHERE va.driver_id=? ORDER BY va.date_assigned DESC");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();
            tableData.clear();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("vehicle_id"),
                    rs.getString("plate_number"),
                    rs.getString("vehicle_type"),
                    rs.getDate("date_assigned"),
                    rs.getString("assignment_status")
                });
            }
            if (!tableData.isEmpty()) {
                assignmentTable.getSelectionModel().selectFirst();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static Label detailLbl() {
        Label l = new Label("-");
        l.getStyleClass().add("form-label");
        return l;
    }
}