package com.admin.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProfilePanel extends VBox {

    private Connection conn;
    private int currentUserId = -1;

    private final TextField txtFirstName  = FxUtil.styledField();
    private final TextField txtMiddleName = FxUtil.styledField();
    private final TextField txtLastName   = FxUtil.styledField();
    private final TextField txtAddress    = FxUtil.styledField();
    private final TextField txtUsername   = FxUtil.styledField();
    private final TextField txtEmail      = FxUtil.styledField();
    private final TextField txtPassword   = FxUtil.styledField();

    private String snapFN, snapMN, snapLN, snapAddr, snapUser, snapEmail, snapPwd;
    private final List<String> snapContacts = new ArrayList<>();

    private final VBox contactsBox  = new VBox(4);
    private final List<TextField> contactFields = new ArrayList<>();
    private final Label lblStatus   = FxUtil.statusLabel();

    public ProfilePanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));
        setAlignment(Pos.CENTER);
        buildUI();
    }

    private void buildUI() {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setMaxWidth(640);
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Personal Information");
        title.getStyleClass().add("title-medium");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        GridPane form = FxUtil.formGrid();
        int y = 0;
        FxUtil.addFormRow(form, "First Name:",  txtFirstName,  y++);
        FxUtil.addFormRow(form, "Middle Name:", txtMiddleName, y++);
        FxUtil.addFormRow(form, "Last Name:",   txtLastName,   y++);
        FxUtil.addFormRow(form, "Address:",     txtAddress,    y++);
        FxUtil.addFormRow(form, "Username:",    txtUsername,   y++);
        FxUtil.addFormRow(form, "Email:",       txtEmail,      y++);
        FxUtil.addFormRow(form, "Password:",    txtPassword,   y++);

        // Contact numbers section
        Label contactTitle = FxUtil.sectionLabel("Contact Numbers");
        GridPane.setColumnSpan(contactTitle, 2);
        form.add(contactTitle, 0, y++);

        contactsBox.setBackground(Background.fill(Color.WHITE));
        GridPane.setColumnSpan(contactsBox, 2);
        form.add(contactsBox, 0, y++);

        Button btnAddContact = new Button("+ Add Contact Number");
        btnAddContact.getStyleClass().add("btn-add-contact");
        btnAddContact.setOnAction(e -> addContactRow(""));
        GridPane.setColumnSpan(btnAddContact, 2);
        form.add(btnAddContact, 0, y);

        // Buttons
        Button btnSave   = FxUtil.btnPrimary("Save");
        Button btnCancel = FxUtil.btnOutlineDanger("Cancel");
        btnSave.setPrefWidth(100);
        btnCancel.setPrefWidth(100);

        HBox btnRow = new HBox(15, btnSave, btnCancel);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(20, 0, 0, 0));

        lblStatus.setAlignment(Pos.CENTER);
        lblStatus.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(title, FxUtil.spacer(20), form, btnRow, FxUtil.spacer(10), lblStatus);

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setPrefSize(640, 620);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);

        btnSave.setOnAction(e -> saveProfile());
        btnCancel.setOnAction(e -> confirmCancel());
    }

    private void confirmCancel() {
        int choice = FxUtil.showOptions(this,
            "Do you want to cancel your changes?\nAll unsaved edits will be lost.",
            "Cancel Changes", "Yes, discard changes", "Keep Editing");
        if (choice == 0) { restoreSnapshot(); lblStatus.setText(" "); }
    }

    private void addContactRow(String value) {
        TextField field = FxUtil.styledField();
        field.setText(value);

        Button btnRemove = new Button("X");
        btnRemove.getStyleClass().addAll("btn", "btn-outline-danger");
        btnRemove.setPrefSize(38, 32);

        HBox row = new HBox(6, field, btnRemove);
        row.setAlignment(Pos.CENTER_LEFT);
        contactFields.add(field);
        contactsBox.getChildren().add(row);

        btnRemove.setOnAction(e -> {
            contactFields.remove(field);
            contactsBox.getChildren().remove(row);
        });
    }

    public void loadProfile(int userId) {
        this.currentUserId = userId;
        lblStatus.setText(" ");
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Users WHERE user_id=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtFirstName.setText(nvl(rs.getString("first_name")));
                txtMiddleName.setText(nvl(rs.getString("middle_name")));
                txtLastName.setText(nvl(rs.getString("last_name")));
                txtAddress.setText(nvl(rs.getString("address")));
                txtUsername.setText(nvl(rs.getString("username")));
                txtEmail.setText(nvl(rs.getString("email_address")));
                txtPassword.setText(nvl(rs.getString("password")));
            }
            contactFields.clear();
            contactsBox.getChildren().clear();
            PreparedStatement psC = conn.prepareStatement(
                    "SELECT contact_number FROM User_Contact_Number WHERE user_id=? ORDER BY contact_id");
            psC.setInt(1, userId);
            ResultSet rsC = psC.executeQuery();
            while (rsC.next()) addContactRow(rsC.getString("contact_number"));
            takeSnapshot();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveProfile() {
        if (currentUserId == -1) { FxUtil.setError(lblStatus, "No user loaded!"); return; }
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Users SET first_name=?,middle_name=?,last_name=?,address=?," +
                "username=?,email_address=?,password=? WHERE user_id=?");
            ps.setString(1, txtFirstName.getText().trim());
            ps.setString(2, txtMiddleName.getText().trim());
            ps.setString(3, txtLastName.getText().trim());
            ps.setString(4, txtAddress.getText().trim());
            ps.setString(5, txtUsername.getText().trim());
            ps.setString(6, txtEmail.getText().trim());
            ps.setString(7, txtPassword.getText().trim());
            ps.setInt(8, currentUserId);
            ps.executeUpdate();

            conn.prepareStatement(
                "DELETE FROM User_Contact_Number WHERE user_id=" + currentUserId).executeUpdate();

            PreparedStatement psIns = conn.prepareStatement(
                "INSERT INTO User_Contact_Number (user_id, contact_number) VALUES (?,?)");
            for (TextField f : contactFields) {
                String num = f.getText().trim();
                if (!num.isEmpty()) {
                    if (num.length() != 10) {
                        FxUtil.setError(lblStatus, "Contact number must be exactly 10 digits!"); return;
                    }
                    psIns.setInt(1, currentUserId); psIns.setString(2, num); psIns.addBatch();
                }
            }
            psIns.executeBatch();
            takeSnapshot();
            FxUtil.setSuccess(lblStatus, "Profile updated successfully!");
        } catch (Exception e) {
            FxUtil.setError(lblStatus, "Error saving profile!");
            e.printStackTrace();
        }
    }

    private void takeSnapshot() {
        snapFN   = txtFirstName.getText();  snapMN    = txtMiddleName.getText();
        snapLN   = txtLastName.getText();   snapAddr  = txtAddress.getText();
        snapUser = txtUsername.getText();   snapEmail = txtEmail.getText();
        snapPwd  = txtPassword.getText();
        snapContacts.clear();
        contactFields.forEach(f -> snapContacts.add(f.getText()));
    }

    private void restoreSnapshot() {
        txtFirstName.setText(snapFN);  txtMiddleName.setText(snapMN);
        txtLastName.setText(snapLN);   txtAddress.setText(snapAddr);
        txtUsername.setText(snapUser); txtEmail.setText(snapEmail);
        txtPassword.setText(snapPwd);
        contactFields.clear();
        contactsBox.getChildren().clear();
        snapContacts.forEach(this::addContactRow);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}