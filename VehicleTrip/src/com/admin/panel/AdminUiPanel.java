package com.admin.panel;

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

public class AdminUiPanel extends BorderPane {

    private final CardPane       mainPane;
    private final int            loggedInUserId;
    private       Connection     conn;
    private final CardPane       contentPane = new CardPane();
    private       Button         selectedBtn = null;
    private       ProfilePanel   profilePanel;

    public AdminUiPanel(CardPane mainPane, int loggedInUserId) {
        this.mainPane       = mainPane;
        this.loggedInUserId = loggedInUserId;
        DbConnectMsSql db   = new DbConnectMsSql();
        this.conn           = db.conn;
        buildUI();
    }

    private void buildUI() {
        setLeft(buildSidebar());
        setCenter(contentPane);
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("gradient-panel");
        sidebar.setPrefWidth(260);

        // Brand
        HBox brand = new HBox(8);
        brand.setPadding(new Insets(18, 14, 14, 14));
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logoImg;
        try {
            Image img = new Image(getClass().getResourceAsStream(
                    "/com/project/resources/companyLogo.png"), 36, 36, true, true);
            logoImg = new ImageView(img);
        } catch (Exception e) { logoImg = new ImageView(); }
        Label brandLbl = new Label("EduTRIP");
        brandLbl.setStyle("-fx-font-weight:bold;-fx-font-size:22px;-fx-text-fill:white;");
        brand.getChildren().addAll(logoImg, brandLbl);

        // Nav items
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(0, 10, 10, 10));
        VBox.setVgrow(nav, Priority.ALWAYS);

        // Content panels
        DashboardPanel         dashPanel    = new DashboardPanel();
        UserPanel              userPanel    = new UserPanel();
        VehiclePanel           vehiclePanel = new VehiclePanel();
        VehicleAssignmentPanel vaPanel      = new VehicleAssignmentPanel();
        TripPanel              tripPanel    = new TripPanel(loggedInUserId);
        TripRatingPanel        ratingPanel  = new TripRatingPanel();
        AuditLogPanel          logPanel     = new AuditLogPanel();
        profilePanel = new ProfilePanel();

        dashPanel.setLoggedInUserId(loggedInUserId);
        vaPanel.setLoggedInAdminId(loggedInUserId);
        tripPanel.createPanel.setAdminId(loggedInUserId);

        contentPane.addCard("Dashboard",          wrap("Dashboard",          dashPanel));
        contentPane.addCard("Users",              wrap("Users",              userPanel));
        contentPane.addCard("Vehicles",           wrap("Vehicles",           vehiclePanel));
        contentPane.addCard("Vehicle Assignment", wrap("Vehicle Assignment", vaPanel));
        contentPane.addCard("Trips",              wrap("Trips",              tripPanel));
        contentPane.addCard("Ratings",            wrap("Ratings",            ratingPanel));
        contentPane.addCard("Audit Logs",         wrap("Audit Logs",         logPanel));
        contentPane.addCard("Profile",            wrap("Profile",            profilePanel));

        Button btnDashboard = navBtn("Dashboard");
        Button btnUsers     = navBtn("Users");
        Button btnVehicles  = navBtn("Vehicles");
        Button btnVehAss    = navBtn("Vehicle Assignment");
        Button btnTrips     = navBtn("Trips");
        Button btnRatings   = navBtn("Ratings");
        Button btnAudit     = navBtn("Audit Logs");
        Button btnLogout    = navBtn("Log Out");

        addNav(btnDashboard, "Dashboard");
        addNav(btnUsers,     "Users");
        addNav(btnVehicles,  "Vehicles");
        addNav(btnVehAss,    "Vehicle Assignment");
        addNav(btnTrips,     "Trips");
        addNav(btnRatings,   "Ratings");
        addNav(btnAudit,     "Audit Logs");

        btnLogout.setOnAction(e -> {
            if (FxUtil.confirm(this, "Are you sure you want to logout?", "Logout Confirmation")) {
                insertAuditLog("Logged Out");
                mainPane.show("LOGIN");
            }
        });

        nav.getChildren().addAll(
            btnDashboard, btnUsers, btnVehicles, btnVehAss,
            btnTrips, btnRatings, btnAudit,
            FxUtil.vgrow(), btnLogout, FxUtil.spacer(10)
        );

        sidebar.getChildren().addAll(brand, nav);
        setSelectedBtn(btnDashboard);
        contentPane.show("Dashboard");
        return sidebar;
    }

    // ── Content wrapper (header + body) ─────────────────────────────────────
    private BorderPane wrap(String title, javafx.scene.layout.Region body) {
        BorderPane bp = new BorderPane();

        HBox header = new HBox();
        header.getStyleClass().add("content-header");
        header.setAlignment(Pos.CENTER_RIGHT);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("page-header-title");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Button profileIcon = new Button("👤");
        profileIcon.getStyleClass().add("profile-icon-btn");
        profileIcon.setOnAction(e -> {
            profilePanel.loadProfile(loggedInUserId);
            clearSelection();
            contentPane.show("Profile");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLbl, spacer, profileIcon);
        body.setBackground(Background.fill(Color.WHITE));
        bp.setTop(header);
        bp.setCenter(body);
        return bp;
    }

    // ── Nav button factory ───────────────────────────────────────────────────
    private Button navBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void addNav(Button btn, String card) {
        btn.setOnAction(e -> { contentPane.show(card); setSelectedBtn(btn); });
    }

    private void setSelectedBtn(Button btn) {
        if (selectedBtn != null)
            selectedBtn.getStyleClass().remove("nav-btn-selected");
        selectedBtn = btn;
        btn.getStyleClass().add("nav-btn-selected");
    }

    private void clearSelection() {
        if (selectedBtn != null)
            selectedBtn.getStyleClass().remove("nav-btn-selected");
        selectedBtn = null;
    }

    // ── Audit log ────────────────────────────────────────────────────────────
    public void insertAuditLog(String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, loggedInUserId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}