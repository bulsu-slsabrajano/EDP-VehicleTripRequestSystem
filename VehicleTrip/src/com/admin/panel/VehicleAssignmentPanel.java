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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class VehicleAssignmentPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel container;

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbFilter;
    private Connection conn;

    private AssignPanel       assignPanel;
    private UpdateAssignPanel updatePanel;
    private CancelAssignPanel cancelPanel;

    private static final Color BLUE       = new Color(0, 150, 199);
    private static final Color RED        = new Color(220, 53, 69);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private int loggedInAdminId = -1;

    public VehicleAssignmentPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;

        cardLayout = new CardLayout();
        container  = new JPanel(cardLayout);
        container.setBackground(WHITE);

        assignPanel  = new AssignPanel();
        updatePanel  = new UpdateAssignPanel();
        cancelPanel  = new CancelAssignPanel();

        container.add(listPanel(),   "LIST");
        container.add(assignPanel,   "ASSIGN");
        container.add(updatePanel,   "UPDATE");
        container.add(cancelPanel,   "CANCEL");

        setLayout(new BorderLayout());
        setBackground(WHITE);
        add(container, BorderLayout.CENTER);
        loadAssignments("All");
    }

    public void setLoggedInAdminId(int id) {
        this.loggedInAdminId = id;
        assignPanel.setAdminId(id);
        updatePanel.setAdminId(id);
    }

 
    //When an assignment becomes Inactive, revert all its Approved trips to Pending.
    private void cascadeAssignmentInactive(int assignmentId) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Trip SET trip_status='Pending' " +
            "WHERE assignment_id=? AND trip_status='Approved'");
        ps.setInt(1, assignmentId);
        ps.executeUpdate();
    }

    private JPanel listPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(WHITE);
        top.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setBackground(WHITE);
        JLabel lblFilter = new JLabel("Filter by Status:");
        lblFilter.setFont(LABEL_FONT);
        cmbFilter = new JComboBox<>(new String[]{"All", "Active", "Inactive"});
        cmbFilter.setPreferredSize(new Dimension(130, 30));
        cmbFilter.addActionListener(e -> loadAssignments(cmbFilter.getSelectedItem().toString()));
        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilter);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> { cmbFilter.setSelectedItem("All"); loadAssignments("All"); });
        refreshPanel.add(btnRefresh);

        top.add(filterPanel,  BorderLayout.WEST);
        top.add(refreshPanel, BorderLayout.EAST);

        String[] cols = {"Assignment ID", "Driver", "Vehicle", "Admin", "Date Assigned", "Status"};
        model = new DefaultTableModel(cols, 0) {
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
                if (col == 5 && val != null) {
                    String status = val.toString();
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground("Active".equalsIgnoreCase(status)
                            ? new Color(39, 174, 96) : new Color(220, 53, 69));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
                if (sel) c.setBackground(t.getSelectionBackground());
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);

        JScrollPane scroll = TableStyleUtil.modernScroll(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bottom.setBackground(WHITE);

        JButton btnAssign = new JButton("Assign");
        JButton btnUpdate = new JButton("Update");
        JButton btnCancel = new JButton("Cancel Assignment");
        styleButtonFilled(btnAssign, BLUE);
        styleButtonFilled(btnUpdate, BLUE);
        styleButtonFilled(btnCancel, RED);

        btnAssign.addActionListener(e -> {
            assignPanel.resetFields();
            cardLayout.show(container, "ASSIGN");
        });

        btnUpdate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an assignment first!"); return; }
            int assignId = (int) model.getValueAt(row, 0);
            updatePanel.loadAssignment(assignId);
            cardLayout.show(container, "UPDATE");
        });

        btnCancel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an assignment first!"); return; }
            cancelPanel.setDetails(
                (int)    model.getValueAt(row, 0),
                (String) model.getValueAt(row, 1),
                (String) model.getValueAt(row, 2),
                (String) model.getValueAt(row, 3),
                String.valueOf(model.getValueAt(row, 4)),
                (String) model.getValueAt(row, 5));
            cardLayout.show(container, "CANCEL");
        });

        bottom.add(btnAssign);
        bottom.add(btnUpdate);
        bottom.add(btnCancel);

        main.add(top,    BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        return main;
    }

    private void loadAssignments(String filter) {
        try {
            model.setRowCount(0);
            String sql = "SELECT assignment_id,driver,vehicle,admin,date_assigned,assignment_status " +
                         "FROM vw_VehicleAssignments";
            if (!filter.equals("All")) sql += " WHERE LOWER(assignment_status)=LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!filter.equals("All")) ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("assignment_id"),
                    rs.getString("driver"),
                    rs.getString("vehicle"),
                    rs.getString("admin"),
                    rs.getDate("date_assigned"),
                    rs.getString("assignment_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public class AssignPanel extends JPanel {
        private JComboBox<String> cmbDriver  = new JComboBox<>();
        private JComboBox<String> cmbVehicle = new JComboBox<>();
        private JComboBox<String> cmbStatus  = new JComboBox<>(new String[]{"Active", "Inactive"});
        private JTextField txtDate  = styledField();
        private JTextField txtAdmin = styledField();
        private JLabel lblStatus = new JLabel(" ");
        private int[] driverIds  = new int[0];
        private int[] vehicleIds = new int[0];
        private int adminId = -1;

        public AssignPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            buildUI();
        }

        public void setAdminId(int id) {
            this.adminId = id;
            loadAdminName(id);
        }

        private void loadAdminName(int id) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT first_name+' '+last_name AS full_name FROM Users WHERE user_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) txtAdmin.setText(rs.getString("full_name"));
            } catch (Exception e) { e.printStackTrace(); }
        }

        public void resetFields() {
            loadDropdowns();
            txtDate.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            cmbStatus.setSelectedIndex(0);
            lblStatus.setText(" ");
            if (adminId != -1) loadAdminName(adminId);
        }

        private void loadDropdowns() {
            try {
                cmbDriver.removeAllItems();
                PreparedStatement psD = conn.prepareStatement(
                    "SELECT d.driver_id, u.first_name+' '+u.last_name AS name " +
                    "FROM Driver d JOIN Users u ON d.driver_id=u.user_id WHERE d.driver_status='Available'");
                ResultSet rsD = psD.executeQuery();
                java.util.List<Integer> dIds = new java.util.ArrayList<>();
                while (rsD.next()) { cmbDriver.addItem(rsD.getString("name")); dIds.add(rsD.getInt("driver_id")); }
                driverIds = dIds.stream().mapToInt(i -> i).toArray();

                cmbVehicle.removeAllItems();
                PreparedStatement psV = conn.prepareStatement(
                    "SELECT vehicle_id, vehicle_model+'  ('+plate_number+')' AS label " +
                    "FROM Vehicle WHERE vehicle_status='Available'");
                ResultSet rsV = psV.executeQuery();
                java.util.List<Integer> vIds = new java.util.ArrayList<>();
                while (rsV.next()) { cmbVehicle.addItem(rsV.getString("label")); vIds.add(rsV.getInt("vehicle_id")); }
                vehicleIds = vIds.stream().mapToInt(i -> i).toArray();
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void buildUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(25, 40, 25, 40)));

            JLabel title = new JLabel("Assign Vehicle", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbDriver.setPreferredSize(new Dimension(260, 32));
            cmbVehicle.setPreferredSize(new Dimension(260, 32));
            cmbStatus.setPreferredSize(new Dimension(260, 32));
            txtAdmin.setEditable(false);
            txtAdmin.setBackground(new Color(240, 240, 240));

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "Driver:",        cmbDriver,  y++);
            addFormRow(form, gbc, "Vehicle:",       cmbVehicle, y++);
            addFormRow(form, gbc, "Admin:",         txtAdmin,   y++);
            addFormRow(form, gbc, "Date Assigned:", txtDate,    y++);
            addFormRow(form, gbc, "Status:",        cmbStatus,  y++);
            card.add(form);
            card.add(Box.createVerticalStrut(20));

            JButton btnSave = new JButton("Save");
            JButton btnBack = new JButton("Back");
            styleButtonFilled(btnSave, BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnSave.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnSave);
            btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus.setFont(LABEL_FONT);
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnSave.addActionListener(e -> save());
            btnBack.addActionListener(e -> { cardLayout.show(container, "LIST"); loadAssignments("All"); });
        }

        private void save() {
            if (cmbDriver.getItemCount() == 0)  { showError("No available drivers!");  return; }
            if (cmbVehicle.getItemCount() == 0) { showError("No available vehicles!"); return; }
            if (adminId == -1)                  { showError("Admin not set!");          return; }

            java.sql.Date date;
            try { date = java.sql.Date.valueOf(txtDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid date format! Use yyyy-MM-dd"); return; }

            try {
                int dId = driverIds[cmbDriver.getSelectedIndex()];
                int vId = vehicleIds[cmbVehicle.getSelectedIndex()];
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Vehicle_Assignment " +
                    "(driver_id,vehicle_id,admin_id,date_assigned,assignment_status) VALUES (?,?,?,?,?)");
                ps.setInt(1, dId); ps.setInt(2, vId); ps.setInt(3, adminId);
                ps.setDate(4, date);
                ps.setString(5, cmbStatus.getSelectedItem().toString());
                ps.executeUpdate();
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Assignment saved successfully!");
                loadAssignments("All");
            } catch (Exception e) {
                showError("Error saving assignment!");
                e.printStackTrace();
            }
        }

        private void showError(String msg) { lblStatus.setForeground(RED); lblStatus.setText(msg); }
    }

    public class UpdateAssignPanel extends JPanel {
        private int assignmentId    = -1;
        private String prevStatus   = "";
        private JComboBox<String> cmbDriver  = new JComboBox<>();
        private JComboBox<String> cmbVehicle = new JComboBox<>();
        private JComboBox<String> cmbStatus  = new JComboBox<>(new String[]{"Active", "Inactive"});
        private JTextField txtDate  = styledField();
        private JTextField txtAdmin = styledField();
        private JLabel lblStatus = new JLabel(" ");
        private int[] driverIds  = new int[0];
        private int[] vehicleIds = new int[0];
        private int adminId = -1;

        public UpdateAssignPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            buildUI();
        }

        public void setAdminId(int id) { this.adminId = id; }

        public void loadAssignment(int id) {
            this.assignmentId = id;
            lblStatus.setText(" ");
            loadDropdownsAll();
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT va.*, du.first_name+' '+du.last_name AS driver_name, " +
                    "v.vehicle_model+'  ('+v.plate_number+')' AS vehicle_label, " +
                    "au.first_name+' '+au.last_name AS admin_name " +
                    "FROM Vehicle_Assignment va " +
                    "JOIN Driver d  ON va.driver_id=d.driver_id " +
                    "JOIN Users  du ON d.driver_id=du.user_id " +
                    "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id " +
                    "JOIN Admin a   ON va.admin_id=a.admin_id " +
                    "JOIN Users  au ON a.admin_id=au.user_id " +
                    "WHERE va.assignment_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    for (int i = 0; i < driverIds.length;  i++)
                        if (driverIds[i]  == rs.getInt("driver_id"))  { cmbDriver.setSelectedIndex(i);  break; }
                    for (int i = 0; i < vehicleIds.length; i++)
                        if (vehicleIds[i] == rs.getInt("vehicle_id")) { cmbVehicle.setSelectedIndex(i); break; }
                    txtDate.setText(rs.getDate("date_assigned").toString());
                    prevStatus = rs.getString("assignment_status");
                    cmbStatus.setSelectedItem(prevStatus);
                    txtAdmin.setText(rs.getString("admin_name"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void loadDropdownsAll() {
            try {
                cmbDriver.removeAllItems();
                PreparedStatement psD = conn.prepareStatement(
                    "SELECT d.driver_id, u.first_name+' '+u.last_name AS name " +
                    "FROM Driver d JOIN Users u ON d.driver_id=u.user_id");
                ResultSet rsD = psD.executeQuery();
                java.util.List<Integer> dIds = new java.util.ArrayList<>();
                while (rsD.next()) { cmbDriver.addItem(rsD.getString("name")); dIds.add(rsD.getInt("driver_id")); }
                driverIds = dIds.stream().mapToInt(i -> i).toArray();

                cmbVehicle.removeAllItems();
                PreparedStatement psV = conn.prepareStatement(
                    "SELECT vehicle_id, vehicle_model+'  ('+plate_number+')' AS label FROM Vehicle");
                ResultSet rsV = psV.executeQuery();
                java.util.List<Integer> vIds = new java.util.ArrayList<>();
                while (rsV.next()) { cmbVehicle.addItem(rsV.getString("label")); vIds.add(rsV.getInt("vehicle_id")); }
                vehicleIds = vIds.stream().mapToInt(i -> i).toArray();
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void buildUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(25, 40, 25, 40)));

            JLabel title = new JLabel("Update Assignment", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbDriver.setPreferredSize(new Dimension(260, 32));
            cmbVehicle.setPreferredSize(new Dimension(260, 32));
            cmbStatus.setPreferredSize(new Dimension(260, 32));
            txtAdmin.setEditable(false);
            txtAdmin.setBackground(new Color(240, 240, 240));

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "Driver:",        cmbDriver,  y++);
            addFormRow(form, gbc, "Vehicle:",       cmbVehicle, y++);
            addFormRow(form, gbc, "Admin:",         txtAdmin,   y++);
            addFormRow(form, gbc, "Date Assigned:", txtDate,    y++);
            addFormRow(form, gbc, "Status:",        cmbStatus,  y++);
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
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnUpdate.addActionListener(e -> update());
            btnBack.addActionListener(e -> { cardLayout.show(container, "LIST"); loadAssignments("All"); });
        }

        private void update() {
            if (assignmentId == -1) return;
            if (driverIds.length == 0 || vehicleIds.length == 0) {
                lblStatus.setForeground(RED); lblStatus.setText("No drivers or vehicles available!"); return;
            }
            java.sql.Date date;
            try { date = java.sql.Date.valueOf(txtDate.getText().trim()); }
            catch (Exception ex) { lblStatus.setForeground(RED); lblStatus.setText("Invalid date! Use yyyy-MM-dd"); return; }

            String newStatus = cmbStatus.getSelectedItem().toString();

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Vehicle_Assignment SET driver_id=?,vehicle_id=?,date_assigned=?,assignment_status=? " +
                    "WHERE assignment_id=?");
                ps.setInt(1, driverIds[cmbDriver.getSelectedIndex()]);
                ps.setInt(2, vehicleIds[cmbVehicle.getSelectedIndex()]);
                ps.setDate(3, date);
                ps.setString(4, newStatus);
                ps.setInt(5, assignmentId);
                ps.executeUpdate();

                if ("Active".equalsIgnoreCase(prevStatus) && "Inactive".equalsIgnoreCase(newStatus)) {
                    cascadeAssignmentInactive(assignmentId);
                }

                prevStatus = newStatus;
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Assignment updated successfully!");
                loadAssignments("All");
            } catch (Exception e) {
                lblStatus.setForeground(RED); lblStatus.setText("Error updating assignment!");
                e.printStackTrace();
            }
        }
    }

    public class CancelAssignPanel extends JPanel {
        private int    assignmentId;
        private JLabel lblAssignId = valueLabel();
        private JLabel lblDriver   = valueLabel();
        private JLabel lblVehicle  = valueLabel();
        private JLabel lblAdmin    = valueLabel();
        private JLabel lblDate     = valueLabel();
        private JLabel lblStat     = valueLabel();
        private JLabel lblStatus   = new JLabel(" ");

        public CancelAssignPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);

            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(25, 40, 25, 40)));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            JLabel title = new JLabel("Cancel Assignment", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(RED);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 18, 10);
            card.add(title, gbc);
            gbc.gridwidth = 1;
            gbc.insets    = new Insets(6, 10, 6, 10);

            int y = 1;
            addInfoRow(card, gbc, "Assignment ID:", lblAssignId, y++);
            addInfoRow(card, gbc, "Driver:",        lblDriver,   y++);
            addInfoRow(card, gbc, "Vehicle:",       lblVehicle,  y++);
            addInfoRow(card, gbc, "Admin:",         lblAdmin,    y++);
            addInfoRow(card, gbc, "Date Assigned:", lblDate,     y++);
            addInfoRow(card, gbc, "Status:",        lblStat,     y++);

            // Warning label
            JLabel lblWarn = new JLabel(
                "<html><i>Cancelling will set the assignment to Inactive<br>" +
                "and revert any Approved trips back to Pending.</i></html>");
            lblWarn.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblWarn.setForeground(new Color(180, 100, 0));
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(4, 10, 10, 10);
            card.add(lblWarn, gbc);
            y++;

            JButton btnConfirm = new JButton("Confirm Cancel");
            JButton btnBack    = new JButton("Back");
            styleButtonFilled(btnConfirm, RED);
            styleButtonOutline(btnBack, BLUE);
            btnConfirm.setPreferredSize(new Dimension(150, 35));
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

            btnConfirm.addActionListener(e -> cancelAssignment());
            btnBack.addActionListener(e -> { cardLayout.show(container, "LIST"); loadAssignments("All"); });
        }

        public void setDetails(int id, String driver, String vehicle,
                String admin, String date, String status) {
            assignmentId = id;
            lblAssignId.setText(String.valueOf(id));
            lblDriver.setText(driver);
            lblVehicle.setText(vehicle);
            lblAdmin.setText(admin);
            lblDate.setText(date);
            lblStat.setText(status);
            lblStatus.setText(" ");
        }

        private void cancelAssignment() {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this assignment?\n" +
                "Approved trips linked to it will revert to Pending.",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                //Set assignment Inactive
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id=?");
                ps.setInt(1, assignmentId);
                ps.executeUpdate();


                cascadeAssignmentInactive(assignmentId);

                loadAssignments("All");
                cardLayout.show(container, "LIST");
            } catch (Exception e) {
                lblStatus.setForeground(RED);
                lblStatus.setText("Error cancelling assignment!");
                e.printStackTrace();
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

    private JLabel valueLabel() {
        JLabel lbl = new JLabel();
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(new Color(50, 50, 50));
        return lbl;
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