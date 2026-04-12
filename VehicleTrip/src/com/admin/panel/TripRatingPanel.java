package com.admin.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TripRatingPanel extends BorderPane {

    private ObservableList<Object[]> tableData;
    private Connection conn;

    public TripRatingPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(20, 30, 20, 30));
        buildUI();
        loadRatings();
    }

    private void buildUI() {
        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> loadRatings());

        HBox topBar = new HBox(btnRefresh);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        TableView<Object[]> table = FxUtil.buildTable(
            "Rating ID", "Trip ID", "Passenger Name", "Rating", "Feedback", "Date");
        tableData = FxUtil.tableData(table);

        setTop(topBar);
        setCenter(FxUtil.tableScroll(table));
    }

    private void loadRatings() {
        try {
            tableData.clear();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM vw_TripRatings ORDER BY rating_date DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("rating_id"),
                    rs.getInt("trip_id"),
                    rs.getString("passenger_name"),
                    rs.getInt("rating_value"),
                    rs.getString("feedback"),
                    rs.getTimestamp("rating_date")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}