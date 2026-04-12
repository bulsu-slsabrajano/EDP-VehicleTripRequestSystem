package com.driver.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VehiclePanel extends VBox {

    private TableView<Object[]> vehicleTable;
    private ObservableList<Object[]> tableData;
    private ComboBox<String> vehicleDropdown;
    private final List<Integer> vehicleIds = new ArrayList<>();
    private final List<Object[]> allRows   = new ArrayList<>();

    public VehiclePanel() {
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(20));
        setSpacing(15);
        buildUI();
    }

    public void loadData() { loadVehicles(); }

    private void buildUI() {
        Label title = new Label("Vehicle");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");

        // ── Dropdown filter ───────────────────────────────────────────────────
        vehicleDropdown = new ComboBox<>(FXCollections.observableArrayList("All Types"));
        vehicleDropdown.getStyleClass().add("combo-field");
        vehicleDropdown.setPrefWidth(200);
        vehicleDropdown.setValue("All Types");

        vehicleDropdown.setOnAction(e -> {
            int idx = vehicleDropdown.getSelectionModel().getSelectedIndex();
            if (idx == 0) {
                DriverData.selectedVehicleId = null;
                tableData.setAll(allRows);
            } else {
                String selectedType = vehicleDropdown.getValue();
                DriverData.selectedVehicleId = null;
                tableData.clear();
                for (Object[] row : allRows) {
                    if (row[3].toString().equalsIgnoreCase(selectedType)) {
                        tableData.add(row);
                        if (DriverData.selectedVehicleId == null)
                            DriverData.selectedVehicleId = (int) row[0];
                    }
                }
            }
            TripPanel.refreshTrips();
        });

        HBox filterBar = new HBox(10,
            new Label("Filter by Vehicle Type:"), vehicleDropdown);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // ── Table ─────────────────────────────────────────────────────────────
        vehicleTable = FxUtil.buildTable(
            "Vehicle ID", "Model", "Plate", "Type", "Capacity", "Status");
        FxUtil.applyStatusRenderer(vehicleTable, 5);
        tableData = FxUtil.tableData(vehicleTable);
        VBox.setVgrow(vehicleTable, Priority.ALWAYS);

        vehicleTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> {
                if (row == null) return;
                DriverData.selectedVehicleId = (int) row[0];
                TripPanel.refreshTrips();
            });

        getChildren().addAll(title, filterBar, FxUtil.tableScroll(vehicleTable));
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    private void loadVehicles() {
        allRows.clear();
        vehicleIds.clear();
        tableData.clear();
        vehicleDropdown.getItems().setAll("All Types");
        vehicleDropdown.setValue("All Types");

        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT v.vehicle_id, v.vehicle_model, v.plate_number," +
                "       v.vehicle_type, v.passenger_capacity, v.vehicle_status " +
                "FROM Vehicle_Assignment va " +
                "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id " +
                "WHERE va.driver_id=?");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();
            List<String> addedTypes = new ArrayList<>();

            while (rs.next()) {
                int vid  = rs.getInt("vehicle_id");
                vehicleIds.add(vid);
                String type = rs.getString("vehicle_type");
                if (!addedTypes.contains(type)) {
                    vehicleDropdown.getItems().add(type);
                    addedTypes.add(type);
                }
                Object[] row = {
                    vid,
                    rs.getString("vehicle_model"),
                    rs.getString("plate_number"),
                    type,
                    rs.getInt("passenger_capacity"),
                    rs.getString("vehicle_status")
                };
                allRows.add(row);
                tableData.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}