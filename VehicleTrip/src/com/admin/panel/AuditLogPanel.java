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

public class AuditLogPanel extends BorderPane {

    private ObservableList<Object[]> tableData;
    private TableView<Object[]> table;
    private ComboBox<String> cmbMonth;
    private ComboBox<String> cmbYear;
    private Connection conn;

    public AuditLogPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(20, 30, 20, 30));
        buildUI();
        loadYears();
        loadLogs("All", "All", -1);
    }

    private void buildUI() {
    
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 0, 10, 0));

        cmbMonth = FxUtil.styledCombo(javafx.collections.FXCollections.observableArrayList(
            "All","January","February","March","April","May","June",
            "July","August","September","October","November","December"));
        cmbMonth.setValue("All");
        cmbMonth.setPrefWidth(120);

        cmbYear = new ComboBox<>();
        cmbYear.getStyleClass().add("combo-field");
        cmbYear.setPrefWidth(90);

        Button btnFilter = FxUtil.btnPrimary("Filter");
        btnFilter.setOnAction(e -> loadLogs(
            cmbMonth.getValue(), cmbYear.getValue(), -1));

        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> {
            cmbMonth.setValue("All"); cmbYear.getSelectionModel().selectFirst();
            loadLogs("All", "All", -1);
        });

        filterBar.getChildren().addAll(
            new Label("Month:"), cmbMonth,
            FxUtil.hspacer(8), new Label("Year:"), cmbYear,
            FxUtil.hspacer(8), btnFilter,
            FxUtil.hgrow(), btnRefresh
        );

        //Table
        table = FxUtil.buildTable("Log ID", "User", "Date", "Status");
        FxUtil.applyStatusRenderer(table, 3);
        tableData = FxUtil.tableData(table);

        //Bottom bar
        Button btnShowUser = FxUtil.btnPrimary("Show Logs for Selected User");
        btnShowUser.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a log row first."); return; }
            int logId = (int) row[0];
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM Audit_Log WHERE log_id=?");
                ps.setInt(1, logId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cmbMonth.setValue("All"); cmbYear.getSelectionModel().selectFirst();
                    loadLogs("All", "All", rs.getInt("user_id"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        HBox bottom = new HBox(btnShowUser);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        setTop(filterBar);
        setCenter(FxUtil.tableScroll(table));
        setBottom(bottom);
    }

    private void loadYears() {
        try {
            cmbYear.getItems().clear();
            cmbYear.getItems().add("All");
            PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT YEAR(log_date) AS yr FROM Audit_Log ORDER BY yr DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbYear.getItems().add(String.valueOf(rs.getInt("yr")));
            cmbYear.setValue("All");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadLogs(String month, String year, int userId) {
        try {
            tableData.clear();
            StringBuilder sql = new StringBuilder(
                "SELECT al.log_id, u.first_name+' '+u.last_name AS full_name," +
                " al.log_date, al.log_status " +
                "FROM Audit_Log al JOIN Users u ON al.user_id=u.user_id WHERE 1=1");
            if (!"All".equals(month)) sql.append(" AND MONTH(al.log_date)=").append(monthToInt(month));
            if (year != null && !"All".equals(year)) sql.append(" AND YEAR(al.log_date)=").append(year);
            if (userId != -1) sql.append(" AND al.user_id=").append(userId);
            sql.append(" ORDER BY al.log_date DESC");
            ResultSet rs = conn.prepareStatement(sql.toString()).executeQuery();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("log_id"),
                    rs.getString("full_name"),
                    rs.getTimestamp("log_date"),
                    rs.getString("log_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int monthToInt(String m) {
        return switch (m) {
            case "January" -> 1; case "February" -> 2; case "March"    -> 3;
            case "April"   -> 4; case "May"      -> 5; case "June"     -> 6;
            case "July"    -> 7; case "August"   -> 8; case "September"-> 9;
            case "October" ->10; case "November" ->11; case "December" ->12;
            default -> 0;
        };
    }
}