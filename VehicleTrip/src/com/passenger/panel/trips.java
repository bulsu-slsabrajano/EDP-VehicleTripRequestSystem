package com.passenger.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.FxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class trips extends VBox {

    private ObservableList<Object[]> pendingData   = FXCollections.observableArrayList();
    private ObservableList<Object[]> approvedData  = FXCollections.observableArrayList();
    private ObservableList<Object[]> completedData = FXCollections.observableArrayList();
    private ObservableList<Object[]> cancelledData = FXCollections.observableArrayList();

    private TableView<Object[]> pendingT, approvedT, completedT, cancelledT;
    private Connection conn;
    private int passengerId;

    private final TabPane tabPane = new TabPane();
    private int currentRateTripId = -1;

    // Rate panel elements (shown inside a Dialog)
    private Spinner<Integer> rateSpinner;
    private TextArea         expArea;

    public trips(int passengerId) {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        this.passengerId = passengerId;
        setBackground(Background.fill(Color.WHITE));
        buildUI();
    }

    private void buildUI() {
       

        //Pending tab 
        pendingT = buildTripTable(pendingData);
        VBox pendingContent = new VBox(FxUtil.tableScroll(pendingT));
        VBox.setVgrow(pendingT, Priority.ALWAYS);

        Button btnCancelPending = FxUtil.btnDanger("Cancel Trip");
        Button pendingRefresh   = FxUtil.btnPrimary("Refresh");
        pendingRefresh.setOnAction(e -> { pendingData.clear(); loadTrips(pendingData, "Pending"); });
        btnCancelPending.setOnAction(e -> moveRow(pendingT, pendingData, cancelledData, "Cancelled"));

        pendingContent.getChildren().addAll(bottomBar(btnCancelPending));
        Tab pendingTab = tab("Pending", pendingContent);

        //Approved tab
        approvedT = buildTripTable(approvedData);
        VBox approvedContent = new VBox(FxUtil.tableScroll(approvedT));
        VBox.setVgrow(approvedT, Priority.ALWAYS);

        Button btnCancelApproved  = FxUtil.btnDanger("Cancel Trip");
        Button btnCompleteApproved = FxUtil.btnSuccess("Completed");
        Button approvedRefresh    = FxUtil.btnPrimary("Refresh");
        approvedRefresh.setOnAction(e -> { approvedData.clear();
        loadTrips(approvedData, "Approved"); });
        btnCancelApproved.setOnAction(e -> moveRow(approvedT, approvedData, cancelledData, "Cancelled"));
        btnCompleteApproved.setOnAction(e -> moveRow(approvedT, approvedData, completedData, "Completed"));

        approvedContent.getChildren().add(bottomBar(btnCancelApproved, btnCompleteApproved));
        Tab approvedTab = tab("Approved", approvedContent);

        //Completed tab
        completedT = buildTripTable(completedData);
        VBox completedContent = new VBox(FxUtil.tableScroll(completedT));
        VBox.setVgrow(completedT, Priority.ALWAYS);

        Button btnRate          = FxUtil.btnPrimary("Rate Trip");
        Button completedRefresh = FxUtil.btnPrimary("Refresh");
        completedRefresh.setOnAction(e -> { completedData.clear();
        loadTrips(completedData, "Completed"); });
        btnRate.setOnAction(e -> {
            Object[] row = completedT.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(null, "Please select a completed trip to rate."); return; }
            currentRateTripId = (int) row[0];
            showRateDialog();
        });

        completedContent.getChildren().add(bottomBar(btnRate));
        Tab completedTab = tab("Completed", completedContent);

        //Cancelled tab
        cancelledT = buildTripTable(cancelledData);
        VBox cancelledContent = new VBox(FxUtil.tableScroll(cancelledT));
        VBox.setVgrow(cancelledT, Priority.ALWAYS);

        Button cancelledRefresh = FxUtil.btnPrimary("Refresh");
        cancelledRefresh.setOnAction(e -> { cancelledData.clear();
        loadTrips(cancelledData, "Cancelled"); });

        Tab cancelledTab = tab("Cancelled", cancelledContent);

        tabPane.getStyleClass().add("custom-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(pendingTab, approvedTab, completedTab, cancelledTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().addAll(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        loadAll();
    }

  
    private TableView<Object[]> buildTripTable(ObservableList<Object[]> data) {
        TableView<Object[]> t = FxUtil.buildTable(
            "Trip ID","Assignment ID","Admin ID","Start Date","End Date",
            "Start Time","End Time","Pickup","Destination","Passengers","Status");
        t.setItems(data);
        FxUtil.applyStatusRenderer(t, 10);
        return t;
    }

    private Tab tab(String name, VBox content) {
        content.setBackground(Background.fill(Color.WHITE));
        VBox.setVgrow(content, Priority.ALWAYS);
        Tab t = new Tab(name, content);
        t.setClosable(false);
        return t;
    }

    private HBox bottomBar(Button... buttons) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(8));
        bar.setStyle("-fx-border-color:#C8D7E1;-fx-border-width:1 0 0 0;-fx-background-color:white;");
        bar.getChildren().addAll(buttons);
        return bar;
    }

    private void loadAll() {
        loadTrips(pendingData,   "Pending");
        loadTrips(approvedData,  "Approved");
        loadTrips(completedData, "Completed");
        loadTrips(cancelledData, "Cancelled");
    }

    private void loadTrips(ObservableList<Object[]> model, String status) {
    	 model.clear();
    	
    	try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Trip WHERE trip_status=? AND passenger_id=?");
            ps.setString(1, status); ps.setInt(2, passengerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.add(new Object[]{
                    rs.getInt("trip_id"), 
                    rs.getObject("assignment_id"),
                    rs.getObject("admin_id"),
                    rs.getDate("start_date"), 
                    rs.getDate("end_date"),
                    rs.getObject("start_time"),
                    rs.getObject("end_time"),
                    rs.getString("pick_up_location"),
                    rs.getString("destination"),
                    rs.getInt("passenger_count"),
                    rs.getString("trip_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateTripStatus(int tripId, String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Trip SET trip_status=? WHERE trip_id=?");
            ps.setString(1, status);
            ps.setInt(2, tripId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void restoreAssignmentResources(int tripId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT assignment_id FROM Trip WHERE trip_id=?");
            ps.setInt(1, tripId);
            ResultSet rs = ps.executeQuery();
           
            if (!rs.next()) return;
            Object assignObj = rs.getObject("assignment_id");
           
            if (assignObj == null) 
            	return;
            
            int aid = (int) assignObj;
            
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT driver_id, vehicle_id FROM Vehicle_Assignment WHERE assignment_id=?");
            ps2.setInt(1, aid);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                conn.prepareStatement("UPDATE Driver SET driver_status='Available' WHERE driver_id=" + rs2.getInt("driver_id")).executeUpdate();
                conn.prepareStatement("UPDATE Vehicle SET vehicle_status='Available' WHERE vehicle_id=" + rs2.getInt("vehicle_id")).executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void moveRow(TableView<Object[]> table, ObservableList<Object[]> source,
                         ObservableList<Object[]> target, String newStatus) {
        Object[] row = table.getSelectionModel().getSelectedItem();
        if (row == null) { 
        	FxUtil.showInfo(null, "Please select a trip first.");
        	return;
        }

        String msg = "Cancelled".equals(newStatus)
            ? "Are you sure you want to cancel this trip?"
            : "Are you sure this trip is completed?";
        if (!FxUtil.confirm(null, msg, "Confirm")) 
        	return;

        int tripId = (int) row[0];
        updateTripStatus(tripId, newStatus);
        if ("Completed".equals(newStatus) || "Cancelled".equals(newStatus))
            restoreAssignmentResources(tripId);

        Object[] updated = row.clone();
        updated[updated.length - 1] = newStatus;
        target.add(updated);
        source.remove(row);
    }

    private void showRateDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rate Trip");
        dialog.setHeaderText("How would you rate this trip?");

        rateSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1));
        rateSpinner.setPrefWidth(80);
        expArea = new TextArea();
        expArea.getStyleClass().add("feedback-area");
        expArea.setPromptText("Share your experience...");

        GridPane grid = FxUtil.formGrid();
        FxUtil.addFormRow(grid, "Rate (1–5):",          rateSpinner, 0);
        FxUtil.addFormRow(grid, "Share your experience:", expArea,    1);
        dialog.getDialogPane().setContent(grid);

        ButtonType submitType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == submitType) {
                String result = insertRating(currentRateTripId,
                    rateSpinner.getValue(), expArea.getText().trim());
                switch (result) {
                    case "SUCCESS"   -> FxUtil.showInfo(null, "Thank you for your feedback!");
                    case "DUPLICATE" -> FxUtil.showWarning(null, "This trip has already been rated.");
                    default          -> FxUtil.showError(null, "Failed to submit rating.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private String insertRating(int tripId, int rating, String feedback) {
        try {
            PreparedStatement chk = conn.prepareStatement(
                "SELECT COUNT(*) FROM trip_rating WHERE trip_id=?");
            chk.setInt(1, tripId);
            ResultSet rc = chk.executeQuery();
           
            if (rc.next() && rc.getInt(1) > 0) return "DUPLICATE";
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO trip_rating (trip_id, rating_value, feedback) VALUES (?,?,?)");
            ps.setInt(1, tripId); 
            ps.setInt(2, rating);
            ps.setString(3, feedback);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "FAILED";
        } catch (Exception e) { e.printStackTrace(); return "ERROR"; }
    }

    public void resetToPending() {
        loadAll();
        tabPane.getSelectionModel().selectFirst();
    }
}