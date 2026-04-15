package com.passenger.panel;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class reservation extends VBox {

    private final CardPane  innerPane;
    private final trips     tripsPanel;
    private final int       passengerId;
    private       Connection conn;

    private TextField  txtPickup      = FxUtil.styledField();
    private TextField  txtDestination = FxUtil.styledField();
    private DatePicker dpStartDate    = new DatePicker(LocalDate.now());
    private DatePicker dpEndDate      = new DatePicker(LocalDate.now());
    private TextField  txtStartTime   = FxUtil.styledField();
    private TextField  txtEndTime     = FxUtil.styledField();
    private Spinner<Integer>    passengersSpinner;
    private ComboBox<String>    vehicleBox = new ComboBox<>();
    private final List<Integer> vehicleAssignmentIds = new ArrayList<>();

    public reservation(CardPane innerPane, trips tripsPanel, int passengerId) {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        this.innerPane    = innerPane;
        this.tripsPanel   = tripsPanel;
        this.passengerId  = passengerId;
        setBackground(Background.fill(Color.WHITE));
        setAlignment(Pos.TOP_CENTER);
        buildUI();
    }

    private void buildUI() {
       
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 25, 20, 25));
        header.setPrefHeight(76);

        Label hTitle = new Label("Book a Trip");
        hTitle.getStyleClass().add("welcome-title");
        hTitle.setStyle("-fx-font-weight:bold;-fx-font-size:26px;-fx-text-fill:#1A2B6D;");
        header.getChildren().add(hTitle);

        
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setMaxWidth(640);
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Trip Reservation");
        title.getStyleClass().add("title-medium");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        GridPane form = FxUtil.formGrid();
        int y = 0;

        // Trip Schedule
        Label schedLbl = FxUtil.sectionLabel("Trip Schedule");
        GridPane.setColumnSpan(schedLbl, 2);
        form.add(schedLbl, 0, y++);

        //no past dates
        dpStartDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#ffe0e0;");
                }
            }
        });

        dpEndDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate start = dpStartDate.getValue() != null
                        ? dpStartDate.getValue() : LocalDate.now();
                if (date.isBefore(start)) {
                    setDisable(true);
                    setStyle("-fx-background-color:#ffe0e0;");
                }
            }
        });

        // Auto-adjust end date when start date changes
        dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dpEndDate.getValue() != null
                    && dpEndDate.getValue().isBefore(newVal)) {
                dpEndDate.setValue(newVal);
            }
        });
        

        dpStartDate.getStyleClass().add("date-field"); dpStartDate.setPrefWidth(260);
        dpEndDate.getStyleClass().add("date-field");   dpEndDate.setPrefWidth(260);
        txtStartTime.setPromptText("HH:MM");
        txtEndTime.setPromptText("HH:MM");

        FxUtil.addFormRow(form, "Start Date:", dpStartDate,    y++);
        FxUtil.addFormRow(form, "End Date:",   dpEndDate,      y++);
        FxUtil.addFormRow(form, "Start Time:", txtStartTime,   y++);
        FxUtil.addFormRow(form, "End Time:",   txtEndTime,     y++);

        // Locations
        Label locLbl = FxUtil.sectionLabel("Locations");
        GridPane.setColumnSpan(locLbl, 2);
        form.add(locLbl, 0, y++);

        FxUtil.addFormRow(form, "Pickup:",      txtPickup,      y++);
        FxUtil.addFormRow(form, "Destination:", txtDestination, y++);

        // Passengers & Vehicle
        Label pvLbl = FxUtil.sectionLabel("Passengers & Vehicle");
        GridPane.setColumnSpan(pvLbl, 2);
        form.add(pvLbl, 0, y++);

        passengersSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 54, 1));
        passengersSpinner.getStyleClass().add("spinner-field");
        passengersSpinner.setPrefWidth(260);
        passengersSpinner.setEditable(false);
        passengersSpinner.valueProperty().addListener(
            (obs, old, val) -> loadAvailableAssignments(val));

        vehicleBox.getStyleClass().add("combo-field");
        vehicleBox.setPrefWidth(260);

        FxUtil.addFormRow(form, "No. of Passengers:", passengersSpinner, y++);
        FxUtil.addFormRow(form, "Vehicle / Assignment:", vehicleBox,     y);

        loadAvailableAssignments(1);

        // Buttons
        Button btnSubmit = FxUtil.btnPrimary("Book Trip");
        Button btnCancel = FxUtil.btnOutlineDanger("Cancel");
        btnSubmit.setPrefWidth(110);
        btnCancel.setPrefWidth(110);

        HBox btnRow = new HBox(15, btnSubmit, btnCancel);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(20, 0, 0, 0));

        card.getChildren().addAll(title, FxUtil.spacer(20), form, btnRow, FxUtil.spacer(10));

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setPrefSize(640, 600);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);

        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);

        btnSubmit.setOnAction(e -> submitBooking());
        btnCancel.setOnAction(e -> {
            if (FxUtil.confirm(this, "Are you sure you want to cancel?", "Confirm Cancel"))
                resetForm();
        });
    }

    //Validation
    private boolean validateDatesAndTimes() {
        LocalDate today     = LocalDate.now();
        LocalTime now       = LocalTime.now();
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate   = dpEndDate.getValue();

       
        if (startDate == null || endDate == null) {
            FxUtil.showError(this, "Please select valid Start and End dates.");
            return false;
        }

        //Start date must not be in the past
        if (startDate.isBefore(today)) {
            FxUtil.showError(this, "Start date cannot be in the past.");
            return false;
        }

        //End date must not be before start date
        if (endDate.isBefore(startDate)) {
            FxUtil.showError(this, "End date cannot be before Start date.");
            return false;
        }

        //Parse times
        	Time startTime, endTime;
        try {
            startTime = parseTime(txtStartTime.getText().trim());
            endTime   = parseTime(txtEndTime.getText().trim());
        } catch (Exception e) {
            FxUtil.showError(this, "Invalid time format! Use HH:MM");
            return false;
        }

        LocalTime parsedStart = startTime.toLocalTime();
        LocalTime parsedEnd   = endTime.toLocalTime();

        //If booking is today, start time must not be in the past
        if (startDate.isEqual(today) && parsedStart.isBefore(now)) {
            FxUtil.showError(this,
                "Start time cannot be in the past for today's booking.\n" +
                "Current time is: " + now.getHour() + ":" +
                String.format("%02d", now.getMinute()));
            return false;
        }

        //If same day booking, end time must be after start time
        if (startDate.isEqual(endDate) && !parsedEnd.isAfter(parsedStart)) {
            FxUtil.showError(this,
                "End time must be after Start time on the same day.");
            return false;
        }

        return true;
    }

    private void loadAvailableAssignments(int numPassengers) {
        vehicleBox.getItems().clear();
        vehicleAssignmentIds.clear();
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT va.assignment_id," +
                "du.first_name+' '+du.last_name+' - '+v.vehicle_model AS label," +
                "v.vehicle_type, v.passenger_capacity " +
                "FROM Vehicle_Assignment va " +
                "JOIN Driver d  ON va.driver_id=d.driver_id " +
                "JOIN Users du  ON d.driver_id=du.user_id " +
                "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id " +
                "WHERE va.assignment_status='Active' " +
                "AND v.passenger_capacity>=? AND v.vehicle_status='Available'");
            ps.setInt(1, numPassengers);
            ResultSet rs = ps.executeQuery();
            boolean hasAny = false;
            while (rs.next()) {
                hasAny = true;
                vehicleBox.getItems().add(rs.getString("label") +
                    " (Cap: " + rs.getInt("passenger_capacity") + ")");
                vehicleAssignmentIds.add(rs.getInt("assignment_id"));
            }
            if (!hasAny) {
                vehicleBox.getItems().add("No vehicle available yet — admin will assign");
                vehicleAssignmentIds.add(-1);
            }
            if (!vehicleBox.getItems().isEmpty())
                vehicleBox.getSelectionModel().selectFirst();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void submitBooking() {
        //Required field check
        if (txtPickup.getText().trim().isEmpty() || txtDestination.getText().trim().isEmpty()) {
            FxUtil.showError(this, "Pickup and Destination are required.");
            return;
        }

        //Date & time validation
        if (!validateDatesAndTimes()) return;

        
        Date startDate = Date.valueOf(dpStartDate.getValue());
        Date endDate   = Date.valueOf(dpEndDate.getValue());
        Time startTime, endTime;
        try {
            startTime = parseTime(txtStartTime.getText().trim());
            endTime   = parseTime(txtEndTime.getText().trim());
        } catch (Exception e) {
            FxUtil.showError(this, "Invalid time format! Use HH:MM");
            return;
        }

        int numPax = passengersSpinner.getValue();
        int selIdx = vehicleBox.getSelectionModel().getSelectedIndex();
        Integer assignmentId = (selIdx >= 0 && selIdx < vehicleAssignmentIds.size())
            ? vehicleAssignmentIds.get(selIdx) : null;
        if (assignmentId != null && assignmentId == -1) assignmentId = null;

        boolean ok = insertTrip(startDate, endDate, startTime, endTime,
            txtPickup.getText().trim(), txtDestination.getText().trim(), numPax, assignmentId);

        if (ok) {
            if (assignmentId != null) setAssignmentResourcesUnavailable(assignmentId);
            FxUtil.showInfo(this,
                "Reservation submitted! An admin and vehicle will be assigned shortly.");
            resetForm();
            tripsPanel.resetToPending();
            innerPane.show("trips");
        } else {
            FxUtil.showError(this, "Failed to submit booking. Please try again.");
        }
    }

    private boolean insertTrip(java.sql.Date sd, java.sql.Date ed,
                                java.sql.Time st, java.sql.Time et,
                                String pickup, String dest, int pax, Integer assignId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Trip " +
                "(passenger_id,assignment_id,start_date,end_date,start_time,end_time," +
                "pick_up_location,destination,passenger_count,trip_status) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)");
            ps.setInt(1, passengerId);
            if (assignId != null) ps.setInt(2, assignId); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setDate(3, sd); ps.setDate(4, ed);
            ps.setTime(5, st); ps.setTime(6, et);
            ps.setString(7, pickup); ps.setString(8, dest);
            ps.setInt(9, pax); ps.setString(10, "Pending");
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void setAssignmentResourcesUnavailable(int assignmentId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT driver_id, vehicle_id FROM Vehicle_Assignment WHERE assignment_id=?");
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                conn.prepareStatement("UPDATE Driver SET driver_status='Not Available' WHERE driver_id=" + rs.getInt("driver_id")).executeUpdate();
                conn.prepareStatement("UPDATE Vehicle SET vehicle_status='Not Available' WHERE vehicle_id=" + rs.getInt("vehicle_id")).executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void resetForm() {
        txtPickup.clear(); txtDestination.clear();
        txtStartTime.clear(); txtEndTime.clear();
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now());
        passengersSpinner.getValueFactory().setValue(1);
        loadAvailableAssignments(1);
    }

    private java.sql.Time parseTime(String s) throws Exception {
        if (s == null || s.isBlank()) return java.sql.Time.valueOf(LocalTime.now());
        if (s.matches("\\d{2}:\\d{2}")) s += ":00";
        return java.sql.Time.valueOf(s);
    }
}