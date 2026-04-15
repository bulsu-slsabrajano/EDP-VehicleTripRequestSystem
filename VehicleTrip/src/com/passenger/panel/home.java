package com.passenger.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class home extends VBox {

    private Label lblTotalTrips = new Label("0");
    private Label lblCompleted  = new Label("0");
    private Label lblPending    = new Label("0");
    private Label lblCancelled  = new Label("0");
    private ObservableList<Object[]> tableData;
    private final DbConnectMsSql conn;
    private final String         username;
    private final CardPane       innerPane;

    public home(String username, CardPane innerPane) {
        this.username  = username;
        this.innerPane = innerPane;
        this.conn      = new DbConnectMsSql();
        setBackground(Background.fill(Color.WHITE));
        buildUI();
        loadDashboard();
    }

    private void buildUI() {
        // ── Gradient header ───────────────────────────────────────────────────
        //HBox header = new HBox();
        //header.getStyleClass().add("gradient-panel");
       // header.setPadding(new Insets(10, 20, 10, 20));
//        header.setPrefHeight(76);

        Label welcomeLbl = new Label("WELCOME, " + username + " !");
        welcomeLbl.getStyleClass().add("welcome-title");
        welcomeLbl.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");
        //header.getChildren().add(welcomeLbl);
        
        Button refresh = FxUtil.btnPrimary("Refresh");
        refresh.setOnAction(e -> loadDashboard());
        
        HBox header = new HBox(welcomeLbl, FxUtil.hgrow(), refresh);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 6, 20));

        
        VBox content = new VBox(8);
        content.setPadding(new Insets(0, 15, 15, 15));
        content.setBackground(Background.fill(Color.TRANSPARENT));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Quick actions row
        HBox quickWrapper = buildRoundedCard();
        quickWrapper.setAlignment(Pos.CENTER_LEFT);
        quickWrapper.setSpacing(0);
        quickWrapper.setPadding(new Insets(10));

        VBox quickInner = new VBox(8);
        quickInner.setBackground(Background.fill(Color.WHITE));
        HBox.setHgrow(quickInner, Priority.ALWAYS);

        Label quickTitle = new Label("Quick Actions");
        quickTitle.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");

        

        HBox quickTop = new HBox(quickTitle);
        quickTop.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(10,
            quickActionBtn("Book Now",           () -> innerPane.show("reservation")),
            quickActionBtn("View Profile",        () -> innerPane.show("profile")),
            quickActionBtn("Show History Trips",  () -> innerPane.show("trips")));
        HBox.setHgrow(btnRow, Priority.ALWAYS);

        quickInner.getChildren().addAll(quickTop, btnRow);
        quickWrapper.getChildren().add(quickInner);

        //Summary cards row
        VBox summaryWrapper = new VBox(8);
        summaryWrapper.getStyleClass().add("rounded-card");
        summaryWrapper.setPadding(new Insets(10));

        Label summaryTitle = new Label("Dashboard Summary");
        summaryTitle.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");

        HBox summaryRow = new HBox(10,
            summaryCard("Total Trips", lblTotalTrips),
            summaryCard("Completed",   lblCompleted),
            summaryCard("Pending",     lblPending),
            summaryCard("Cancelled",   lblCancelled));

        summaryWrapper.getChildren().addAll(summaryTitle, summaryRow);

        VBox topSection = new VBox(10, quickWrapper, summaryWrapper);

        // Recent trips 
        VBox tableBox = new VBox(8);
        tableBox.getStyleClass().add("rounded-card");
        tableBox.setPadding(new Insets(10));
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        Label tableTitle = new Label("My Recent Trips");
        tableTitle.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");

        TableView<Object[]> table = FxUtil.buildTable(
            "Trip ID", "Vehicle", "Destination", "Start Date", "End Date", "Status");
        FxUtil.applyStatusRenderer(table, 5);
        tableData = FxUtil.tableData(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        tableBox.getChildren().addAll(tableTitle, FxUtil.tableScroll(table));
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        content.getChildren().addAll(topSection, tableBox);

        getChildren().addAll(header, content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    private HBox buildRoundedCard() {
        HBox box = new HBox();
        box.setStyle(
            "-fx-background-color:white;-fx-background-radius:15px;" +
            "-fx-border-radius:15px;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),4,0,1,1);");
        return box;
    }

    private VBox summaryCard(String title, Label value) {
        VBox p = new VBox(4);
        p.setStyle(
            "-fx-background-color:#0096C7;-fx-background-radius:15px;" +
            "-fx-border-radius:15px;-fx-padding:12px 10px;");
        p.setAlignment(Pos.CENTER);
        HBox.setHgrow(p, Priority.ALWAYS);

        value.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:white;");
        value.setAlignment(Pos.CENTER);
        value.setMaxWidth(Double.MAX_VALUE);

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:15px;-fx-text-fill:white;");
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);

        p.getChildren().addAll(value, lbl);
        return p;
    }

    private VBox quickActionBtn(String text, Runnable action) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("quick-action-card");
        wrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(wrapper, Priority.ALWAYS);

        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:transparent;-fx-border-width:0;-fx-cursor:hand;");
        btn.getStyleClass().add("quick-action-text");
        btn.setOnAction(e -> action.run());

        wrapper.getChildren().add(btn);
        return wrapper;
    }

    private void loadDashboard() {
        try {
            int pId = 0;
            PreparedStatement ps = conn.conn.prepareStatement(
                "SELECT p.passenger_id FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id WHERE u.username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) pId = rs.getInt(1);

            PreparedStatement p1 = conn.conn.prepareStatement("SELECT COUNT(*) FROM Trip WHERE passenger_id=?");
            p1.setInt(1, pId); 
            ResultSet r1 = p1.executeQuery();
            if (r1.next()) lblTotalTrips.setText(r1.getString(1));

            PreparedStatement p2 = conn.conn.prepareStatement(
                "SELECT trip_status, COUNT(*) c FROM Trip WHERE passenger_id=? GROUP BY trip_status");
            p2.setInt(1, pId); ResultSet r2 = p2.executeQuery();
            int pend = 0, comp = 0, canc = 0;
            while (r2.next()) {
                String s = r2.getString(1);
                if ("Pending".equalsIgnoreCase(s))   pend = r2.getInt(2);
                if ("Completed".equalsIgnoreCase(s)) comp = r2.getInt(2);
                if ("Cancelled".equalsIgnoreCase(s)) canc = r2.getInt(2);
            }
            lblPending.setText(String.valueOf(pend));
            lblCompleted.setText(String.valueOf(comp));
            lblCancelled.setText(String.valueOf(canc));

            tableData.clear();
            PreparedStatement p3 = conn.conn.prepareStatement(
                "SELECT TOP 10 t.trip_id,v.vehicle_model,t.destination,t.start_date,t.end_date,t.trip_status " +
                "FROM Trip t JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id WHERE t.passenger_id=? ORDER BY t.start_date DESC");
            p3.setInt(1, pId); 
            ResultSet r3 = p3.executeQuery();
            
            while (r3.next()) {
                tableData.add(new Object[]{
                    r3.getInt(1), r3.getString(2), r3.getString(3),
                    r3.getDate(4), r3.getDate(5), r3.getString(6)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}