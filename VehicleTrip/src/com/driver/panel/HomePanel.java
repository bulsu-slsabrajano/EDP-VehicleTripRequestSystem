package com.driver.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HomePanel extends VBox {

    private Label pendingCount  = new Label("0");
    private Label approvedCount = new Label("0");
    private Label totalCount    = new Label("0");

    private Label  statusLbl  = new Label("Available");
    private Button toggleBtn  = new Button("Set Unavailable");
    private boolean isAvailable = true;

    private TableView<Object[]> recentTable;
    private javafx.collections.ObservableList<Object[]> recentData;
    private Label welcomeLabel;

    public HomePanel() { this(DriverData.username); }

    public HomePanel(String username) {
        setBackground(Background.fill(Color.WHITE));
        buildUI();
    }

    private void buildUI() {
        
        welcomeLabel = new Label("Welcome, " + nvl(DriverData.username));
        welcomeLabel.getStyleClass().add("welcome-title");
        //welcomeLabel.getStyleClass().add("welcome-style");
        welcomeLabel.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");

        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> loadData());

        HBox topBar = new HBox(welcomeLabel, FxUtil.hgrow(), btnRefresh);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));

        //cards row
        HBox statsRow = new HBox(10,
            statCard("Pending Trips",  pendingCount,  "#FF8C00"),
            statCard("Approved Trips", approvedCount, "#0066CC"),
            statCard("Total Trips",    totalCount,    "#008000"));
        statsRow.setPadding(new Insets(0, 20, 10, 20));

        
        VBox statusCard = buildStatusCard();
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        HBox statusRow = new HBox(statusCard);
        statusRow.setPadding(new Insets(0, 20, 10, 20));

        //Recent trips 
        Label recentTitle = new Label("Recent Trips");
        recentTitle.getStyleClass().add("title-small");
        recentTitle.setPadding(new Insets(0, 0, 6, 0));

        recentTable = FxUtil.buildTable("Trip ID", "Destination", "Date", "Status");
        FxUtil.applyStatusRenderer(recentTable, 3);
        recentData = FxUtil.tableData(recentTable);
        VBox.setVgrow(recentTable, Priority.ALWAYS);

        VBox recentBox = new VBox(6, recentTitle, FxUtil.tableScroll(recentTable));
        recentBox.setPadding(new Insets(0, 20, 20, 20));
        VBox.setVgrow(recentBox, Priority.ALWAYS);

        getChildren().addAll(topBar, statsRow, statusRow, recentBox);
        VBox.setVgrow(recentBox, Priority.ALWAYS);
    }

    //Card 
    private VBox statCard(String title, Label value, String colour) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:white;-fx-border-color:" + colour +
                ";-fx-border-width:2px;-fx-border-radius:6px;-fx-background-radius:6px;-fx-padding:10px;");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefHeight(90);

        value.setStyle("-fx-font-weight:bold;-fx-font-size:28px;-fx-text-fill:" + colour + ";");
        value.setAlignment(Pos.CENTER);
        value.setMaxWidth(Double.MAX_VALUE);

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + colour + ";");
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(lbl, value);
        return card;
    }

    private VBox buildStatusCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("driver-status-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(90);
        card.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Driver Status");
        title.getStyleClass().add("title-small");

        statusLbl.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");

        toggleBtn.getStyleClass().addAll("btn");
        toggleBtn.setPrefSize(140, 35);
        toggleBtn.setOnAction(e -> {
            isAvailable = !isAvailable;
            updateDriverStatus(isAvailable ? "Available" : "Not Available");
            applyStatusUI();
        });

        HBox center = new HBox(20, statusLbl, toggleBtn);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(6));
        center.setStyle("-fx-border-color:black;-fx-border-width:1px;-fx-background-color:white;");

        card.getChildren().addAll(title, center);
        return card;
    }


    public void loadData() {
        welcomeLabel.setText("Welcome, " + nvl(DriverData.username));
        loadStats();
        loadDriverStatus();
        loadRecentTrips();
    }

    private void loadStats() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT COUNT(*) AS total," +
                "COALESCE(SUM(CASE WHEN t.trip_status='PENDING'   THEN 1 ELSE 0 END),0) AS pending," +
                "COALESCE(SUM(CASE WHEN t.trip_status='APPROVED'  THEN 1 ELSE 0 END),0) AS accepted " +
                "FROM Trip t JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id WHERE va.driver_id=?");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pendingCount.setText(String.valueOf(rs.getInt("pending")));
                approvedCount.setText(String.valueOf(rs.getInt("accepted")));
                totalCount.setText(String.valueOf(rs.getInt("total")));
            } else {
                pendingCount.setText("0"); approvedCount.setText("0"); totalCount.setText("0");
            }
        } catch (Exception e) {
            e.printStackTrace();
            pendingCount.setText("0"); approvedCount.setText("0"); totalCount.setText("0");
        }
    }

    private void loadDriverStatus() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT driver_status FROM Driver WHERE driver_id=?");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                isAvailable = "Available".equalsIgnoreCase(rs.getString("driver_status"));
                applyStatusUI();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateDriverStatus(String status) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "UPDATE Driver SET driver_status=? WHERE driver_id=?");
            ps.setString(1, status);
            ps.setInt(2, DriverData.driverId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applyStatusUI() {
        if (isAvailable) {
            statusLbl.setText("Available");
            statusLbl.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#228B22;");
            toggleBtn.setText("Set Unavailable");
            toggleBtn.getStyleClass().removeAll("btn-success", "btn-danger");
            toggleBtn.getStyleClass().add("btn-danger");
        } else {
            statusLbl.setText("Not Available");
            statusLbl.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#DC3545;");
            toggleBtn.setText("Set Available");
            toggleBtn.getStyleClass().removeAll("btn-success", "btn-danger");
            toggleBtn.getStyleClass().add("btn-success");
        }
    }

    private void loadRecentTrips() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT TOP 5 trip_id, destination, start_date, trip_status " +
                "FROM Trip t JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                "WHERE va.driver_id=? ORDER BY start_date DESC");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();
            recentData.clear();
            while (rs.next()) {
                recentData.add(new Object[]{
                    rs.getInt("trip_id"),
                    rs.getString("destination"),
                    rs.getDate("start_date"),
                    rs.getString("trip_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}