package com.passenger.panel;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.project.dbConnection.DbConnectMsSql;

public class Passenger extends JPanel {

	private CardLayout cardLayout, layout;
	private JPanel mainPanel, container;
	private final String username;
	private int userId;

	private trips tripsPanel;
	private profile profilePanel;
	private reservation reservationPanel;

	public Passenger(String username, JPanel container, CardLayout layout) {
	    this.username = username;
	    this.container = container;
	    this.layout = layout;
		this.userId = getUserIdFromUsername(username);

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(1000, 600));

		createNavbar();
		createPanels();
	}

	private void createNavbar() {

		JPanel nav = new JPanel(new BorderLayout());
		nav.setBackground(new Color(20, 30, 50));

		
		ImageIcon logoIcon = new ImageIcon(getClass().getResource("/com/project/resources/companyLogo.png"));
		Image img = logoIcon.getImage().getScaledInstance(80, 60, Image.SCALE_SMOOTH);
		JLabel logo = new JLabel(new ImageIcon(img));

		JPanel logoPanel = new JPanel();
		logoPanel.setBackground(new Color(20, 30, 50));
		logoPanel.add(logo);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 20, 0));
		buttonPanel.setBackground(new Color(20, 30, 50));

		JButton dashboard = new JButton("Dashboard");
		JButton reservation = new JButton("Reservation");
		JButton trips = new JButton("Trips");
		JButton profile = new JButton("Profile");
		JButton logout = new JButton("Logout");

		JButton[] buttons = { dashboard, reservation, trips, profile, logout };

		for (JButton b : buttons) {
			b.setForeground(Color.WHITE);
			b.setBackground(new Color(20, 30, 50));
			b.setBorderPainted(false);
			b.setFocusPainted(false);
			b.setFont(new Font("Segoe UI", Font.BOLD, 18));
			b.setPreferredSize(new Dimension(150, 60));
			b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

			buttonPanel.add(b);
		}

		dashboard.addActionListener(e -> cardLayout.show(mainPanel, "dashboard"));

		reservation.addActionListener(e -> {
			cardLayout.show(mainPanel, "reservation");
		});

		trips.addActionListener(e -> {
			tripsPanel.resetToPending();
			cardLayout.show(mainPanel, "trips");
		});

		profile.addActionListener(e -> {
			profilePanel.loadProfile(userId);
			cardLayout.show(mainPanel, "profile");
		});

		logout.addActionListener(e -> {
		    int choice = JOptionPane.showConfirmDialog(null,
		            "Are you sure you want to Logout?",
		            "Confirm",
		            JOptionPane.YES_NO_OPTION);

		    if (choice == JOptionPane.YES_OPTION) {
		        insertAuditLog(userId, "Logged Out");
		        layout.show(container, "LOGIN");
		    }
		});
		
		nav.add(logoPanel, BorderLayout.WEST);
		nav.add(buttonPanel, BorderLayout.CENTER);

		add(nav, BorderLayout.NORTH);
	}

	private void createPanels() {

		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);

		// Dashboard
		mainPanel.add(new home(username, mainPanel, cardLayout), "dashboard");

		// Trips
		tripsPanel = new trips(userId);
		mainPanel.add(tripsPanel, "trips");

		// Profile
		profilePanel = new profile(userId);
		mainPanel.add(profilePanel, "profile");

		// Reservation
		reservationPanel = new reservation(cardLayout, mainPanel, tripsPanel, this.userId);
		mainPanel.add(reservationPanel, "reservation");

		add(mainPanel, BorderLayout.CENTER);
	}

	private int getUserIdFromUsername(String username) {
		int id = -1;

		try {
			DbConnectMsSql db = new DbConnectMsSql();
			Connection conn = db.conn;

			String sql = "SELECT user_id FROM Users WHERE username = ?";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, username);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				id = rs.getInt("user_id");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return id;
	}
	
	private void insertAuditLog(int userId, String status) {
	    try {
	        DbConnectMsSql db = new DbConnectMsSql();
	        java.sql.PreparedStatement ps = db.conn.prepareStatement(
	            "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
	        ps.setInt(1, userId);
	        ps.setString(2, status);
	        ps.executeUpdate();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
