package com.passenger.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.sql.Connection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.project.customSwing.GradientPanel;
import com.project.dbConnection.DbConnectMsSql;

public class trips extends JPanel {

	private static final Color NAVY = new Color(20, 40, 80);
	private static final Color BG = new Color(235, 242, 245);
	private static final Color BORDER = new Color(200, 215, 225);
	private static final Color ROW_ALT = new Color(245, 251, 253);
	private static final Color TEAL = new Color(0, 150, 199);

	private JPanel pTrips = new JPanel();
	private JButton pendingB, approvedB, completedB, cancelledB;
	private JPanel pendingP, approvedP, completedP, cancelledP;

	private CardLayout outerChoice;
	private JPanel outerPanel;

	private static JPanel tabPanel;
	private static CardLayout tabChoice;

	private JTable pendingT, approvedT, completedT, cancelledT;
	private DefaultTableModel pendingM, approvedM, completedM, cancelledM;
	private Connection conn;

	private JSpinner rateSpinner;
	private JTextArea expArea;
	private int currentRateTripId = -1;

	private int passengerId;

	public trips(int passengerId) {
		DbConnectMsSql db = new DbConnectMsSql();
		conn = db.conn;

		this.passengerId = passengerId;

		setLayout(new BorderLayout());
		setBackground(BG);

		outerChoice = new CardLayout();
		outerPanel = new JPanel(outerChoice);
		outerPanel.setBackground(Color.WHITE);
		add(outerPanel, BorderLayout.CENTER);

		pTrips.setLayout(new GridBagLayout());
		pTrips.setBackground(Color.WHITE);

		GridBagConstraints gbc = new GridBagConstraints();

		tabChoice = new CardLayout();
		tabPanel = new JPanel(tabChoice);
		tabPanel.setBackground(Color.WHITE);

		JPanel tabBar = new GradientPanel();
		tabBar.setLayout(new BorderLayout());
		tabBar.setOpaque(true);
		tabBar.setPreferredSize(new Dimension(0, 76));
		tabBar.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

		JPanel tabButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 10));
		tabButtons.setOpaque(false);

		pendingB = createTabButton("PENDING");
		approvedB = createTabButton("APPROVED");
		completedB = createTabButton("COMPLETED");
		cancelledB = createTabButton("CANCELLED");

		tabButtons.add(pendingB);
		tabButtons.add(approvedB);
		tabButtons.add(completedB);
		tabButtons.add(cancelledB);

		tabBar.add(tabButtons, BorderLayout.WEST);

		JButton[] btns = { pendingB, approvedB, completedB, cancelledB };
		ActionListener tabListener = e -> {
			JButton clicked = (JButton) e.getSource();
			for (JButton b : btns)
				setTabInactive(b);
			setTabActive(clicked);
		};
		for (JButton b : btns)
			b.addActionListener(tabListener);

		pendingB.addActionListener(e -> tabChoice.show(tabPanel, "Pending"));
		approvedB.addActionListener(e -> tabChoice.show(tabPanel, "Approved"));
		completedB.addActionListener(e -> tabChoice.show(tabPanel, "Completed"));
		cancelledB.addActionListener(e -> tabChoice.show(tabPanel, "Cancelled"));

		setTabActive(pendingB);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		pTrips.add(tabBar, gbc);

		pendingP = new JPanel(new BorderLayout());
		pendingP.setBackground(Color.WHITE);
		String[] pCols = { "Trip ID", "Assignment ID", "Admin ID", "Start Date", "End Date", "Start Time", "End Time",
				"Pickup", "Destination", "Passengers", "Status" };
		pendingM = new DefaultTableModel(pCols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		loadTrips(pendingM, "Pending");
		pendingT = createStyledTable(pendingM);
		pendingP.add(new JScrollPane(pendingT), BorderLayout.CENTER);

		JLabel pendingEmpty = createEmptyLabel("No Pending Trips for now");
		pendingEmpty.setVisible(pendingM.getRowCount() == 0);
		JButton pendingRefresh = createSolidButton("Refresh");
		pendingP.add(buildTopBar(pendingEmpty, pendingRefresh), BorderLayout.NORTH);

		JButton pCancelTrip = createSolidButton("Cancel Trip");
		pendingP.add(buildBottomBar(pCancelTrip), BorderLayout.SOUTH);

		pendingM.addTableModelListener(e -> pendingEmpty.setVisible(pendingM.getRowCount() == 0));
		pendingRefresh.addActionListener(e -> {
			pendingM.setRowCount(0);
			loadTrips(pendingM, "Pending");
		});
		// CHANGE: pass pendingT and pendingM to moveRow so availability is restored on cancel
		pCancelTrip.addActionListener(e -> moveRow(pendingT, pendingM, cancelledM, "Cancelled"));

		approvedP = new JPanel(new BorderLayout());
		approvedP.setBackground(Color.WHITE);
		String[] aCols = { "Trip ID", "Assignment ID", "Admin ID", "Start Date", "End Date", "Start Time", "End Time",
				"Pickup", "Destination", "Passengers", "Status" };
		approvedM = new DefaultTableModel(aCols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		loadTrips(approvedM, "Approved");
		approvedT = createStyledTable(approvedM);
		approvedP.add(new JScrollPane(approvedT), BorderLayout.CENTER);

		JLabel approvedEmpty = createEmptyLabel("No Approved Trips for now");
		approvedEmpty.setVisible(approvedM.getRowCount() == 0);
		JButton approvedRefresh = createSolidButton("Refresh");
		approvedP.add(buildTopBar(approvedEmpty, approvedRefresh), BorderLayout.NORTH);

		JButton aCancelTrip = createSolidButton("Cancel Trip");
		JButton aTripComplete = createSolidButton("Completed");
		approvedP.add(buildBottomBar(aCancelTrip, aTripComplete), BorderLayout.SOUTH);

		approvedM.addTableModelListener(e -> approvedEmpty.setVisible(approvedM.getRowCount() == 0));
		approvedRefresh.addActionListener(e -> {
			approvedM.setRowCount(0);
			loadTrips(approvedM, "Approved");
		});
		// CHANGE: restore availability on cancel or complete from approved
		aCancelTrip.addActionListener(e -> moveRow(approvedT, approvedM, cancelledM, "Cancelled"));
		aTripComplete.addActionListener(e -> moveRow(approvedT, approvedM, completedM, "Completed"));

		completedP = new JPanel(new BorderLayout());
		completedP.setBackground(Color.WHITE);
		completedM = new DefaultTableModel(aCols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		loadTrips(completedM, "Completed");
		completedT = createStyledTable(completedM);
		completedP.add(new JScrollPane(completedT), BorderLayout.CENTER);

		JLabel completedEmpty = createEmptyLabel("No Completed Trips for now");
		completedEmpty.setVisible(completedM.getRowCount() == 0);
		JButton completedRefresh = createSolidButton("Refresh");
		completedP.add(buildTopBar(completedEmpty, completedRefresh), BorderLayout.NORTH);

		JButton compRate = createSolidButton("Rate Trip");
		completedP.add(buildBottomBar(compRate), BorderLayout.SOUTH);

		completedM.addTableModelListener(e -> completedEmpty.setVisible(completedM.getRowCount() == 0));
		completedRefresh.addActionListener(e -> {
			completedM.setRowCount(0);
			loadTrips(completedM, "Completed");
		});

		compRate.addActionListener(e -> {
			int selectedRow = completedT.getSelectedRow();
			if (selectedRow == -1) {
				JOptionPane.showMessageDialog(this, "Please select a completed trip to rate.", "No Trip Selected",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			currentRateTripId = (int) completedM.getValueAt(selectedRow, 0);
			rateSpinner.setValue(1);
			expArea.setText("");
			outerChoice.show(outerPanel, "rate");
		});

		cancelledP = new JPanel(new BorderLayout());
		cancelledP.setBackground(Color.WHITE);
		cancelledM = new DefaultTableModel(aCols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		loadTrips(cancelledM, "Cancelled");
		cancelledT = createStyledTable(cancelledM);
		cancelledP.add(new JScrollPane(cancelledT), BorderLayout.CENTER);

		JLabel cancelledEmpty = createEmptyLabel("No Cancelled Trips for now");
		cancelledEmpty.setVisible(cancelledM.getRowCount() == 0);
		JButton cancelledRefresh = createSolidButton("Refresh");
		cancelledP.add(buildTopBar(cancelledEmpty, cancelledRefresh), BorderLayout.NORTH);

		cancelledM.addTableModelListener(e -> cancelledEmpty.setVisible(cancelledM.getRowCount() == 0));
		cancelledRefresh.addActionListener(e -> {
			cancelledM.setRowCount(0);
			loadTrips(cancelledM, "Cancelled");
		});

		tabPanel.add(pendingP, "Pending");
		tabPanel.add(approvedP, "Approved");
		tabPanel.add(completedP, "Completed");
		tabPanel.add(cancelledP, "Cancelled");

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		pTrips.add(tabPanel, gbc);

		outerPanel.add(pTrips, "trips");
		outerPanel.add(buildRatePanel(), "rate");

		outerChoice.show(outerPanel, "trips");
	}

	private JPanel buildRatePanel() {
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setBackground(BG);

		JPanel card = new JPanel(new GridBagLayout());
		card.setBackground(Color.WHITE);
		card.setBorder(new EmptyBorder(30, 35, 30, 35));
		card.setPreferredSize(new Dimension(480, 380));

		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		gc.insets = new Insets(8, 0, 8, 0);

		JLabel title = new JLabel("How would you rate this trip?");
		title.setFont(new Font("Segoe UI", Font.BOLD, 18));
		title.setForeground(NAVY);
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;
		card.add(title, gc);

		JPanel divider = new JPanel();
		divider.setBackground(new Color(180, 200, 210));
		divider.setPreferredSize(new Dimension(1, 1));
		divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		gc.gridy = 1;
		gc.insets = new Insets(0, 0, 16, 0);
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weighty = 0;
		card.add(divider, gc);
		gc.insets = new Insets(8, 0, 8, 0);

		JLabel rateLabel = new JLabel("Rate from 1 - 5:");
		rateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		rateLabel.setForeground(new Color(60, 80, 100));
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.weightx = 0.5;
		card.add(rateLabel, gc);

		rateSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
		rateSpinner.setFont(new Font("Segoe UI", Font.BOLD, 13));
		rateSpinner.setPreferredSize(new Dimension(80, 30));
		JFormattedTextField rtf = ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField();
		rtf.setHorizontalAlignment(JTextField.LEFT);
		gc.gridx = 1;
		gc.weightx = 0.5;
		card.add(rateSpinner, gc);

		JLabel expLabel = new JLabel("Share your experience:");
		expLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		expLabel.setForeground(new Color(60, 80, 100));
		gc.gridx = 0;
		gc.gridy = 3;
		gc.gridwidth = 2;
		gc.weightx = 1;
		gc.insets = new Insets(12, 0, 4, 0);
		card.add(expLabel, gc);

		expArea = new JTextArea(5, 20);
		expArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		expArea.setForeground(NAVY);
		expArea.setLineWrap(true);
		expArea.setWrapStyleWord(true);
		expArea.setBorder(new EmptyBorder(8, 10, 8, 10));

		JScrollPane expScroll = new JScrollPane(expArea);
		expScroll.setBorder(BorderFactory.createLineBorder(BORDER));
		expScroll.setPreferredSize(new Dimension(200, 100));
		gc.gridy = 4;
		gc.insets = new Insets(0, 0, 8, 0);
		gc.fill = GridBagConstraints.BOTH;
		gc.weighty = 1;
		card.add(expScroll, gc);

		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
		btnRow.setOpaque(false);

		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setBackground(Color.WHITE);
		cancelBtn.setForeground(TEAL);
		cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
		cancelBtn.setBorder(BorderFactory.createLineBorder(TEAL, 1));
		cancelBtn.setFocusPainted(false);
		cancelBtn.setPreferredSize(new Dimension(100, 34));
		cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

		JButton submitBtn = new JButton("Submit");
		submitBtn.setBackground(TEAL);
		submitBtn.setForeground(Color.WHITE);
		submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
		submitBtn.setBorderPainted(false);
		submitBtn.setFocusPainted(false);
		submitBtn.setPreferredSize(new Dimension(100, 34));
		submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

		cancelBtn.addActionListener(e -> outerChoice.show(outerPanel, "trips"));

		submitBtn.addActionListener(e -> {
			int rating = (int) rateSpinner.getValue();
			String feedback = expArea.getText().trim();
			String result = insertRating(currentRateTripId, rating, feedback);
			switch (result) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(this, "Thank you for your feedback!", "Rating Submitted",
						JOptionPane.INFORMATION_MESSAGE);
				outerChoice.show(outerPanel, "trips");
				break;
			case "DUPLICATE":
				JOptionPane.showMessageDialog(this, "This trip has already been rated.", "Duplicate Rating",
						JOptionPane.WARNING_MESSAGE);
				break;
			default:
				JOptionPane.showMessageDialog(this, "Failed to submit rating. Please try again.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});

		btnRow.add(cancelBtn);
		btnRow.add(submitBtn);
		gc.gridy = 5;
		gc.insets = new Insets(16, 0, 8, 0);
		card.add(btnRow, gc);

		wrapper.add(card, new GridBagConstraints());
		return wrapper;
	}

	private JTable createStyledTable(DefaultTableModel model) {
		JTable table = new JTable(model) {
			@Override
			public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
				Component c = super.prepareRenderer(r, row, col);
				if (!isRowSelected(row)) {
					c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
					c.setForeground(NAVY);
				}
				return c;
			}
		};
		table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		table.setRowHeight(28);
		table.setShowVerticalLines(false);
		table.setGridColor(BORDER);
		table.setSelectionBackground(new Color(0, 150, 199, 50));
		table.setSelectionForeground(NAVY);
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JTableHeader header = table.getTableHeader();
		header.setFont(new Font("Segoe UI", Font.BOLD, 13));
		header.setBackground(TEAL);
		header.setForeground(Color.WHITE);
		header.setPreferredSize(new Dimension(header.getWidth(), 34));
		((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
		table.getTableHeader().setResizingAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);

		return table;
	}

	private JLabel createEmptyLabel(String text) {
		JLabel label = new JLabel(text, JLabel.CENTER);
		label.setForeground(new Color(150, 170, 185));
		label.setFont(new Font("Segoe UI", Font.ITALIC, 13));
		label.setBorder(new EmptyBorder(8, 0, 8, 0));
		return label;
	}

	private JPanel buildTopBar(JLabel emptyLabel, JButton refreshBtn) {
		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(Color.WHITE);
		top.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
		right.setBackground(Color.WHITE);
		right.add(refreshBtn);
		top.add(emptyLabel, BorderLayout.CENTER);
		top.add(right, BorderLayout.EAST);
		return top;
	}

	private JPanel buildBottomBar(JButton... btns) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
		panel.setBackground(Color.WHITE);
		panel.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
		for (JButton b : btns)
			panel.add(b);
		return panel;
	}

	private JButton createTabButton(String text) {
		JButton b = new JButton(text);
		b.setFont(new Font("Segoe UI", Font.BOLD, 16));
		b.setForeground(new Color(255, 255, 255, 160));
		b.setBackground(new Color(0, 150, 199));
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		b.setOpaque(false);
		b.setContentAreaFilled(false);
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		b.setMargin(new Insets(10, 22, 10, 22));
		return b;
	}

	private void setTabActive(JButton b) {
		b.setForeground(Color.WHITE);
		b.setContentAreaFilled(false);
		b.setOpaque(false);
		b.setBorderPainted(true);
		b.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.WHITE));
	}

	private void setTabInactive(JButton b) {
		b.setForeground(new Color(200, 230, 240));
		b.setContentAreaFilled(false);
		b.setOpaque(false);
		b.setBorderPainted(false);
		b.setBorder(null);
	}

	private JButton createSolidButton(String text) {
		JButton b = new JButton(text);
		b.setBackground(TEAL);
		b.setForeground(Color.WHITE);
		b.setFont(new Font("Segoe UI", Font.BOLD, 12));
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return b;
	}

	public void resetToPending() {
		pendingM.setRowCount(0);
		loadTrips(pendingM, "Pending");
		JButton[] btns = { pendingB, approvedB, completedB, cancelledB };
		for (JButton b : btns)
			setTabInactive(b);
		setTabActive(pendingB);
		tabChoice.show(tabPanel, "Pending");
		outerChoice.show(outerPanel, "trips");
	}

	private void loadTrips(DefaultTableModel model, String status) {
		try {
			String sql = "SELECT * FROM Trip WHERE trip_status = ? AND passenger_id = ?";
			java.sql.PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, status);
			ps.setInt(2, passengerId);
			java.sql.ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Object[] row = {
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
				};
				model.addRow(row);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateTripStatus(int tripId, String status) {
		try {
			String sql = "UPDATE Trip SET trip_status = ? WHERE trip_id = ?";
			java.sql.PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, status);
			ps.setInt(2, tripId);
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// CHANGE: When a trip is Completed or Cancelled, restore driver and vehicle to Available
	private void restoreAssignmentResourcesAvailable(int tripId) {
		try {
			java.sql.PreparedStatement psGet = conn.prepareStatement(
				"SELECT assignment_id FROM Trip WHERE trip_id = ?");
			psGet.setInt(1, tripId);
			java.sql.ResultSet rs = psGet.executeQuery();
			if (rs.next()) {
				Object assignObj = rs.getObject("assignment_id");
				if (assignObj == null) return;
				int assignmentId = (int) assignObj;

				java.sql.PreparedStatement psIds = conn.prepareStatement(
					"SELECT driver_id, vehicle_id FROM Vehicle_Assignment WHERE assignment_id = ?");
				psIds.setInt(1, assignmentId);
				java.sql.ResultSet rsIds = psIds.executeQuery();
				if (rsIds.next()) {
					int driverId = rsIds.getInt("driver_id");
					int vehicleId = rsIds.getInt("vehicle_id");

					// CHANGE: Set driver back to Available
					java.sql.PreparedStatement psD = conn.prepareStatement(
						"UPDATE Driver SET driver_status = 'Available' WHERE driver_id = ?");
					psD.setInt(1, driverId);
					psD.executeUpdate();

					// CHANGE: Set vehicle back to Available
					java.sql.PreparedStatement psV = conn.prepareStatement(
						"UPDATE Vehicle SET vehicle_status = 'Available' WHERE vehicle_id = ?");
					psV.setInt(1, vehicleId);
					psV.executeUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void moveRow(JTable table, DefaultTableModel source, DefaultTableModel target, String newStatus) {
		int selectedRow = table.getSelectedRow();
		if (selectedRow == -1) {
			JOptionPane.showMessageDialog(null, "Please select a trip first.");
			return;
		}

		String message;
		if (newStatus.equals("Cancelled")) {
			message = "Are you sure you want to cancel this trip?";
		} else if (newStatus.equals("Completed")) {
			message = "Are you sure this trip is completed?";
		} else {
			message = "Are you sure you want to set this trip as " + newStatus + "?";
		}

		int confirm = JOptionPane.showConfirmDialog(null, message, "Confirm", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION)
			return;

		Object[] rowData = new Object[source.getColumnCount()];
		for (int i = 0; i < source.getColumnCount(); i++) {
			rowData[i] = source.getValueAt(selectedRow, i);
		}
		rowData[source.getColumnCount() - 1] = newStatus;

		int tripId = (int) source.getValueAt(selectedRow, 0);
		updateTripStatus(tripId, newStatus);

		// CHANGE: Restore driver and vehicle availability when trip is Completed or Cancelled
		if (newStatus.equals("Completed") || newStatus.equals("Cancelled")) {
			restoreAssignmentResourcesAvailable(tripId);
		}

		target.addRow(rowData);
		source.removeRow(selectedRow);

		switch (newStatus) {
		case "Cancelled":
			cancelledB.doClick();
			break;
		case "Completed":
			completedB.doClick();
			break;
		case "Approved":
			approvedB.doClick();
			break;
		}
	}

	private String insertRating(int tripId, int rating, String feedback) {
		try {
			String checkSql = "SELECT COUNT(*) FROM trip_rating WHERE trip_id = ?";
			java.sql.PreparedStatement checkPs = conn.prepareStatement(checkSql);
			checkPs.setInt(1, tripId);
			java.sql.ResultSet rs = checkPs.executeQuery();
			if (rs.next() && rs.getInt(1) > 0) {
				return "DUPLICATE";
			}
			String sql = "INSERT INTO trip_rating (trip_id, rating_value, feedback) VALUES (?, ?, ?)";
			java.sql.PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, tripId);
			ps.setInt(2, rating);
			ps.setString(3, feedback);
			return ps.executeUpdate() > 0 ? "SUCCESS" : "FAILED";
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR";
		}
	}
}