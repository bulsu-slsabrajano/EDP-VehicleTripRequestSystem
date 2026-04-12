package com.driver.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;

public class DriverUI extends BorderPane {

    private final CardPane      mainPane;
    private final CardPane      contentPane = new CardPane();
    private final Button[]      navButtons  = new Button[5];

    public  HomePanel         homePanel;
    private AssignmentPanel   assignmentPanel;
    private VehiclePanel      vehiclePanel;
    private TripPanel         tripPanel;
    private RatingPanel       ratingPanel;
    private DriverProfilePanel profilePanel;

    public DriverUI(CardPane mainPane) {
        this.mainPane = mainPane;
        setBackground(Background.fill(Color.WHITE));
        buildUI();
    }

    private void buildUI() {
        // ── Header bar ───────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setBackground(Background.fill(Color.WHITE));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 15));

        ImageView logoImg;
        try {
            Image img = new Image(getClass().getResourceAsStream(
                    "/com/project/resources/companyLogo.png"), 40, 40, true, true);
            logoImg = new ImageView(img);
        } catch (Exception e) { logoImg = new ImageView(); }
        logoImg.setFitWidth(40); logoImg.setFitHeight(40);

        Label titleLbl = new Label("EduTrip");
        titleLbl.setStyle("-fx-font-weight:bold;-fx-font-size:24px;-fx-padding:0 0 0 6px;");

        HBox brand = new HBox(6, logoImg, titleLbl);
        brand.setAlignment(Pos.CENTER_LEFT);

        Button profileBtn = new Button("👤");
        profileBtn.getStyleClass().add("profile-icon-btn");
        profileBtn.setOnAction(e -> {
            if (DriverData.driverId != 0) profilePanel.loadProfile(DriverData.driverId);
            contentPane.show("PROFILE");
            clearActiveNav();
        });

        Button logoutBtn = FxUtil.btnDanger("Log Out");
        logoutBtn.setOnAction(e -> {
            if (FxUtil.confirm(this, "Are you sure you want to logout?", "Logout Confirmation")) {
                insertAuditLog(DriverData.driverId, "Logged Out");
                DriverData.driverId          = 0;
                DriverData.username          = null;
                DriverData.selectedVehicleId = null;
                mainPane.show("LOGIN");
            }
        });

        HBox rightBar = new HBox(10, profileBtn, logoutBtn);
        rightBar.setAlignment(Pos.CENTER_RIGHT);
        rightBar.setPadding(new Insets(0, 10, 0, 0));

        header.getChildren().addAll(brand, FxUtil.hgrow(), rightBar);

        // ── Navigation bar ────────────────────────────────────────────────────
        HBox navBar = new HBox(0);
        navBar.getStyleClass().add("gradient-panel");
        navBar.setPrefHeight(45);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 30, 0, 30));

        String[] navLabels = {"HOME", "ASSIGNMENTS", "VEHICLE", "TRIPS", "RATINGS"};
        String[] navCards  = {"HOME", "ASSIGNMENTS", "VEHICLE", "TRIPS", "RATINGS"};

        for (int i = 0; i < navLabels.length; i++) {
            final int idx = i;
            Button btn = new Button(navLabels[i]);
            btn.getStyleClass().addAll("topnav-btn");
            btn.setOnAction(e -> {
                contentPane.show(navCards[idx]);
                setActiveNav(idx);
            });
            navButtons[i] = btn;
            navBar.getChildren().add(btn);
        }

        VBox topSection = new VBox(header, navBar);

        // ── Content panels ────────────────────────────────────────────────────
        homePanel       = new HomePanel(DriverData.username);
        assignmentPanel = new AssignmentPanel();
        vehiclePanel    = new VehiclePanel();
        tripPanel       = new TripPanel(DriverData.username);
        ratingPanel     = new RatingPanel();
        profilePanel    = new DriverProfilePanel();

        contentPane.addCard("HOME",        homePanel);
        contentPane.addCard("ASSIGNMENTS", assignmentPanel);
        contentPane.addCard("VEHICLE",     vehiclePanel);
        contentPane.addCard("TRIPS",       tripPanel);
        contentPane.addCard("RATINGS",     ratingPanel);
        contentPane.addCard("PROFILE",     profilePanel);
        contentPane.show("HOME");

        setTop(topSection);
        setCenter(contentPane);
        setActiveNav(0);
    }

    public void loadAllData() {
        homePanel.loadData();
        assignmentPanel.loadData();
        vehiclePanel.loadData();
        TripPanel.refreshTrips();
        ratingPanel.loadData();
        profilePanel.loadProfile(DriverData.driverId);
    }

    private void setActiveNav(int activeIdx) {
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].getStyleClass().removeAll("topnav-btn-active");
            if (i == activeIdx) navButtons[i].getStyleClass().add("topnav-btn-active");
        }
    }

    private void clearActiveNav() {
        for (Button b : navButtons) b.getStyleClass().remove("topnav-btn-active");
    }

    private void insertAuditLog(int userId, String status) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}