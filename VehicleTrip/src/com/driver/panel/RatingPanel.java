package com.driver.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RatingPanel extends VBox {

    private Label avgRatingValue   = new Label("0.0 ⭐");
    private Label totalRatingsValue = new Label("0");
    private ObservableList<Object[]> tableData;

    public RatingPanel() {
        setBackground(Background.fill(Color.WHITE));
        setPadding(new Insets(0, 0, 0, 0));
        buildUI();
    }

    public void loadData() { loadFromDb(); }

    private void buildUI() {
        Label title = new Label("Ratings");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");
        title.setPadding(new Insets(10, 20, 10, 20));

        VBox body = new VBox(15);
        body.setPadding(new Insets(0, 20, 20, 20));
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── Summary card ─────────────────────────────────────────────────────
        VBox summaryCard = new VBox(10);
        summaryCard.setStyle(
            "-fx-border-color:#C8D2E6;-fx-border-width:1px;-fx-border-radius:6px;" +
            "-fx-background-color:white;-fx-padding:15px;");

        Label summaryTitle = new Label("Rating Summary");
        summaryTitle.getStyleClass().add("title-small");

        GridPane grid = new GridPane();
        grid.setBackground(Background.fill(Color.WHITE));
        grid.setHgap(12); grid.setVgap(10);

        Label avgLbl   = new Label("Average Rating:");
        avgLbl.getStyleClass().add("form-label");
        avgRatingValue.getStyleClass().add("rating-avg");

        Label totalLbl = new Label("Total Ratings:");
        totalLbl.getStyleClass().add("form-label");
        totalRatingsValue.setStyle("-fx-font-weight:bold;-fx-font-size:18px;-fx-text-fill:#1A2B6D;");

        grid.add(avgLbl,          0, 0); grid.add(avgRatingValue,    1, 0);
        grid.add(totalLbl,        0, 1); grid.add(totalRatingsValue, 1, 1);

        summaryCard.getChildren().addAll(summaryTitle, grid);

        // ── History table ─────────────────────────────────────────────────────
        VBox historyCard = new VBox(10);
        historyCard.setStyle(
            "-fx-border-color:#C8D2E6;-fx-border-width:1px;-fx-border-radius:6px;" +
            "-fx-background-color:white;-fx-padding:15px;");
        VBox.setVgrow(historyCard, Priority.ALWAYS);

        Label historyTitle = new Label("Rating History");
        historyTitle.getStyleClass().add("title-small");

        TableView<Object[]> table = FxUtil.buildTable(
            "Trip ID", "Passenger", "Rating", "Feedback", "Date");
        tableData = FxUtil.tableData(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        historyCard.getChildren().addAll(historyTitle, FxUtil.tableScroll(table));
        VBox.setVgrow(historyCard, Priority.ALWAYS);

        body.getChildren().addAll(summaryCard, historyCard);
        getChildren().addAll(title, body);
        VBox.setVgrow(body, Priority.ALWAYS);
    }

    private void loadFromDb() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "SELECT t.trip_id, u.username AS passenger_name," +
                "       tr.rating_value, tr.feedback, tr.rating_date " +
                "FROM trip_rating tr " +
                "JOIN Trip t ON tr.trip_id=t.trip_id " +
                "JOIN Passenger p ON t.passenger_id=p.passenger_id " +
                "JOIN Users u ON p.passenger_id=u.user_id " +
                "JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                "WHERE va.driver_id=? ORDER BY tr.rating_date DESC");
            ps.setInt(1, DriverData.driverId);
            ResultSet rs = ps.executeQuery();

            tableData.clear();
            double total = 0; int count = 0;
            while (rs.next()) {
                int rating = rs.getInt("rating_value");
                total += rating; count++;
                tableData.add(new Object[]{
                    rs.getInt("trip_id"),
                    rs.getString("passenger_name"),
                    rating + " /5",
                    rs.getString("feedback"),
                    rs.getTimestamp("rating_date")
                });
            }
            avgRatingValue.setText(count > 0 ? String.format("%.1f ★", total / count) : "No ratings yet");
            totalRatingsValue.setText(String.valueOf(count));
        } catch (Exception e) { e.printStackTrace(); }
    }
}