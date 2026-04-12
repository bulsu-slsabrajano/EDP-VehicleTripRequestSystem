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

    public Passenger(String username, CardPane mainPane) {
        this.username = username;
        this.mainPane = mainPane;
        this.userId   = getUserIdFromUsername(username);
        setBackground(Background.fill(Color.WHITE));
        createNavbar();
        createPanels();
    }

    private void createNavbar() {
        HBox nav = new HBox();
        nav.setBackground(Background.fill(Color.web("#141E32")));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(0, 20, 0, 0));
        nav.setPrefHeight(60);

        // Logo
        ImageView logo;
        try {
            Image img = new Image(getClass().getResourceAsStream(
                "/com/project/resources/companyLogo.png"), 80, 60, true, true);
            logo = new ImageView(img);
        } catch (Exception e) { logo = new ImageView(); }
        logo.setFitWidth(80); logo.setFitHeight(60);

        HBox logoBox = new HBox(logo);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPadding(new Insets(0, 10, 0, 0));

        String btnStyle =
            "-fx-text-fill:white;-fx-background-color:transparent;" +
            "-fx-font-weight:bold;-fx-font-size:16px;" +
            "-fx-pref-width:150px;-fx-pref-height:60px;" +
            "-fx-cursor:hand;-fx-border-width:0;";

        Button dashboard   = navButton("Dashboard",   btnStyle);
        Button reservation = navButton("Reservation", btnStyle);
        Button trips       = navButton("Trips",       btnStyle);
        Button profile     = navButton("Profile",     btnStyle);
        Button logout      = navButton("Logout",      btnStyle);

        dashboard.setOnAction(e   -> innerPane.show("dashboard"));
        reservation.setOnAction(e -> innerPane.show("reservation"));
        trips.setOnAction(e -> {
            tripsPanel.resetToPending();
            innerPane.show("trips");
        });
        profile.setOnAction(e -> {
            profilePanel.loadProfile(userId);
            innerPane.show("profile");
        });
        logout.setOnAction(e -> {
            if (FxUtil.confirm(this, "Are you sure you want to Logout?", "Confirm")) {
                insertAuditLog(userId, "Logged Out");
                mainPane.show("LOGIN");
            }
        });

        nav.getChildren().addAll(logoBox, dashboard, reservation, trips, profile, logout);
        setTop(nav);
    }

    private void createPanels() {
        home        homePanel     = new home(username, innerPane);
        tripsPanel                = new trips(userId);
        profilePanel              = new profile(userId);
        reservation reservPanel   = new reservation(innerPane, tripsPanel, userId);

        innerPane.addCard("dashboard",   homePanel);
        innerPane.addCard("trips",       tripsPanel);
        innerPane.addCard("profile",     profilePanel);
        innerPane.addCard("reservation", reservPanel);
        innerPane.show("dashboard");
        setCenter(innerPane);
    }

    private Button navButton(String text, String style) {
        Button b = new Button(text);
        b.setStyle(style);
        b.setOnMouseEntered(e -> b.setStyle(style + "-fx-background-color:rgba(255,255,255,0.10);"));
        b.setOnMouseExited(e  -> b.setStyle(style));
        return b;
    }

    private int getUserIdFromUsername(String uname) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            Connection c = db.conn;
            PreparedStatement ps = c.prepareStatement("SELECT user_id FROM Users WHERE username=?");
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
            ps.setInt(1, uid); ps.setString(2, status); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}