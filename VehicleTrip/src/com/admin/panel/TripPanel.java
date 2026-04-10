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
import java.time.LocalDate;

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

import com.project.dbConnection.DbConnectMsSql;

public class TripPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel container;
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbFilter;
    private Connection conn;

    private int    loggedInAdminId   = -1;
    private String loggedInAdminName = "";

    CreateTripPanel  createPanel;
    private UpdateTripPanel  updatePanel;
    private ViewTripPanel    viewPanel;

    private static final Color BLUE   = new Color(0, 150, 199);
    private static final Color RED    = new Color(220, 53, 69);
    private static final Color ORANGE = new Color(230, 126, 34);
    private static final Color GREEN  = new Color(39, 174, 96);
    private static final Color WHITE  = Color.WHITE;
    private static final Color STRIPE = new Color(245, 248, 252);
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private java.util.List<Integer> tripIds = new java.util.ArrayList<>();

    public TripPanel(int adminId) {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;

        this.loggedInAdminId   = adminId;
        this.loggedInAdminName = fetchAdminName(adminId);

        cardLayout  = new CardLayout();
        container   = new JPanel(cardLayout);
        container.setBackground(WHITE);

        createPanel = new CreateTripPanel();
        updatePanel = new UpdateTripPanel();
        viewPanel   = new ViewTripPanel();

        container.add(listPanel(),  "LIST");
        container.add(createPanel, "CREATE");
        container.add(updatePanel, "UPDATE");
        container.add(viewPanel,   "VIEW");

        setLayout(new BorderLayout());
        setBackground(WHITE);
        add(container, BorderLayout.CENTER);
        loadTrips("All");
    }

    public TripPanel() {
        this(-1);
    }

    private String fetchAdminName(int adminId) {
        if (adminId <= 0) return "—";
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT first_name+' '+last_name AS full_name FROM Users WHERE user_id=?");
            ps.setInt(1, adminId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (Exception e) { e.printStackTrace(); }
        return "—";
    }

    private JScrollPane modernScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(WHITE);
        sp.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0, 150, 199, 120);
                trackColor = new Color(240, 244, 250);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setBorderPainted(false);
                return b;
            }
        });
        sp.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0, 150, 199, 120);
                trackColor = new Color(240, 244, 250);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setBorderPainted(false);
                return b;
            }
        });
        return sp;
    }

    private void applyTableStyle(JTable t) {
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setOpaque(true);
                setBackground(sel ? new Color(200, 230, 255) : (row % 2 == 0 ? WHITE : STRIPE));
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });
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
        cmbFilter = new JComboBox<>(new String[]{"All", "Pending", "Approved", "Completed", "Cancelled"});
        cmbFilter.setPreferredSize(new Dimension(140, 30));
        cmbFilter.addActionListener(e -> loadTrips(cmbFilter.getSelectedItem().toString()));
        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilter);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> { cmbFilter.setSelectedItem("All"); loadTrips("All"); });
        refreshPanel.add(btnRefresh);
        top.add(filterPanel,  BorderLayout.WEST);
        top.add(refreshPanel, BorderLayout.EAST);

        String[] cols = {"Trip ID", "Passenger", "Admin", "Assignment",
                "Start Date", "Start Time", "End Date", "End Time",
                "Pickup", "Destination", "Pax", "Status"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.setRowHeight(36);
        table.setBackground(WHITE);
        table.setFont(LABEL_FONT);
        table.setGridColor(new Color(230, 235, 245));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(200, 230, 255));
        table.getTableHeader().setBackground(BLUE);
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        applyTableStyle(table);

        table.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(LEFT);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? new Color(200, 230, 255) : (row % 2 == 0 ? WHITE : STRIPE));
                setForeground(switch (s.toLowerCase()) {
                    case "pending"   -> ORANGE;
                    case "approved"  -> BLUE;
                    case "completed" -> GREEN;
                    case "cancelled" -> RED;
                    default          -> Color.DARK_GRAY;
                });
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        JScrollPane scroll = modernScroll(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bottom.setBackground(WHITE);

        JButton btnView    = new JButton("View");
        JButton btnCreate  = new JButton("Create Trip");
        JButton btnUpdate  = new JButton("Update");
        JButton btnApprove = new JButton("Approve");
        JButton btnReject  = new JButton("Reject");

        styleButtonFilled(btnView,    BLUE);
        styleButtonFilled(btnCreate,  BLUE);
        styleButtonFilled(btnUpdate,  BLUE);
        styleButtonFilled(btnApprove, GREEN);
        styleButtonFilled(btnReject,  RED);

        btnView.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a trip first!"); return; }
            viewPanel.loadTrip(tripIds.get(row));
            cardLayout.show(container, "VIEW");
        });

        btnCreate.addActionListener(e -> {
            createPanel.resetFields();
            cardLayout.show(container, "CREATE");
        });

        btnApprove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a trip first!"); return; }
            if (!"Pending".equalsIgnoreCase((String) model.getValueAt(row, 11))) {
                JOptionPane.showMessageDialog(this, "Only Pending trips can be approved.");
                return;
            }
            Object assignVal = model.getValueAt(row, 3);
            if (assignVal == null || assignVal.toString().equals("—") || assignVal.toString().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "This trip has no assignment yet.\nPlease update it with an assignment first.",
                    "No Assignment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            updateTripStatus(tripIds.get(row), "Approved");
        });

        btnReject.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a trip first!"); return; }
            if (!"Pending".equalsIgnoreCase((String) model.getValueAt(row, 11))) {
                JOptionPane.showMessageDialog(this, "Only Pending trips can be rejected.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Reject this trip? It will be marked as Cancelled.",
                "Confirm Reject", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION)
                updateTripStatus(tripIds.get(row), "Cancelled");
        });

        btnUpdate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a trip first!"); return; }
            updatePanel.loadTrip(tripIds.get(row));
            cardLayout.show(container, "UPDATE");
        });

        bottom.add(btnView);
        bottom.add(btnCreate);
        bottom.add(btnUpdate);
        bottom.add(btnApprove);
        bottom.add(btnReject);

        main.add(top,    BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        return main;
    }

    private void updateTripStatus(int tripId, String newStatus) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Trip SET trip_status=? WHERE trip_id=?");
            ps.setString(1, newStatus);
            ps.setInt(2, tripId);
            ps.executeUpdate();
            loadTrips(cmbFilter.getSelectedItem().toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    void loadTrips(String filter) {
        try {
            model.setRowCount(0);
            tripIds.clear();
            String sql =
                "SELECT t.trip_id, " +
                "u.first_name+' '+u.last_name AS passenger, " +
                "ISNULL(au.first_name+' '+au.last_name, '—') AS admin_name, " +
                "ISNULL(du.first_name+' '+du.last_name+' - '+v.vehicle_model, '—') AS assignment, " +
                "t.start_date, t.start_time, t.end_date, t.end_time, " +
                "t.pick_up_location, t.destination, t.passenger_count, t.trip_status " +
                "FROM Trip t " +
                "JOIN  Passenger p               ON t.passenger_id  = p.passenger_id " +
                "JOIN  Users u                   ON p.passenger_id  = u.user_id " +
                "LEFT JOIN Admin a               ON t.admin_id      = a.admin_id " +
                "LEFT JOIN Users au              ON a.admin_id      = au.user_id " +
                "LEFT JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id " +
                "LEFT JOIN Driver d              ON va.driver_id    = d.driver_id " +
                "LEFT JOIN Users du              ON d.driver_id     = du.user_id " +
                "LEFT JOIN Vehicle v             ON va.vehicle_id   = v.vehicle_id";
            if (!filter.equals("All")) sql += " WHERE LOWER(t.trip_status)=LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!filter.equals("All")) ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tripIds.add(rs.getInt("trip_id"));
                model.addRow(new Object[]{
                    rs.getInt("trip_id"),
                    rs.getString("passenger"),
                    rs.getString("admin_name"),
                    rs.getString("assignment"),
                    rs.getObject("start_date"),
                    rs.getObject("start_time"),
                    rs.getObject("end_date"),
                    rs.getObject("end_time"),
                    rs.getString("pick_up_location"),
                    rs.getString("destination"),
                    rs.getInt("passenger_count"),
                    rs.getString("trip_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }


    private java.util.List<Integer> getAvailableAssignmentIds(
            java.sql.Date start, java.sql.Date end, int excludeTripId) {
        java.util.List<Integer> available = new java.util.ArrayList<>();
        try {
            PreparedStatement psAll = conn.prepareStatement(
                "SELECT assignment_id FROM Vehicle_Assignment WHERE assignment_status='Active'");
            ResultSet rsAll = psAll.executeQuery();
            java.util.List<Integer> all = new java.util.ArrayList<>();
            while (rsAll.next()) all.add(rsAll.getInt("assignment_id"));

            String blockSql =
                "SELECT DISTINCT assignment_id FROM Trip " +
                "WHERE trip_status IN ('Pending','Approved','Completed') " +
                "AND assignment_id IS NOT NULL " +
                "AND NOT (end_date < ? OR start_date > ?)";
            if (excludeTripId > 0) blockSql += " AND trip_id <> ?";
            PreparedStatement psBlk = conn.prepareStatement(blockSql);
            psBlk.setDate(1, start);
            psBlk.setDate(2, end);
            if (excludeTripId > 0) psBlk.setInt(3, excludeTripId);
            ResultSet rsBlk = psBlk.executeQuery();
            java.util.Set<Integer> blocked = new java.util.HashSet<>();
            while (rsBlk.next()) blocked.add(rsBlk.getInt("assignment_id"));

            for (int id : all)
                if (!blocked.contains(id)) available.add(id);
        } catch (Exception e) { e.printStackTrace(); }
        return available;
    }


    public class ViewTripPanel extends JPanel {
        private JLabel lblTripId      = valueLabel();
        private JLabel lblPassenger   = valueLabel();
        private JLabel lblAdmin       = valueLabel();
        private JLabel lblAssignment  = valueLabel();
        private JLabel lblStartDate   = valueLabel();
        private JLabel lblStartTime   = valueLabel();
        private JLabel lblEndDate     = valueLabel();
        private JLabel lblEndTime     = valueLabel();
        private JLabel lblPickup      = valueLabel();
        private JLabel lblDestination = valueLabel();
        private JLabel lblPax         = valueLabel();
        private JLabel lblStatus      = valueLabel();

        public ViewTripPanel() {
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

            JLabel title = new JLabel("Trip Details", SwingConstants.CENTER);
            title.setFont(TITLE_FONT); title.setForeground(BLUE);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 18, 10);
            card.add(title, gbc);
            gbc.gridwidth = 1; gbc.insets = new Insets(6, 10, 6, 10);

            int y = 1;
            addInfoRow(card, gbc, "Trip ID:",     lblTripId,      y++);
            addInfoRow(card, gbc, "Passenger:",   lblPassenger,   y++);
            addInfoRow(card, gbc, "Admin:",       lblAdmin,       y++);
            addInfoRow(card, gbc, "Assignment:",  lblAssignment,  y++);
            addInfoRow(card, gbc, "Start Date:",  lblStartDate,   y++);
            addInfoRow(card, gbc, "Start Time:",  lblStartTime,   y++);
            addInfoRow(card, gbc, "End Date:",    lblEndDate,     y++);
            addInfoRow(card, gbc, "End Time:",    lblEndTime,     y++);
            addInfoRow(card, gbc, "Pickup:",      lblPickup,      y++);
            addInfoRow(card, gbc, "Destination:", lblDestination, y++);
            addInfoRow(card, gbc, "Pax Count:",   lblPax,         y++);
            addInfoRow(card, gbc, "Status:",      lblStatus,      y++);

            JButton btnBack = new JButton("Back");
            styleButtonOutline(btnBack, BLUE);
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE); btnRow.add(btnBack);
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(18, 10, 0, 10);
            card.add(btnRow, gbc);
            add(card);

            btnBack.addActionListener(e -> { cardLayout.show(container, "LIST"); loadTrips("All"); });
        }

        public void loadTrip(int id) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.trip_id, " +
                    "u.first_name+' '+u.last_name AS passenger, " +
                    "ISNULL(au.first_name+' '+au.last_name, '—') AS admin_name, " +
                    "ISNULL(du.first_name+' '+du.last_name+' - '+v.vehicle_model, '—') AS assignment, " +
                    "t.start_date, t.start_time, t.end_date, t.end_time, " +
                    "t.pick_up_location, t.destination, t.passenger_count, t.trip_status " +
                    "FROM Trip t " +
                    "JOIN  Passenger p               ON t.passenger_id  = p.passenger_id " +
                    "JOIN  Users u                   ON p.passenger_id  = u.user_id " +
                    "LEFT JOIN Admin a               ON t.admin_id      = a.admin_id " +
                    "LEFT JOIN Users au              ON a.admin_id      = au.user_id " +
                    "LEFT JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id " +
                    "LEFT JOIN Driver d              ON va.driver_id    = d.driver_id " +
                    "LEFT JOIN Users du              ON d.driver_id     = du.user_id " +
                    "LEFT JOIN Vehicle v             ON va.vehicle_id   = v.vehicle_id " +
                    "WHERE t.trip_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    lblTripId.setText(String.valueOf(rs.getInt("trip_id")));
                    lblPassenger.setText(rs.getString("passenger"));
                    lblAdmin.setText(rs.getString("admin_name"));
                    lblAssignment.setText(rs.getString("assignment"));
                    lblStartDate.setText(rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "—");
                    lblStartTime.setText(rs.getTime("start_time") != null ? rs.getTime("start_time").toString() : "—");
                    lblEndDate.setText(rs.getDate("end_date")     != null ? rs.getDate("end_date").toString()   : "—");
                    lblEndTime.setText(rs.getTime("end_time")     != null ? rs.getTime("end_time").toString()   : "—");
                    lblPickup.setText(rs.getString("pick_up_location"));
                    lblDestination.setText(rs.getString("destination"));
                    lblPax.setText(String.valueOf(rs.getInt("passenger_count")));
                    String s = rs.getString("trip_status");
                    lblStatus.setText(s);
                    lblStatus.setForeground(switch (s.toLowerCase()) {
                        case "pending"   -> ORANGE;
                        case "approved"  -> BLUE;
                        case "completed" -> GREEN;
                        case "cancelled" -> RED;
                        default          -> Color.DARK_GRAY;
                    });
                    lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }


    public class CreateTripPanel extends JPanel {
        private JComboBox<String> cmbPassenger  = new JComboBox<>();
        private JComboBox<String> cmbAssignment = new JComboBox<>();
        private JComboBox<String> cmbStatus     = new JComboBox<>(
                new String[]{"Pending", "Approved", "Completed", "Cancelled"});
        private JTextField txtStartDate   = styledField();
        private JTextField txtStartTime   = styledField();
        private JTextField txtEndDate     = styledField();
        private JTextField txtEndTime     = styledField();
        private JTextField txtPickup      = styledField();
        private JTextField txtDestination = styledField();
        private JTextField txtPaxCount    = styledField();
        private JTextField txtAdmin       = styledField();
        private JLabel     lblStatus      = new JLabel(" ");

        private int[] passengerIds         = new int[0];
        private int[] assignmentIds        = new int[0];
        private int[] assignmentCapacities = new int[0];

        public CreateTripPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            buildUI();
        }

        public void setAdminId(int id) {}

        public void resetFields() {
            loadPassengers();
            cmbAssignment.removeAllItems();
            assignmentIds        = new int[0];
            assignmentCapacities = new int[0];
            txtStartDate.setText("");
            txtStartTime.setText("HH:MM");
            txtEndDate.setText("");
            txtEndTime.setText("HH:MM");
            txtPickup.setText("");
            txtDestination.setText("");
            txtPaxCount.setText("");
            cmbStatus.setSelectedIndex(0);
            lblStatus.setText(" ");
            txtAdmin.setText(loggedInAdminName);
        }

        private void loadPassengers() {
            try {
                cmbPassenger.removeAllItems();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.passenger_id, u.first_name+' '+u.last_name AS name " +
                    "FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id " +
                    "WHERE u.user_status='Active'");
                ResultSet rs = ps.executeQuery();
                java.util.List<Integer> ids = new java.util.ArrayList<>();
                while (rs.next()) {
                    cmbPassenger.addItem(rs.getString("name"));
                    ids.add(rs.getInt("passenger_id"));
                }
                passengerIds = ids.stream().mapToInt(i -> i).toArray();
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void buildUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Create Trip", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbPassenger.setPreferredSize(new Dimension(260, 32));
            cmbAssignment.setPreferredSize(new Dimension(260, 32));
            cmbStatus.setPreferredSize(new Dimension(260, 32));

            txtAdmin.setEditable(false);
            txtAdmin.setBackground(new Color(240, 240, 240));
            txtAdmin.setText(loggedInAdminName);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "Passenger:",   cmbPassenger,   y++);
            addFormRow(form, gbc, "Admin:",        txtAdmin,      y++);
            addFormRow(form, gbc, "Start Date:",  txtStartDate,   y++);
            addFormRow(form, gbc, "Start Time:",  txtStartTime,   y++);
            addFormRow(form, gbc, "End Date:",    txtEndDate,     y++);
            addFormRow(form, gbc, "End Time:",    txtEndTime,     y++);
            addFormRow(form, gbc, "Assignment:",  cmbAssignment,  y++);
            addFormRow(form, gbc, "Pickup:",      txtPickup,      y++);
            addFormRow(form, gbc, "Destination:", txtDestination, y++);
            addFormRow(form, gbc, "Pax Count:",   txtPaxCount,    y++);
            addFormRow(form, gbc, "Status:",      cmbStatus,      y++);

            JLabel hint = new JLabel("* Enter dates first, then click Check to see available assignments.");
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(Color.GRAY);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(form);
            card.add(Box.createVerticalStrut(6));
            card.add(hint);
            card.add(Box.createVerticalStrut(10));

            JButton btnCheck = new JButton("Check Available Assignments");
            styleButtonOutline(btnCheck, BLUE);
            btnCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnCheck.setMaximumSize(new Dimension(280, 34));
            card.add(btnCheck);
            card.add(Box.createVerticalStrut(14));

            JButton btnSave = new JButton("Save");
            JButton btnBack = new JButton("Back");
            styleButtonFilled(btnSave, BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnSave.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnSave); btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus.setFont(LABEL_FONT);
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);

            JScrollPane scroll = new JScrollPane(card,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(WHITE);
            add(scroll, new GridBagConstraints());

            btnCheck.addActionListener(e -> checkAvailability());
            btnSave.addActionListener(e  -> save());
            btnBack.addActionListener(e  -> { cardLayout.show(container, "LIST"); loadTrips("All"); });
        }

        private void checkAvailability() {
            java.sql.Date start, end;
            try { start = java.sql.Date.valueOf(txtStartDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid start date! Use yyyy-MM-dd"); return; }
            try { end = java.sql.Date.valueOf(txtEndDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid end date! Use yyyy-MM-dd"); return; }
            if (!start.toLocalDate().isAfter(LocalDate.now().minusDays(1))) {
                showError("Start date cannot be in the past!"); return;
            }
            if (end.before(start)) { showError("End date must be after start date!"); return; }

            java.util.List<Integer> avail = getAvailableAssignmentIds(start, end, -1);
            if (avail.isEmpty()) {
                showError("No available assignments for these dates!");
                cmbAssignment.removeAllItems();
                assignmentIds = new int[0]; assignmentCapacities = new int[0];
                return;
            }
            try {
                cmbAssignment.removeAllItems();
                java.util.List<Integer> aIds = new java.util.ArrayList<>();
                java.util.List<Integer> caps = new java.util.ArrayList<>();
                for (int aId : avail) {
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT va.assignment_id, " +
                        "du.first_name+' '+du.last_name+' - '+v.vehicle_model AS label, " +
                        "v.passenger_capacity " +
                        "FROM Vehicle_Assignment va " +
                        "JOIN Driver d  ON va.driver_id  = d.driver_id " +
                        "JOIN Users du  ON d.driver_id   = du.user_id " +
                        "JOIN Vehicle v ON va.vehicle_id = v.vehicle_id " +
                        "WHERE va.assignment_id=?");
                    ps.setInt(1, aId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        // Show capacity in the label so admin can see vehicle limit
                        cmbAssignment.addItem(rs.getString("label") +
                            " (Cap: " + rs.getInt("passenger_capacity") + ")");
                        aIds.add(aId); caps.add(rs.getInt("passenger_capacity"));
                    }
                }
                assignmentIds        = aIds.stream().mapToInt(i -> i).toArray();
                assignmentCapacities = caps.stream().mapToInt(i -> i).toArray();
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText(aIds.size() + " assignment(s) available for these dates.");
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        private java.sql.Time parseTime(String s) throws Exception {
            if (s.matches("\\d{2}:\\d{2}")) s += ":00";
            return java.sql.Time.valueOf(s);
        }

        private void save() {
            if (loggedInAdminId <= 0) { showError("No logged-in admin detected!"); return; }
            if (passengerIds.length == 0)  { showError("No passengers available!"); return; }
            if (assignmentIds.length == 0) { showError("Run 'Check Available Assignments' first!"); return; }
            if (txtPickup.getText().trim().isEmpty())      { showError("Pickup is required!");      return; }
            if (txtDestination.getText().trim().isEmpty()) { showError("Destination is required!"); return; }

            java.sql.Date startDate, endDate;
            try { startDate = java.sql.Date.valueOf(txtStartDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid start date! Use yyyy-MM-dd"); return; }
            try { endDate = java.sql.Date.valueOf(txtEndDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid end date! Use yyyy-MM-dd"); return; }
            if (!startDate.toLocalDate().isAfter(LocalDate.now().minusDays(1))) {
                showError("Start date cannot be in the past!"); return;
            }
            if (endDate.before(startDate)) { showError("End date must be after start date!"); return; }

            java.sql.Time startTime, endTime;
            try { startTime = parseTime(txtStartTime.getText().trim()); }
            catch (Exception ex) { showError("Invalid start time! Use HH:MM"); return; }
            try { endTime = parseTime(txtEndTime.getText().trim()); }
            catch (Exception ex) { showError("Invalid end time! Use HH:MM"); return; }

            int paxCount;
            try { paxCount = Integer.parseInt(txtPaxCount.getText().trim()); }
            catch (Exception ex) { showError("Pax count must be a number!"); return; }
            if (paxCount <= 0) { showError("Pax count must be > 0!"); return; }

            int selIdx      = cmbAssignment.getSelectedIndex();
            if (selIdx < 0) { showError("Please select an assignment!"); return; }
            int selAssignId = assignmentIds[selIdx];

            // VALIDATION: pax count must not exceed the selected vehicle's capacity
            int vehicleCapacity = assignmentCapacities[selIdx];
            if (paxCount > vehicleCapacity) {
                showError("Pax count (" + paxCount + ") exceeds vehicle capacity (" + vehicleCapacity + ")!");
                return;
            }

            if (!getAvailableAssignmentIds(startDate, endDate, -1).contains(selAssignId)) {
                showError("Assignment is no longer available — please re-check!"); return;
            }

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Trip (passenger_id,admin_id,assignment_id,start_date,start_time," +
                    "end_date,end_time,pick_up_location,destination,passenger_count,trip_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                ps.setInt(1,     passengerIds[cmbPassenger.getSelectedIndex()]);
                ps.setInt(2,     loggedInAdminId);
                ps.setInt(3,     selAssignId);
                ps.setDate(4,    startDate);
                ps.setTime(5,    startTime);
                ps.setDate(6,    endDate);
                ps.setTime(7,    endTime);
                ps.setString(8,  txtPickup.getText().trim());
                ps.setString(9,  txtDestination.getText().trim());
                ps.setInt(10,    paxCount);
                ps.setString(11, cmbStatus.getSelectedItem().toString());
                ps.executeUpdate();
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Trip created successfully!");
                loadTrips("All");
            } catch (Exception e) {
                showError("Error creating trip! " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void showError(String m) { lblStatus.setForeground(RED); lblStatus.setText(m); }
    }


    public class UpdateTripPanel extends JPanel {
        private int tripId = -1;
        private JComboBox<String> cmbPassenger  = new JComboBox<>();
        private JComboBox<String> cmbAssignment = new JComboBox<>();
        private JComboBox<String> cmbStatus     = new JComboBox<>(
                new String[]{"Pending", "Approved", "Completed", "Cancelled"});
        private JTextField txtStartDate   = styledField();
        private JTextField txtStartTime   = styledField();
        private JTextField txtEndDate     = styledField();
        private JTextField txtEndTime     = styledField();
        private JTextField txtPickup      = styledField();
        private JTextField txtDestination = styledField();
        private JTextField txtPaxCount    = styledField();
        private JTextField txtAdmin       = styledField();
        private JLabel     lblStatus      = new JLabel(" ");

        private int[] passengerIds         = new int[0];
        private int[] assignmentIds        = new int[0];
        private int[] assignmentCapacities = new int[0];
        private int   currentAssignmentId  = -1;

        public UpdateTripPanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            buildUI();
        }

        public void loadTrip(int id) {
            this.tripId = id;
            lblStatus.setText(" ");
            loadDropdowns();

            txtAdmin.setText(loggedInAdminName);

            try {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM Trip WHERE trip_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int pId = rs.getInt("passenger_id");
                    int aId = rs.getInt("assignment_id");
                    currentAssignmentId = rs.wasNull() ? -1 : aId;

                    for (int i = 0; i < passengerIds.length; i++)
                        if (passengerIds[i] == pId) { cmbPassenger.setSelectedIndex(i); break; }

                    if (currentAssignmentId != -1) {
                        for (int i = 0; i < assignmentIds.length; i++) {
                            if (assignmentIds[i] == currentAssignmentId) {
                                cmbAssignment.setSelectedIndex(i); break;
                            }
                        }
                    }

                    txtStartDate.setText(rs.getDate("start_date") != null
                            ? rs.getDate("start_date").toString() : "");
                    txtStartTime.setText(rs.getTime("start_time") != null
                            ? rs.getTime("start_time").toString() : "HH:MM");
                    txtEndDate.setText(rs.getDate("end_date") != null
                            ? rs.getDate("end_date").toString() : "");
                    txtEndTime.setText(rs.getTime("end_time") != null
                            ? rs.getTime("end_time").toString() : "HH:MM");
                    txtPickup.setText(rs.getString("pick_up_location"));
                    txtDestination.setText(rs.getString("destination"));
                    txtPaxCount.setText(String.valueOf(rs.getInt("passenger_count")));
                    cmbStatus.setSelectedItem(rs.getString("trip_status"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void loadDropdowns() {
            try {
                cmbPassenger.removeAllItems();
                PreparedStatement psP = conn.prepareStatement(
                    "SELECT p.passenger_id, u.first_name+' '+u.last_name AS name " +
                    "FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id " +
                    "WHERE u.user_status='Active'");
                ResultSet rsP = psP.executeQuery();
                java.util.List<Integer> pIds = new java.util.ArrayList<>();
                while (rsP.next()) {
                    cmbPassenger.addItem(rsP.getString("name"));
                    pIds.add(rsP.getInt("passenger_id"));
                }
                passengerIds = pIds.stream().mapToInt(i -> i).toArray();

                cmbAssignment.removeAllItems();
                PreparedStatement psA = conn.prepareStatement(
                    "SELECT va.assignment_id, " +
                    "du.first_name+' '+du.last_name+' - '+v.vehicle_model AS label, " +
                    "v.passenger_capacity " +
                    "FROM Vehicle_Assignment va " +
                    "JOIN Driver d  ON va.driver_id  = d.driver_id " +
                    "JOIN Users du  ON d.driver_id   = du.user_id " +
                    "JOIN Vehicle v ON va.vehicle_id = v.vehicle_id");
                ResultSet rsA = psA.executeQuery();
                java.util.List<Integer> aIds = new java.util.ArrayList<>();
                java.util.List<Integer> caps = new java.util.ArrayList<>();
                while (rsA.next()) {
                    // Show capacity in the label so admin can easily compare with pax count
                    cmbAssignment.addItem(rsA.getString("label") +
                        " (Cap: " + rsA.getInt("passenger_capacity") + ")");
                    aIds.add(rsA.getInt("assignment_id"));
                    caps.add(rsA.getInt("passenger_capacity"));
                }
                assignmentIds        = aIds.stream().mapToInt(i -> i).toArray();
                assignmentCapacities = caps.stream().mapToInt(i -> i).toArray();
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void buildUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Update Trip", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbPassenger.setPreferredSize(new Dimension(260, 32));
            cmbAssignment.setPreferredSize(new Dimension(260, 32));
            cmbStatus.setPreferredSize(new Dimension(260, 32));

            txtAdmin.setEditable(false);
            txtAdmin.setBackground(new Color(240, 240, 240));
            txtAdmin.setText(loggedInAdminName);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "Passenger:",   cmbPassenger,   y++);
            addFormRow(form, gbc, "Admin:",        txtAdmin,      y++);
            addFormRow(form, gbc, "Assignment:",  cmbAssignment,  y++);
            addFormRow(form, gbc, "Start Date:",  txtStartDate,   y++);
            addFormRow(form, gbc, "Start Time:",  txtStartTime,   y++);
            addFormRow(form, gbc, "End Date:",    txtEndDate,     y++);
            addFormRow(form, gbc, "End Time:",    txtEndTime,     y++);
            addFormRow(form, gbc, "Pickup:",      txtPickup,      y++);
            addFormRow(form, gbc, "Destination:", txtDestination, y++);
            addFormRow(form, gbc, "Pax Count:",   txtPaxCount,    y++);
            addFormRow(form, gbc, "Status:",      cmbStatus,      y++);
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
            btnRow.add(btnUpdate); btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus.setFont(LABEL_FONT);
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);

            JScrollPane scroll = new JScrollPane(card,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(WHITE);
            add(scroll, new GridBagConstraints());

            btnUpdate.addActionListener(e -> update());
            btnBack.addActionListener(e -> { cardLayout.show(container, "LIST"); loadTrips("All"); });
        }

        private java.sql.Time parseTime(String s) throws Exception {
            if (s.matches("\\d{2}:\\d{2}")) s += ":00";
            return java.sql.Time.valueOf(s);
        }

        private void update() {
            if (tripId == -1) return;
            if (loggedInAdminId <= 0) { showError("No logged-in admin detected!"); return; }
            if (txtPickup.getText().trim().isEmpty())      { showError("Pickup is required!");      return; }
            if (txtDestination.getText().trim().isEmpty()) { showError("Destination is required!"); return; }

            java.sql.Date startDate, endDate;
            try { startDate = java.sql.Date.valueOf(txtStartDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid start date!"); return; }
            try { endDate = java.sql.Date.valueOf(txtEndDate.getText().trim()); }
            catch (Exception ex) { showError("Invalid end date!"); return; }
            if (!startDate.toLocalDate().isAfter(LocalDate.now().minusDays(1))) {
                showError("Start date cannot be in the past!"); return;
            }
            if (endDate.before(startDate)) { showError("End date must be after start date!"); return; }

            java.sql.Time startTime, endTime;
            try { startTime = parseTime(txtStartTime.getText().trim()); }
            catch (Exception ex) { showError("Invalid start time! Use HH:MM"); return; }
            try { endTime = parseTime(txtEndTime.getText().trim()); }
            catch (Exception ex) { showError("Invalid end time! Use HH:MM"); return; }

            int paxCount;
            try { paxCount = Integer.parseInt(txtPaxCount.getText().trim()); }
            catch (Exception ex) { showError("Pax count must be a number!"); return; }
            if (paxCount <= 0) { showError("Pax count must be > 0!"); return; }

            int selIdx      = cmbAssignment.getSelectedIndex();
            int selAssignId = (selIdx >= 0 && assignmentIds.length > 0) ? assignmentIds[selIdx] : -1;

            if (selAssignId != -1) {
                // VALIDATION: pax count must not exceed the selected vehicle's capacity
                int vehicleCapacity = assignmentCapacities[selIdx];
                if (paxCount > vehicleCapacity) {
                    showError("Pax count (" + paxCount + ") exceeds vehicle capacity (" + vehicleCapacity + ")!");
                    return;
                }

                java.util.List<Integer> avail = getAvailableAssignmentIds(startDate, endDate, tripId);
                if (!avail.contains(selAssignId)) {
                    showError("Assignment has an overlapping Pending/Approved/Completed trip!"); return;
                }
            }

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Trip SET passenger_id=?,admin_id=?,assignment_id=?," +
                    "start_date=?,start_time=?,end_date=?,end_time=?," +
                    "pick_up_location=?,destination=?,passenger_count=?," +
                    "trip_status=? WHERE trip_id=?");
                ps.setInt(1,    passengerIds[cmbPassenger.getSelectedIndex()]);
                ps.setInt(2,    loggedInAdminId);
                if (selAssignId != -1) ps.setInt(3, selAssignId);
                else                   ps.setNull(3, java.sql.Types.INTEGER);
                ps.setDate(4,   startDate);
                ps.setTime(5,   startTime);
                ps.setDate(6,   endDate);
                ps.setTime(7,   endTime);
                ps.setString(8, txtPickup.getText().trim());
                ps.setString(9, txtDestination.getText().trim());
                ps.setInt(10,   paxCount);
                ps.setString(11, cmbStatus.getSelectedItem().toString());
                ps.setInt(12,   tripId);
                ps.executeUpdate();
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Trip updated successfully!");
                loadTrips("All");
            } catch (Exception e) {
                showError("Error updating trip! " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void showError(String m) { lblStatus.setForeground(RED); lblStatus.setText(m); }
    }


    private void addFormRow(JPanel panel, GridBagConstraints gbc, String label, Component field, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(label); lbl.setFont(LABEL_FONT); panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1; panel.add(field, gbc);
    }

    private void addInfoRow(JPanel panel, GridBagConstraints gbc, String label, JLabel value, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1; panel.add(value, gbc);
    }

    private JTextField styledField() {
        JTextField f = new JTextField(15);
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
        btn.setBackground(bg); btn.setForeground(WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setOpaque(true); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void styleButtonOutline(JButton btn, Color c) {
        btn.setBackground(WHITE); btn.setForeground(c);
        btn.setFocusPainted(false); btn.setBorder(BorderFactory.createLineBorder(c, 2));
        btn.setOpaque(true); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }
}