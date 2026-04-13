package com.admin.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardPanel extends VBox {

    private Label lblUsers, lblDrivers, lblVehicles, lblTotalTrips;
    private Label lblPending, lblApproved, lblCompleted, lblCancelled;
    private Label lblWelcome;
    private ObservableList<Object[]> tableData;
    private Connection conn;
    private int loggedInUserId = -1;

    public DashboardPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(15, 30, 20, 30));
        setSpacing(0);
        buildUI();
    }

    public void setLoggedInUserId(int id) {
        this.loggedInUserId = id;
        loadWelcomeName();
    }

    private void loadWelcomeName() {
        if (loggedInUserId == -1 || lblWelcome == null) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT first_name FROM Users WHERE user_id=?");
            ps.setInt(1, loggedInUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) lblWelcome.setText("Welcome, " + rs.getString("first_name") + "!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void buildUI() {
        // ── Top bar ─────────────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        lblWelcome = new Label("Welcome!");
        lblWelcome.getStyleClass().add("welcome-title");
        HBox.setHgrow(lblWelcome, Priority.ALWAYS);

        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> loadDashboard());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        topBar.getChildren().addAll(lblWelcome, spacer, btnRefresh);

        // ── Summary cards ────────────────────────────────────────────────────
        lblUsers      = new Label("0");
        lblDrivers    = new Label("0");
        lblVehicles   = new Label("0");
        lblTotalTrips = new Label("0");

        HBox summaryRow = new HBox(15);
        summaryRow.getChildren().addAll(
            summaryCard("Total Users",        lblUsers),
            summaryCard("Available Drivers",  lblDrivers),
            summaryCard("Available Vehicles", lblVehicles),
            summaryCard("Total Trips",        lblTotalTrips)
        );

        // ── Status cards ─────────────────────────────────────────────────────
        lblPending   = statusLabel();
        lblApproved  = statusLabel();
        lblCompleted = statusLabel();
        lblCancelled = statusLabel();

        HBox statusRow = new HBox(15);
        statusRow.setPadding(new Insets(10, 0, 0, 0));
        statusRow.getChildren().addAll(
            statusCard("Pending",   lblPending,   "status-card-pending"),
            statusCard("Approved",  lblApproved,  "status-card-approved"),
            statusCard("Completed", lblCompleted, "status-card-completed"),
            statusCard("Cancelled", lblCancelled, "status-card-cancelled")
        );

        // ── Upcoming trips table ─────────────────────────────────────────────
        Label tblTitle = new Label("Upcoming Trips");
        tblTitle.getStyleClass().add("title-small");
        tblTitle.setPadding(new Insets(14, 0, 6, 0));

        TableView<Object[]> table = FxUtil.buildTable(
            "Trip ID","Passenger","Driver","Vehicle","Status","Start Date");
        FxUtil.applyStatusRenderer(table, 4);
        tableData = FxUtil.tableData(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        ScrollPane sp = FxUtil.tableScroll(table);

        getChildren().addAll(topBar, summaryRow, statusRow, tblTitle, sp);
        loadDashboard();
    }

    // ── Card builders ─────────────────────────────────────────────────────────
    private VBox summaryCard(String title, Label valueLabel) {
        VBox card = new VBox(4);
        card.getStyleClass().add("shadow-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefHeight(100);

        valueLabel.getStyleClass().add("shadow-card-label-value");
        valueLabel.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("shadow-card-label-title");
        titleLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(valueLabel, titleLbl);
        return card;
    }

    private Label statusLabel() {
        Label l = new Label("0");
        l.setStyle("-fx-font-weight:bold;-fx-font-size:28px;");
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private VBox statusCard(String title, Label valueLabel, String styleClass) {
        VBox card = new VBox(4);
        card.getStyleClass().add(styleClass);
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefHeight(80);

        String colour = switch (styleClass) {
            case "status-card-pending"   -> "#E67E22";
            case "status-card-approved"  -> "#0096C7";
            case "status-card-completed" -> "#27AE60";
            default                      -> "#DC3545";
        };
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:" + colour + ";");
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setMaxWidth(Double.MAX_VALUE);

        valueLabel.setStyle("-fx-font-weight:bold;-fx-font-size:28px;-fx-text-fill:" + colour + ";");
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(titleLbl, valueLabel);
        return card;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadDashboard() {
        try {
            ResultSet r1 = conn.prepareStatement("SELECT COUNT(*) FROM Users").executeQuery();
            if (r1.next()) lblUsers.setText(String.valueOf(r1.getInt(1)));

            ResultSet r2 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Driver WHERE driver_status='Available'").executeQuery();
            if (r2.next()) lblDrivers.setText(String.valueOf(r2.getInt(1)));

            ResultSet r3 = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Vehicle WHERE vehicle_status='Available'").executeQuery();
            if (r3.next()) lblVehicles.setText(String.valueOf(r3.getInt(1)));

            ResultSet r4 = conn.prepareStatement("SELECT COUNT(*) FROM Trip").executeQuery();
            if (r4.next()) lblTotalTrips.setText(String.valueOf(r4.getInt(1)));

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT trip_status, COUNT(*) AS total FROM Trip GROUP BY trip_status");
            ResultSet rs = ps.executeQuery();
            int pend = 0, appr = 0, comp = 0, canc = 0;
            while (rs.next()) {
                String s = rs.getString("trip_status");
                int    c = rs.getInt("total");
                if      (s.equalsIgnoreCase("Pending"))   pend = c;
                else if (s.equalsIgnoreCase("Approved"))  appr = c;
                else if (s.equalsIgnoreCase("Completed")) comp = c;
                else if (s.equalsIgnoreCase("Cancelled")) canc = c;
            }
            lblPending.setText(String.valueOf(pend));
            lblApproved.setText(String.valueOf(appr));
            lblCompleted.setText(String.valueOf(comp));
            lblCancelled.setText(String.valueOf(canc));

            tableData.clear();
            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT * FROM vw_UpcomingTrips ORDER BY start_date ASC");
            ResultSet r5 = ps2.executeQuery();
            while (r5.next()) {
                tableData.add(new Object[]{
                    r5.getInt("trip_id"),
                    r5.getString("passenger"),
                    r5.getString("driver_name"),
                    r5.getString("vehicle_name"),
                    r5.getString("trip_status"),
                    r5.getDate("start_date")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}