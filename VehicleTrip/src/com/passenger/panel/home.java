package com.passenger.panel;



import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.GradientPanel;
import com.project.customSwing.ShadowPanel;
import com.project.customSwingPassenger.RoundedPanel;
import com.project.dbConnection.DbConnectMsSql;

public class home extends JPanel {

	private JLabel lblTotalTrips, lblUpcoming, lblPending, lblCancelled;
	private DefaultTableModel model;
	private DbConnectMsSql conn;
	private String username;

	public home(String username, JPanel container, CardLayout cardlayout) {

		this.username = username;
		conn = new DbConnectMsSql();

		setLayout(new BorderLayout());
		setBackground(new Color(240, 242, 245));

		JPanel header = new GradientPanel();
		header.setLayout(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

		JLabel welcome = new JLabel("WELCOME, " + username + " !");
		welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
		welcome.setForeground(Color.WHITE);

		header.add(welcome, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);

		JPanel content = new JPanel(new BorderLayout(15, 15));
		content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		content.setOpaque(false);
		add(content, BorderLayout.CENTER);

		JPanel topWrapper = new JPanel(new GridLayout(2, 1, 10, 10));
		topWrapper.setOpaque(false);

		JPanel quickWrapper = new RoundedPanel(15);
		quickWrapper.setLayout(new BorderLayout());
		quickWrapper.setBackground(Color.WHITE);
		quickWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10)); // dito yung size ng button

		JPanel quickTop = new JPanel(new BorderLayout());
		quickTop.setOpaque(false);

		JLabel quickTitle = new JLabel("Quick Actions");
		quickTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));

		JButton refresh = new JButton("Refresh");
		refresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
		refresh.setFocusPainted(false);
		refresh.setBackground(new Color(0, 150, 199));
		refresh.setForeground(Color.WHITE);
		refresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
		refresh.addActionListener(e -> loadDashboard());

		quickTop.add(quickTitle, BorderLayout.WEST);
		quickTop.add(refresh, BorderLayout.EAST);

		JPanel quickPanel = new JPanel(new GridLayout(1, 3, 10, 5));
		quickPanel.setOpaque(false);

		quickPanel.add(smallButton("Book Now", e -> cardlayout.show(container, "reservation")));
		quickPanel.add(smallButton("View Profile", e -> cardlayout.show(container, "profile")));
		quickPanel.add(smallButton("Show History Trips", e -> cardlayout.show(container, "trips")));
		
		quickWrapper.add(quickTop, BorderLayout.NORTH);
		quickWrapper.add(quickPanel, BorderLayout.CENTER);

		JPanel summaryWrapper = new RoundedPanel(20);
		summaryWrapper.setLayout(new BorderLayout());
		summaryWrapper.setBackground(Color.WHITE);
		summaryWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel summaryTitle = new JLabel("Dashboard Summary");
		summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));

		JPanel summary = new JPanel(new GridLayout(1, 4, 10, 0));
		summary.setOpaque(false);

		lblTotalTrips = new JLabel("0", SwingConstants.CENTER);
		lblUpcoming = new JLabel("0", SwingConstants.CENTER);
		lblPending = new JLabel("0", SwingConstants.CENTER);
		lblCancelled = new JLabel("0", SwingConstants.CENTER);

		summary.add(card("Total Trips", lblTotalTrips));
		summary.add(card("Completed", lblUpcoming));
		summary.add(card("Pending", lblPending));
		summary.add(card("Cancelled", lblCancelled));

		summaryWrapper.add(summaryTitle, BorderLayout.NORTH);
		summaryWrapper.add(summary, BorderLayout.CENTER);

		topWrapper.add(quickWrapper);
		topWrapper.add(summaryWrapper);

		content.add(topWrapper, BorderLayout.NORTH);

		String[] cols = { "Trip ID", "Vehicle", "Destination", "Start Date", "End Date", "Status" };

		model = new DefaultTableModel(cols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		JTable table = new JTable(model);
		table.setRowHeight(26);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

		table.getTableHeader().setBackground(new Color(0, 150, 199));
		table.getTableHeader().setForeground(Color.WHITE);

		JScrollPane sp = new JScrollPane(table);

		RoundedPanel tableBox = new RoundedPanel(20);
		tableBox.setLayout(new BorderLayout());
		tableBox.setBackground(Color.WHITE);
		tableBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel tableTitle = new JLabel("My Recent Trips");
		tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));

		tableBox.add(tableTitle, BorderLayout.NORTH);
		tableBox.add(sp, BorderLayout.CENTER);

		content.add(tableBox, BorderLayout.CENTER);

		loadDashboard();
	}

	private JPanel smallButton(String text, java.awt.event.ActionListener act) {

	    JPanel wrapper = new JPanel(new BorderLayout());
	    wrapper.setOpaque(false);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 10));

	    JPanel shadowBox = new JPanel(new BorderLayout()) {
	        @Override
	        protected void paintComponent(java.awt.Graphics g) {
	            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
	            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
	                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

	            for (int i = 6; i >= 1; i--) {
	                int alpha = 20 - (i * 2);
	                g2.setColor(new Color(0, 0, 0, Math.max(alpha, 5)));
	                g2.fillRoundRect(i, i, getWidth() - 1, getHeight() - 1, 14, 14);
	            }
	            g2.dispose();
	        }
	    };
	    shadowBox.setOpaque(false);
	    shadowBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));


	    RoundedPanel box = new RoundedPanel(12);
	    box.setLayout(new BorderLayout());
	    box.setBackground(Color.WHITE);
	    box.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(0, 150, 199), 1),
	        BorderFactory.createEmptyBorder(22, 10, 22, 10)
	    ));

	    JButton btn = new JButton(text);
	    btn.setBorder(null);
	    btn.setFocusPainted(false);
	    btn.setContentAreaFilled(false);
	    btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
	    btn.setForeground(new Color(0, 150, 199));
	    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btn.addActionListener(act);

	    box.add(btn, BorderLayout.CENTER);
	    shadowBox.add(box, BorderLayout.CENTER);
	    wrapper.add(shadowBox, BorderLayout.CENTER);

	    return wrapper;
	}

	private JPanel card(String title, JLabel value) {
		RoundedPanel p = new RoundedPanel(15);
		p.setLayout(new BorderLayout());
		p.setBackground(new Color(0, 150, 199));
		p.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

		value.setFont(new Font("Segoe UI", Font.BOLD, 26));
		value.setForeground(Color.WHITE);

		JLabel lbl = new JLabel(title, SwingConstants.CENTER);
		lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		lbl.setForeground(Color.WHITE);

		p.add(value, BorderLayout.CENTER);
		p.add(lbl, BorderLayout.SOUTH);

		return p;
	}


	private void loadDashboard() {
		try {

			int passengerId = 0;

			PreparedStatement ps = conn.conn.prepareStatement(
					"SELECT p.passenger_id FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id WHERE u.username=?");

			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();

			if (rs.next())
				passengerId = rs.getInt(1);

			PreparedStatement p1 = conn.conn.prepareStatement("SELECT COUNT(*) FROM Trip WHERE passenger_id=?");
			p1.setInt(1, passengerId);
			ResultSet r1 = p1.executeQuery();
			if (r1.next())
				lblTotalTrips.setText(r1.getString(1));

			PreparedStatement p2 = conn.conn.prepareStatement(
					"SELECT trip_status, COUNT(*) c FROM Trip WHERE passenger_id=? GROUP BY trip_status");
			p2.setInt(1, passengerId);
			ResultSet r2 = p2.executeQuery();

			int pend = 0, comp = 0, canc = 0;

			while (r2.next()) {
				String s = r2.getString(1);
				if (s.equalsIgnoreCase("Pending"))
					pend = r2.getInt(2);
				else if (s.equalsIgnoreCase("Completed"))
					comp = r2.getInt(2);
				else if (s.equalsIgnoreCase("Cancelled"))
					canc = r2.getInt(2);
			}

			lblPending.setText(String.valueOf(pend));
			lblUpcoming.setText(String.valueOf(comp));
			lblCancelled.setText(String.valueOf(canc));

			model.setRowCount(0);

			PreparedStatement p3 = conn.conn.prepareStatement(
					"SELECT TOP 10 t.trip_id,v.vehicle_model,t.destination,t.start_date,t.end_date,t.trip_status "
							+ "FROM Trip t JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id "
							+ "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id WHERE t.passenger_id=? ORDER BY t.start_date DESC");

			p3.setInt(1, passengerId);
			ResultSet r3 = p3.executeQuery();

			while (r3.next()) {
				model.addRow(new Object[] { r3.getInt(1), r3.getString(2), r3.getString(3), r3.getDate(4),
						r3.getDate(5), r3.getString(6) });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}