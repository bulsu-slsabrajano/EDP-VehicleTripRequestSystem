package com.project.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.admin.panel.TripPanel;
import com.driver.panel.DriverUI;
import com.passenger.panel.Passenger;
import com.project.panel.LoginPanel;

public class MainFrame {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Vehicle Trip Reservation System");
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);

        DriverUI driverPanel = new DriverUI("Driver", cardLayout, mainPanel); 
        Passenger passengerPanel = new Passenger("Guest", mainPanel, cardLayout);
        
        //nilipat ko dito
        LoginPanel loginPanel = new LoginPanel(cardLayout, mainPanel, driverPanel);
        
        

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(driverPanel, "DRIVER");
        mainPanel.add(passengerPanel, "PASSENGER");

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        cardLayout.show(mainPanel, "LOGIN");
	}

}

