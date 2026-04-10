package com.passenger.panel;



import java.awt.BorderLayout;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.project.customSwing.GradientPanel;
import com.project.dbConnection.DbConnectMsSql;

public class profile extends JPanel {

	private Connection conn;

	private static final Color BLUE = new Color(0, 150, 199);
	private static final Color RED = new Color(220, 53, 69);
	private static final Color WHITE = Color.WHITE;
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
	private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);


	private JTextField txtFirstName = styledField();
	private JTextField txtMiddleName = styledField();
	private JTextField txtLastName = styledField();
	private JTextField txtAddress = styledField();
	private JTextField txtUsername = styledField();
	private JTextField txtEmail = styledField();
	private JTextField txtPassword = styledField();


	private String snapFirstName, snapMiddleName, snapLastName;
	private String snapAddress, snapUsername, snapEmail, snapPassword;
	private List<String> snapContacts = new ArrayList<>();


	private JPanel contactsPanel;
	private List<JTextField> contactFields = new ArrayList<>();

	private JLabel lblStatus = new JLabel(" ");
	private int currentUserId = -1;

	public profile(int userId) {
		DbConnectMsSql db = new DbConnectMsSql();
		conn = db.conn;
		setLayout(new GridBagLayout());
		setBackground(WHITE);
		buildUI();

		loadProfile(userId);
	}

	private void buildUI() {
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true), new EmptyBorder(25, 40, 25, 40)));
		
		// header
		JPanel header = new GradientPanel();
		header.setLayout(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

		JLabel profile = new JLabel("My Profile");
		profile.setFont(new Font("Segoe UI", Font.BOLD, 26));
		profile.setForeground(Color.WHITE);

		header.add(profile, BorderLayout.CENTER);

		GridBagConstraints gbcMain = new GridBagConstraints();
		gbcMain.gridx = 0;
		gbcMain.gridy = 0;
		gbcMain.weightx = 1;
		gbcMain.fill = GridBagConstraints.HORIZONTAL;
		gbcMain.anchor = GridBagConstraints.NORTH;

		add(header, gbcMain);

		// Title
		JLabel title = new JLabel("Personal Information", SwingConstants.CENTER);
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
		addFormRow(form, gbc, "First Name:", txtFirstName, y++);
		addFormRow(form, gbc, "Middle Name:", txtMiddleName, y++);
		addFormRow(form, gbc, "Last Name:", txtLastName, y++);
		addFormRow(form, gbc, "Address:", txtAddress, y++);
		addFormRow(form, gbc, "Username:", txtUsername, y++);
		addFormRow(form, gbc, "Email:", txtEmail, y++);
		addFormRow(form, gbc, "Password:", txtPassword, y++);

		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets = new Insets(14, 10, 4, 10);
		JLabel contactTitle = new JLabel("Contact Numbers");
		contactTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
		contactTitle.setForeground(BLUE);
		form.add(contactTitle, gbc);
		gbc.gridwidth = 1;
		gbc.insets = new Insets(6, 10, 6, 10);
		y++;

		contactsPanel = new JPanel();
		contactsPanel.setLayout(new BoxLayout(contactsPanel, BoxLayout.Y_AXIS));
		contactsPanel.setBackground(WHITE);
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		form.add(contactsPanel, gbc);
		y++;


		JButton btnAddContact = new JButton("+ Add Contact Number");
		btnAddContact.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		btnAddContact.setForeground(BLUE);
		btnAddContact.setBackground(WHITE);
		btnAddContact.setBorder(BorderFactory.createLineBorder(BLUE));
		btnAddContact.setFocusPainted(false);
		btnAddContact.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnAddContact.setAlignmentX(Component.LEFT_ALIGNMENT);
		btnAddContact.addActionListener(e -> addContactRow(""));
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.insets = new Insets(4, 10, 10, 10);
		form.add(btnAddContact, gbc);

		card.add(form);
		card.add(Box.createVerticalStrut(20));


		JButton btnSave = new JButton("Save");
		JButton btnCancel = new JButton("Cancel");
		styleButtonFilled(btnSave, BLUE);
		styleButtonOutline(btnCancel, RED);
		btnSave.setPreferredSize(new Dimension(100, 35));
		btnCancel.setPreferredSize(new Dimension(100, 35));

		JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
		btnRow.setBackground(WHITE);
		btnRow.add(btnSave);
		btnRow.add(btnCancel);
		card.add(btnRow);
		card.add(Box.createVerticalStrut(10));


		lblStatus.setFont(LABEL_FONT);
		lblStatus.setForeground(new Color(0, 150, 80));
		lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
		card.add(lblStatus);


		JScrollPane scroll = new JScrollPane(card, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(WHITE);
		scroll.setBackground(WHITE);
		scroll.setPreferredSize(new Dimension(620, 600));

		GridBagConstraints gbcScroll = new GridBagConstraints();
		gbcScroll.gridx = 0;
		gbcScroll.gridy = 1; 
		gbcScroll.weightx = 1;
		gbcScroll.weighty = 1; 
		gbcScroll.anchor = GridBagConstraints.NORTH;
		gbcScroll.fill = GridBagConstraints.NONE;
		gbcScroll.anchor = GridBagConstraints.CENTER;

		add(scroll, gbcScroll);

		btnSave.addActionListener(e -> saveProfile());
		btnCancel.addActionListener(e -> confirmCancel());
	}

	private void confirmCancel() {
		Object[] options = { "Yes, discard changes", "Keep Editing" };
		int choice = JOptionPane.showOptionDialog(this,
				"Do you want to cancel your changes?\nAll unsaved edits will be lost.", "Cancel Changes",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1] 
		);

		if (choice == 0) { // "Yes, discard changes"
			restoreSnapshot();
			lblStatus.setText(" ");
		}
		// choice == 1 ("Keep Editing") or dialog closed, do nothing
	}

	private void takeSnapshot() {
		snapFirstName = txtFirstName.getText();
		snapMiddleName = txtMiddleName.getText();
		snapLastName = txtLastName.getText();
		snapAddress = txtAddress.getText();
		snapUsername = txtUsername.getText();
		snapEmail = txtEmail.getText();
		snapPassword = txtPassword.getText();

		snapContacts.clear();
		for (JTextField f : contactFields) {
			snapContacts.add(f.getText());
		}
	}

	private void restoreSnapshot() {
		txtFirstName.setText(snapFirstName);
		txtMiddleName.setText(snapMiddleName);
		txtLastName.setText(snapLastName);
		txtAddress.setText(snapAddress);
		txtUsername.setText(snapUsername);
		txtEmail.setText(snapEmail);
		txtPassword.setText(snapPassword);

		contactFields.clear();
		contactsPanel.removeAll();
		for (String num : snapContacts) {
			addContactRow(num);
		}
		contactsPanel.revalidate();
		contactsPanel.repaint();
	}

	private void addContactRow(String value) {
		JTextField field = styledField();
		field.setText(value);

		JButton btnRemove = new JButton("X");
		btnRemove.setForeground(RED);
		btnRemove.setBackground(WHITE);
		btnRemove.setBorder(BorderFactory.createLineBorder(RED));
		btnRemove.setFocusPainted(false);
		btnRemove.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnRemove.setFont(new Font("Segoe UI", Font.BOLD, 11));
		btnRemove.setPreferredSize(new Dimension(38, 32));
		btnRemove.setMaximumSize(new Dimension(38, 32));
		btnRemove.setMinimumSize(new Dimension(38, 32));

		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		row.setBackground(WHITE);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.add(field);
		row.add(btnRemove);

		contactFields.add(field);
		contactsPanel.add(row);

		btnRemove.addActionListener(e -> {
			contactFields.remove(field);
			contactsPanel.remove(row);
			contactsPanel.revalidate();
			contactsPanel.repaint();
		});

		contactsPanel.revalidate();
		contactsPanel.repaint();
	}

	public void loadProfile(int userId) {
		this.currentUserId = userId;
		lblStatus.setText(" ");

		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM Users WHERE user_id = ?");
			ps.setInt(1, userId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				txtFirstName.setText(nvl(rs.getString("first_name")));
				txtMiddleName.setText(nvl(rs.getString("middle_name")));
				txtLastName.setText(nvl(rs.getString("last_name")));
				txtAddress.setText(nvl(rs.getString("address")));
				txtUsername.setText(nvl(rs.getString("username")));
				txtEmail.setText(nvl(rs.getString("email_address")));
				txtPassword.setText(nvl(rs.getString("password")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getCurrentUserId() {
		return currentUserId;
	}

	private void saveProfile() {
		if (currentUserId == -1) {
			lblStatus.setForeground(RED);
			lblStatus.setText("No user loaded!");
			return;
		}

		try {
			PreparedStatement psU = conn
					.prepareStatement("UPDATE Users SET first_name=?, middle_name=?, last_name=?, address=?, "
							+ "username=?, email_address=?, password=? WHERE user_id=?");
			psU.setString(1, txtFirstName.getText().trim());
			psU.setString(2, txtMiddleName.getText().trim());
			psU.setString(3, txtLastName.getText().trim());
			psU.setString(4, txtAddress.getText().trim());
			psU.setString(5, txtUsername.getText().trim());
			psU.setString(6, txtEmail.getText().trim());
			psU.setString(7, txtPassword.getText().trim());
			psU.setInt(8, currentUserId);
			psU.executeUpdate();

			PreparedStatement psDel = conn.prepareStatement("DELETE FROM User_Contact_Number WHERE user_id = ?");
			psDel.setInt(1, currentUserId);
			psDel.executeUpdate();

			PreparedStatement psIns = conn
					.prepareStatement("INSERT INTO User_Contact_Number (user_id, contact_number) VALUES (?, ?)");
			for (JTextField field : contactFields) {
			    String num = field.getText().trim();

			    if (!num.isEmpty()) {

			       
			        if (num.length() != 10) {
			            lblStatus.setForeground(RED);
			            lblStatus.setText("Contact number must be exactly 10 digits!");
			            return;
			        }

			        psIns.setInt(1, currentUserId);
			        psIns.setString(2, num);
			        psIns.addBatch();
			    }
			}
			
			psIns.executeBatch();

			takeSnapshot();

			lblStatus.setForeground(new Color(0, 150, 80));
			lblStatus.setText("Profile updated successfully!");

		} catch (Exception e) {
			lblStatus.setForeground(RED);
			lblStatus.setText("Error saving profile!");
			e.printStackTrace();
		}
	}

	private String nvl(String s) {
		return s != null ? s : "";
	}

	private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, Component field, int y) {
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

	private JTextField styledField() {
		JTextField f = new JTextField(15);
		f.setPreferredSize(new Dimension(260, 32));
		f.setMinimumSize(new Dimension(200, 32));
		f.setFont(LABEL_FONT);
		return f;
	}

	private void styleButtonFilled(JButton btn, Color bg) {
		btn.setBackground(bg);
		btn.setForeground(WHITE);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		btn.setOpaque(true);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
	}

	private void styleButtonOutline(JButton btn, Color borderColor) {
		btn.setBackground(WHITE);
		btn.setForeground(borderColor);
		btn.setFocusPainted(false);
		btn.setBorder(BorderFactory.createLineBorder(borderColor, 2));
		btn.setOpaque(true);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
	}
}
