package com.project.panel;

import com.admin.panel.AdminUiPanel;
import com.driver.panel.DriverData;
import com.driver.panel.DriverUI;
import com.passenger.panel.Passenger;
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

public class LoginPanel extends HBox {

    private final CardPane mainPane;
    private final DriverUI driverPanel;

    public LoginPanel(CardPane mainPane, DriverUI driverPanel) {
        this.mainPane    = mainPane;
        this.driverPanel = driverPanel;
        buildUI();
    }

    private void buildUI() {
        setBackground(Background.fill(Color.WHITE));
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // ── Left brand pane ──────────────────────────────────────────────────
        StackPane brandPane = new StackPane();
        brandPane.getStyleClass().add("gradient-panel");
        brandPane.setPrefWidth(700);
        HBox.setHgrow(brandPane, Priority.NEVER);

        VBox brandContent = new VBox(8);
        brandContent.setAlignment(Pos.CENTER);

        ImageView logo;
        try {
            Image img = new Image(getClass().getResourceAsStream(
                    "/com/project/resources/companyLogo.png"), 300, 240, true, true);
            logo = new ImageView(img);
        } catch (Exception e) {
            logo = new ImageView();
        }

        Label welcomeLbl = new Label("WELCOME TO");
        welcomeLbl.getStyleClass().add("brand-welcome");

        Label brandName = new Label("EduTRIP");
        brandName.getStyleClass().add("brand-title");

        Label subLbl = new Label("Vehicle Trip Reservation System");
        subLbl.getStyleClass().add("brand-welcome");

        brandContent.getChildren().addAll(logo, welcomeLbl, brandName, subLbl);
        brandPane.getChildren().add(brandContent);

        // ── Right login form ─────────────────────────────────────────────────
        VBox formPane = new VBox(0);
        formPane.getStyleClass().add("login-panel");
        formPane.setAlignment(Pos.CENTER);
        HBox.setHgrow(formPane, Priority.ALWAYS);
        formPane.setPadding(new Insets(0, 60, 0, 60));

        Label titleLbl = new Label("LOGIN");
        titleLbl.getStyleClass().add("login-title");

        Label usernameLbl = new Label("USERNAME");
        usernameLbl.getStyleClass().add("login-subtitle");
        usernameLbl.setPadding(new Insets(28, 0, 5, 0));

        TextField txtUsername = new TextField();
        txtUsername.getStyleClass().add("form-field");
        txtUsername.setMaxWidth(265);

        Label passwordLbl = new Label("PASSWORD");
        passwordLbl.getStyleClass().add("login-subtitle");
        passwordLbl.setPadding(new Insets(16, 0, 5, 0));

        PasswordField pwfPassword = new PasswordField();
        pwfPassword.getStyleClass().add("password-field");
        pwfPassword.setMaxWidth(265);

        Label lblError = new Label(" ");
        lblError.getStyleClass().add("status-error");
        lblError.setPadding(new Insets(8, 0, 8, 0));

        Button loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().addAll("btn", "btn-dark");
        loginBtn.setMaxWidth(120);

        loginBtn.setOnAction(e -> doLogin(txtUsername.getText(), pwfPassword.getText(), lblError));
        pwfPassword.setOnAction(e -> doLogin(txtUsername.getText(), pwfPassword.getText(), lblError));

        formPane.getChildren().addAll(titleLbl, usernameLbl, txtUsername,
                passwordLbl, pwfPassword, lblError, loginBtn);

        getChildren().addAll(brandPane, formPane);
    }

    private void doLogin(String username, String password, Label lblError) {
        lblError.setText(" ");
        if (username.isBlank() && password.isBlank()) {
            lblError.setText("Username and password are required."); return;
        }
        if (username.isBlank()) { lblError.setText("Username is required.");  return; }
        if (password.isBlank()) { lblError.setText("Password is required.");  return; }

        try {
            DbConnectMsSql db = new DbConnectMsSql();
            Connection conn = db.conn;

            String sql = "SELECT * FROM users " +
                    "WHERE username = ? COLLATE SQL_Latin1_General_CP1_CS_AS " +
                    "AND   password = ? COLLATE SQL_Latin1_General_CP1_CS_AS";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role   = rs.getString("user_role");
                String status = rs.getString("user_status");
                int    userId = rs.getInt("user_id");

                if ("Inactive".equalsIgnoreCase(status)) {
                    lblError.setText("This account has been deactivated."); return;
                }

                insertAuditLog(conn, userId, "Logged In");

                if ("admin".equalsIgnoreCase(role)) {
                    AdminUiPanel adminPanel = new AdminUiPanel(mainPane, userId);
                    mainPane.addCard("ADMIN", adminPanel);
                    mainPane.show("ADMIN");

                } else if ("driver".equalsIgnoreCase(role)) {
                    DriverData.driverId = userId;
                    DriverData.username = rs.getString("username");
                    mainPane.show("DRIVER");
                    driverPanel.loadAllData();

                } else if ("passenger".equalsIgnoreCase(role)) {
                    String loggedUser = rs.getString("username");
                    Passenger passengerPanel = new Passenger(loggedUser, mainPane);
                    mainPane.addCard("PASSENGER", passengerPanel);
                    mainPane.show("PASSENGER");
                }
            } else {
                PreparedStatement psCheck = conn.prepareStatement(
                        "SELECT COUNT(*) FROM users WHERE username=?");
                psCheck.setString(1, username);
                ResultSet rsCheck = psCheck.executeQuery();
                if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                    lblError.setText("Incorrect password.");
                } else {
                    lblError.setText("Account does not exist.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblError.setText("Connection error.");
        }
    }

    private void insertAuditLog(Connection conn, int userId, String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}