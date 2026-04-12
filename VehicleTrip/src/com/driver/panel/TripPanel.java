package com.driver.panel;

import com.driver.query.VehicleAvailabilityService;
import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.*;

public class TripPanel extends VBox {

    private static ObservableList<Object[]> pendingData   = FXCollections.observableArrayList();
    private static ObservableList<Object[]> approvedData  = FXCollections.observableArrayList();
    private static ObservableList<Object[]> completedData = FXCollections.observableArrayList();
    private static ObservableList<Object[]> cancelledData = FXCollections.observableArrayList();

    public TripPanel() { this("Driver"); }

    public TripPanel(String username) {
        setBackground(Background.fill(Color.WHITE));
        buildUI();
    }

    private void buildUI() {
        Label title = new Label("Trips");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");
        title.setPadding(new Insets(10, 20, 10, 20));

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
            buildTab("Pending",   pendingData),
            buildTab("Accepted",  approvedData),
            buildTab("Completed", completedData),
            buildTab("Cancelled", cancelledData)
        );

        getChildren().addAll(title, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        refreshTrips();
    }

    private Tab buildTab(String name, ObservableList<Object[]> data) {
        TableView<Object[]> table = FxUtil.buildTable(
            "Trip ID", "Passenger", "Pick Up", "Destination",
            "No. of Passengers", "Vehicle", "Start Date", "End Date", "Status");
        table.setItems(data);
        FxUtil.applyStatusRenderer(table, 8);

        VBox content = new VBox(FxUtil.tableScroll(table));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.setBackground(Background.fill(Color.WHITE));

        Tab tab = new Tab(name, content);
        return tab;
    }

    public static void refreshTrips() {
        pendingData.clear();
        approvedData.clear();
        completedData.clear();
        cancelledData.clear();

        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT t.trip_id, u.username AS passenger_name," +
                "       t.pick_up_location, t.destination, t.passenger_count," +
                "       v.vehicle_model, v.plate_number," +
                "       t.start_date, t.end_date, t.trip_status, va.vehicle_id " +
                "FROM Trip t " +
                "JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id " +
                "JOIN Users u   ON t.passenger_id=u.user_id " +
                "WHERE va.driver_id=? " +
                "AND (? IS NULL OR va.vehicle_id=?)");

            ps.setInt(1, DriverData.driverId);
            if (DriverData.selectedVehicleId == null) {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(2, DriverData.selectedVehicleId);
                ps.setInt(3, DriverData.selectedVehicleId);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("trip_id"),
                    rs.getString("passenger_name"),
                    rs.getString("pick_up_location"),
                    rs.getString("destination"),
                    rs.getInt("passenger_count"),
                    rs.getString("vehicle_model") + " (" + rs.getString("plate_number") + ")",
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("trip_status")
                };
                switch (rs.getString("trip_status").toUpperCase()) {
                    case "PENDING"   -> pendingData.add(row);
                    case "APPROVED"  -> approvedData.add(row);
                    case "COMPLETED" -> completedData.add(row);
                    case "CANCELLED" -> cancelledData.add(row);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}