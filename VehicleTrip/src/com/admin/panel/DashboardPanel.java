package com.admin.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.ShadowPanel;
import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class DashboardPanel extends JPanel {

    private JLabel lblUsers, lblDrivers, lblVehicles, lblTotalTrips;
    private JLabel lblPendingStatus, lblApprovedStatus, lblCompletedStatus, lblCancelledStatus;
    private JLabel lblWelcome;
    private DefaultTableModel model;
    private Connection conn;
    private int loggedInUserId = -1;

    
    private static final Color BLUE        = new Color(0, 150, 199);   
    private static final Color GREEN       = new Color(39, 174, 96);    
    private static final Color ORANGE      = new Color(230, 126, 34);   
    private static final Color RED         = new Color(220, 53, 69);    
    private static final Color WHITE       = Color.WHITE;
    private static final Font  LABEL_FONT  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 20);

    public DashboardPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setLayout(new BorderLayout());
        setBackground(WHITE);
        topPanel();
        centerPanel();
        loadDashboard();
    }

    
    public void setLoggedInUserId(int userId) {
        this.loggedInUserId = userId;
        loadWelcomeName();
    }

    private void loadWelcomeName() {
        if (loggedInUserId == -1 || lblWelcome == null) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT first_name FROM Users WHERE user_id=?");
            ps.setInt(1, loggedInUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) lblWelcome.setText("Welcome, " + rs.getString("first_name") + "!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void topPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 5, 30));

        lblWelcome = new JLabel("Welcome!");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(Color.BLACK);

        JPanel rightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrapper.setBackground(WHITE);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBackground(BLUE);
        btnRefresh.setForeground(WHITE);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setOpaque(true);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(120, 36));
        btnRefresh.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btnRefresh.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnRefresh.setBackground(new Color(0, 120, 165)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btnRefresh.setBackground(BLUE); }
        });
        btnRefresh.addActionListener(e -> loadDashboard());

        rightWrapper.add(btnRefresh);
        topPanel.add(lblWelcome,   BorderLayout.WEST);
        topPanel.add(rightWrapper, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
    }

    private void centerPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setBackground(WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(WHITE);

        lblUsers      = new JLabel("0");
        lblDrivers    = new JLabel("0");
        lblVehicles   = new JLabel("0");
        lblTotalTrips = new JLabel("0");

        summaryPanel.add(createSummaryCard("Total Users",        lblUsers,      BLUE));
        summaryPanel.add(createSummaryCard("Available Drivers",  lblDrivers,    BLUE));
        summaryPanel.add(createSummaryCard("Available Vehicles", lblVehicles,   BLUE));
        summaryPanel.add(createSummaryCard("Total Trips",        lblTotalTrips, BLUE));

        JPanel statusPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statusPanel.setBackground(WHITE);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        lblPendingStatus   = new JLabel("0", SwingConstants.CENTER);
        lblApprovedStatus  = new JLabel("0", SwingConstants.CENTER);
        lblCompletedStatus = new JLabel("0", SwingConstants.CENTER);
        lblCancelledStatus = new JLabel("0", SwingConstants.CENTER);

        statusPanel.add(createStatusCard("Pending",   lblPendingStatus,   ORANGE));
        statusPanel.add(createStatusCard("Approved",  lblApprovedStatus,  BLUE));
        statusPanel.add(createStatusCard("Completed", lblCompletedStatus, GREEN));
        statusPanel.add(createStatusCard("Cancelled", lblCancelledStatus, RED));

        JLabel lblTableTitle = new JLabel("Upcoming Trips");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTableTitle.setForeground(new Color(50, 50, 50));
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));

        String[] columns = {"Trip ID", "Passenger", "Driver", "Vehicle", "Status", "Start Date"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        
        TableStyleUtil.applyStyle(table);
        
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {

                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);

                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
                }

                if (col == 4 && val != null) { 
                    String status = val.toString();

                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));

                    if (status.equalsIgnoreCase("Pending")) {
                        c.setForeground(new Color(230, 126, 34)); // ORANGE
                    } else if (status.equalsIgnoreCase("Approved")) {
                        c.setForeground(new Color(0, 150, 199)); // BLUE
                    } else if (status.equalsIgnoreCase("Completed")) {
                        c.setForeground(new Color(39, 174, 96)); // GREEN
                    } else if (status.equalsIgnoreCase("Cancelled")) {
                        c.setForeground(new Color(220, 53, 69)); // RED
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }

                return c;
            }
        };

        //APPLY RENDERER
        table.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);
        
        JScrollPane scrollPane = TableStyleUtil.modernScroll(table);

        JPanel tableSection = new JPanel(new BorderLayout());
        tableSection.setBackground(WHITE);
        tableSection.add(lblTableTitle, BorderLayout.NORTH);
        tableSection.add(scrollPane,    BorderLayout.CENTER);

        JPanel topSection = new JPanel(new BorderLayout(0, 0));
        topSection.setBackground(WHITE);
        topSection.add(summaryPanel, BorderLayout.NORTH);
        topSection.add(statusPanel,  BorderLayout.SOUTH);

        centerPanel.add(topSection,   BorderLayout.NORTH);
        centerPanel.add(tableSection, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel, Color bgColor) {
        ShadowPanel panel = new ShadowPanel();
        panel.setPreferredSize(new Dimension(200, 100));
        panel.setBackground(bgColor);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        valueLabel.setForeground(WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(valueLabel,  BorderLayout.CENTER);
        panel.add(titleLabel,  BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatusCard(String title, JLabel valueLabel, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(color);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setForeground(color);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private void loadDashboard() {
        try {
            ResultSet rs1 = conn.prepareStatement("SELECT COUNT(*) FROM Users").executeQuery();
            if (rs1.next()) lblUsers.setText(String.valueOf(rs1.getInt(1)));

            ResultSet rs2 = conn.prepareStatement("SELECT COUNT(*) FROM Driver WHERE driver_status='Available'").executeQuery();
            if (rs2.next()) lblDrivers.setText(String.valueOf(rs2.getInt(1)));

            ResultSet rs3 = conn.prepareStatement("SELECT COUNT(*) FROM Vehicle WHERE vehicle_status='Available'").executeQuery();
            if (rs3.next()) lblVehicles.setText(String.valueOf(rs3.getInt(1)));

            ResultSet rs4 = conn.prepareStatement("SELECT COUNT(*) FROM Trip").executeQuery();
            if (rs4.next()) lblTotalTrips.setText(String.valueOf(rs4.getInt(1)));

            PreparedStatement psStatus = conn.prepareStatement(
                "SELECT trip_status, COUNT(*) AS total FROM Trip GROUP BY trip_status");
            ResultSet rsStatus = psStatus.executeQuery();
            int pending = 0, approved = 0, completed = 0, cancelled = 0;
            while (rsStatus.next()) {
                String s = rsStatus.getString("trip_status");
                int    c = rsStatus.getInt("total");
                if      (s.equalsIgnoreCase("Pending"))   pending   = c;
                else if (s.equalsIgnoreCase("Approved"))  approved  = c;
                else if (s.equalsIgnoreCase("Completed")) completed = c;
                else if (s.equalsIgnoreCase("Cancelled")) cancelled = c;
            }
            lblPendingStatus.setText(String.valueOf(pending));
            lblApprovedStatus.setText(String.valueOf(approved));
            lblCompletedStatus.setText(String.valueOf(completed));
            lblCancelledStatus.setText(String.valueOf(cancelled));

            model.setRowCount(0);
            
            PreparedStatement ps = conn.prepareStatement(
            		"SELECT * FROM vw_UpcomingTrips ORDER BY start_date ASC"
            );
            ResultSet rs5 = ps.executeQuery();
            while (rs5.next()) {
                model.addRow(new Object[]{
                    rs5.getInt("trip_id"),
                    rs5.getString("passenger"),
                    rs5.getString("driver_name"),
                    rs5.getString("vehicle_name"),
                    rs5.getString("trip_status"),
                    rs5.getDate("start_date")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}