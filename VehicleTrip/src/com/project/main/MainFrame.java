package com.project.main;

import com.driver.panel.DriverData;
import com.driver.panel.DriverUI;
import com.passenger.panel.Passenger;
import com.project.panel.LoginPanel;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainFrame extends Application {

    public static CardPane mainPane;

    @Override
    public void start(Stage stage) {
        mainPane = new CardPane();

        // Driver panel (pre-created so LoginPanel can reference it)
        DriverUI driverPanel = new DriverUI(mainPane);

        // Login
        LoginPanel loginPanel = new LoginPanel(mainPane, driverPanel);

        // Passenger (placeholder – LoginPanel replaces it per session)
        Passenger passengerPlaceholder = new Passenger("Guest", mainPane);

        mainPane.addCard("LOGIN",     loginPanel);
        mainPane.addCard("DRIVER",    driverPanel);
        mainPane.addCard("PASSENGER", passengerPlaceholder);
        mainPane.show("LOGIN");

        double w = Screen.getPrimary().getVisualBounds().getWidth();
        double h = Screen.getPrimary().getVisualBounds().getHeight();
        Scene scene = new Scene(mainPane, w, h);
        scene.getStylesheets().add(FxUtil.CSS);

        stage.setTitle("Vehicle Trip Reservation System – EduTRIP");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}