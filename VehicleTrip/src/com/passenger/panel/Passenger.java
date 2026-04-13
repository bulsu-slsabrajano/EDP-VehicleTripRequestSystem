package com.passenger.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Passenger extends BorderPane {

    private final String   username;
    private final CardPane mainPane;
    private final CardPane innerPane = new CardPane();
    private       int      userId;

    private trips  tripsPanel;
    private profile profilePanel;

    // ── Active nav tracking (same as DriverUI) ─────────────────────────────
   // private final Button[] navButtons = new Button[3];
    private Button[] navButtons;

    public Passenger(String username, CardPane mainPane) {
        this.username = username;
        this.mainPane = mainPane;
        this.userId   = getUserIdFromUsername(username);
        setBackground(Background.fill(Color.WHITE));
        createNavbar();
        createPanels();
    }

    private void createNavbar() {

        // ── Header bar (logo + title + profile icon + logout) ──────────────
        HBox header = new HBox();
        header.setBackground(Background.fill(Color.web("#141E32")));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 15));

        ImageView logoImg;
        try {
            Image img = new Image(getClass().getResourceAsStream(
                    "/com/project/resources/companyLogo.png"), 80, 60, true, true);
            logoImg = new ImageView(img);
        } catch (Exception e) { logoImg = new ImageView(); }
        logoImg.setFitWidth(80);
        logoImg.setFitHeight(60);

        Label titleLbl = new Label("EduTrip");
        titleLbl.getStyleClass().addAll("header-font");

        HBox brand = new HBox(6, logoImg, titleLbl);
        brand.setAlignment(Pos.CENTER_LEFT);

        // Profile icon button — mirrors DriverUI's profileBtn
        Button profileBtn = new Button("👤");
        profileBtn.getStyleClass().add("profile-icon-btn");
        profileBtn.setOnAction(e -> {
            profilePanel.loadProfile(userId);
            innerPane.show("profile");
            clearActiveNav();
        });

        Button logoutBtn = FxUtil.btnDanger("Log Out");
        logoutBtn.setOnAction(e -> {
            if (FxUtil.confirm(this, "Are you sure you want to logout?", "Logout Confirmation")) {
                insertAuditLog(userId, "Logged Out");
                mainPane.show("LOGIN");
            }
        });

        HBox rightBar = new HBox(10, profileBtn, logoutBtn);
        rightBar.setAlignment(Pos.CENTER_RIGHT);
        rightBar.setPadding(new Insets(0, 10, 0, 0));

        header.getChildren().addAll(brand, FxUtil.hgrow(), rightBar);

        // ── Navigation bar (gradient, same structure as DriverUI) ──────────
        HBox navBar = new HBox(20);
        navBar.getStyleClass().add("gradient-panel");
        navBar.setPrefHeight(45);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 30, 0, 30));

        String[] navLabels = {"DASHBOARD", "RESERVATION", "TRIPS"};
        String[] navCards  = {"dashboard", "reservation", "trips"};
        
        navButtons = new Button[navLabels.length];

        for (int i = 0; i < navLabels.length; i++) {
            final int idx = i;
            Button btn = new Button(navLabels[i]);
            btn.getStyleClass().add("topnav-btn");
            btn.setOnAction(e -> {
                // Extra logic for trips and profile, same as before
                if (navCards[idx].equals("trips"))   tripsPanel.resetToPending();
                //if (navCards[idx].equals("profile"))  profilePanel.loadProfile(userId);
                innerPane.show(navCards[idx]);
                setActiveNav(idx);
            });
            navButtons[i] = btn;
            navBar.getChildren().add(btn);
        }

        // ── Stack header + navBar as top section ───────────────────────────
        VBox topSection = new VBox(header, navBar);
        setTop(topSection);
    }

    private void createPanels() {
        home        homePanel   = new home(username, innerPane);
        tripsPanel              = new trips(userId);
        profilePanel            = new profile(userId);
        reservation reservPanel = new reservation(innerPane, tripsPanel, userId);

        innerPane.addCard("dashboard",   homePanel);
        innerPane.addCard("trips",       tripsPanel);
        innerPane.addCard("profile",     profilePanel);
        innerPane.addCard("reservation", reservPanel);
        innerPane.show("dashboard");
        setCenter(innerPane);

        // Mark Dashboard as active on startup
        setActiveNav(0);
    }

    // ── Nav active state helpers (mirrors DriverUI exactly) ────────────────
    private void setActiveNav(int activeIdx) {
        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i] == null) continue;
            navButtons[i].getStyleClass().removeAll("topnav-btn-active");
            if (i == activeIdx) navButtons[i].getStyleClass().add("topnav-btn-active");
        }
    }

    private void clearActiveNav() {
        for (Button b : navButtons) b.getStyleClass().remove("topnav-btn-active");
    }

    // ── DB helpers ─────────────────────────────────────────────────────────
    private int getUserIdFromUsername(String uname) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            Connection c = db.conn;
            PreparedStatement ps = c.prepareStatement(
                    "SELECT user_id FROM Users WHERE username=?");
            ps.setString(1, uname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private void insertAuditLog(int uid, String status) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                    "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, uid);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}