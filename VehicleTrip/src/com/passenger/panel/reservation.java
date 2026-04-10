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
import java.sql.Connection;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.project.customSwing.GradientPanel;
import com.project.dbConnection.DbConnectMsSql;

public class reservation extends JPanel {

	private static final Color TEAL = new Color(0, 150, 199);
	private static final Color BG = new Color(235, 242, 245);
	private static final Color NAVY = new Color(20, 40, 80);
	private static final Color GRAY_TEXT = new Color(80, 100, 120);
	private static final Color BORDER = new Color(200, 215, 225);
	private static final Color WHITE = Color.WHITE;
	private static final Color RED = new Color(220, 53, 69);
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
	private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

	private CardLayout cardLayout;
	private JPanel mainPanel;
	private trips tripsPanel;
	private int passengerId;
	private Connection conn;

	private JTextField pickup, destination;
	private JSpinner startDate, endDate, startTime, endTime, passengers;
	private JComboBox<String> vehicleBox;

	private java.util.List<Integer> vehicleAssignmentIds = new java.util.ArrayList<>();

	public reservation(java.awt.CardLayout cardLayout, JPanel mainPanel, trips tripsPanel, int passengerId) {
		DbConnectMsSql db = new DbConnectMsSql();
		conn = db.conn;

		this.cardLayout = cardLayout;
		this.mainPanel = mainPanel;
		this.tripsPanel = tripsPanel;
		this.passengerId = passengerId;

		setLayout(new GridBagLayout());
		setBackground(WHITE);

		JPanel header = new GradientPanel();
		header.setLayout(new BorderLayout());
		header.setPreferredSize(new Dimension(0, 76));
		header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

		JLabel headerTitle = new JLabel("Book a Trip");
		headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
		headerTitle.setForeground(WHITE);

		JPanel headerText = new JPanel(new BorderLayout(0, 4));
		headerText.setOpaque(false);
		headerText.add(headerTitle, BorderLayout.NORTH);
		header.add(headerText, BorderLayout.CENTER);

		GridBagConstraints gbcHeader = new GridBagConstraints();
		gbcHeader.gridx = 0;
		gbcHeader.gridy = 0;
		gbcHeader.weightx = 1;
		gbcHeader.fill = GridBagConstraints.HORIZONTAL;
		add(header, gbcHeader);

		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true), new EmptyBorder(25, 40, 25, 40)));

		JLabel title = new JLabel("Trip Reservation", SwingConstants.CENTER);
		title.setFont(TITLE_FONT);
		title.setForeground(new Color(30, 30, 30));
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		card.add(title);
		card.add(Box.createVerticalStrut(20));

		JPanel form = new JPanel(new GridBagLayout());
		form.setBackground(WHITE);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 10, 6, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		int y = 0;

		addSectionLabel(form, gbc, "Trip Schedule", y++);

		startDate = createDateSpinner("dd MMM yyyy");
		endDate = createDateSpinner("dd MMM yyyy");
		addFormRow(form, gbc, "Start Date:", startDate, y++);
		addFormRow(form, gbc, "End Date:", endDate, y++);

		startTime = createDateSpinner("HH:mm");
		endTime = createDateSpinner("HH:mm");
		addFormRow(form, gbc, "Start Time:", startTime, y++);
		addFormRow(form, gbc, "End Time:", endTime, y++);

		addSectionLabel(form, gbc, "Locations", y++);

		pickup = createTextField();
		destination = createTextField();
		addFormRow(form, gbc, "Pickup:", pickup, y++);
		addFormRow(form, gbc, "Destination:", destination, y++);

		addSectionLabel(form, gbc, "Passengers & Vehicle", y++);

		passengers = new JSpinner(new SpinnerNumberModel(1, 1, 54, 1));
		((JSpinner.DefaultEditor) passengers.getEditor()).getTextField().setEditable(false);
		JFormattedTextField tf = ((JSpinner.DefaultEditor) passengers.getEditor()).getTextField();
		tf.setHorizontalAlignment(JTextField.LEFT);

		vehicleBox = new JComboBox<>();
		vehicleBox.setFont(LABEL_FONT);
		loadAvailableAssignments(vehicleBox, 1);

		addFormRow(form, gbc, "No. of Passengers:", passengers, y++);
		addFormRow(form, gbc, "Vehicle Type:", vehicleBox, y++);

		passengers.addChangeListener(e -> {
			int p = (int) passengers.getValue();
			loadAvailableAssignments(vehicleBox, p);
		});

		card.add(form);
		card.add(Box.createVerticalStrut(20));

		JButton cancel = new JButton("Cancel");
		cancel.setBackground(WHITE);
		cancel.setForeground(RED);
		cancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		cancel.setBorder(BorderFactory.createLineBorder(RED, 2));
		cancel.setFocusPainted(false);
		cancel.setOpaque(true);
		cancel.setPreferredSize(new Dimension(110, 35));
		cancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

		JButton submit = new JButton("Book Trip");
		submit.setBackground(TEAL);
		submit.setForeground(WHITE);
		submit.setFont(new Font("Segoe UI", Font.BOLD, 13));
		submit.setBorderPainted(false);
		submit.setFocusPainted(false);
		submit.setOpaque(true);
		submit.setPreferredSize(new Dimension(110, 35));
		submit.setCursor(new Cursor(Cursor.HAND_CURSOR));

		JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
		btnRow.setBackground(WHITE);
		btnRow.add(submit);
		btnRow.add(cancel);
		card.add(btnRow);
		card.add(Box.createVerticalStrut(10));

		JScrollPane scroll = new JScrollPane(card, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG);
		scroll.setBackground(BG);
		scroll.setPreferredSize(new Dimension(620, 600));
		scroll.getVerticalScrollBar().setUnitIncrement(12);

		GridBagConstraints gbcScroll = new GridBagConstraints();
		gbcScroll.gridx = 0;
		gbcScroll.gridy = 1;
		gbcScroll.weightx = 1;
		gbcScroll.weighty = 1;
		gbcScroll.fill = GridBagConstraints.NONE;
		gbcScroll.anchor = GridBagConstraints.CENTER;
		add(scroll, gbcScroll);

		cancel.addActionListener(e -> {
			int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?", "Confirm Cancel",
					JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION)
				resetForm();
		});

		submit.addActionListener(e -> {
			try {
				if (pickup.getText().trim().isEmpty() || destination.getText().trim().isEmpty()) {
					JOptionPane.showMessageDialog(this, "Some fields are empty.", "Submit Reservation",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				Date startD = (Date) startDate.getValue();
				Date endD = (Date) endDate.getValue();
				java.sql.Time startSqlTime = new java.sql.Time(((Date) startTime.getValue()).getTime());
				java.sql.Time endSqlTime = new java.sql.Time(((Date) endTime.getValue()).getTime());
				String pickupLoc = pickup.getText().trim();
				String destLoc = destination.getText().trim();
				int numPassengers = (int) passengers.getValue();

				int selectedIdx = vehicleBox.getSelectedIndex();
				Integer assignmentIdToUse = null;
				if (selectedIdx >= 0 && selectedIdx < vehicleAssignmentIds.size()) {
					int aid = vehicleAssignmentIds.get(selectedIdx);
					if (aid != -1) assignmentIdToUse = aid;
				}

				boolean success = insertTrip(startD, endD, startSqlTime, endSqlTime, pickupLoc, destLoc,
						numPassengers, passengerId, assignmentIdToUse);

				if (success) {
					// CHANGE: After booking, set the driver and vehicle as Not Available
					if (assignmentIdToUse != null) {
						setAssignmentResourcesUnavailable(assignmentIdToUse);
					}

					JOptionPane.showMessageDialog(this,
					        "Reservation submitted! An admin and vehicle will be assigned shortly.",
					        "Booking Submitted",
					        JOptionPane.INFORMATION_MESSAGE);
					resetForm();
					tripsPanel.resetToPending();
					cardLayout.show(mainPanel, "trips");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Something went wrong:\n" + ex.getMessage(), "System Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	// CHANGE: Sets the driver and vehicle linked to an assignment as Not Available when a trip is booked
	private void setAssignmentResourcesUnavailable(int assignmentId) {
		try {
			java.sql.PreparedStatement ps = conn.prepareStatement(
				"SELECT driver_id, vehicle_id FROM Vehicle_Assignment WHERE assignment_id = ?");
			ps.setInt(1, assignmentId);
			java.sql.ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int driverId = rs.getInt("driver_id");
				int vehicleId = rs.getInt("vehicle_id");

				java.sql.PreparedStatement psD = conn.prepareStatement(
					"UPDATE Driver SET driver_status = 'Not Available' WHERE driver_id = ?");
				psD.setInt(1, driverId);
				psD.executeUpdate();

				java.sql.PreparedStatement psV = conn.prepareStatement(
					"UPDATE Vehicle SET vehicle_status = 'Not Available' WHERE vehicle_id = ?");
				psV.setInt(1, vehicleId);
				psV.executeUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addSectionLabel(JPanel panel, GridBagConstraints gbc, String text, int y) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("Segoe UI", Font.BOLD, 13));
		label.setForeground(TEAL);
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets = new Insets(14, 10, 4, 10);
		panel.add(label, gbc);
		gbc.gridwidth = 1;
		gbc.insets = new Insets(6, 10, 6, 10);
	}

	private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field, int y) {
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		JLabel lbl = new JLabel(labelText);
		lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
		panel.add(lbl, gbc);
		gbc.gridx = 1;
		gbc.weightx = 1;
		panel.add(field, gbc);
	}

	private JTextField createTextField() {
		JTextField tf = new JTextField(15);
		tf.setPreferredSize(new Dimension(260, 32));
		tf.setMinimumSize(new Dimension(200, 32));
		tf.setFont(LABEL_FONT);
		return tf;
	}

	private JSpinner createDateSpinner(String format) {
		SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE);
		JSpinner spinner = new JSpinner(model);
		spinner.setEditor(new JSpinner.DateEditor(spinner, format));
		spinner.setPreferredSize(new Dimension(260, 32));
		return spinner;
	}

	public void resetForm() {
		pickup.setText("");
		destination.setText("");
		passengers.setValue(1);
		startDate.setValue(new Date());
		endDate.setValue(new Date());
		startTime.setValue(new Date());
		endTime.setValue(new Date());
		vehicleBox.removeAllItems();
		vehicleAssignmentIds.clear();
		loadAvailableAssignments(vehicleBox, 1);
	}

	private void loadAvailableAssignments(JComboBox<String> vehicleBox, int numPassengers) {
		vehicleBox.removeAllItems();
		vehicleAssignmentIds.clear();

		try {
			String sql =
				"SELECT va.assignment_id, " +
				"du.first_name + ' ' + du.last_name + ' - ' + v.vehicle_model AS label, " +
				"v.vehicle_type, v.passenger_capacity " +
				"FROM Vehicle_Assignment va " +
				"JOIN Driver d  ON va.driver_id  = d.driver_id " +
				"JOIN Users du  ON d.driver_id   = du.user_id " +
				"JOIN Vehicle v ON va.vehicle_id = v.vehicle_id " +
				"WHERE va.assignment_status = 'Active' " +
				"AND v.passenger_capacity >= ? " +
				"AND v.vehicle_status = 'Available'";

			java.sql.PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, numPassengers);
			java.sql.ResultSet rs = ps.executeQuery();

			boolean hasAny = false;
			while (rs.next()) {
				hasAny = true;
				String label = rs.getString("label") + " (Cap: " + rs.getInt("passenger_capacity") + ")";
				vehicleBox.addItem(label);
				vehicleAssignmentIds.add(rs.getInt("assignment_id"));
			}

			if (!hasAny) {
				vehicleBox.addItem("No vehicle available yet — admin will assign");
				vehicleAssignmentIds.add(-1);
			}

		} catch (Exception e) {
			e.printStackTrace();
			vehicleBox.addItem("No vehicle available yet — admin will assign");
			vehicleAssignmentIds.add(-1);
		}
	}

	private boolean insertTrip(Date startD, Date endD, java.sql.Time startT, java.sql.Time endT,
	        String pickupLoc, String destLoc, int numPassengers, int passengerId,
	        Integer assignmentId) {

	    try {
	        String sql = "INSERT INTO Trip "
	                + "(passenger_id, assignment_id, start_date, end_date, start_time, end_time, "
	                + "pick_up_location, destination, passenger_count, trip_status) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	        java.sql.PreparedStatement ps = conn.prepareStatement(sql);

	        ps.setInt(1, passengerId);

	        if (assignmentId != null) {
	            ps.setInt(2, assignmentId);
	        } else {
	            ps.setNull(2, java.sql.Types.INTEGER);
	        }

	        ps.setDate(3, new java.sql.Date(startD.getTime()));
	        ps.setDate(4, new java.sql.Date(endD.getTime()));
	        ps.setTime(5, startT);
	        ps.setTime(6, endT);
	        ps.setString(7, pickupLoc);
	        ps.setString(8, destLoc);
	        ps.setInt(9, numPassengers);
	        ps.setString(10, "Pending");

	        return ps.executeUpdate() > 0;

	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
}