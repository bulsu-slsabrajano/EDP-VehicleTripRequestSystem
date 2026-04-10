package com.driver.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.project.dbConnection.DbConnectMsSql;

public class HomePanel extends JPanel {
	
	private JLabel pendingCountValue;
    private JLabel totalCountValue;
    private JLabel acceptedCountValue;

    private JLabel statusLbl = new JLabel("Available");
    private JButton toggleBtn = new JButton("Set Unavailable");
    boolean isAvailableStatus = true;
    
    private JTable recentTable = new JTable();
    private JTextArea currentStatusArea = new JTextArea();
    private JLabel welcomeLabel = new JLabel();
    
    private String username;

    public HomePanel() {
    	this(DriverData.username);
    }

    public HomePanel(String username) {
    	this.username = username;
        setLayout(new BorderLayout());
        add(buildHomePanel(DriverData.username), BorderLayout.CENTER);
    }
   
    private JPanel buildHomePanel(String username) {
    	
    	JPanel root = new JPanel(new BorderLayout());
    	
    	welcomeLabel = sectionTitle("Welcome, " + DriverData.username);
    	welcomeLabel.setFont(new Font("Arial",Font.BOLD,26));
    	welcomeLabel.setForeground(new Color(26,43,109));
    	
    	JButton refreshBtn = new JButton("Refresh");
    	refreshBtn.setFocusPainted(false);
        refreshBtn.setBackground(new Color(0, 150, 199));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setOpaque(true);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setPreferredSize(new Dimension(120, 36));
        refreshBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        
        refreshBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { refreshBtn.setBackground(new Color(0, 120, 165)); }
            public void mouseExited(MouseEvent e)  { refreshBtn.setBackground(Color.BLUE); }
        });
    	
        refreshBtn.addActionListener(e -> loadData());
        
        JPanel refreshPanel = new JPanel();
        refreshPanel.setBackground(Color.WHITE);
        refreshPanel.add(refreshBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.add(welcomeLabel, BorderLayout.WEST);
        topBar.add(refreshPanel, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel row1 = new JPanel(new GridLayout(1, 3, 10, 0));
        row1.setBackground(Color.WHITE);
        row1.add(statCard("Pending Trips", "pendingCountValue"));
        row1.add(statCard("Approved Trips", "acceptedCountValue"));
        row1.add(statCard("Total Trips", "totalCountValue"));

        JPanel row2 = new JPanel(new GridLayout(1,2,10,10));
        row2.add(buildStatusToggleCard());

        JPanel row3 = new JPanel(new GridLayout(1,1,10,10));
        row3.add(buildRecentTripsCard());

        body.add(row1);
        body.add(Box.createVerticalStrut(10));
        body.add(row2);
        body.add(Box.createVerticalStrut(10));
        body.add(row3);

        root.add(body, BorderLayout.CENTER);
        return root;
    }
   
    private JPanel statCard(String label, String tag){

        JPanel card = card();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        
        Color borderColor;
        switch (tag) {
            case "pendingCountValue"  -> borderColor = new Color(255, 140, 0); 
            case "acceptedCountValue" -> borderColor = new Color(0, 102, 204);  
            case "totalCountValue"    -> borderColor = new Color(0, 128, 0); 
            default                   -> borderColor = new Color(200, 210, 230);
        }
        
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

        JLabel value = new JLabel("0", JLabel.CENTER);
        value.setFont(new Font("Arial", Font.BOLD, 28));
        
        JLabel name = new JLabel(label, JLabel.CENTER);
        name.setFont(new Font("Arial", Font.PLAIN, 13));
        
        switch (tag) {
            case "pendingCountValue"  -> pendingCountValue  = value;
            case "acceptedCountValue" -> acceptedCountValue = value;
            case "totalCountValue"    -> totalCountValue    = value;
        }

        card.add(value, BorderLayout.CENTER);
        card.add(name, BorderLayout.NORTH);

        return card;
    }

    private void loadStats() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT
                    COUNT(*) AS total,
                    COALESCE(SUM(CASE WHEN t.trip_status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending,
                    COALESCE(SUM(CASE WHEN t.trip_status = 'APPROVED' THEN 1 ELSE 0 END), 0) AS accepted
                FROM Trip t
                JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
                WHERE va.driver_id = ?
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                pendingCountValue.setText(String.valueOf(rs.getInt("pending")));
                acceptedCountValue.setText(String.valueOf(rs.getInt("accepted")));
                totalCountValue.setText(String.valueOf(rs.getInt("total")));
            } else {
                pendingCountValue.setText("0");
                acceptedCountValue.setText("0");
                totalCountValue.setText("0");
            }

        } catch (Exception e) {
            e.printStackTrace();
            pendingCountValue.setText("0");
            acceptedCountValue.setText("0");
            totalCountValue.setText("0");
        }
    }

    private JPanel buildStatusToggleCard(){

        JPanel card = card();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        card.setLayout(new BorderLayout());

        card.add(cardTitle("Driver Status"), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setOpaque(true);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setPreferredSize(new Dimension(120, 35));
        
        toggleBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toggleBtn.setBackground(toggleBtn.getBackground().darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                applyStatusUI(isAvailableStatus);
            }
        });
        
        // CHANGE: Toggle button only allows manual change; loadDriverStatus() syncs with DB automatically
        toggleBtn.addActionListener(e -> {
            isAvailableStatus = !isAvailableStatus;
            String newStatus = isAvailableStatus ? "Available" : "Not Available";
            updateDriverStatus(newStatus);
            applyStatusUI(isAvailableStatus);
        });

        center.add(statusLbl);
        center.add(toggleBtn);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    // CHANGE: loadDriverStatus now reads from DB so it reflects auto-set status from booking
    private void loadDriverStatus() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = "SELECT driver_status FROM Driver WHERE driver_id = ?";
            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("driver_status");
                // CHANGE: treat both "Not Available" and "Unavailable" as not available
                isAvailableStatus = "Available".equalsIgnoreCase(status);
                applyStatusUI(isAvailableStatus);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDriverStatus(String status) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = "UPDATE Driver SET driver_status = ? WHERE driver_id = ?";
            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setString(1, status);
            pstmt.setInt(2, DriverData.driverId);

            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyStatusUI(boolean isAvailable) {
        if (isAvailable) {
            statusLbl.setText("Available");
            statusLbl.setForeground(new Color(34, 139, 34));
            toggleBtn.setText("Set Unavailable");
            toggleBtn.setBackground(new Color(220, 53, 69)); 
            toggleBtn.setForeground(Color.WHITE);
        } else {
            
            statusLbl.setText("Not Available");
            statusLbl.setForeground(Color.RED);
            toggleBtn.setText("Set Available");
            toggleBtn.setBackground(new Color(34, 139, 34));  
            toggleBtn.setForeground(Color.WHITE);
        }
    }

    private JPanel buildRecentTripsCard(){

        JPanel card = card();
        card.setLayout(new BorderLayout());

        card.add(cardTitle("Recent Trips"), BorderLayout.NORTH);

        String[] cols = {"Trip ID", "Destination", "Date", "Status"};
        recentTable.setModel(new DefaultTableModel(cols, 0));

        card.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        return card;
    }

    private void loadRecentTrips() {
        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT TOP 5 trip_id, destination, start_date, trip_status
                FROM Trip t
                JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
                WHERE va.driver_id = ?
                ORDER BY start_date DESC
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            String[] cols = {"Trip ID", "Destination", "Date", "Status"};
            
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("trip_id"),
                    rs.getString("destination"),
                    rs.getDate("start_date"),
                    rs.getString("trip_status")
                });
            }

            recentTable.setModel(model);
            styleTable(recentTable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void loadData() {
    	welcomeLabel.setText("Welcome, " + DriverData.username);
        loadStats();
        // CHANGE: loadDriverStatus is called on every refresh so the toggle always reflects the current DB status
        loadDriverStatus();
        loadRecentTrips();
        revalidate();
        repaint();
    }

    private void styleTable(JTable table){

        table.setFont(new Font("Arial",Font.PLAIN,13));
        table.setRowHeight(32);

        table.setGridColor(new Color(220,225,235));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);

        table.setBackground(Color.WHITE);

        table.setSelectionBackground(new Color(210,220,245));
        table.setSelectionForeground(new Color(26,43,109));

        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();

        header.setFont(new Font("Arial",Font.BOLD,13));
        header.setBackground(new Color(240,244,255));
        header.setForeground(new Color(26,43,109));
        header.setReorderingAllowed(false);
    }
    
    private JPanel card(){
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JLabel cardTitle(String text){
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(new Color(26, 43, 109));
        return lbl;
    }

    private JLabel sectionTitle(String text){
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 0));
        return lbl;
    }
}