package com.admin.panel;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class UserPanel extends JPanel {
    private CardLayout cardLayout;
    private JPanel container;
    private DeleteUserPanel deletePanel;
    private UpdateUserPanel updatePanel;
    private ViewUserPanel viewPanel;
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbFilter;
    private JComboBox<String> cmbStatusFilter;
    private Connection conn;

    private static final Color BLUE       = new Color(0, 150, 199);
    private static final Color RED        = new Color(220, 53, 69);
    private static final Color GREEN      = new Color(39, 174, 96);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public UserPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        cardLayout = new CardLayout();
        container  = new JPanel(cardLayout);
        container.setBackground(WHITE);
        container.add(userListPanel(), "USER_LIST");
        container.add(new AddUserPanel(), "ADD_USER");
        deletePanel = new DeleteUserPanel();
        container.add(deletePanel, "DELETE_USER");
        updatePanel = new UpdateUserPanel();
        container.add(updatePanel, "UPDATE_USER");
        viewPanel = new ViewUserPanel();
        container.add(viewPanel, "VIEW_USER");
        setLayout(new BorderLayout());
        setBackground(WHITE);
        add(container, BorderLayout.CENTER);
        loadUsers("All", "All");
    }


    private void cascadeDriverNotAvailable(int driverId) throws Exception {
        PreparedStatement psFind = conn.prepareStatement(
            "SELECT assignment_id FROM Vehicle_Assignment " +
            "WHERE driver_id=? AND assignment_status='Active'");
        psFind.setInt(1, driverId);
        ResultSet rs = psFind.executeQuery();
        java.util.List<Integer> assignIds = new java.util.ArrayList<>();
        while (rs.next()) assignIds.add(rs.getInt("assignment_id"));

        for (int aId : assignIds) {
            PreparedStatement psUpd = conn.prepareStatement(
                "UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id=?");
            psUpd.setInt(1, aId);
            psUpd.executeUpdate();
            cascadeAssignmentInactive(aId);
        }
    }

    private void cascadeAssignmentInactive(int assignmentId) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Trip SET trip_status='Pending' " +
            "WHERE assignment_id=? AND trip_status='Approved'");
        ps.setInt(1, assignmentId);
        ps.executeUpdate();
    }

    private JPanel userListPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(WHITE);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setBackground(WHITE);

        JLabel lblFilter = new JLabel("Filter by Role:");
        lblFilter.setFont(LABEL_FONT);
        cmbFilter = new JComboBox<>(new String[]{"All", "Admin", "Driver", "Passenger"});
        cmbFilter.setPreferredSize(new Dimension(130, 30));
        cmbFilter.addActionListener(e -> loadUsers(
                cmbFilter.getSelectedItem().toString(),
                cmbStatusFilter.getSelectedItem().toString()));

        JLabel lblStatusFilter = new JLabel("Account Status:");
        lblStatusFilter.setFont(LABEL_FONT);
        cmbStatusFilter = new JComboBox<>(new String[]{"All", "Active", "Inactive"});
        cmbStatusFilter.setPreferredSize(new Dimension(110, 30));
        cmbStatusFilter.addActionListener(e -> loadUsers(
                cmbFilter.getSelectedItem().toString(),
                cmbStatusFilter.getSelectedItem().toString()));

        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(lblStatusFilter);
        filterPanel.add(cmbStatusFilter);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> {
            cmbFilter.setSelectedItem("All");
            cmbStatusFilter.setSelectedItem("All");
            loadUsers("All", "All");
        });
        refreshPanel.add(btnRefresh);

        topPanel.add(filterPanel,  BorderLayout.WEST);
        topPanel.add(refreshPanel, BorderLayout.EAST);

        String[] columns = {"User ID", "First Name", "Middle Name", "Last Name",
                "Username", "Email", "Address", "Role", "Account Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        TableStyleUtil.applyStyle(table);

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.LEFT);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
                }
                if (col == 8 && val != null) {
                    String status = val.toString();
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground("Active".equalsIgnoreCase(status)
                            ? new Color(39, 174, 96) : new Color(220, 53, 69));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);

        JScrollPane scrollPane = TableStyleUtil.modernScroll(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bottom.setBackground(WHITE);
        JButton btnView   = new JButton("View");
        JButton btnAdd    = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Deactivate");
        styleButtonFilled(btnView,   BLUE);
        styleButtonFilled(btnAdd,    BLUE);
        styleButtonFilled(btnUpdate, BLUE);
        styleButtonFilled(btnDelete, RED);

        btnAdd.addActionListener(e -> cardLayout.show(container, "ADD_USER"));

        btnView.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a user first!"); return; }
            viewPanel.loadUser((int) model.getValueAt(row, 0), (String) model.getValueAt(row, 7));
            cardLayout.show(container, "VIEW_USER");
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a user first!"); return; }
            deletePanel.setUserDetails(
                (int)    model.getValueAt(row, 0),
                (String) model.getValueAt(row, 1),
                (String) model.getValueAt(row, 2),
                (String) model.getValueAt(row, 3),
                (String) model.getValueAt(row, 4),
                (String) model.getValueAt(row, 5),
                (String) model.getValueAt(row, 6),
                (String) model.getValueAt(row, 7),
                (String) model.getValueAt(row, 8));
            cardLayout.show(container, "DELETE_USER");
        });

        btnUpdate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a user first!"); return; }
            int id = (int) model.getValueAt(row, 0);
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM Users WHERE user_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    updatePanel.setUserData(
                        rs.getInt("user_id"),       rs.getString("first_name"),
                        rs.getString("middle_name"), rs.getString("last_name"),
                        rs.getString("username"),    rs.getString("email_address"),
                        rs.getString("address"),     rs.getString("password"),
                        rs.getString("user_role"),   rs.getString("user_status"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            cardLayout.show(container, "UPDATE_USER");
        });

        bottom.add(btnView);
        bottom.add(btnAdd);
        bottom.add(btnUpdate);
        bottom.add(btnDelete);

        mainPanel.add(topPanel,   BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottom,     BorderLayout.SOUTH);
        return mainPanel;
    }

    private void loadUsers(String role, String status) {
        try {
            model.setRowCount(0);

            boolean filterRole   = !role.equals("All");
            boolean filterStatus = !status.equals("All");

            StringBuilder sql = new StringBuilder("SELECT * FROM Users WHERE 1=1");
            if (filterRole)   sql.append(" AND user_role=?");
            if (filterStatus) sql.append(" AND user_status=?");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (filterRole)   ps.setString(idx++, role);
            if (filterStatus) ps.setString(idx,   status);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("user_id"),        rs.getString("first_name"),
                    rs.getString("middle_name"), rs.getString("last_name"),
                    rs.getString("username"),    rs.getString("email_address"),
                    rs.getString("address"),     rs.getString("user_role"),
                    rs.getString("user_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public class ViewUserPanel extends JPanel {
        private JLabel lblUserId     = valueLabel();
        private JLabel lblFirstName  = valueLabel();
        private JLabel lblMiddleName = valueLabel();
        private JLabel lblLastName   = valueLabel();
        private JLabel lblUsername   = valueLabel();
        private JLabel lblEmail      = valueLabel();
        private JLabel lblAddress    = valueLabel();
        private JLabel lblRole       = valueLabel();
        private JLabel lblAccStatus  = valueLabel();
        private JLabel lblLicense    = valueLabel();
        private JLabel lblDrvStatus  = valueLabel();
        private JLabel lblLicenseKey = new JLabel("License No:");
        private JLabel lblDrvStatKey = new JLabel("Driver Status:");
        private JPanel contactsPanel = new JPanel();

        public ViewUserPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            buildUI();
        }

        private void buildUI() {
            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            JLabel title = new JLabel("User Details", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(BLUE);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 18, 10);
            card.add(title, gbc);
            gbc.gridwidth = 1;
            gbc.insets    = new Insets(6, 10, 6, 10);

            int y = 1;
            addInfoRow(card, gbc, "User ID:",        lblUserId,     y++);
            addInfoRow(card, gbc, "First Name:",     lblFirstName,  y++);
            addInfoRow(card, gbc, "Middle Name:",    lblMiddleName, y++);
            addInfoRow(card, gbc, "Last Name:",      lblLastName,   y++);
            addInfoRow(card, gbc, "Username:",       lblUsername,   y++);
            addInfoRow(card, gbc, "Email:",          lblEmail,      y++);
            addInfoRow(card, gbc, "Address:",        lblAddress,    y++);
            addInfoRow(card, gbc, "Role:",           lblRole,       y++);
            addInfoRow(card, gbc, "Account Status:", lblAccStatus,  y++);

            lblLicenseKey.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblLicenseKey.setVisible(false);
            lblLicense.setVisible(false);
            lblDrvStatKey.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblDrvStatKey.setVisible(false);
            lblDrvStatus.setVisible(false);

            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
            card.add(lblLicenseKey, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            card.add(lblLicense, gbc);
            y++;
            gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0;
            card.add(lblDrvStatKey, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            card.add(lblDrvStatus, gbc);
            y++;

            JLabel lblContactTitle = new JLabel("Contact Numbers:");
            lblContactTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2; gbc.weightx = 1;
            gbc.insets = new Insets(14, 10, 4, 10);
            card.add(lblContactTitle, gbc);
            y++;

            contactsPanel.setBackground(WHITE);
            contactsPanel.setLayout(new BoxLayout(contactsPanel, BoxLayout.Y_AXIS));
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 6, 10);
            card.add(contactsPanel, gbc);
            y++;

            JButton btnBack = new JButton("Back");
            styleButtonOutline(btnBack, BLUE);
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnRow.setBackground(WHITE);
            btnRow.add(btnBack);
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(18, 10, 0, 10);
            card.add(btnRow, gbc);
            add(card);

            btnBack.addActionListener(e -> {
                cardLayout.show(container, "USER_LIST");
                loadUsers("All", "All");
            });
        }

        public void loadUser(int userId, String role) {
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM Users WHERE user_id=?");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    lblUserId.setText(String.valueOf(userId));
                    lblFirstName.setText(rs.getString("first_name"));
                    lblMiddleName.setText(rs.getString("middle_name") != null ? rs.getString("middle_name") : "—");
                    lblLastName.setText(rs.getString("last_name"));
                    lblUsername.setText(rs.getString("username"));
                    lblEmail.setText(rs.getString("email_address"));
                    lblAddress.setText(rs.getString("address") != null ? rs.getString("address") : "—");
                    lblRole.setText(role);
                    String status = rs.getString("user_status");
                    lblAccStatus.setText(status);
                    lblAccStatus.setForeground("Active".equalsIgnoreCase(status) ? GREEN : RED);
                }
                if ("Driver".equalsIgnoreCase(role)) {
                    PreparedStatement psD = conn.prepareStatement(
                        "SELECT license_number, driver_status FROM Driver WHERE driver_id=?");
                    psD.setInt(1, userId);
                    ResultSet rsD = psD.executeQuery();
                    if (rsD.next()) {
                        lblLicense.setText(rsD.getString("license_number") != null
                                ? rsD.getString("license_number") : "—");
                        String ds = rsD.getString("driver_status");
                        lblDrvStatus.setText(ds);
                        lblDrvStatus.setForeground("Available".equalsIgnoreCase(ds) ? GREEN : RED);
                    }
                    lblLicenseKey.setVisible(true);
                    lblLicense.setVisible(true);
                    lblDrvStatKey.setVisible(true);
                    lblDrvStatus.setVisible(true);
                } else {
                    lblLicenseKey.setVisible(false);
                    lblLicense.setVisible(false);
                    lblDrvStatKey.setVisible(false);
                    lblDrvStatus.setVisible(false);
                }
                contactsPanel.removeAll();
                PreparedStatement psC = conn.prepareStatement(
                    "SELECT contact_number FROM User_Contact_Number WHERE user_id=?");
                psC.setInt(1, userId);
                ResultSet rsC = psC.executeQuery();
                boolean has = false;
                while (rsC.next()) {
                    has = true;
                    JLabel lbl = new JLabel("• " + rsC.getString("contact_number"));
                    lbl.setFont(LABEL_FONT);
                    lbl.setForeground(new Color(50, 50, 50));
                    contactsPanel.add(lbl);
                }
                if (!has) {
                    JLabel lbl = new JLabel("No contact numbers on record.");
                    lbl.setFont(LABEL_FONT);
                    lbl.setForeground(Color.GRAY);
                    contactsPanel.add(lbl);
                }
                contactsPanel.revalidate();
                contactsPanel.repaint();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public class AddUserPanel extends JPanel {
        private JTextField     txtFirstName, txtMiddleName, txtLastName;
        private JTextField     txtUsername, txtEmail, txtAddress;
        private JPasswordField pwdPassword;
        private JTextField     txtLicenseNumber;
        private JComboBox<String> cmbRole;
        private JLabel lblStatus, lblLicense;

        public AddUserPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            initUI();
        }

        private void initUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Add User", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            txtFirstName     = styledField();
            txtMiddleName    = styledField();
            txtLastName      = styledField();
            txtAddress       = styledField();
            txtEmail         = styledField();
            txtUsername      = styledField();
            pwdPassword      = styledPasswordField();
            txtLicenseNumber = styledField();
            cmbRole = new JComboBox<>(new String[]{"Admin", "Driver", "Passenger"});
            cmbRole.setPreferredSize(new Dimension(260, 32));

            int y = 0;
            addFormRow(form, gbc, "First Name *:", txtFirstName,  y++);
            addFormRow(form, gbc, "Middle Name:",  txtMiddleName, y++);
            addFormRow(form, gbc, "Last Name *:",  txtLastName,   y++);
            addFormRow(form, gbc, "Address:",      txtAddress,    y++);
            addFormRow(form, gbc, "Email *:",      txtEmail,      y++);
            addFormRow(form, gbc, "Username *:",   txtUsername,   y++);
            addFormRow(form, gbc, "Password *:",   pwdPassword,   y++);
            addFormRow(form, gbc, "Role *:",       cmbRole,       y++);

            lblLicense = new JLabel("License No:");
            lblLicense.setFont(LABEL_FONT);
            lblLicense.setVisible(false);
            txtLicenseNumber.setVisible(false);
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
            form.add(lblLicense, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            form.add(txtLicenseNumber, gbc);

            cmbRole.addActionListener(e -> {
                boolean isDriver = "Driver".equals(cmbRole.getSelectedItem());
                lblLicense.setVisible(isDriver);
                txtLicenseNumber.setVisible(isDriver);
                form.revalidate();
                form.repaint();
            });

            card.add(form);
            card.add(Box.createVerticalStrut(20));

            JButton btnAdd  = new JButton("Add");
            JButton btnBack = new JButton("Back");
            styleButtonFilled(btnAdd,   BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnAdd.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnAdd);
            btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus = new JLabel(" ");
            lblStatus.setFont(LABEL_FONT);
            lblStatus.setForeground(new Color(0, 150, 80));
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnAdd.addActionListener(e -> saveUser());
            btnBack.addActionListener(e -> {
                clearFields();
                lblStatus.setText(" ");
                cardLayout.show(container, "USER_LIST");
                loadUsers("All", "All");
            });
        }

        private void saveUser() {
            String role      = cmbRole.getSelectedItem().toString();
            String password  = new String(pwdPassword.getPassword());
            String firstName = txtFirstName.getText().trim();
            String lastName  = txtLastName.getText().trim();
            String username  = txtUsername.getText().trim();
            String email     = txtEmail.getText().trim();

            if (firstName.isEmpty()) { showError("First name is required!"); return; }
            if (lastName.isEmpty())  { showError("Last name is required!");  return; }
            if (username.isEmpty())  { showError("Username is required!");   return; }
            if (password.isEmpty())  { showError("Password is required!");   return; }
            if (password.length() < 8) { showError("Password must be at least 8 characters!"); return; }
            if (email.isEmpty())     { showError("Email is required!");      return; }
            if (!EMAIL_PATTERN.matcher(email).matches()) { showError("Invalid email format!"); return; }

            // VALIDATION: license number must be unique if driver role is selected
            if ("Driver".equals(role)) {
                String license = txtLicenseNumber.getText().trim();
                if (!license.isEmpty()) {
                    try {
                        PreparedStatement psChkL = conn.prepareStatement(
                            "SELECT COUNT(*) FROM Driver WHERE license_number=?");
                        psChkL.setString(1, license);
                        ResultSet rsL = psChkL.executeQuery();
                        if (rsL.next() && rsL.getInt(1) > 0) {
                            showError("License number already exists!");
                            return;
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }

            try {
                PreparedStatement psChkU = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Users WHERE username=?");
                psChkU.setString(1, username);
                ResultSet rsU = psChkU.executeQuery();
                if (rsU.next() && rsU.getInt(1) > 0) { showError("Username already exists!"); return; }

                PreparedStatement psChkE = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Users WHERE email_address=?");
                psChkE.setString(1, email);
                ResultSet rsE = psChkE.executeQuery();
                if (rsE.next() && rsE.getInt(1) > 0) { showError("Email already exists!"); return; }

                PreparedStatement psUser = conn.prepareStatement(
                    "INSERT INTO Users (first_name,middle_name,last_name,address,email_address," +
                    "username,password,user_role,user_status) VALUES (?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                psUser.setString(1, firstName);
                psUser.setString(2, txtMiddleName.getText().trim().isEmpty() ? null : txtMiddleName.getText().trim());
                psUser.setString(3, lastName);
                psUser.setString(4, txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
                psUser.setString(5, email);
                psUser.setString(6, username);
                psUser.setString(7, password);
                psUser.setString(8, role);
                psUser.setString(9, "Active");
                psUser.executeUpdate();

                int newId = -1;
                ResultSet keys = psUser.getGeneratedKeys();
                if (keys.next()) newId = keys.getInt(1);
                if (newId == -1) { showError("Failed to retrieve new user ID!"); return; }

                switch (role) {
                    case "Admin" -> {
                        PreparedStatement psA = conn.prepareStatement("INSERT INTO Admin (admin_id) VALUES (?)");
                        psA.setInt(1, newId); psA.executeUpdate();
                    }
                    case "Driver" -> {
                        PreparedStatement psD = conn.prepareStatement(
                            "INSERT INTO Driver (driver_id,license_number,driver_status) VALUES (?,?,?)");
                        psD.setInt(1, newId);
                        psD.setString(2, txtLicenseNumber.getText().trim().isEmpty()
                                ? null : txtLicenseNumber.getText().trim());
                        psD.setString(3, "Available");
                        psD.executeUpdate();
                    }
                    case "Passenger" -> {
                        PreparedStatement psP = conn.prepareStatement(
                            "INSERT INTO Passenger (passenger_id) VALUES (?)");
                        psP.setInt(1, newId); psP.executeUpdate();
                    }
                }
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("User added successfully!");
                clearFields();
                loadUsers("All", "All");
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void showError(String m) {
            lblStatus.setForeground(RED);
            lblStatus.setText(m);
        }

        private void clearFields() {
            txtFirstName.setText(""); txtMiddleName.setText(""); txtLastName.setText("");
            txtAddress.setText(""); txtEmail.setText(""); txtUsername.setText("");
            pwdPassword.setText(""); txtLicenseNumber.setText("");
            cmbRole.setSelectedIndex(0);
            lblLicense.setVisible(false);
            txtLicenseNumber.setVisible(false);
        }
    }

    public class DeleteUserPanel extends JPanel {
        private int    userId;
        private String userRole;
        private JLabel lblUserId     = valueLabel(), lblFirstName  = valueLabel();
        private JLabel lblMiddleName = valueLabel(), lblLastName   = valueLabel();
        private JLabel lblUsername   = valueLabel(), lblEmail      = valueLabel();
        private JLabel lblAddress    = valueLabel(), lblRole       = valueLabel();
        private JLabel lblAccStatus  = valueLabel();
        private JLabel lblStatus     = new JLabel(" ");

        public DeleteUserPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            JLabel title = new JLabel("Deactivate User", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(RED);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 18, 10);
            card.add(title, gbc);
            gbc.gridwidth = 1;
            gbc.insets    = new Insets(6, 10, 6, 10);

            int y = 1;
            addInfoRow(card, gbc, "User ID:",        lblUserId,     y++);
            addInfoRow(card, gbc, "First Name:",     lblFirstName,  y++);
            addInfoRow(card, gbc, "Middle Name:",    lblMiddleName, y++);
            addInfoRow(card, gbc, "Last Name:",      lblLastName,   y++);
            addInfoRow(card, gbc, "Username:",       lblUsername,   y++);
            addInfoRow(card, gbc, "Email:",          lblEmail,      y++);
            addInfoRow(card, gbc, "Address:",        lblAddress,    y++);
            addInfoRow(card, gbc, "Role:",           lblRole,       y++);
            addInfoRow(card, gbc, "Account Status:", lblAccStatus,  y++);

            JButton btnConfirm = new JButton("Confirm Deactivate");
            JButton btnBack    = new JButton("Back");
            styleButtonFilled(btnConfirm, RED);
            styleButtonOutline(btnBack, BLUE);
            btnConfirm.setPreferredSize(new Dimension(170, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnConfirm);
            btnRow.add(btnBack);

            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(18, 10, 6, 10);
            card.add(btnRow, gbc);
            gbc.gridy++;
            gbc.insets = new Insets(4, 10, 0, 10);
            lblStatus.setFont(LABEL_FONT);
            card.add(lblStatus, gbc);
            add(card);

            btnConfirm.addActionListener(e -> deactivateUser());
            btnBack.addActionListener(e -> {
                cardLayout.show(container, "USER_LIST");
                loadUsers("All", "All");
            });
        }

        public void setUserDetails(int id, String fn, String mn, String ln,
                String un, String em, String addr, String role, String status) {
            userId   = id;
            userRole = role;
            lblUserId.setText(String.valueOf(id));
            lblFirstName.setText(fn);
            lblMiddleName.setText(mn != null ? mn : "—");
            lblLastName.setText(ln);
            lblUsername.setText(un);
            lblEmail.setText(em);
            lblAddress.setText(addr != null ? addr : "—");
            lblRole.setText(role);
            lblAccStatus.setText(status);
            lblAccStatus.setForeground("Active".equalsIgnoreCase(status) ? GREEN : RED);
            lblStatus.setText(" ");
        }

        private void deactivateUser() {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Set this user's account to Inactive?",
                "Confirm Deactivate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Users SET user_status='Inactive' WHERE user_id=?");
                ps.setInt(1, userId);
                ps.executeUpdate();

                if ("Driver".equalsIgnoreCase(userRole)) {
                    PreparedStatement psD = conn.prepareStatement(
                        "UPDATE Driver SET driver_status='Not Available' WHERE driver_id=?");
                    psD.setInt(1, userId);
                    psD.executeUpdate();
                    cascadeDriverNotAvailable(userId);
                }

                loadUsers("All", "All");
                cardLayout.show(container, "USER_LIST");
            } catch (Exception e) {
                lblStatus.setForeground(RED);
                lblStatus.setText("Error deactivating user!");
                e.printStackTrace();
            }
        }
    }

    public class UpdateUserPanel extends JPanel {
        private int     userId;
        private boolean passwordChanged = false;
        private String  currentRole     = "";
        private String  currentStatus   = "";
        // Store the original license so we can exclude the current driver from uniqueness check
        private String  originalLicense = "";

        private JTextField     txtFirstName     = styledField();
        private JTextField     txtMiddleName    = styledField();
        private JTextField     txtLastName      = styledField();
        private JTextField     txtUsername      = styledField();
        private JTextField     txtEmail         = styledField();
        private JTextField     txtAddress       = styledField();
        private JPasswordField pwdPassword      = styledPasswordField();
        private JTextField     txtLicenseNumber = styledField();
        private JComboBox<String> cmbRole   = new JComboBox<>(new String[]{"Admin", "Driver", "Passenger"});
        private JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Active", "Inactive"});
        private JLabel lblLicense = new JLabel("License No:");
        private JLabel lblStatus  = new JLabel(" ");

        public UpdateUserPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Update User", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbRole.setPreferredSize(new Dimension(260, 32));
            cmbStatus.setPreferredSize(new Dimension(260, 32));
            lblLicense.setFont(LABEL_FONT);
            lblLicense.setVisible(false);
            txtLicenseNumber.setVisible(false);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "First Name *:",   txtFirstName,     y++);
            addFormRow(form, gbc, "Middle Name:",     txtMiddleName,    y++);
            addFormRow(form, gbc, "Last Name *:",    txtLastName,      y++);
            addFormRow(form, gbc, "Address:",         txtAddress,       y++);
            addFormRow(form, gbc, "Email *:",         txtEmail,         y++);
            addFormRow(form, gbc, "Username *:",      txtUsername,      y++);
            addFormRow(form, gbc, "Password:",        pwdPassword,      y++);
            addFormRow(form, gbc, "Role *:",          cmbRole,          y++);
            addFormRow(form, gbc, "Account Status:",  cmbStatus,        y++);
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
            form.add(lblLicense, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            form.add(txtLicenseNumber, gbc);

            cmbRole.addActionListener(e -> {
                boolean isDriver = "Driver".equals(cmbRole.getSelectedItem());
                lblLicense.setVisible(isDriver);
                txtLicenseNumber.setVisible(isDriver);
                form.revalidate();
                form.repaint();
            });

            card.add(form);
            card.add(Box.createVerticalStrut(20));

            JButton btnUpdate = new JButton("Update");
            JButton btnBack   = new JButton("Back");
            styleButtonFilled(btnUpdate, BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnUpdate.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnUpdate);
            btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus.setFont(LABEL_FONT);
            lblStatus.setForeground(new Color(0, 150, 80));
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnUpdate.addActionListener(e -> updateUser());
            btnBack.addActionListener(e -> {
                lblStatus.setText(" ");
                cardLayout.show(container, "USER_LIST");
                loadUsers("All", "All");
            });
        }

        public void setUserData(int id, String fn, String mn, String ln,
                String un, String em, String addr, String pwd, String role, String status) {
            userId        = id;
            currentRole   = role;
            currentStatus = status != null ? status : "Active";
            passwordChanged = false;

            txtFirstName.setText(fn   != null ? fn   : "");
            txtMiddleName.setText(mn  != null ? mn   : "");
            txtLastName.setText(ln    != null ? ln   : "");
            txtUsername.setText(un);
            txtEmail.setText(em);
            txtAddress.setText(addr   != null ? addr : "");
            pwdPassword.setText(pwd   != null ? pwd  : "");
            cmbStatus.setSelectedItem(currentStatus);

            if ("Driver".equals(role)) {
                try {
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT license_number FROM Driver WHERE driver_id=?");
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String lic = rs.getString("license_number");
                        // Store original license to allow keeping the same value without uniqueness error
                        originalLicense = lic != null ? lic : "";
                        txtLicenseNumber.setText(originalLicense);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                lblLicense.setVisible(true);
                txtLicenseNumber.setVisible(true);
            } else {
                lblLicense.setVisible(false);
                txtLicenseNumber.setVisible(false);
                txtLicenseNumber.setText("");
                originalLicense = "";
            }

            for (DocumentListener dl :
                    ((javax.swing.text.AbstractDocument) pwdPassword.getDocument()).getDocumentListeners())
                pwdPassword.getDocument().removeDocumentListener(dl);
            pwdPassword.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { passwordChanged = true; }
                public void removeUpdate(DocumentEvent e)  { passwordChanged = true; }
                public void changedUpdate(DocumentEvent e) { passwordChanged = true; }
            });
            SwingUtilities.invokeLater(() -> passwordChanged = false);
            cmbRole.setSelectedItem(role);
        }

        private void updateUser() {
            String newRole   = cmbRole.getSelectedItem().toString();
            String newStatus = cmbStatus.getSelectedItem().toString();
            String newPwd    = new String(pwdPassword.getPassword());
            String email     = txtEmail.getText().trim();
            String username  = txtUsername.getText().trim();

            if (txtFirstName.getText().trim().isEmpty()) { showError("First name is required!"); return; }
            if (txtLastName.getText().trim().isEmpty())  { showError("Last name is required!");  return; }
            if (username.isEmpty())                      { showError("Username is required!");   return; }
            if (email.isEmpty())                         { showError("Email is required!");      return; }
            if (!EMAIL_PATTERN.matcher(email).matches()) { showError("Invalid email format!");   return; }
            if (passwordChanged && !newPwd.isEmpty() && newPwd.length() < 8) {
                showError("Password must be at least 8 characters!"); return; }
            if (newRole.equals("Driver") && txtLicenseNumber.getText().trim().isEmpty()) {
                showError("License number is required for Driver!"); return; }

            // VALIDATION: license number must be unique, excluding the current driver's own record
            if ("Driver".equals(newRole)) {
                String license = txtLicenseNumber.getText().trim();
                if (!license.isEmpty() && !license.equals(originalLicense)) {
                    try {
                        PreparedStatement psChkL = conn.prepareStatement(
                            "SELECT COUNT(*) FROM Driver WHERE license_number=? AND driver_id<>?");
                        psChkL.setString(1, license);
                        psChkL.setInt(2, userId);
                        ResultSet rsL = psChkL.executeQuery();
                        if (rsL.next() && rsL.getInt(1) > 0) {
                            showError("License number already exists!");
                            return;
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }

            try {
                PreparedStatement psU = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Users WHERE username=? AND user_id<>?");
                psU.setString(1, username); psU.setInt(2, userId);
                ResultSet rsU = psU.executeQuery();
                if (rsU.next() && rsU.getInt(1) > 0) { showError("Username already taken!"); return; }

                PreparedStatement psE = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Users WHERE email_address=? AND user_id<>?");
                psE.setString(1, email); psE.setInt(2, userId);
                ResultSet rsE = psE.executeQuery();
                if (rsE.next() && rsE.getInt(1) > 0) { showError("Email already taken!"); return; }

                PreparedStatement psUser;
                if (passwordChanged && !newPwd.trim().isEmpty()) {
                    psUser = conn.prepareStatement(
                        "UPDATE Users SET first_name=?,middle_name=?,last_name=?,address=?," +
                        "email_address=?,username=?,password=?,user_role=?,user_status=? WHERE user_id=?");
                    psUser.setString(1, txtFirstName.getText().trim());
                    psUser.setString(2, txtMiddleName.getText().trim().isEmpty() ? null : txtMiddleName.getText().trim());
                    psUser.setString(3, txtLastName.getText().trim());
                    psUser.setString(4, txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
                    psUser.setString(5, email);
                    psUser.setString(6, username);
                    psUser.setString(7, newPwd.trim());
                    psUser.setString(8, newRole);
                    psUser.setString(9, newStatus);
                    psUser.setInt(10, userId);
                } else {
                    psUser = conn.prepareStatement(
                        "UPDATE Users SET first_name=?,middle_name=?,last_name=?,address=?," +
                        "email_address=?,username=?,user_role=?,user_status=? WHERE user_id=?");
                    psUser.setString(1, txtFirstName.getText().trim());
                    psUser.setString(2, txtMiddleName.getText().trim().isEmpty() ? null : txtMiddleName.getText().trim());
                    psUser.setString(3, txtLastName.getText().trim());
                    psUser.setString(4, txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
                    psUser.setString(5, email);
                    psUser.setString(6, username);
                    psUser.setString(7, newRole);
                    psUser.setString(8, newStatus);
                    psUser.setInt(9, userId);
                }
                psUser.executeUpdate();

                boolean statusBecomingInactive =
                    "Active".equalsIgnoreCase(currentStatus) && "Inactive".equalsIgnoreCase(newStatus);

                if (statusBecomingInactive && "Driver".equalsIgnoreCase(newRole)) {
                    PreparedStatement psD = conn.prepareStatement(
                        "UPDATE Driver SET driver_status='Not Available' WHERE driver_id=?");
                    psD.setInt(1, userId);
                    psD.executeUpdate();
                    cascadeDriverNotAvailable(userId);
                }

                if (!currentRole.equals(newRole)) {
                    deleteFromRoleTable(currentRole, userId);
                    insertIntoRoleTable(newRole, userId, txtLicenseNumber.getText().trim());
                } else if (newRole.equals("Driver")) {
                    PreparedStatement psL = conn.prepareStatement(
                        "UPDATE Driver SET license_number=? WHERE driver_id=?");
                    psL.setString(1, txtLicenseNumber.getText().trim().isEmpty()
                            ? null : txtLicenseNumber.getText().trim());
                    psL.setInt(2, userId);
                    psL.executeUpdate();
                    // Update stored original license after successful save
                    originalLicense = txtLicenseNumber.getText().trim();
                }

                currentRole     = newRole;
                currentStatus   = newStatus;
                passwordChanged = false;
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("User updated successfully!");
                loadUsers("All", "All");
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void showError(String m) {
            lblStatus.setForeground(RED);
            lblStatus.setText(m);
        }

        private void deleteFromRoleTable(String role, int id) throws Exception {
            String sql = switch (role) {
                case "Admin"     -> "DELETE FROM Admin     WHERE admin_id=?";
                case "Driver"    -> "DELETE FROM Driver    WHERE driver_id=?";
                case "Passenger" -> "DELETE FROM Passenger WHERE passenger_id=?";
                default          -> null;
            };
            if (sql != null) {
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }

        private void insertIntoRoleTable(String role, int id, String license) throws Exception {
            switch (role) {
                case "Admin" -> {
                    PreparedStatement psA = conn.prepareStatement("INSERT INTO Admin (admin_id) VALUES (?)");
                    psA.setInt(1, id); psA.executeUpdate();
                }
                case "Driver" -> {
                    PreparedStatement psD = conn.prepareStatement(
                        "INSERT INTO Driver (driver_id,license_number,driver_status) VALUES (?,?,?)");
                    psD.setInt(1, id);
                    psD.setString(2, license.isEmpty() ? null : license);
                    psD.setString(3, "Available");
                    psD.executeUpdate();
                }
                case "Passenger" -> {
                    PreparedStatement psP = conn.prepareStatement(
                        "INSERT INTO Passenger (passenger_id) VALUES (?)");
                    psP.setInt(1, id); psP.executeUpdate();
                }
            }
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, Component field, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void addInfoRow(JPanel panel, GridBagConstraints gbc, String labelText, JLabel value, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(value, gbc);
    }

    private JTextField styledField() {
        JTextField f = new JTextField(15);
        f.setPreferredSize(new Dimension(260, 32));
        f.setMinimumSize(new Dimension(260, 32));
        f.setFont(LABEL_FONT);
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField(15);
        f.setPreferredSize(new Dimension(260, 32));
        f.setMinimumSize(new Dimension(260, 32));
        f.setFont(LABEL_FONT);
        return f;
    }

    private JLabel valueLabel() {
        JLabel l = new JLabel();
        l.setFont(LABEL_FONT);
        l.setForeground(new Color(50, 50, 50));
        return l;
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

    private void styleButtonOutline(JButton btn, Color c) {
        btn.setBackground(WHITE);
        btn.setForeground(c);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(c, 2));
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }
}

